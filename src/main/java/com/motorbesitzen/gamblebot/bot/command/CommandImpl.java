package com.motorbesitzen.gamblebot.bot.command;

import com.motorbesitzen.gamblebot.data.dao.DiscordGuild;
import com.motorbesitzen.gamblebot.data.dao.GambleSettings;
import com.motorbesitzen.gamblebot.util.EnvironmentUtil;
import com.motorbesitzen.gamblebot.util.ParseUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.springframework.stereotype.Service;

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
	public abstract String getUsage();

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
	 * @return
	 */
	@Override
	public abstract boolean isGlobalCommand();

	/**
	 * {@inheritDoc}
	 * Default command implementation without command functionality. Declared as 'unknown command'.
	 */
	@Override
	public abstract void execute(final GuildMessageReceivedEvent event);

	/**
	 * Sends an answer to a channel. Does not do anything different than {@link #sendMessage(TextChannel, String)} but
	 * clarifies that the message will be send as an answer to a command in the caller channel.
	 *
	 * @param channel <a href="https://ci.dv8tion.net/job/JDA/javadoc/net/dv8tion/jda/api/entities/TextChannel.html">TextChannel</a>
	 *                to send the message in.
	 * @param message The message content to send as answer.
	 */
	protected void answer(final TextChannel channel, final String message) {
		if(isValidMessage(channel, message)) {
			sendMessage(channel, message);
		}
	}

	/**
	 * Sends an embedded message as answer to a channel. Does not do anything different than
	 * {@link #sendMessage(TextChannel, MessageEmbed)} but clarifies that the message will be send as an answer to a
	 * command in the caller channel.
	 *
	 * @param channel <a href="https://ci.dv8tion.net/job/JDA/javadoc/net/dv8tion/jda/api/entities/TextChannel.html">TextChannel</a>
	 *                to send the message in.
	 * @param message The <a href="https://ci.dv8tion.net/job/JDA/javadoc/net/dv8tion/jda/api/entities/MessageEmbed.html">embedded message</a>
	 *                to send as answer.
	 */
	protected void answer(final TextChannel channel, final MessageEmbed message) {
		if(isValidMessage(channel, message)) {
			sendMessage(channel, message);
		}
	}

	/**
	 * Sends a message to a channel. Does not do anything if bot can not write in that channel.
	 *
	 * @param channel <a href="https://ci.dv8tion.net/job/JDA/javadoc/net/dv8tion/jda/api/entities/TextChannel.html">TextChannel</a>
	 *                to send the message in.
	 * @param message The message content to send as answer.
	 */
	private void sendMessage(final TextChannel channel, final String message) {
		channel.sendMessage(message).queue();
	}

	/**
	 * Sends an embedded message to a channel. Does not do anything if bot can not write in that channel.
	 *
	 * @param channel <a href="https://ci.dv8tion.net/job/JDA/javadoc/net/dv8tion/jda/api/entities/TextChannel.html">TextChannel</a>
	 *                to send the message in.
	 * @param message The <a href="https://ci.dv8tion.net/job/JDA/javadoc/net/dv8tion/jda/api/entities/MessageEmbed.html">embedded message</a>
	 *                to send as answer.
	 */
	private void sendMessage(final TextChannel channel, final MessageEmbed message) {
		channel.sendMessage(message).queue();
	}

	/**
	 * Used to clarify in the code that an error message is sent, doesn't do anything else than a normal answer message.
	 *
	 * @param channel      The <a href="https://ci.dv8tion.net/job/JDA/javadoc/net/dv8tion/jda/api/entities/TextChannel.html">TextChannel</a>
	 *                     where the original message is located in.
	 * @param errorMessage The error message to send.
	 */
	protected void sendErrorMessage(final TextChannel channel, final String errorMessage) {
		if(isValidMessage(channel, errorMessage)) {
			sendMessage(channel, errorMessage);
		}
	}

	/**
	 * Replies to a given message and pings the user without needing to mention him.
	 * If the {@param message} does not exist or the content to reply with is invalid (e.g. blank) it does not reply!
	 * If the bot does not have the 'Read Message History' permission it will send a normal message with a
	 * prepended mention of the user the bot replies to.
	 *
	 * @param message    the message to reply to.
	 * @param newMessage the content to reply with.
	 */
	protected void reply(final Message message, final String newMessage) {
		if(!canReply(message)) {
			sendMessage(message.getTextChannel(), message.getAuthor().getAsMention() + "\n" + newMessage);
			return;
		}

		if(isValidMessage(message, newMessage)) {
			message.reply(newMessage).queue();
		}
	}

	/**
	 * Checks if the bot has the needed permissions to reply to a message.
	 * @param message The message to reply to.
	 * @return {@code true} if the bod has the 'Read Message History' permission.
	 */
	private boolean canReply(final Message message) {
		final TextChannel channel = message.getTextChannel();
		final Guild guild = channel.getGuild();
		final Member self = guild.getSelfMember();
		return self.hasPermission(Permission.MESSAGE_HISTORY);
	}

	protected MessageEmbed buildGambleInfoEmbed(final DiscordGuild dcGuild) {
		final GambleSettings settings = dcGuild.getGambleSettings();
		final EmbedBuilder eb = new EmbedBuilder();
		final String prizeText = settings.getPrizeText();
		eb.setTitle("Gamble information:")
				.addField("Duration:", dcGuild.getTimeToEndText(), false)
				.addField("Cooldown between participations:", ParseUtil.parseMillisecondsToText(settings.getCooldownMs()), false)
				.addField("Prizes:", prizeText.substring(0, Math.min(1999, prizeText.length())), false)
				.setFooter("Use " + EnvironmentUtil.getEnvironmentVariable("CMD_PREFIX") + "gamble to participate!");
		return eb.build();
	}

	/**
	 * Determines if the new message is part of the original dialog.
	 *
	 * @param newEvent          The event of the new guild message.
	 * @param originalChannelId The channel ID of the channel where the original dialog started in.
	 * @param originalAuthorId  The member ID of the member who started the conversation.
	 * @return {@code true} if the dialog is the original one. {@code false} if not.
	 */
	protected boolean isWrongDialog(final GuildMessageReceivedEvent newEvent, final long originalChannelId, final long originalAuthorId) {
		final TextChannel channel = newEvent.getChannel();
		if (channel.getIdLong() != originalChannelId) {
			return true;
		}

		final Member author = newEvent.getMember();
		if (author == null) {
			return true;
		}

		return author.getIdLong() != originalAuthorId;
	}

	/**
	 * Used to clarify in the code that a log message is sent, doesn't do anything else than a normal answer message.
	 *
	 * @param channel      The <a href="https://ci.dv8tion.net/job/JDA/javadoc/net/dv8tion/jda/api/entities/TextChannel.html">TextChannel</a>
	 *                     where the original message is located in.
	 * @param logMessage The log message to send.
	 */
	protected void sendLogMessage(final TextChannel channel, final String logMessage) {
		if(isValidMessage(channel, logMessage)) {
			sendMessage(channel, logMessage);
		}
	}

	private boolean isValidMessage(final TextChannel channel, final String content) {
		if(channel == null) {
			return false;
		}

		if(!channel.canTalk()) {
			return false;
		}

		return isValidContent(content);
	}

	private boolean isValidMessage(final TextChannel channel, final MessageEmbed content) {
		if(channel == null) {
			return false;
		}

		if(!channel.canTalk()) {
			return false;
		}

		return isValidContent(content);
	}

	private boolean isValidMessage(final Message replyMessage, final String content) {
		if(replyMessage == null) {
			return false;
		}

		if(!replyMessage.getTextChannel().canTalk()) {
			return false;
		}

		return isValidContent(content);
	}

	private boolean isValidContent(final String content) {
		if(content == null) {
			return false;
		}

		return !content.isBlank();
	}

	private boolean isValidContent(final MessageEmbed content) {
		if(content == null) {
			return false;
		}

		return content.isSendable();
	}
}
