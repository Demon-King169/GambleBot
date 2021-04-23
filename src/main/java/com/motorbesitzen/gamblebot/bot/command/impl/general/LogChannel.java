package com.motorbesitzen.gamblebot.bot.command.impl.general;

import com.motorbesitzen.gamblebot.bot.command.CommandImpl;
import com.motorbesitzen.gamblebot.data.dao.DiscordGuild;
import com.motorbesitzen.gamblebot.data.repo.DiscordGuildRepo;
import com.motorbesitzen.gamblebot.util.DiscordMessageUtil;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service("log")
class LogChannel extends CommandImpl {

	private final DiscordGuildRepo guildRepo;

	@Autowired
	private LogChannel(final DiscordGuildRepo guildRepo) {
		this.guildRepo = guildRepo;
	}

	@Override
	public String getName() {
		return "log";
	}

	@Override
	public String getUsage() {
		return getName() + " (#channel|id)";
	}

	@Override
	public String getDescription() {
		return "Sets the log channel for all events.";
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
	public void execute(final GuildMessageReceivedEvent event) {
		final Guild guild = event.getGuild();
		final long guildId = guild.getIdLong();
		final Message message = event.getMessage();
		final List<TextChannel> mentionedChannels = message.getMentionedChannels();
		TextChannel mentionedChannel;
		if(mentionedChannels.size() != 0) {
			mentionedChannel = mentionedChannels.get(0);
		} else {
			final long mentionedId = DiscordMessageUtil.getMentionedRawId(message);
			mentionedChannel = guild.getTextChannelById(mentionedId);
		}

		if(mentionedChannel == null) {
			sendErrorMessage(event.getChannel(), "Invalid channel! Please select a valid channel.");
			return;
		}

		if(!mentionedChannel.canTalk()) {
			sendErrorMessage(event.getChannel(), "Invalid channel! Please select a channel where I can read and write in.");
			return;
		}

		setLogChannel(guildId, mentionedChannel.getIdLong());
		answer(event.getChannel(), "Set the log channel to " + mentionedChannel.getAsMention() + "!");
	}

	private void setLogChannel(final long guildId, final long channelId) {
		final Optional<DiscordGuild> dcGuildOpt = guildRepo.findById(guildId);
		final DiscordGuild dcGuild = dcGuildOpt.orElseGet(() -> DiscordGuild.withGuildId(guildId));
		dcGuild.setLogChannelId(channelId);
		guildRepo.save(dcGuild);
	}
}
