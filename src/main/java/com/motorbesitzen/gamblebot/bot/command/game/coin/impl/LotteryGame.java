package com.motorbesitzen.gamblebot.bot.command.game.coin.impl;

import com.motorbesitzen.gamblebot.bot.command.game.coin.Game;
import com.motorbesitzen.gamblebot.bot.command.game.coin.GameBet;
import com.motorbesitzen.gamblebot.bot.command.game.coin.GameWinInfo;
import com.motorbesitzen.gamblebot.util.LogUtil;
import com.motorbesitzen.gamblebot.util.ParseUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Random;

@Service
public class LotteryGame implements Game {

	private final Random random;

	@Autowired
	private LotteryGame(final Random random) {
		this.random = random;
	}

	/*
		6 + s 	-> wager * 250k
		6 		-> wager * 100k
		5 + s 	-> wager * 1400
		5 		-> wager * 600
		4 + s 	-> wager * 25
		4 		-> wager * 6.5
		3 + s 	-> wager * 2.5
		3 		-> wager * 1.35
		2 + s 	-> wager * 0.7
		else 	-> loss
	 */

	public GameWinInfo play(final GameBet bet) {
		final int[] betNumbers = getBetNumbers(bet);
		final int superNumber = getSuperNumber();
		return calcWin(bet.getWager(), betNumbers, superNumber);
	}

	private int[] getBetNumbers(final GameBet bet) {
		final String[] betNumbers = bet.getBetInfo().split(",");
		return Arrays.stream(betNumbers).mapToInt(ParseUtil::safelyParseStringToInt).toArray();
	}

	private int getSuperNumber() {
		return random.nextInt(10);
	}

	private int[] getWinningNumbers() {
		final int[] winningNumbers = new int[6];
		for (int i = 0; i < 6; i++) {
			winningNumbers[i] = random.nextInt(49) + 1; // 1 to 49
		}

		return winningNumbers;
	}

	private GameWinInfo calcWin(long wager, int[] betNumbers, int superNumber) {
		final int[] winningNumbers = getWinningNumbers();
		final int winningSuperNumber = getSuperNumber();

		int hits = 0;
		for (int winningNumber : winningNumbers) {
			for (int betNumber : betNumbers) {
				if (winningNumber == betNumber) {
					hits++;
				}
			}
		}

		final String winningNumbersText = Arrays.toString(winningNumbers);
		final String summary = "Winning numbers: **" + winningNumbersText.substring(1, winningNumbersText.length() - 1) +
				"**, your hits: **" + hits + "**\nSuper number: **" + winningSuperNumber + "**, your super number: **" + superNumber + "**";
		final boolean matchingSuperNumber = superNumber == winningSuperNumber;
		final double winRate = getWinRate(hits, matchingSuperNumber);
		if (winRate == 0.0) {
			return new GameWinInfo(-1, summary);
		}

		final long winAmount = Math.max(1, safelyMultiply(wager, winRate));
		return new GameWinInfo(winAmount, summary);
	}

	private double getWinRate(final int hits, final boolean matchingSuperNumber) {
		final double winRate;
		switch (hits) {
			case 2:
				winRate = matchingSuperNumber ? 0.7 : 0.0;
				break;
			case 3:
				winRate = matchingSuperNumber ? 2.5 : 1.35;
				break;
			case 4:
				winRate = matchingSuperNumber ? 25.0 : 6.5;
				break;
			case 5:
				winRate = matchingSuperNumber ? 1400.0 : 600.0;
				break;
			case 6:
				winRate = matchingSuperNumber ? 250000.0 : 100000.0;
				break;
			default:
				winRate = 0.0;
		}

		return winRate;
	}

	private long safelyMultiply(final long a, final double b) {
		final BigDecimal bigA = BigDecimal.valueOf(a);
		final BigDecimal bigB = BigDecimal.valueOf(b);
		final BigDecimal result = bigA.multiply(bigB);
		return ParseUtil.safelyParseBigDecToLong(result);
	}
}
