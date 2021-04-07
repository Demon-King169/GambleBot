package com.motorbesitzen.gamblebot.bot.command.impl;

import com.motorbesitzen.gamblebot.bot.command.CommandImpl;
import com.motorbesitzen.gamblebot.data.dao.DiscordGuild;
import com.motorbesitzen.gamblebot.data.repo.DiscordGuildRepo;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service("gamblelog")
class GambleLog extends CommandImpl {

	private final DiscordGuildRepo guildRepo;

	@Autowired
	GambleLog(final DiscordGuildRepo guildRepo) {
		this.guildRepo = guildRepo;
	}

	@Override
	public String getName() {
		return "gamblelog";
	}

	@Override
	public String getUsage() {
		return getName() + " #channel";
	}

	@Override
	public String getDescription() {
		return "Sets the log channel for the gamble events.";
	}

	@Override
	public boolean isAdminCommand() {
		return true;
	}

	@Override
	public void execute(final GuildMessageReceivedEvent event) {
		final Message message = event.getMessage();
		final List<TextChannel> mentionedChannels = message.getMentionedChannels();
		if (mentionedChannels.size() == 0) {
			sendErrorMessage(event.getChannel(), "Please mention a channel to log gamble events in!");
			return;
		}

		final TextChannel logChannel = mentionedChannels.get(0);
		if (!logChannel.canTalk()) {
			sendErrorMessage(event.getChannel(), "I can not write messages in that channel. Please make sure I have read and write permission in the mentioned channel!");
			return;
		}

		final long guildId = event.getGuild().getIdLong();
		final long logChannelId = logChannel.getIdLong();
		final Optional<DiscordGuild> dcGuildOpt = guildRepo.findById(guildId);
		final DiscordGuild dcGuild = dcGuildOpt.orElseGet(() -> DiscordGuild.createDefault(guildId, logChannelId));
		dcGuild.setLogChannelId(logChannelId);
		guildRepo.save(dcGuild);

		answer(event.getChannel(), "Saved " + logChannel.getAsMention() + " as logging channel.");
	}
}
