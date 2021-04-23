package com.motorbesitzen.gamblebot.bot.event;

import com.motorbesitzen.gamblebot.bot.command.Command;
import com.motorbesitzen.gamblebot.data.dao.DiscordGuild;
import com.motorbesitzen.gamblebot.data.repo.DiscordGuildRepo;
import com.motorbesitzen.gamblebot.util.EnvironmentUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
public class GuildMessageListener extends ListenerAdapter {


	private final Map<String, Command> commandMap;
	private final DiscordGuildRepo guildRepo;

	@Autowired
	public GuildMessageListener(final Map<String, Command> commandMap, final DiscordGuildRepo guildRepo) {
		this.commandMap = commandMap;
		this.guildRepo = guildRepo;
	}

	@Override
	public void onGuildMessageReceived(final GuildMessageReceivedEvent event) {
		// handle bot/webhook messages
		final Message message = event.getMessage();
		if (isInvalidMessage(message)) {
			return;
		}

		// handle channel the bot can not talk in
		final TextChannel senderChannel = event.getChannel();
		if (!senderChannel.canTalk()) {
			return;
		}

		// check for command
		final String prefix = EnvironmentUtil.getEnvironmentVariableOrDefault("CMD_PREFIX", "");
		final String rawMessage = message.getContentRaw();
		if (rawMessage.startsWith(prefix)) {
			final Command command = identifyCommand(prefix, rawMessage);
			if (command != null) {
				executeCommand(event, command);
			}
		}
	}

	private boolean isInvalidMessage(final Message message) {
		final User author = message.getAuthor();
		return message.isWebhookMessage() || author.isBot();
	}

	private Command identifyCommand(final String cmdPrefix, final String rawMessage) {
		final String commandName = identifyCommandName(cmdPrefix, rawMessage);
		return commandMap.get(commandName);
	}

	private String identifyCommandName(final String cmdPrefix, final String messageContent) {
		final String[] tokens = messageContent.split(" ");
		final String fullCommand = tokens[0];
		final String commandName = fullCommand.replace(cmdPrefix, "");
		return commandName.toLowerCase();        // lower case is needed for the matching to work in any case! DO NOT remove it!
	}

	/**
	 * Executes a command and handles exception if the bot does not have the needed permissions to
	 * execute that command in the channel/guild.
	 *
	 * @param event   The GuildMessageReceivedEvent provided by JDA.
	 * @param command The command to execute.
	 */
	private void executeCommand(final GuildMessageReceivedEvent event, final Command command) {
		if (!isAuthorizedMember(command, event.getMember())) {
			return;
		}

		if(!isAuthorizedChannel(command, event)) {
			return;
		}

		try {
			command.execute(event);
		} catch (InsufficientPermissionException e) {
			String message = "Bot does not have the needed permission " + e.getPermission() + " for that command.";
			event.getChannel().sendMessage(message).queue();
		}
	}

	private boolean isAuthorizedMember(final Command command, final Member member) {
		if (member == null) {
			return false;
		}

		return !command.isAdminCommand() || member.hasPermission(Permission.ADMINISTRATOR);
	}

	private boolean isAuthorizedChannel(final Command command, final GuildMessageReceivedEvent event) {
		if(command.isGlobalCommand()) {
			return true;
		}

		final Guild guild = event.getGuild();
		final long guildId = guild.getIdLong();
		final Optional<DiscordGuild> dcGuildOpt = guildRepo.findById(guildId);
		final DiscordGuild dcGuild = dcGuildOpt.orElseGet(() -> DiscordGuild.withGuildId(guildId));
		if(dcGuild.getCoinChannelId() == 0) {
			return true;
		}

		final TextChannel channel = event.getChannel();
		return dcGuild.getCoinChannelId() == channel.getIdLong();
	}
}
