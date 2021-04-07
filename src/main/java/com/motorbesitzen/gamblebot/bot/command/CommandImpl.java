package com.motorbesitzen.gamblebot.bot.command;

import com.motorbesitzen.gamblebot.data.dao.DiscordGuild;
import com.motorbesitzen.gamblebot.data.dao.GambleSettings;
import com.motorbesitzen.gamblebot.util.EnvironmentUtil;
import com.motorbesitzen.gamblebot.util.ParseUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
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
	 */
	@Override
	public abstract void execute(final GuildMessageReceivedEvent event);

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void answer(final TextChannel channel, final String message) {
		sendMessage(channel, message);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void answer(final TextChannel channel, final MessageEmbed message) {
		sendMessage(channel, message);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void sendMessage(final TextChannel channel, final String message) {
		if (channel.canTalk()) {
			channel.sendMessage(message).queue();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void sendMessage(final TextChannel channel, final MessageEmbed message) {
		if (channel.canTalk()) {
			channel.sendMessage(message).queue();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Message answerPlaceholder(final TextChannel channel, final String placeholderMessage) {
		if (!channel.canTalk()) {
			return null;
		}

		return channel.sendMessage(placeholderMessage).complete();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void editPlaceholder(final Message message, final String newMessage) {
		if (!message.getTextChannel().canTalk()) {
			return;
		}

		try {
			message.editMessage(newMessage).queue();
		} catch (IllegalStateException e) {
			sendErrorMessage(
					message.getTextChannel(),
					"Can not edit message (" + message.getId() + ") from another user!"
			);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void editPlaceholder(final TextChannel channel, final long messageId, final String newMessage) {
		if (!channel.canTalk()) {
			return;
		}

		channel.retrieveMessageById(messageId).queue(
				message -> editPlaceholder(message, newMessage),
				throwable -> sendErrorMessage(
						channel, "Can not edit message!\nMessage ID " + messageId + " not found in " +
								channel.getAsMention() + ".\n New message: \"" + newMessage + "\""
				)
		);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void sendErrorMessage(final TextChannel channel, final String errorMessage) {
		answer(channel, errorMessage);
	}


	protected MessageEmbed buildGambleInfoEmbed(final DiscordGuild dcGuild) {
		final GambleSettings settings = dcGuild.getGambleSettings();
		final EmbedBuilder eb = new EmbedBuilder();
		final String prizeText = settings.getPrizeText();
		eb.setTitle("Gamble information:")
				.addField("Ends in:", dcGuild.getTimeToEndText(), false)
				.addField("Cooldown between participations:", ParseUtil.parseMillisecondsToText(settings.getCooldownMs()), false)
				.addField("Prizes:", prizeText.substring(0, Math.min(1999, prizeText.length())), false)
				.setFooter("Use " + EnvironmentUtil.getEnvironmentVariable("CMD_PREFIX") + "gamble to participate!");
		return eb.build();
	}
}
