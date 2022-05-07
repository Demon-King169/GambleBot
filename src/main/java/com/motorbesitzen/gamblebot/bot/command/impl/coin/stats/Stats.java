package com.motorbesitzen.gamblebot.bot.command.impl.coin.stats;

import com.motorbesitzen.gamblebot.bot.command.CommandImpl;
import com.motorbesitzen.gamblebot.data.dao.DiscordMember;
import com.motorbesitzen.gamblebot.data.repo.DiscordMemberRepo;
import com.motorbesitzen.gamblebot.util.LogUtil;
import com.motorbesitzen.gamblebot.util.SlashOptionUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Shows the stats of the user.
 */
@Service("stats")
class Stats extends CommandImpl {

	private static final String USER_OPTION_NAME = "member";

	private final DiscordMemberRepo memberRepo;

	@Autowired
	private Stats(DiscordMemberRepo memberRepo) {
		this.memberRepo = memberRepo;
	}

	@Override
	public String getName() {
		return "stats";
	}

	@Override
	public String getDescription() {
		return "Request stats of members.";
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
				.addOption(
						OptionType.USER,
						USER_OPTION_NAME,
						"The member you want to see the stats of. Do not provide a member to see your own stats."
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
			displayOwnStats(event, guild);
			return;
		}

		displayUserStats(event, guild, user);
	}

	private void displayOwnStats(SlashCommandEvent event, Guild guild) {
		User author = event.getUser();
		long guildId = guild.getIdLong();
		long authorId = author.getIdLong();
		Optional<DiscordMember> dcMemberOpt = memberRepo.findByDiscordIdAndGuild_GuildId(authorId, guildId);
		displayStats(event, dcMemberOpt);
	}

	private void displayUserStats(SlashCommandEvent event, Guild guild, User user) {
		long userId = user.getIdLong();
		long guildId = guild.getIdLong();
		Optional<DiscordMember> dcMemberOpt = memberRepo.findByDiscordIdAndGuild_GuildId(userId, guildId);
		displayStats(event, dcMemberOpt);
	}

	private void displayStats(SlashCommandEvent event, Optional<DiscordMember> dcMemberOpt) {
		dcMemberOpt.ifPresentOrElse(
				dcMember -> event.getJDA().retrieveUserById(dcMember.getDiscordId()).queue(
						requestedUser -> {
							MessageEmbed embed = buildStatsMessage(requestedUser.getAsTag(), dcMember);
							reply(event, embed);
						},
						throwable -> {
							LogUtil.logDebug("Could not find user with ID: " + dcMember.getDiscordId());
							MessageEmbed embed = buildStatsMessage("Unknown User", dcMember);
							reply(event, embed);
						}
				),
				() -> reply(event, "No stats available!")
		);
	}

	private MessageEmbed buildStatsMessage(String tag, DiscordMember dcMember) {
		EmbedBuilder eb = new EmbedBuilder();
		eb.setTitle("Stats of " + tag + ":")
				.addField("Coins:", String.valueOf(dcMember.getCoins()), true)
				.addField("Coins won: ", String.valueOf(dcMember.getCoinsWon()), true)
				.addField("Coins lost: ", String.valueOf(dcMember.getCoinsLost()), true)
				.addField("Coins received: ", String.valueOf(dcMember.getCoinsReceived()), true)
				.addField("Coins spent: ", String.valueOf(dcMember.getCoinsSpend()), true)
				.addBlankField(true)
				.addField("Games: ", String.valueOf(dcMember.getGamesPlayed()), true)
				.addField("Wins: ", String.valueOf(dcMember.getGamesWon()), true)
				.addField("Losses: ", String.valueOf(dcMember.getGamesLost()), true);
		return eb.build();
	}
}
