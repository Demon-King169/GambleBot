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
 * Give a user coins without taking them from your balance.
 */
@Service("give")
class GiveCoin extends CommandImpl {

	private static final String USER_OPTION_NAME = "member";
	private static final String AMOUNT_OPTION_NAME = "amount";

	private final DiscordMemberRepo memberRepo;
	private final DiscordGuildRepo guildRepo;

	@Autowired
	private GiveCoin(DiscordMemberRepo memberRepo, DiscordGuildRepo guildRepo) {
		this.memberRepo = memberRepo;
		this.guildRepo = guildRepo;
	}

	@Override
	public String getName() {
		return "give";
	}

	@Override
	public String getDescription() {
		return "Adds an amount of coins to a given user.";
	}

	@Override
	public boolean isAdminCommand() {
		return true;
	}

	@Override
	public boolean isGlobalCommand() {
		return true;
	}

	@Override
	public void register(JDA jda) {
		jda.upsertCommand(getName(), getDescription())
				.addOptions(
						new OptionData(
								OptionType.USER,
								USER_OPTION_NAME,
								"The member you want to give coins to.",
								true
						),
						new OptionData(
								OptionType.INTEGER,
								AMOUNT_OPTION_NAME,
								"The amount of coins you want to give the user.",
								true
						).setRequiredRange(0, 9007199254740991L)
				).queue();
	}

	@Override
	public void execute(SlashCommandEvent event) {
		User user = SlashOptionUtil.getUserOption(event, USER_OPTION_NAME);
		if (user == null) {
			reply(event, "Please provide a member you want to give coins to.");
			return;
		}

		Guild guild = event.getGuild();
		if (guild == null) {
			return;
		}

		event.getGuild().retrieveMember(user).queue(
				member -> addCoins(event, member),
				throwable -> reply(event, "That user is not in this guild!")
		);
	}

	private void addCoins(SlashCommandEvent event, Member member) {
		if (member.getUser().isBot()) {
			reply(event, "You can not give coins to a bot!");
			return;
		}

		long coinAmount = getCoinAmount(event);
		if (coinAmount < 1) {
			reply(event, "Please set a valid coin amount (> 0)!");
			return;
		}

		long guildId = member.getGuild().getIdLong();
		Optional<DiscordMember> dcMemberOpt = memberRepo.findByDiscordIdAndGuild_GuildId(member.getIdLong(), guildId);
		DiscordMember dcMember = dcMemberOpt.orElseGet(() -> createNewMember(member.getIdLong(), guildId));
		dcMember.giveCoins(coinAmount);
		memberRepo.save(dcMember);
		replyNoPings(event, "Added **" + coinAmount + "** coins to the balance of " + member.getAsMention() + ".");
		LogUtil.logInfo(event.getUser().getIdLong() + " gave " + coinAmount + " coins to " + dcMember.getDiscordId());
	}

	private long getCoinAmount(SlashCommandEvent event) {
		Long coinAmount = SlashOptionUtil.getIntegerOption(event, AMOUNT_OPTION_NAME);
		if (coinAmount == null) {
			coinAmount = -1L;
		}

		return coinAmount;
	}

	private DiscordMember createNewMember(long memberId, long guildId) {
		Optional<DiscordGuild> dcGuildOpt = guildRepo.findById(guildId);
		DiscordGuild dcGuild = dcGuildOpt.orElseGet(() -> createNewGuild(guildId));
		return DiscordMember.createDefault(memberId, dcGuild);
	}

	private DiscordGuild createNewGuild(long guildId) {
		DiscordGuild dcGuild = DiscordGuild.withGuildId(guildId);
		guildRepo.save(dcGuild);
		return dcGuild;
	}
}
