package com.motorbesitzen.gamblebot.bot.command.impl.coin.action;

import com.motorbesitzen.gamblebot.bot.command.CommandImpl;
import com.motorbesitzen.gamblebot.data.dao.DiscordGuild;
import com.motorbesitzen.gamblebot.data.dao.DiscordMember;
import com.motorbesitzen.gamblebot.data.repo.DiscordGuildRepo;
import com.motorbesitzen.gamblebot.data.repo.DiscordMemberRepo;
import com.motorbesitzen.gamblebot.util.LogUtil;
import com.motorbesitzen.gamblebot.util.SlashOptionUtil;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Pay an amount of coins to a user.
 */
@Service("pay")
class PayCoins extends CommandImpl {

	private static final String USER_OPTION_NAME = "member";
	private static final String AMOUNT_OPTION_NAME = "amount";

	private final DiscordMemberRepo memberRepo;
	private final DiscordGuildRepo guildRepo;

	@Autowired
	private PayCoins(final DiscordMemberRepo memberRepo, final DiscordGuildRepo guildRepo) {
		this.memberRepo = memberRepo;
		this.guildRepo = guildRepo;
	}

	@Override
	public String getName() {
		return "pay";
	}

	@Override
	public String getDescription() {
		return "Transfers the specified amount of coins to the mentioned user.";
	}

	@Override
	public boolean isAdminCommand() {
		return false;
	}

	@Override
	public boolean isGlobalCommand() {
		return false;
	}

	@Override
	public void register(JDA jda) {
		jda.upsertCommand(getName(), getDescription())
				.addOptions(
						new OptionData(
								OptionType.USER,
								USER_OPTION_NAME,
								"The member you want to pay coins to.",
								true
						),
						new OptionData(
								OptionType.INTEGER,
								AMOUNT_OPTION_NAME,
								"The amount of coins you want to pay the user.",
								true
						).setRequiredRange(0, 9007199254740991L)
				).queue();
	}

	@Override
	public void execute(SlashCommandEvent event) {
		Guild guild = event.getGuild();
		if (guild == null) {
			return;
		}

		User user = SlashOptionUtil.getUserOption(event, USER_OPTION_NAME);
		if (user == null) {
			reply(event, "Please provide a member you want to pay coins to.");
			return;
		}

		final long authorId = event.getUser().getIdLong();
		if (user.getIdLong() == authorId) {
			reply(event, "You can not pay coins to yourself!");
			return;
		}

		final long guildId = guild.getIdLong();
		final Optional<DiscordMember> dcAuthorOpt = memberRepo.findByDiscordIdAndGuild_GuildId(authorId, guildId);
		if (dcAuthorOpt.isEmpty()) {
			reply(event, "You do not have any coins!");
			return;
		}

		guild.retrieveMember(user).queue(
				member -> payCoins(event, dcAuthorOpt.get(), member),
				throwable -> reply(event, "That user is not in this guild!")
		);
	}

	private void payCoins(final SlashCommandEvent event, final DiscordMember author, final Member member) {
		if (member.getUser().isBot()) {
			reply(event, "You can not pay coins to a bot!");
			return;
		}

		final long guildId = member.getGuild().getIdLong();
		final Optional<DiscordGuild> dcGuildOpt = guildRepo.findById(guildId);
		final DiscordGuild dcGuild = dcGuildOpt.orElseGet(() -> createNewGuild(guildId));
		final Optional<DiscordMember> dcMemberOpt = memberRepo.findByDiscordIdAndGuild_GuildId(member.getIdLong(), guildId);
		final DiscordMember dcMember = dcMemberOpt.orElseGet(() -> createNewMember(dcGuild, member.getIdLong()));
		final long coinAmount = getCoinAmount(event);
		if (coinAmount <= 0) {
			reply(event, "Please choose a valid coin amount of at least 1!");
			return;
		}

		final long taxValue = calcTax(dcGuild, coinAmount);
		final long taxedCoins = coinAmount + taxValue;
		final long authorCoins = author.getCoins();
		if (taxedCoins > authorCoins) {
			final String errorMsg = authorCoins > 0 ?
					(dcGuild.getTaxRate() > 0 ?
							"Please set a valid taxed coin amount (1 - " + (authorCoins - calcTax(dcGuild, authorCoins)) + ")!" :
							"Please set a valid coin amount (1 - " + authorCoins + ")!"
					) :
					(dcGuild.getTaxRate() > 0 ?
							"You do not have enough coins for that after tax!\nYou only have **" + authorCoins + "** coins right now. You need " + taxedCoins + " coins." :
							"You do not have enough coins for that!\nYou only have **" + authorCoins + "** coins right now."
					);
			reply(event, errorMsg);
			return;
		}

		author.spendCoins(taxedCoins);
		memberRepo.save(author);
		dcMember.receiveCoins(coinAmount);
		memberRepo.save(dcMember);
		replyNoPings(event, "Added **" + coinAmount + "** coins to the balance of " + member.getAsMention() + ". " +
				(dcGuild.getTaxRate() > 0 ?
						"Payment tax cost you an additional " + taxValue + " coins." :
						""
				));
		LogUtil.logInfo(author.getDiscordId() + " paid " + coinAmount + " coins to " + dcMember.getDiscordId());
	}

	private long getCoinAmount(SlashCommandEvent event) {
		Long coinAmount = SlashOptionUtil.getIntegerOption(event, AMOUNT_OPTION_NAME);
		if (coinAmount == null) {
			coinAmount = -1L;
		}

		return coinAmount;
	}

	private DiscordMember createNewMember(final DiscordGuild dcGuild, final long memberId) {
		return DiscordMember.createDefault(memberId, dcGuild);
	}

	private DiscordGuild createNewGuild(final long guildId) {
		final DiscordGuild dcGuild = DiscordGuild.withGuildId(guildId);
		guildRepo.save(dcGuild);
		return dcGuild;
	}

	private long calcTax(final DiscordGuild guild, final long value) {
		final double tax = (double) value * guild.getTaxRate();
		return Math.max(0, Math.round(tax));
	}
}
