package com.motorbesitzen.gamblebot.bot.command.impl.coin.stats;

import com.motorbesitzen.gamblebot.bot.command.CommandImpl;
import com.motorbesitzen.gamblebot.data.dao.DiscordMember;
import com.motorbesitzen.gamblebot.data.repo.DiscordMemberRepo;
import com.motorbesitzen.gamblebot.util.DiscordMessageUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
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
				dcMember -> {
					final MessageEmbed embed = buildStatsMessage(event.getAuthor(), dcMember);
					answer(event.getChannel(), embed);
				},
				() -> {
					final String errorMsg = mentionedUserId <= 0 ?
							"There are no stats for you yet!" :
							"There are no stats for that user!";
					sendErrorMessage(event.getChannel(), errorMsg);
				}
		);
	}

	private MessageEmbed buildStatsMessage(final User author, final DiscordMember dcMember) {
		final EmbedBuilder eb = new EmbedBuilder();
		eb.setTitle("Stats of " + author.getAsTag() + ":")
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
