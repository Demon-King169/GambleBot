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
	public SlashCommandListener(Map<String, Command> commandMap, DiscordGuildRepo guildRepo) {
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

	private boolean isInvalidMessage(Member member) {
		if (member == null) {
			return true;
		}

		return member.getUser().isBot();
	}

	private void denyUsage(SlashCommandEvent event, String message) {
		event.reply(message).setEphemeral(true).queue();
	}

	private boolean isInvalidChannel(TextChannel channel) {
		if (channel == null) {
			return true;
		}

		return !channel.canTalk();
	}

	private Command identifyCommand(String commandName) {
		return commandMap.get(commandName);
	}

	/**
	 * Executes a command and handles exception if the bot does not have the needed permissions to
	 * execute that command in the channel/guild.
	 *
	 * @param event   The GuildMessageReceivedEvent provided by JDA.
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

	private boolean isAuthorizedMember(Command command, Member member) {
		if (member == null) {
			return false;
		}

		return !command.isAdminCommand() || member.hasPermission(Permission.ADMINISTRATOR);
	}

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
