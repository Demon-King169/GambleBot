package com.motorbesitzen.gamblebot.util;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper functions for Discord messages.
 */
public final class DiscordMessageUtil {

	/**
	 * Get a mentioned member ID from a message. If the message has a mention it uses the ID of the first mentioned member.
	 * If there is no mention it checks for a numeric ID token and if there are multiple chooses the first one. If
	 * there is a mention due to a reply mention that mention gets ignored.
	 *
	 * @param message The Discord message object.
	 * @return If there is a member ID found it returns the ID as a {@code long}. If a raw ID exceeds the {@code Long}
	 * limits it returns -1 as well if there is no ID found in the message.
	 */
	public static long getMentionedMemberId(final Message message) {
		final List<Member> mentionedUsers = message.getMentionedMembers();
		if (mentionedUsers.size() == 0) {
			return getRawMention(message);
		}

		if (message.getReferencedMessage() != null) {
			// if the message is a reply to another message there is no way to know who got mentioned at which position.
			// the author of the referenced message is the mention on index 0 but if he also gets mentioned
			// in the message content the author does not appear on index 1 so any user that also gets mentioned
			// in the message content after the author of the referenced message will be the mentioned user
			// on index 1 although the user should not be there.
			// the only way to get around Discords limitation in that point is by ignoring mentions of a reply and
			// to look for a user mention in the raw message content instead.
			return getRawMention(message);
		}

		if (mentionedUsers.size() != 0) {
			return mentionedUsers.get(0).getIdLong();
		}

		return getMentionedRawId(message);
	}

	/**
	 * Searches the message for a 'raw' user mention ("<@...>" with "..." being the user ID).
	 *
	 * @param message The Discord message object.
	 * @return If there is a raw mention found it returns the ID as a {@code long}, if not or if it exceeds the {@code Long}
	 * limits it returns -1.
	 */
	public static long getRawMention(final Message message) {
		final String rawMessage = message.getContentRaw();
		final String[] tokens = rawMessage.split(" ");
		for (String token : tokens) {
			if (token.matches("<@!?[0-9]+>")) {
				token = token.replaceAll("[^\\d+]", "");
				return ParseUtil.safelyParseStringToLong(token);
			}
		}

		return -1;
	}

	/**
	 * Searches the message for a 'raw' numeric ID.
	 *
	 * @param message The Discord message object.
	 * @return If there is a ID found it returns the ID as a {@code long}, if not or if it exceeds the {@code Long}
	 * limits it returns -1.
	 */
	public static long getMentionedRawId(final Message message) {
		final String rawMessage = message.getContentRaw();
		final String[] tokens = rawMessage.split(" ");
		for (String token : tokens) {
			if (token.matches("[0-9]+")) {
				return ParseUtil.safelyParseStringToLong(token);
			}
		}

		return -1;
	}

	public static List<String> getStringsInQuotationMarks(final String content) {
		final Pattern pattern = Pattern.compile("\"[^\"]*\"");
		final Matcher matcher = pattern.matcher(content);
		final List<String> quoted = new ArrayList<>();
		while (matcher.find()) {
			final String quote = matcher.group().replaceAll("\"", "");
			quoted.add(quote);
		}

		return quoted;
	}
}
