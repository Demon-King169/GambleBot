package com.motorbesitzen.gamblebot.bot.service;

import com.motorbesitzen.gamblebot.bot.service.entity.RouletteBet;
import com.motorbesitzen.gamblebot.bot.service.entity.RouletteInfo;
import com.motorbesitzen.gamblebot.bot.service.entity.RouletteWinInfo;
import com.motorbesitzen.gamblebot.util.LogUtil;
import com.motorbesitzen.gamblebot.util.ParseUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.Random;

@Service
public class RouletteGame {

	private final Random random;

	@Autowired
	private RouletteGame(final Random random) {
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

	public RouletteWinInfo play(final RouletteBet bet) {
		final int result = random.nextInt(37);	// 0 -36
		LogUtil.logDebug("Result: " + result);
		final long winAmount = bet.getBetInfo().matches("(?i)[BREULH]") ?
				getSectionWin(bet, result) :
				getNumberWin(bet, result);
		return new RouletteWinInfo(winAmount, result);
	}

	private long getSectionWin(final RouletteBet bet, final int result) {
		switch (bet.getBetInfo().toLowerCase()) {
			case "b":
				if(RouletteInfo.isBlackField(result)) {
					return bet.getWager();
				}
				break;
			case "r":
				if(RouletteInfo.isRedField(result)) {
					return bet.getWager();
				}
				break;
			case "e":
				if(result != 0 && result % 2 == 0) {
					return bet.getWager();
				}
				break;
			case "u":
				if(result % 2 == 1) {
					return bet.getWager();
				}
				break;
			case "l":
				if(result > 0 && result <= 18) {
					return bet.getWager();
				}
				break;
			case "h":
				if(result >= 19 && result < 37) {
					return bet.getWager();
				}
				break;
		}

		return 0;
	}

	private long getNumberWin(final RouletteBet bet, final int result) {
		final String[] bets = bet.getBetInfo().split(",");
		for(String singleBet : bets) {
			if (singleBet.equals(String.valueOf(result))) {
				return getMultiBetWin(bets.length, bet.getWager());
			}
		}

		return 0;
	}

	private long getMultiBetWin(final int betSize, final long wager) {
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
				return 0;
		}
	}

	private long safelyMultiply(final long a, final long b) {
		final BigInteger bigA = BigInteger.valueOf(a);
		final BigInteger bigB = BigInteger.valueOf(b);
		final BigInteger result = bigA.multiply(bigB);
		return ParseUtil.safelyParseBigIntToLong(result);
	}
}
