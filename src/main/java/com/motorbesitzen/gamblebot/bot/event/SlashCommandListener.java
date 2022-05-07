package com.motorbesitzen.gamblebot.bot.event;

import com.motorbesitzen.gamblebot.bot.command.Command;
import com.motorbesitzen.gamblebot.data.dao.DiscordGuild;
import com.motorbesitzen.gamblebot.data.repo.DiscordGuildRepo;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
public class SlashCommandListener extends ListenerAdapter {


	private final Map<String, Command> commandMap;
	private final DiscordGuildRepo guildRepo;

	@Autowired
	public SlashCommandListener(final Map<String, Command> commandMap, final DiscordGuildRepo guildRepo) {
		this.commandMap = commandMap;
		this.guildRepo = guildRepo;
	}


	@Override
	public void onSlashCommand(@NotNull SlashCommandEvent event) {
		Member member = event.getMember();
		if (isInvalidMessage(member)) {
			denyUsage(event, "You are not allowed to use this command!");
			return;
		}

		final TextChannel senderChannel = event.getTextChannel();
		if (isInvalidChannel(senderChannel)) {
			denyUsage(event, "You can not use that command in this channel!");
			return;
		}

		String commandName = event.getName();
		final Command command = identifyCommand(commandName);
		if (command == null) {
			denyUsage(event, "Unknown command!");
			return;
		}

		executeCommand(event, command);
	}

	private boolean isInvalidMessage(final Member member) {
		if (member == null) {
			return true;
		}

		return member.getUser().isBot();
	}

	private void denyUsage(SlashCommandEvent event, String message) {
		event.reply(message).setEphemeral(true).queue();
	}

	private boolean isInvalidChannel(final TextChannel channel) {
		if (channel == null) {
			return true;
		}

		return !channel.canTalk();
	}

	private Command identifyCommand(final String commandName) {
		return commandMap.get(commandName);
	}

	/**
	 * Executes a command and handles exception if the bot does not have the needed permissions to
	 * execute that command in the channel/guild.
	 *
	 * @param event   The GuildMessageReceivedEvent provided by JDA.
	 * @param command The command to execute.
	 */
	private void executeCommand(final SlashCommandEvent event, final Command command) {
		if (!isAuthorizedMember(command, event.getMember())) {
			denyUsage(event, "You are not allowed to use this command!");
			return;
		}

		if (!isAuthorizedChannel(command, event)) {
			denyUsage(event, "You can not use that command in this channel!");
			return;
		}

		try {
			command.execute(event);
		} catch (InsufficientPermissionException e) {
			String message = "Bot does not have the needed permission " + e.getPermission() + " for that command.";
			event.reply(message).queue();
		}
	}

	private boolean isAuthorizedMember(final Command command, final Member member) {
		if (member == null) {
			return false;
		}

		return !command.isAdminCommand() || member.hasPermission(Permission.ADMINISTRATOR);
	}

	private boolean isAuthorizedChannel(final Command command, final SlashCommandEvent event) {
		final Guild guild = event.getGuild();
		if (guild == null) {
			return false;
		}

		if (event.getChannelType() != ChannelType.TEXT) {
			return false;
		}

		if (command.isGlobalCommand()) {
			return true;
		}

		final long guildId = guild.getIdLong();
		final Optional<DiscordGuild> dcGuildOpt = guildRepo.findById(guildId);
		final DiscordGuild dcGuild = dcGuildOpt.orElseGet(() -> DiscordGuild.withGuildId(guildId));
		if (dcGuild.getCoinChannelId() == 0) {
			return true;
		}

		final TextChannel channel = event.getTextChannel();
		return dcGuild.getCoinChannelId() == channel.getIdLong();
	}
}
