package com.motorbesitzen.gamblebot.bot.command.game.coin.impl;

import com.motorbesitzen.gamblebot.bot.command.game.coin.Game;
import com.motorbesitzen.gamblebot.bot.command.game.coin.GameBet;
import com.motorbesitzen.gamblebot.bot.command.game.coin.GameWinInfo;
import com.motorbesitzen.gamblebot.util.ParseUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.Random;

@Service
public class RouletteGame implements Game {

	private final Random random;

	private static final int[] BLACK_FIELDS = {15, 4, 2, 17, 6, 13, 11, 8, 10, 24, 33, 20, 31, 22, 29, 28, 35, 26};
	private static final int[] RED_FIELDS = {32, 19, 21, 25, 34, 27, 36, 30, 23, 5, 16, 1, 14, 9, 18, 7, 12, 3};

	@Autowired
	private RouletteGame(Random random) {
		this.random = random;
	}

	/*
		roulette payouts (french/european)
		syntax bet:							Rate on win
		B - set on black				-> wager
		R - set on red					-> wager
		E - set on even number		 	-> wager
		U - set on uneven number		-> wager
		L - set on lower half (1-18)	-> wager
		H - set on higher half (19-36)	-> wager
		0-36 - set on number			-> 35*wager
		0-36 - set on 2 numbers			-> 17*wager
		0-36 - set on 3 numbers			-> 11*wager
		0-36 - set on 4 numbers			-> 8*wager
		0-36 - set on 5 numbers			-> 6*wager
		0-36 - set on 6 numbers			-> 5*wager
	*/

	public GameWinInfo play(GameBet bet) {
		int result = random.nextInt(37);    // 0 -36
		String resultText = getColorEmote(result) + " (" + result + ")";
		long winAmount = bet.getBetInfo().matches("(?i)[BREULH]") ?
				getSectionWin(bet, result) :
				getNumberWin(bet, result);
		return winAmount < 0 ? GameWinInfo.lost(winAmount, resultText) : GameWinInfo.won(winAmount, resultText);
	}

	private long getSectionWin(GameBet bet, int result) {
		switch (bet.getBetInfo().toLowerCase()) {
			case "b":
				if (isBlackField(result)) {
					return bet.getWager();
				}
				break;
			case "r":
				if (isRedField(result)) {
					return bet.getWager();
				}
				break;
			case "e":
				if (result != 0 && result % 2 == 0) {
					return bet.getWager();
				}
				break;
			case "u":
				if (result % 2 == 1) {
					return bet.getWager();
				}
				break;
			case "l":
				if (result > 0 && result <= 18) {
					return bet.getWager();
				}
				break;
			case "h":
				if (result >= 19 && result < 37) {
					return bet.getWager();
				}
				break;
		}

		return -1;
	}

	private boolean isBlackField(int field) {
		for (int number : BLACK_FIELDS) {
			if (number == field) {
				return true;
			}
		}

		return false;
	}

	private boolean isRedField(int field) {
		for (int number : RED_FIELDS) {
			if (number == field) {
				return true;
			}
		}

		return false;
	}

	private long getNumberWin(GameBet bet, int result) {
		String[] bets = bet.getBetInfo().split(",");
		for (String singleBet : bets) {
			if (singleBet.equals(String.valueOf(result))) {
				return getMultiBetWin(bets.length, bet.getWager());
			}
		}

		return -1;
	}

	private long getMultiBetWin(int betSize, long wager) {
		switch (betSize) {
			case 1:
				return safelyMultiply(wager, 35);
			case 2:
				return safelyMultiply(wager, 17);
			case 3:
				return safelyMultiply(wager, 11);
			case 4:
				return safelyMultiply(wager, 8);
			case 5:
				return safelyMultiply(wager, 6);
			case 6:
				return safelyMultiply(wager, 5);
			default:
				return -1;
		}
	}

	private long safelyMultiply(long a, long b) {
		BigInteger bigA = BigInteger.valueOf(a);
		BigInteger bigB = BigInteger.valueOf(b);
		BigInteger result = bigA.multiply(bigB);
		return ParseUtil.safelyParseBigIntToLong(result);
	}

	private String getColorEmote(int field) {
		if (field == 0) {
			return ":green_square:";
		}

		if (isRedField(field)) {
			return ":red_square:";
		}

		return ":black_large_square:";
	}
}
