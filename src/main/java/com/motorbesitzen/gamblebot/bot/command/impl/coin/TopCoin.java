package com.motorbesitzen.gamblebot.bot.command.impl.coin;

import com.motorbesitzen.gamblebot.bot.command.CommandImpl;
import com.motorbesitzen.gamblebot.data.dao.DiscordMember;
import com.motorbesitzen.gamblebot.data.repo.DiscordMemberRepo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("top")
public class TopCoin extends CommandImpl {

	private static final int TOP_LIST_LENGTH = 10;
	private final DiscordMemberRepo memberRepo;

	@Autowired
	private TopCoin(final DiscordMemberRepo memberRepo) {
		this.memberRepo = memberRepo;
	}

	@Override
	public String getName() {
		return "top";
	}

	@Override
	public String getUsage() {
		return getName();
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
	public void execute(final GuildMessageReceivedEvent event) {
		final Guild guild = event.getGuild();
		final long guildId = guild.getIdLong();
		final PageRequest pageRequest = PageRequest.of(0, TOP_LIST_LENGTH);
		final List<DiscordMember> topMembers = memberRepo.findAllByGuild_GuildIdOrderByCoinsDesc(guildId, pageRequest);
		if (topMembers.size() == 0) {
			sendErrorMessage(event.getChannel(), "There is not enough data for a top list yet!");
			return;
		}

		final MessageEmbed embed = buildTopMessage(guild, topMembers);
		answer(event.getChannel(), embed);
	}

	private MessageEmbed buildTopMessage(final Guild guild, final List<DiscordMember> topMembers) {
		final EmbedBuilder eb = new EmbedBuilder();
		eb.setTitle("Top " + topMembers.size() + " of " + guild.getName() + ":");
		addMemberFields(eb, topMembers);

		return eb.build();
	}

	private void addMemberFields(final EmbedBuilder eb, final List<DiscordMember> dcMembers) {
		int pos = 1;
		for (DiscordMember dcMember : dcMembers) {
			eb.addField("",
					"**" + pos++ + "**. " + "<@" + dcMember.getDiscordId() + "> - Coins: " + dcMember.getCoins(),
					false
			);
		}
	}
}
