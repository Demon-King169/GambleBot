package com.motorbesitzen.gamblebot.bot.command;

import com.motorbesitzen.gamblebot.data.dao.DiscordGuild;
import com.motorbesitzen.gamblebot.data.dao.GambleSettings;
import com.motorbesitzen.gamblebot.data.repo.DiscordGuildRepo;
import com.motorbesitzen.gamblebot.util.LogUtil;
import com.motorbesitzen.gamblebot.util.ParseUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * Basic implementation of a Command. Has all needed methods to send messages, answer to commands and log (debug) actions.
 * All subclasses (Commands) can use these functions.
 */
@Service
public abstract class CommandImpl implements Command {

	/**
	 * {@inheritDoc}
	 * Default command implementation without command functionality. Declared as 'unknown command'.
	 */
	@Override
	public abstract String getName();

	/**
	 * {@inheritDoc}
	 * Default command implementation without command functionality. Declared as 'unknown command'.
	 */
	@Override
	public abstract String getDescription();

	/**
	 * {@inheritDoc}
	 * Default command implementation without command functionality. Declared as 'unknown command'.
	 */
	@Override
	public abstract boolean isAdminCommand();

	/**
	 * {@inheritDoc}
	 * Default command implementation without command functionality. Declared as 'unknown command'.
	 *
	 * @return
	 */
	@Override
	public abstract boolean isGlobalCommand();

	/**
	 * {@inheritDoc}
	 * Default command implementation without command functionality. Declared as 'unknown command'.
	 */
	@Override
	public abstract void register(JDA jda);

	/**
	 * {@inheritDoc}
	 * Default command implementation without command functionality. Declared as 'unknown command'.
	 */
	@Override
	public abstract void execute(SlashCommandEvent event);

	protected void reply(SlashCommandEvent event, String message) {
		reply(event, message, false);
	}

	protected void reply(SlashCommandEvent event, String message, boolean ephemeral) {
		if (!isValidContent(message)) {
			LogUtil.logError("Tried to send invalid content! Msg: \"" + message + "\"");
			return;
		}

		event.reply(message)
				.setEphemeral(ephemeral)
				.queue();
	}

	protected void reply(SlashCommandEvent event, MessageEmbed embed) {
		reply(event, embed, false);
	}

	protected void reply(SlashCommandEvent event, MessageEmbed embed, boolean ephemeral) {
		if (!isValidContent(embed)) {
			LogUtil.logError("Tried to send invalid embed! Embed: \"" + embed.toData() + "\"");
			return;
		}

		event.replyEmbeds(embed)
				.setEphemeral(ephemeral)
				.queue();
	}

	protected void replyMultipleEmbeds(SlashCommandEvent event, List<MessageEmbed> embeds) {
		for (MessageEmbed embed : embeds) {
			if (!isValidContent(embed)) {
				LogUtil.logError("Tried to send invalid embed! Embed: \"" + embed.toData() + "\"");
				return;
			}
		}

		event.replyEmbeds(embeds).queue();
	}

	protected void replyNoPings(SlashCommandEvent event, String message) {
		if (!isValidContent(message)) {
			LogUtil.logError("Tried to send invalid content! Msg: \"" + message + "\"");
			return;
		}

		event.reply(message)
				.allowedMentions(Collections.emptyList())
				.queue();
	}

	/**
	 * Sends a message to a channel. Does not do anything if bot can not write in that channel.
	 *
	 * @param channel <a href="https://ci.dv8tion.net/job/JDA/javadoc/net/dv8tion/jda/api/entities/TextChannel.html">TextChannel</a>
	 *                to send the message in.
	 * @param message The message content to send as answer.
	 */
	protected void sendMessage(TextChannel channel, String message) {
		if (!isValidMessage(channel, message)) {
			LogUtil.logError("Tried to send invalid content! Msg: \"" + message + "\"");
			return;
		}

		channel.sendMessage(message).queue();
	}

	protected MessageEmbed buildGambleInfoEmbed(DiscordGuild dcGuild) {
		GambleSettings settings = dcGuild.getGambleSettings();
		EmbedBuilder eb = new EmbedBuilder();
		String prizeText = settings.getPrizeText();
		eb.setTitle("Gamble information:")
				.addField("Duration:", dcGuild.getTimeToEndText(), false)
				.addField("Cooldown between participation:", ParseUtil.parseMillisecondsToText(settings.getCooldownMs()), false)
				.addField("Prizes:", prizeText.substring(0, Math.min(1999, prizeText.length())), false)
				.setFooter("Use /gamble to participate!");
		return eb.build();
	}

	/**
	 * Used to clarify in the code that a log message is sent, doesn't do anything else than a normal answer message.
	 *
	 * @param channel    The <a href="https://ci.dv8tion.net/job/JDA/javadoc/net/dv8tion/jda/api/entities/TextChannel.html">TextChannel</a>
	 *                   where the original message is located in.
	 * @param logMessage The log message to send.
	 */
	protected void sendLogMessage(TextChannel channel, String logMessage) {
		if (isValidMessage(channel, logMessage)) {
			sendMessage(channel, logMessage);
		}
	}

	private boolean isValidMessage(TextChannel channel, String content) {
		if (channel == null) {
			return false;
		}

		if (!channel.canTalk()) {
			return false;
		}

		return isValidContent(content);
	}

	private boolean isValidContent(String content) {
		if (content == null) {
			return false;
		}

		return !content.isBlank();
	}

	private boolean isValidContent(MessageEmbed content) {
		if (content == null) {
			return false;
		}

		return content.isSendable();
	}

	protected long calcTaxedValue(DiscordGuild guild, long value) {
		double taxedValue = (double) value - (value * guild.getTaxRate());
		return Math.max(0, Math.round(taxedValue));
	}

	protected DiscordGuild createGuild(DiscordGuildRepo guildRepo, long guildId) {
		DiscordGuild dcGuild = DiscordGuild.withGuildId(guildId);
		guildRepo.save(dcGuild);
		return dcGuild;
	}
}
