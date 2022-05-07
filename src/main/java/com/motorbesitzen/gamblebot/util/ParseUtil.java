package com.motorbesitzen.gamblebot.util;

import java.math.BigDecimal;
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
	public static int safelyParseStringToInt(String integerString) {
		if (integerString == null) {
			return -1;
		}

		String numberString = parseUnitChars(integerString);
		try {
			return Integer.parseInt(numberString);
		} catch (NumberFormatException e) {
			return -1;
		}
	}

	/**
	 * Replaces 'units' in Strings by replacing "k" for "thousand" with "000", "m" for "million" with "000000"
	 * and "b" for "billion" with "000000000".
	 *
	 * @param numberWithUnit The number String that may or may not include the mentioned 'units'.
	 * @return The string with the mentioned 'units' replaced if they exist.
	 */
	private static String parseUnitChars(String numberWithUnit) {
		String lowerNumberWithUnit = numberWithUnit.toLowerCase().trim();
		if (lowerNumberWithUnit.matches("[0-9]+[kmb]")) {
			lowerNumberWithUnit = lowerNumberWithUnit.replaceFirst("k", "000");
			lowerNumberWithUnit = lowerNumberWithUnit.replaceFirst("m", "000000");
			lowerNumberWithUnit = lowerNumberWithUnit.replaceFirst("b", "000000000");
		}

		return lowerNumberWithUnit;
	}

	/**
	 * Tries to parse a {@code String} to a {@code long}. Also supports text like "1k" for "1000".
	 *
	 * @param longString The {@code String} representation of a number.
	 * @return The number as {@code long} or -1 if the {@code String} can not be parsed.
	 */
	public static long safelyParseStringToLong(String longString) {
		if (longString == null) {
			return -1;
		}

		String numberString = parseUnitChars(longString);
		try {
			return Long.parseLong(numberString);
		} catch (NumberFormatException e) {
			return -1;
		}
	}

	public static String parseMillisecondsToText(long ms) {
		if (ms < 1000) {
			return "**" + ms + "**ms";
		}

		long days = ms / 86400000;
		long hours = (ms % 86400000) / 3600000;
		long minutes = ((ms % 86400000) % 3600000) / 60000;
		long seconds = (((ms % 86400000) % 3600000) % 60000) / 1000;

		String durationText = days > 0 ? "**" + days + "**d " : "";
		durationText += hours > 0 ? "**" + hours + "**h " : "";
		durationText += minutes > 0 ? "**" + minutes + "**m " : "";
		durationText += seconds > 0 ? "**" + seconds + "**s" : "";
		return durationText;
	}

	public static long parseTextToMilliseconds(String text) {
		long ms = 0;
		String[] tokens = text.split(" ");
		for (String token : tokens) {
			if (token.matches("[0-9]+d")) {
				String number = token.replaceAll("[^0-9]", "");
				ms += safelyParseStringToLong(number) * 86400000;
				continue;
			}

			if (token.matches("[0-9]+h")) {
				String number = token.replaceAll("[^0-9]", "");
				ms += safelyParseStringToLong(number) * 3600000;
				continue;
			}

			if (token.matches("[0-9]+m")) {
				String number = token.replaceAll("[^0-9]", "");
				ms += safelyParseStringToLong(number) * 60000;
				continue;
			}

			if (token.matches("[0-9]+s")) {
				String number = token.replaceAll("[^0-9]", "");
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
	public static long safelyParseBigIntToLong(BigInteger number) {
		BigInteger lowerLimit = BigInteger.valueOf(Long.MIN_VALUE);
		if (number.compareTo(lowerLimit) <= 0) {
			return Long.MIN_VALUE;
		}

		BigInteger upperLimit = BigInteger.valueOf(Long.MAX_VALUE);
		if (number.compareTo(upperLimit) >= 0) {
			return Long.MAX_VALUE;
		}

		return number.longValue();
	}

	/**
	 * Tries to parse a {@code BigDecimal} to a {@code long}.
	 *
	 * @param number The {@code BigDecimal} representation of a number.
	 * @return The number as {@code long}. If the {@param number} is greater than {@code Long.MAX_VALUE} it
	 * returns {@code Long.MAX_VALUE}. If the {@param number} is smaller than {@code Long.MIN_VALUE} it
	 * returns {@code Long.MIN_VALUE}.
	 */
	public static long safelyParseBigDecToLong(BigDecimal number) {
		BigDecimal lowerLimit = BigDecimal.valueOf(Long.MIN_VALUE);
		if (number.compareTo(lowerLimit) <= 0) {
			return Long.MIN_VALUE;
		}

		BigDecimal upperLimit = BigDecimal.valueOf(Long.MAX_VALUE);
		if (number.compareTo(upperLimit) >= 0) {
			return Long.MAX_VALUE;
		}

		return number.longValue();
	}

	public static double safelyParseStringToDouble(String doubleString) {
		if (doubleString == null) {
			return -1;
		}

		String numberString = parseUnitChars(doubleString);
		try {
			return Double.parseDouble(numberString);
		} catch (NumberFormatException e) {
			return -1;
		}
	}
}
