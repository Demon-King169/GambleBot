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

	@Autowired
	private FlipGame(final Random random) {
		this.random = random;
	}

	@Override
	public GameWinInfo play(final GameBet bet) {
		final double result = random.nextDouble();
		final String headOrTail = Double.compare(0.5, result) < 0 ? "Head" : "Tail";
		final long winAmount = getWin(bet, headOrTail);
		return new GameWinInfo(winAmount, headOrTail);
	}

	private long getWin(final GameBet bet, final String headOrTail) {
		if(isWin(bet.getBetInfo(), headOrTail)) {
			return bet.getWager();
		}

		return 0L;
	}

	private boolean isWin(final String bet, final String headOrTail) {
		return (bet.matches("(?i)H(eads?)?") && headOrTail.equals("Head")) ||
				(bet.matches("(?i)T(ails?)?") && headOrTail.equals("Tail"));
	}
}
