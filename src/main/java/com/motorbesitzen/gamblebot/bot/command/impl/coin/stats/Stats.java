package com.motorbesitzen.gamblebot.bot.command.impl.coin.stats;

import com.motorbesitzen.gamblebot.bot.command.CommandImpl;
import com.motorbesitzen.gamblebot.data.dao.DiscordMember;
import com.motorbesitzen.gamblebot.data.repo.DiscordMemberRepo;
import com.motorbesitzen.gamblebot.util.DiscordMessageUtil;
import com.motorbesitzen.gamblebot.util.LogUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Shows the stats of the user.
 */
@Service("stats")
class Stats extends CommandImpl {

	private final DiscordMemberRepo memberRepo;

	@Autowired
	private Stats(final DiscordMemberRepo memberRepo) {
		this.memberRepo = memberRepo;
	}

	@Override
	public String getName() {
		return "stats";
	}

	@Override
	public String getUsage() {
		return getName();
	}

	@Override
	public String getDescription() {
		return "Request your stats.";
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
		final Message message = event.getMessage();
		final long mentionedUserId = DiscordMessageUtil.getMentionedMemberId(message);
		final long requestedUserId = mentionedUserId <= 0 ?
				event.getAuthor().getIdLong() :
				DiscordMessageUtil.getMentionedMemberId(message);
		final long guildId = event.getGuild().getIdLong();
		final Optional<DiscordMember> dcMemberOpt = memberRepo.findByDiscordIdAndGuild_GuildId(requestedUserId, guildId);
		dcMemberOpt.ifPresentOrElse(
				dcMember -> event.getJDA().retrieveUserById(requestedUserId).queue(
						requestedUser -> {
							final MessageEmbed embed = buildStatsMessage(requestedUser.getAsTag(), dcMember);
							answer(event.getChannel(), embed);
						},
						throwable -> {
							LogUtil.logDebug("Could not find user with ID: " + requestedUserId);
							final MessageEmbed embed = buildStatsMessage("Unknown User", dcMember);
							answer(event.getChannel(), embed);
						}
				),
				() -> {
					final String errorMsg = mentionedUserId <= 0 ?
							"There are no stats for you yet!" :
							"There are no stats for that user!";
					sendErrorMessage(event.getChannel(), errorMsg);
				}
		);
	}

	private MessageEmbed buildStatsMessage(final String tag, final DiscordMember dcMember) {
		final EmbedBuilder eb = new EmbedBuilder();
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
