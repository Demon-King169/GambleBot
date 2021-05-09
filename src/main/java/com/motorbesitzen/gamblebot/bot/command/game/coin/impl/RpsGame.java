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

	private static final String RPS_ROCK = "Rock";
	private static final String RPS_SCISSORS = "Scissors";
	private static final String RPS_PAPER = "Paper";

	@Autowired
	private RpsGame(final Random random) {
		this.random = random;
	}

	@Override
	public GameWinInfo play(final GameBet bet) {
		final int result = random.nextInt(3);
		final String rps = getRps(result);
		final long winAmount = getWin(bet, rps);
		return new GameWinInfo(winAmount, rps);
	}

	private String getRps(final int result) {
		switch (result) {
			case 0:
				return RPS_ROCK;
			case 1:
				return RPS_SCISSORS;
			case 2:
			default:
				return RPS_PAPER;
		}
	}

	private long getWin(final GameBet bet, final String rpsResult) {
		if(isWin(bet.getBetInfo(), rpsResult)) {
			return bet.getWager();
		}

		if(isDraw(bet.getBetInfo(), rpsResult)) {
			return 0L;
		}

		return -1L;
	}

	private boolean isWin(final String bet, final String rpsResult) {
		return (bet.matches("(?i)R(ock)?") && rpsResult.equals(RPS_SCISSORS)) ||
				(bet.matches("(?i)S(cissors?)?") && rpsResult.equals(RPS_PAPER)) ||
				(bet.matches("(?i)P(aper)?") && rpsResult.equals(RPS_ROCK));
	}

	private boolean isDraw(final String bet, final String rpsResult) {
		final char betChar = bet.toLowerCase().charAt(0);
		final char resChar = rpsResult.toLowerCase().charAt(0);
		return betChar == resChar;
	}
}
