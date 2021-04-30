package com.motorbesitzen.gamblebot.bot.command.game.coin.impl;

import com.motorbesitzen.gamblebot.bot.command.game.coin.Game;
import com.motorbesitzen.gamblebot.bot.command.game.coin.GameBet;
import com.motorbesitzen.gamblebot.bot.command.game.coin.GameWinInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class RpsGame implements Game {

	private final Random random;

	@Autowired
	private RpsGame(final Random random) {
		this.random = random;
	}

	@Override
	public GameWinInfo play(final GameBet bet) {
		final double result = random.nextDouble();
		final String rps = getRps(result);
		final long winAmount = getWin(bet, rps);
		return new GameWinInfo(winAmount, rps);
	}

	private String getRps(final double result) {
		if(Double.compare(result, 0.33) < 0) {
			return "Rock";
		} else if(Double.compare(result, 0.66) < 0) {
			return "Scissors";
		}

		return "Paper";
	}

	private long getWin(final GameBet bet, final String headOrTail) {
		if(isWin(bet.getBetInfo(), headOrTail)) {
			return bet.getWager();
		}

		if(isDraw(bet.getBetInfo(), headOrTail)) {
			return 0L;
		}

		return -1L;
	}

	private boolean isWin(final String bet, final String headOrTail) {
		return (bet.matches("(?i)R(ock)?") && headOrTail.equals("Scissors")) ||
				(bet.matches("(?i)S(cissors?)?") && headOrTail.equals("Paper")) ||
				(bet.matches("(?i)P(aper)?") && headOrTail.equals("Rock"));
	}

	private boolean isDraw(final String bet, final String headOrTail) {
		char betChar = bet.toLowerCase().charAt(0);
		char resChar = headOrTail.toLowerCase().charAt(0);
		return betChar == resChar;
	}
}
