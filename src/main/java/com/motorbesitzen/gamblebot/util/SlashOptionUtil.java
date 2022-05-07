package com.motorbesitzen.gamblebot.util;

import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;

/**
 * Utility class that handles the process of retrieving the option values from the SlashCommands a
 * Discord user can use.
 */
public class SlashOptionUtil {

	/**
	 * Retrieves a Discord user from a command option.
	 *
	 * @param event      The received information about the slash command.
	 * @param optionName The name of the option that holds the user.
	 * @return The mentioned user or null if there is no user given or if the given value is not a
	 * representation of a Discord user.
	 */
	public static User getUserOption(SlashCommandEvent event, String optionName) {
		OptionMapping memberOption = event.getOption(optionName);
		if (memberOption == null) {
			LogUtil.logDebug("Provided member option is null!");
			return null;
		}

		if (memberOption.getType() != OptionType.USER) {
			LogUtil.logDebug("Provided member option is not a user!");
			return null;
		}

		return memberOption.getAsUser();
	}

	/**
	 * Retrieves a guild channel from a command option.
	 *
	 * @param event      The received information about the slash command.
	 * @param optionName The name of the option that holds the guild channel.
	 * @return The mentioned channel or null if there is no channel given or if the given value is not a
	 * text channel.
	 */
	public static GuildChannel getGuildChannelOption(SlashCommandEvent event, String optionName) {
		OptionMapping channelOption = event.getOption(optionName);
		if (channelOption == null) {
			LogUtil.logDebug("Provided channel option is null!");
			return null;
		}

		if (channelOption.getType() != OptionType.CHANNEL) {
			LogUtil.logDebug("Provided channel option is no channel!");
			return null;
		}

		if (channelOption.getChannelType() != ChannelType.TEXT) {
			LogUtil.logDebug("Provided channel option is no TEXT channel!");
			return null;
		}

		return channelOption.getAsGuildChannel();
	}

	/**
	 * Retrieves an integer from a command option.
	 *
	 * @param event      The received information about the slash command.
	 * @param optionName The name of the option that holds the integer.
	 * @return The integer value as a {@code Long} as Discord uses JavaScript's {@code MAX_SAFE_INTEGER} which is a
	 * {@code Long} in Java. Returns null if there is no integer value given or if the given value is not a
	 * representation of an integer.
	 */
	public static Long getIntegerOption(SlashCommandEvent event, String optionName) {
		OptionMapping intOption = event.getOption(optionName);
		if (intOption == null) {
			LogUtil.logDebug("Provided integer option is null!");
			return null;
		}

		if (intOption.getType() != OptionType.INTEGER) {
			LogUtil.logDebug("Provided integer option is no integer!");
			return null;
		}

		return intOption.getAsLong();
	}

	/**
	 * Retrieves a {@code String} from a command option.
	 *
	 * @param event      The received information about the slash command.
	 * @param optionName The name of the option that holds the {@code String}.
	 * @return The {@code String} or null if there is no {@code String} given.
	 */
	public static String getStringOption(SlashCommandEvent event, String optionName) {
		OptionMapping stringOption = event.getOption(optionName);
		if (stringOption == null) {
			LogUtil.logDebug("Provided string option is null!");
			return null;
		}

		if (stringOption.getType() != OptionType.STRING) {
			LogUtil.logDebug("Provided string option is no string!");
			return null;
		}

		return stringOption.getAsString();
	}
}
