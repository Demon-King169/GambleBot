package com.motorbesitzen.gamblebot.bot.command.impl.coin;

import com.motorbesitzen.gamblebot.bot.command.CommandImpl;
import com.motorbesitzen.gamblebot.data.dao.DiscordMember;
import com.motorbesitzen.gamblebot.data.repo.DiscordMemberRepo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
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
		final Member author = event.getMember();
		if(author == null) {
			return;
		}

		final Guild guild = event.getGuild();
		final long guildId = guild.getIdLong();
		final Optional<DiscordMember> dcMemberOpt = memberRepo.findByDiscordIdAndGuild_GuildId(author.getIdLong(), guildId);
		dcMemberOpt.ifPresentOrElse(
				dcMember -> {
					final MessageEmbed embed = buildStatsMessage(author, dcMember);
					answer(event.getChannel(), embed);
				},
				() -> sendErrorMessage(event.getChannel(), "There are no stats for you yet!")
		);
	}

	private MessageEmbed buildStatsMessage(final Member author, final DiscordMember dcMember) {
		final EmbedBuilder eb = new EmbedBuilder();
		eb.setTitle("Stats of " + author.getUser().getAsTag() + ":");
		eb.addField("Coins:", String.valueOf(dcMember.getCoins()), true);
		eb.addField("Coins won: ", String.valueOf(dcMember.getCoinsWon()), true);
		eb.addField("Coins lost: ", String.valueOf(dcMember.getCoinsLost()), true);
		eb.addField("Coins received: ", String.valueOf(dcMember.getCoinsReceived()), true);
		eb.addField("Coins spent: ", String.valueOf(dcMember.getCoinsSpend()), true);
		eb.addBlankField(true);
		eb.addField("Games: ", String.valueOf(dcMember.getGamesPlayed()), true);
		eb.addField("Wins: ", String.valueOf(dcMember.getGamesWon()), true);
		eb.addField("Losses: ", String.valueOf(dcMember.getGamesLost()), true);
		return eb.build();
	}
}
