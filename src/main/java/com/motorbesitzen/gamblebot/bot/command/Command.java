package com.motorbesitzen.gamblebot.bot.command;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

/**
 * The interface for any Command the bot can handle.
 */
public interface Command {

	/**
	 * Get the name of the command. The name should be in lower case and should be equal to the service name.
	 *
	 * @return The name of the command.
	 */
	String getName();

	/**
	 * Describes what the command does and includes any information that may be needed.
	 *
	 * @return a short text that describes the command and its functionality.
	 */
	String getDescription();

	/**
	 * Defines if the command can only be used by an admin of the guild.
	 *
	 * @return {@code true} if only an admin can use the command.
	 */
	boolean isAdminCommand();

	/**
	 * Defines if the command can get used globally or only in a certain channel if the guild uses that option.
	 *
	 * @return {@code true} if the command can be used everywhere the bot has access to.
	 */
	boolean isGlobalCommand();

	/**
	 * Registers the command to Discord.
	 */
	void register(JDA jda);

	/**
	 * A method that performs the necessary actions for the given command.
	 *
	 * @param event The Discord event when a slash command (possible command) is received.
	 */
	void execute(SlashCommandEvent event);
}
