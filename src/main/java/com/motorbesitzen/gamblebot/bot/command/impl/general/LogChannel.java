package com.motorbesitzen.gamblebot.bot.command.impl.general;

import com.motorbesitzen.gamblebot.bot.command.CommandImpl;
import com.motorbesitzen.gamblebot.data.dao.DiscordGuild;
import com.motorbesitzen.gamblebot.data.repo.DiscordGuildRepo;
import com.motorbesitzen.gamblebot.util.SlashOptionUtil;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service("log")
class LogChannel extends CommandImpl {

	private static final String CHANNEL_OPTION_NAME = "channel";

	private final DiscordGuildRepo guildRepo;

	@Autowired
	private LogChannel(DiscordGuildRepo guildRepo) {
		this.guildRepo = guildRepo;
	}

	@Override
	public String getName() {
		return "log";
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
	public void register(JDA jda) {
		jda.upsertCommand(getName(), getDescription())
				.addOptions(
						new OptionData(
								OptionType.CHANNEL,
								CHANNEL_OPTION_NAME,
								"The channel you want to receive log messages in.",
								true
						).setChannelTypes(ChannelType.TEXT)
				).queue();
	}

	@Override
	public void execute(SlashCommandEvent event) {
		Guild guild = event.getGuild();
		if (guild == null) {
			return;
		}

		long guildId = guild.getIdLong();
		GuildChannel mentionedChannel = SlashOptionUtil.getGuildChannelOption(event, CHANNEL_OPTION_NAME);
		if (mentionedChannel == null) {
			reply(event, "Please provide a valid channel!");
			return;
		}

		setLogChannel(guildId, mentionedChannel.getIdLong());
		reply(event, "Set the log channel to " + mentionedChannel.getAsMention() + "!");
	}

	private void setLogChannel(long guildId, long channelId) {
		Optional<DiscordGuild> dcGuildOpt = guildRepo.findById(guildId);
		DiscordGuild dcGuild = dcGuildOpt.orElseGet(() -> DiscordGuild.withGuildId(guildId));
		dcGuild.setLogChannelId(channelId);
		guildRepo.save(dcGuild);
	}
}
