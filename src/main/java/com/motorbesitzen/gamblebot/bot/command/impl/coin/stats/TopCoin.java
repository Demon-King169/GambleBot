package com.motorbesitzen.gamblebot.bot.command.impl.coin.stats;

import com.motorbesitzen.gamblebot.bot.command.CommandImpl;
import com.motorbesitzen.gamblebot.data.dao.DiscordMember;
import com.motorbesitzen.gamblebot.data.repo.DiscordMemberRepo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Shows a top 10 user list ordered by coins.
 */
@Service("top")
class TopCoin extends CommandImpl {

	private static final int TOP_LIST_LENGTH = 10;
	private final DiscordMemberRepo memberRepo;

	@Autowired
	private TopCoin(DiscordMemberRepo memberRepo) {
		this.memberRepo = memberRepo;
	}

	@Override
	public String getName() {
		return "top";
	}

	@Override
	public String getDescription() {
		return "Shows the top 10 of users (sorted by coins).";
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
		jda.upsertCommand(getName(), getDescription()).queue();
	}

	@Override
	public void execute(SlashCommandEvent event) {
		Guild guild = event.getGuild();
		if (guild == null) {
			return;
		}

		long guildId = guild.getIdLong();
		PageRequest pageRequest = PageRequest.of(0, TOP_LIST_LENGTH);
		List<DiscordMember> topMembers = memberRepo.findAllByGuild_GuildIdOrderByCoinsDesc(guildId, pageRequest);
		if (topMembers.size() == 0) {
			reply(event, "There is not enough data for a top list yet!");
			return;
		}

		MessageEmbed embed = buildTopMessage(guild, topMembers);
		reply(event, embed);
	}

	private MessageEmbed buildTopMessage(Guild guild, List<DiscordMember> topMembers) {
		EmbedBuilder eb = new EmbedBuilder();
		eb.setTitle("Top " + topMembers.size() + " of " + guild.getName() + ":");
		addMemberFields(eb, topMembers);

		return eb.build();
	}

	private void addMemberFields(EmbedBuilder eb, List<DiscordMember> dcMembers) {
		int pos = 1;
		for (DiscordMember dcMember : dcMembers) {
			eb.addField("",
					"**" + pos++ + "**. " + "<@" + dcMember.getDiscordId() + ">\n" +
							"Coins: " + dcMember.getCoins() + "\n" +
							"Coins won: " + dcMember.getCoinsWon() + "\n" +
							"Coins lost: " + dcMember.getCoinsLost() + "\n" +
							"Games: " + dcMember.getGamesPlayed() + "\n" +
							"Wins: " + dcMember.getGamesWon() + "\n" +
							"Losses: " + dcMember.getGamesLost(),
					true
			);
		}
	}
}
