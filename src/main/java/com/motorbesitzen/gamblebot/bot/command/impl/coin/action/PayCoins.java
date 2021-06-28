package com.motorbesitzen.gamblebot.bot.command.impl.coin.action;

import com.motorbesitzen.gamblebot.bot.command.CommandImpl;
import com.motorbesitzen.gamblebot.data.dao.DiscordGuild;
import com.motorbesitzen.gamblebot.data.dao.DiscordMember;
import com.motorbesitzen.gamblebot.data.repo.DiscordGuildRepo;
import com.motorbesitzen.gamblebot.data.repo.DiscordMemberRepo;
import com.motorbesitzen.gamblebot.util.DiscordMessageUtil;
import com.motorbesitzen.gamblebot.util.LogUtil;
import com.motorbesitzen.gamblebot.util.ParseUtil;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Pay an amount of coins to a user.
 */
@Service("pay")
class PayCoins extends CommandImpl {

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
	public String getUsage() {
		return getName() + " (@user|id) <amount>";
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
	public void execute(final GuildMessageReceivedEvent event) {
		final long authorId = event.getAuthor().getIdLong();
		final long guildId = event.getGuild().getIdLong();
		final Optional<DiscordMember> dcAuthorOpt = memberRepo.findByDiscordIdAndGuild_GuildId(authorId, guildId);
		if (dcAuthorOpt.isEmpty()) {
			replyErrorMessage(event.getMessage(), "You do not have any coins!");
			return;
		}

		final Message message = event.getMessage();
		final long userId = DiscordMessageUtil.getMentionedMemberId(message);
		if (userId == authorId) {
			replyErrorMessage(event.getMessage(), "You can not pay coins to yourself!");
			return;
		}

		if (userId <= 100000000000000L) {
			replyErrorMessage(event.getMessage(), "That user seems to be invalid!");
			return;
		}

		event.getGuild().retrieveMemberById(userId).queue(
				member -> payCoins(event, dcAuthorOpt.get(), member),
				throwable -> sendErrorMessage(event.getChannel(), "That user is not in this guild!")
		);
	}

	private void payCoins(final GuildMessageReceivedEvent event, final DiscordMember author, final Member member) {
		if(member.getUser().isBot()) {
			replyErrorMessage(event.getMessage(), "You can not pay coins to a bot!");
			return;
		}

		final long guildId = event.getGuild().getIdLong();
		final Optional<DiscordMember> dcMemberOpt = memberRepo.findByDiscordIdAndGuild_GuildId(member.getIdLong(), guildId);
		final DiscordMember dcMember = dcMemberOpt.orElseGet(() -> createNewMember(member.getIdLong(), guildId));
		final String content = event.getMessage().getContentRaw();
		final String[] tokens = content.split(" ");
		final String coinText = tokens[tokens.length - 1];
		final long coinAmount = ParseUtil.safelyParseStringToLong(coinText);
		if (coinAmount < 1) {
			replyErrorMessage(event.getMessage(), "Please choose a valid coin amount of at least 1!");
			return;
		}

		final long taxValue = calcTax(coinAmount);
		final long taxedCoins = coinAmount + taxValue;
		final long authorCoins = author.getCoins();
		if (taxedCoins > authorCoins) {
			final String errorMsg = authorCoins > 0 ?
					"Please set a valid coin amount (1 - " + dcMember.getCoins() + ")!" :
					"You do not have enough coins for that after tax!\nYou only have **" + authorCoins + "** coins right now.";
			replyErrorMessage(event.getMessage(), errorMsg);
			return;
		}

		author.spendCoins(taxedCoins);
		memberRepo.save(author);
		dcMember.receiveCoins(coinAmount);
		memberRepo.save(dcMember);
		answer(event.getChannel(), "Added **" + coinAmount + "** coins to the balance of " + member.getAsMention() + ". " +
				"Tax took " + taxValue + " coins.");
		LogUtil.logDebug(author.getDiscordId() + " paid " + coinAmount + " coins to " + dcMember.getDiscordId());
	}

	private DiscordMember createNewMember(final long memberId, final long guildId) {
		final Optional<DiscordGuild> dcGuildOpt = guildRepo.findById(guildId);
		final DiscordGuild dcGuild = dcGuildOpt.orElseGet(() -> createNewGuild(guildId));
		return DiscordMember.createDefault(memberId, dcGuild);
	}

	private DiscordGuild createNewGuild(final long guildId) {
		final DiscordGuild dcGuild = DiscordGuild.withGuildId(guildId);
		guildRepo.save(dcGuild);
		return dcGuild;
	}

	private long calcTax(final long value) {
		final double payout = (double) value * (1.0 - AFTER_TAX_RATE);
		return Math.max(1, Math.round(payout));
	}
}
