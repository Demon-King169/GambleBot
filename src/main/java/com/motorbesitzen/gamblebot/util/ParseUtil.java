package com.motorbesitzen.gamblebot.util;

import java.math.BigInteger;

/**
 * Helper functions for safely parsing inputs. Since mostly IDs need to be parsed -1 is an anomaly
 * that can be used instead of an error/exception.
 */
public final class ParseUtil {

	/**
	 * Tries to parse a {@code String} to an {@code int}.
	 *
	 * @param integerString The {@code String} representation of a number.
	 * @return The number as {@code int} or -1 if the {@code String} can not be parsed.
	 */
	public static int safelyParseStringToInt(final String integerString) {
		if (integerString == null) {
			return -1;
		}

		try {
			return Integer.parseInt(integerString.trim());
		} catch (NumberFormatException e) {
			return -1;
		}
	}

	/**
	 * Tries to parse a {@code String} to a {@code long}.
	 *
	 * @param longString The {@code String} representation of a number.
	 * @return The number as {@code long} or -1 if the {@code String} can not be parsed.
	 */
	public static long safelyParseStringToLong(final String longString) {
		if (longString == null) {
			return -1;
		}

		try {
			return Long.parseLong(longString.trim());
		} catch (NumberFormatException e) {
			return -1;
		}
	}

	public static String parseMillisecondsToText(final long ms) {
		if (ms < 1000) {
			return "**" + ms + "**ms";
		}

		final long days = ms / 86400000;
		final long hours = (ms % 86400000) / 3600000;
		final long minutes = ((ms % 86400000) % 3600000) / 60000;
		final long seconds = (((ms % 86400000) % 3600000) % 60000) / 1000;

		String durationText = days > 0 ? "**" + days + "**d " : "";
		durationText += hours > 0 ? "**" + hours + "**h " : "";
		durationText += minutes > 0 ? "**" + minutes + "**m " : "";
		durationText += seconds > 0 ? "**" + seconds + "**s" : "";
		return durationText;
	}

	public static long parseTextToMilliseconds(final String text) {
		long ms = 0;
		final String[] tokens = text.split(" ");
		for (String token : tokens) {
			if (token.matches("[0-9]+d")) {
				final String number = token.replaceAll("[^0-9]", "");
				ms += safelyParseStringToLong(number) * 86400000;
				continue;
			}

			if (token.matches("[0-9]+h")) {
				final String number = token.replaceAll("[^0-9]", "");
				ms += safelyParseStringToLong(number) * 3600000;
				continue;
			}

			if (token.matches("[0-9]+m")) {
				final String number = token.replaceAll("[^0-9]", "");
				ms += safelyParseStringToLong(number) * 60000;
				continue;
			}

			if (token.matches("[0-9]+s")) {
				final String number = token.replaceAll("[^0-9]", "");
				ms += safelyParseStringToLong(number) * 1000;
			}
		}

		return ms;
	}

	/**
	 * Tries to parse a {@code BigInteger} to a {@code long}.
	 *
	 * @param number The {@code BigInteger} representation of a number.
	 * @return The number as {@code long}. If the {@param number} is greater than {@code Long.MAX_VALUE} it
	 * returns {@code Long.MAX_VALUE}. If the {@param number} is smaller than {@code Long.MIN_VALUE} it
	 * returns {@code Long.MIN_VALUE}.
	 */
	public static long safelyParseBigIntToLong(final BigInteger number) {
		final BigInteger lowerLimit = BigInteger.valueOf(Long.MIN_VALUE);
		if(number.compareTo(lowerLimit) <= 0) {
			return Long.MIN_VALUE;
		}

		final BigInteger upperLimit = BigInteger.valueOf(Long.MAX_VALUE);
		if(number.compareTo(upperLimit) >= 0) {
			return Long.MAX_VALUE;
		}

		return number.longValue();
	}
}
