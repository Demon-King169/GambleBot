package com.motorbesitzen.gamblebot.bot.command.game.coin.impl;

import com.motorbesitzen.gamblebot.bot.command.game.coin.Game;
import com.motorbesitzen.gamblebot.bot.command.game.coin.GameBet;
import com.motorbesitzen.gamblebot.bot.command.game.coin.GameWinInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class FlipGame implements Game {

	private final Random random;

	private static final String COIN_HEADS = "Heads";
	private static final String COIN_TAILS = "Tails";
	private static final double PAYOUT_RATE = 0.95;

	@Autowired
	private FlipGame(Random random) {
		this.random = random;
	}

	@Override
	public GameWinInfo play(GameBet bet) {
		int result = random.nextInt(2);
		String headOrTail = result == 0 ? COIN_HEADS : COIN_TAILS;    // 0 = head, 1 = tail
		return getWin(bet, headOrTail);
	}

	private GameWinInfo getWin(GameBet bet, String headOrTail) {
		if (isWin(bet.getBetInfo(), headOrTail)) {
			return GameWinInfo.won(calcPayout(bet.getWager()), headOrTail);
		}

		return GameWinInfo.lost(-1, headOrTail);
	}

	private boolean isWin(String bet, String headOrTail) {
		return (bet.matches("(?i)H(eads?)?") && headOrTail.equals(COIN_HEADS)) ||
				(bet.matches("(?i)T(ails?)?") && headOrTail.equals(COIN_TAILS));
	}

	private long calcPayout(long wager) {
		double payout = (double) wager * PAYOUT_RATE;
		return Math.max(1, Math.round(payout));
	}
}
