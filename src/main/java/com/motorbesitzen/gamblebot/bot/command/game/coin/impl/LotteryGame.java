package com.motorbesitzen.gamblebot.bot.command.game.coin.impl;

import com.motorbesitzen.gamblebot.bot.command.game.coin.Game;
import com.motorbesitzen.gamblebot.bot.command.game.coin.GameBet;
import com.motorbesitzen.gamblebot.bot.command.game.coin.GameWinInfo;
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
	private LotteryGame(Random random) {
		this.random = random;
	}

	/*
		6 correct: chance of 1 in 14,000,000
		6 		-> wager * 250k
		5 		-> wager * 1400
		4 		-> wager * 25
		3 		-> wager * 2.5
		2 	 	-> wager * 0.7
		else 	-> loss
	 */

	public GameWinInfo play(GameBet bet) {
		int[] betNumbers = getBetNumbers(bet);
		return calcWin(bet.getWager(), betNumbers);
	}

	private int[] getBetNumbers(GameBet bet) {
		String[] betNumbers = bet.getBetInfo().split(",");
		return Arrays.stream(betNumbers).mapToInt(ParseUtil::safelyParseStringToInt).toArray();
	}

	private int[] getWinningNumbers() {
		int[] winningNumbers = new int[6];
		for (int i = 0; i < 6; i++) {
			int number = random.nextInt(49) + 1; // 1 to 49
			System.out.println(i);
			if (isDuplicate(winningNumbers, number)) {
				i--;
				continue;
			}

			winningNumbers[i] = number;
		}

		return winningNumbers;
	}

	private boolean isDuplicate(int[] winningNumbers, int number) {
		// uninitialized int is 0 which is not in [1,49] so uninitialized numbers do not matter
		for (int winningNumber : winningNumbers) {
			if (winningNumber == number) {
				return true;
			}
		}

		return false;
	}

	private GameWinInfo calcWin(long wager, int[] betNumbers) {
		int[] winningNumbers = getWinningNumbers();
		int hits = 0;
		for (int winningNumber : winningNumbers) {
			for (int betNumber : betNumbers) {
				if (winningNumber == betNumber) {
					hits++;
				}
			}
		}

		String winningNumbersText = Arrays.toString(winningNumbers);
		String summary = "Winning numbers: **" + winningNumbersText.substring(1, winningNumbersText.length() - 1) +
				"**, your hits: **" + hits + "**";
		double winRate = getWinRate(hits);
		if (winRate == 0.0) {
			return GameWinInfo.lost(-1, summary);
		}

		long winAmount = Math.max(1, safelyMultiply(wager, winRate));
		return GameWinInfo.won(winAmount, summary);
	}

	private double getWinRate(int hits) {
		double winRate;
		switch (hits) {
			case 2:
				winRate = 0.7;
				break;
			case 3:
				winRate = 2.5;
				break;
			case 4:
				winRate = 25.0;
				break;
			case 5:
				winRate = 1400.0;
				break;
			case 6:
				winRate = 250000.0;
				break;
			default:
				winRate = 0.0;
		}

		return winRate;
	}

	private long safelyMultiply(long a, double b) {
		BigDecimal bigA = BigDecimal.valueOf(a);
		BigDecimal bigB = BigDecimal.valueOf(b);
		BigDecimal result = bigA.multiply(bigB);
		return ParseUtil.safelyParseBigDecToLong(result);
	}
}
