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

/**
 * Listens to slash command events.
 */
@Service
public class SlashCommandListener extends ListenerAdapter {


	private final Map<String, Command> commandMap;
	private final DiscordGuildRepo guildRepo;

	@Autowired
	public SlashCommandListener(Map<String, Command> commandMap, DiscordGuildRepo guildRepo) {
		this.commandMap = commandMap;
		this.guildRepo = guildRepo;
	}


	/**
	 * Gets executed each time a slash command event is received. That mean each time a user uses one of the
	 * bot's slash command we receive the information here. Then we need to check which command got used, so
	 * we can let the command class handle the execution of the command. Before that we check if the user is
	 * allowed to use the command in the channel and if we even know the command the user used.
	 *
	 * @param event The received information about the slash command event.
	 */
	@Override
	public void onSlashCommand(@NotNull SlashCommandEvent event) {
		Member member = event.getMember();
		if (isInvalidEvent(member)) {
			denyUsage(event, "You are not allowed to use this command!");
			return;
		}

		TextChannel senderChannel = event.getTextChannel();
		if (isInvalidChannel(senderChannel)) {
			denyUsage(event, "You can not use that command in this channel!");
			return;
		}

		String commandName = event.getName();
		Command command = identifyCommand(commandName);
		if (command == null) {
			denyUsage(event, "Unknown command!");
			return;
		}

		executeCommand(event, command);
	}

	/**
	 * Checks if the event is invalid by checking if it has an author. The author is the user that triggered the event.
	 * If there is no author there might be something wrong on Discord's end, or we might have retrieved
	 * a webhook or bot event (while they can send messages and trigger some other events they should not be able to
	 * send slash commands events).
	 *
	 * @param member The author of the event.
	 * @return {@code true} if the event is from a real user, {@code false} if not.
	 */
	private boolean isInvalidEvent(Member member) {
		if (member == null) {
			return true;
		}

		return member.getUser().isBot();
	}

	/**
	 * Informs the user that the command cannot be used by the user. Only the user can see this message (ephemeral).
	 *
	 * @param event   The event to reply to.
	 * @param message The message which gives the user some more information.
	 */
	private void denyUsage(SlashCommandEvent event, String message) {
		event.reply(message).setEphemeral(true).queue();
	}

	/**
	 * Checks if the channel the event got triggered in is invalid. A channel is invalid if there is no channel or
	 * if we can not "talk" (reply/send a message) in that channel.
	 *
	 * @param channel The channel we received the event in.
	 * @return {@code true} if the channel is {@code null} or if we can not send messages in the channel.
	 */
	private boolean isInvalidChannel(TextChannel channel) {
		if (channel == null) {
			return true;
		}

		return !channel.canTalk();
	}

	/**
	 * Identify a command of the bot by its name. Returns {@code null} if there is no command with that name.
	 *
	 * @param commandName The name of the command.
	 * @return The {@link Command} with the given name or {@code null} if there is none with that name.
	 */
	private Command identifyCommand(String commandName) {
		return commandMap.get(commandName);
	}

	/**
	 * Executes a command and handles exception if the bot does not have the needed permissions to
	 * execute that command in the channel/guild.
	 *
	 * @param event   The {@code SlashCommandEvent} provided by JDA.
	 * @param command The command to execute.
	 */
	private void executeCommand(SlashCommandEvent event, Command command) {
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

	/**
	 * Checks if a member is authorized to use a command.
	 *
	 * @param command The command the user wants to use.
	 * @param member  The user.
	 * @return {@code true} if the user has the needed permissions for the command, {@code false} if not.
	 */
	private boolean isAuthorizedMember(Command command, Member member) {
		if (member == null) {
			return false;
		}

		return !command.isAdminCommand() || member.hasPermission(Permission.ADMINISTRATOR);
	}

	/**
	 * Checks if a channel is authorized for command usage.
	 *
	 * @param command The command the user wants to use.
	 * @param event   The received information about the slash command event.
	 * @return {@code true} if the command can get used in the channel that got used by the user, {@code false} if not.
	 */
	private boolean isAuthorizedChannel(Command command, SlashCommandEvent event) {
		Guild guild = event.getGuild();
		if (guild == null) {
			return false;
		}

		if (event.getChannelType() != ChannelType.TEXT) {
			return false;
		}

		if (command.isGlobalCommand()) {
			return true;
		}

		long guildId = guild.getIdLong();
		Optional<DiscordGuild> dcGuildOpt = guildRepo.findById(guildId);
		DiscordGuild dcGuild = dcGuildOpt.orElseGet(() -> DiscordGuild.withGuildId(guildId));
		if (dcGuild.getCoinChannelId() == 0) {
			return true;
		}

		TextChannel channel = event.getTextChannel();
		return dcGuild.getCoinChannelId() == channel.getIdLong();
	}
}
