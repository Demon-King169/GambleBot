package com.motorbesitzen.gamblebot.util;

import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;

public class SlashOptionUtil {

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
