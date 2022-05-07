package com.motorbesitzen.gamblebot.bot.command;

import com.motorbesitzen.gamblebot.data.dao.DiscordGuild;
import com.motorbesitzen.gamblebot.data.dao.GambleSettings;
import com.motorbesitzen.gamblebot.data.repo.DiscordGuildRepo;
import com.motorbesitzen.gamblebot.util.LogUtil;
import com.motorbesitzen.gamblebot.util.ParseUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
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

	/**
	 * Reply to a {@code SlashCommandEvent} with a message.
	 *
	 * @param event   The event to reply to.
	 * @param message The message to send.
	 * @see #reply(SlashCommandEvent, String, boolean)
	 */
	protected void reply(SlashCommandEvent event, String message) {
		reply(event, message, false);
	}

	/**
	 * Reply to a {@code SlashCommandEvent} with a message. Can send the message as ephemeral message which
	 * only the author of the event can see. Only sends the message if its content is valid according to
	 * Discord standards.
	 *
	 * @param event     The event to reply to.
	 * @param message   The message to send.
	 * @param ephemeral If the message should only be visible to the user we reply to.
	 */
	protected void reply(SlashCommandEvent event, String message, boolean ephemeral) {
		if (!isValidContent(message)) {
			LogUtil.logError("Tried to send invalid content! Msg: \"" + message + "\"");
			return;
		}

		event.reply(message)
				.setEphemeral(ephemeral)
				.queue();
	}

	/**
	 * Reply to a {@code SlashCommandEvent} with an embedded message.
	 *
	 * @param event The event to reply to.
	 * @param embed The embedded message to send.
	 * @see #reply(SlashCommandEvent, MessageEmbed, boolean)
	 */
	protected void reply(SlashCommandEvent event, MessageEmbed embed) {
		reply(event, embed, false);
	}

	/**
	 * Reply to a {@code SlashCommandEvent} with an embedded message. Can send the embedded message as ephemeral
	 * message which only the author of the event can see. Only sends the message if its content is valid according to
	 * Discord standards.
	 *
	 * @param event     The event to reply to.
	 * @param embed     The embedded message to send.
	 * @param ephemeral If the message should only be visible to the user we reply to.
	 */
	protected void reply(SlashCommandEvent event, MessageEmbed embed, boolean ephemeral) {
		if (!isValidContent(embed)) {
			LogUtil.logError("Tried to send invalid embed! Embed: \"" + embed.toData() + "\"");
			return;
		}

		event.replyEmbeds(embed)
				.setEphemeral(ephemeral)
				.queue();
	}

	/**
	 * Reply to a {@code SlashCommandEvent} with multiple embeds in a message. Can send the embedded message as
	 * ephemeral message which only the author of the event can see. Only sends the message if its content
	 * is valid according to Discord standards.
	 *
	 * @param event     The event to reply to.
	 * @param embeds    The embeds to send in a message.
	 * @param ephemeral The embeds to send in a message.
	 */
	protected void replyMultipleEmbeds(SlashCommandEvent event, List<MessageEmbed> embeds, boolean ephemeral) {
		for (MessageEmbed embed : embeds) {
			if (!isValidContent(embed)) {
				LogUtil.logError("Tried to send invalid embed! Embed: \"" + embed.toData() + "\"");
				return;
			}
		}

		event.replyEmbeds(embeds)
				.setEphemeral(ephemeral)
				.queue();
	}

	/**
	 * Reply to a {@code SlashCommandEvent} with a message. Any ping/mention that may be included in the message gets
	 * disabled. That means a user that gets mentioned in the message does not get an alert. Only sends the message if
	 * its content is valid according to Discord standards.
	 *
	 * @param event   The event to reply to.
	 * @param message The message to send.
	 */
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
	 * Sends a message to a channel. Only sends the message if its content is valid according to Discord standards.
	 *
	 * @param channel The channel to send the message in.
	 * @param message The message content to send as answer.
	 */
	protected void sendMessage(TextChannel channel, String message) {
		if (!isValidMessage(channel, message)) {
			LogUtil.logError("Tried to send invalid content! Msg: \"" + message + "\"");
			return;
		}

		channel.sendMessage(message).queue();
	}

	/**
	 * Builds an embedded message with information about a custom gamble.
	 *
	 * @param dcGuild The guild the info got requested in.
	 * @return An {@code MessageEmbed} with information about the guild's custom gamble.
	 */
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

	/**
	 * Checks if a message and the channel to send it in are valid (exists, and we can send messages in it).
	 *
	 * @param channel The channel to send the message in.
	 * @param content The message to send.
	 * @return {@code true} if we can send the message in the given channel.
	 */
	private boolean isValidMessage(TextChannel channel, String content) {
		if (channel == null) {
			return false;
		}

		if (!channel.canTalk()) {
			return false;
		}

		return isValidContent(content);
	}

	/**
	 * Checks if a message's content is valid. To be valid a message must be not blank.
	 *
	 * @param content The message to send.
	 * @return {@code true} if the message is valid.
	 */
	private boolean isValidContent(String content) {
		if (content == null) {
			return false;
		}

		if (content.isBlank()) {
			return false;
		}

		return content.length() <= Message.MAX_CONTENT_LENGTH;
	}

	/**
	 * Checks if a {@code MessageEmbed} is valid. To check the validity we can use JDA.
	 *
	 * @param content The message to send.
	 * @return {@code true} if the {@code MessageEmbed} is sendable.
	 */
	private boolean isValidContent(MessageEmbed content) {
		if (content == null) {
			return false;
		}

		return content.isSendable();
	}

	/**
	 * Applies tax to an amount of coins if the guild has a tax rate set.
	 *
	 * @param guild The guild that applies its tax on the coins.
	 * @param value The amount of coins to apply the tax on.
	 * @return The amount of coins after tax.
	 */
	protected long calcTaxedValue(DiscordGuild guild, long value) {
		double taxedValue = (double) value - (value * guild.getTaxRate());
		return Math.max(0, Math.round(taxedValue));
	}

	/**
	 * Creates a default guild object with the guild ID and saves it in the database.
	 *
	 * @param guildRepo The guild repository.
	 * @param guildId   The guild ID to use for the guild.
	 * @return The {@link DiscordGuild} that got created and saved in the database.
	 */
	protected DiscordGuild createGuild(DiscordGuildRepo guildRepo, long guildId) {
		DiscordGuild dcGuild = DiscordGuild.withGuildId(guildId);
		guildRepo.save(dcGuild);
		return dcGuild;
	}
}
