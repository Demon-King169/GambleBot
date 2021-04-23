package com.motorbesitzen.gamblebot.bot.service.entity.roulette;

public class RouletteInfo {

	private static final int[] BLACK_FIELDS = {15, 4, 2, 17, 6, 13, 11, 8, 10, 24, 33, 20, 31, 22, 29, 28, 35, 26};
	private static final int[] RED_FIELDS = {32, 19, 21, 25, 34, 27, 36, 30, 23, 5, 16, 1, 14, 9, 18, 7, 12, 3};

	public static boolean isBlackField(final int field) {
		for (int number : BLACK_FIELDS) {
			if (number == field) {
				return true;
			}
		}

		return false;
	}

	public static boolean isRedField(final int field) {
		for (int number : RED_FIELDS) {
			if (number == field) {
				return true;
			}
		}

		return false;
	}

	public static String getColorEmote(final int field) {
		if (field == 0) {
			return ":green_square:";
		}

		if (isRedField(field)) {
			return ":red_square:";
		}

		return ":black_large_square:";
	}
}
