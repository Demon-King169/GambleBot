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

	@Autowired
	private FlipGame(final Random random) {
		this.random = random;
	}

	@Override
	public GameWinInfo play(final GameBet bet) {
		final int result = random.nextInt(2);
		final String headOrTail = result == 0 ? COIN_HEADS : COIN_TAILS;	// 0 = head, 1 = tail
		final long winAmount = getWin(bet, headOrTail);
		return new GameWinInfo(winAmount, headOrTail);
	}

	private long getWin(final GameBet bet, final String headOrTail) {
		if(isWin(bet.getBetInfo(), headOrTail)) {
			return bet.getWager();
		}

		return -1L;
	}

	private boolean isWin(final String bet, final String headOrTail) {
		return (bet.matches("(?i)H(eads?)?") && headOrTail.equals(COIN_HEADS)) ||
				(bet.matches("(?i)T(ails?)?") && headOrTail.equals(COIN_TAILS));
	}
}
