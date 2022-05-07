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
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Take user coins and delete them.
 */
@Service("take")
class TakeCoin extends CommandImpl {

	private static final String USER_OPTION_NAME = "member";
	private static final String AMOUNT_OPTION_NAME = "amount";

	private final DiscordMemberRepo memberRepo;
	private final DiscordGuildRepo guildRepo;

	@Autowired
	private TakeCoin(DiscordMemberRepo memberRepo, DiscordGuildRepo guildRepo) {
		this.memberRepo = memberRepo;
		this.guildRepo = guildRepo;
	}

	@Override
	public String getName() {
		return "take";
	}

	@Override
	public String getDescription() {
		return "Takes an amount of coins from a given user.";
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
								"The member you want to take coins from.",
								true
						),
						new OptionData(
								OptionType.INTEGER,
								AMOUNT_OPTION_NAME,
								"The amount of coins you want to take from the user.",
								true
						).setRequiredRange(0, 9007199254740991L)
				).queue();
	}

	@Override
	public void execute(SlashCommandEvent event) {
		User user = SlashOptionUtil.getUserOption(event, USER_OPTION_NAME);
		if (user == null) {
			reply(event, "Please provide a member you want to take coins from.");
			return;
		}

		takeCoins(event, user.getIdLong());
	}

	private void takeCoins(SlashCommandEvent event, long userId) {
		Guild guild = event.getGuild();
		if (guild == null) {
			return;
		}

		long coinAmount = getCoinAmount(event);
		if (coinAmount <= 0) {
			reply(event, "Please set a valid amount of coins (> 0).");
			return;
		}

		long guildId = guild.getIdLong();
		Optional<DiscordMember> dcMemberOpt = memberRepo.findByDiscordIdAndGuild_GuildId(userId, guildId);
		DiscordMember dcMember = dcMemberOpt.orElseGet(() -> createNewMember(userId, guildId));
		if (coinAmount > dcMember.getCoins()) {
			reply(event, "That user does not have that many coins. <@" +
					dcMember.getDiscordId() + "> balance: " + dcMember.getCoins());
			return;
		}

		dcMember.removeCoins(coinAmount);
		memberRepo.save(dcMember);
		reply(event, "Took **" + coinAmount + "** coins from their balance.");
		LogUtil.logInfo(event.getUser().getIdLong() + " took " + coinAmount + " coins from " + dcMember.getDiscordId());
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
