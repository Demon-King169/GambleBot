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
	private static final double PAYOUT_RATE = 0.95;

	@Autowired
	private RpsGame(Random random) {
		this.random = random;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public GameWinInfo play(GameBet bet) {
		int result = random.nextInt(3);
		String rps = getRps(result);
		return getWin(bet, rps);
	}

	private String getRps(int result) {
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

	private GameWinInfo getWin(GameBet bet, String rpsResult) {
		if (isWin(bet.getBetInfo(), rpsResult)) {
			return GameWinInfo.lost(calcPayout(bet.getWager()), rpsResult);
		}

		if (isDraw(bet.getBetInfo(), rpsResult)) {
			return GameWinInfo.draw(0, rpsResult);
		}

		return GameWinInfo.lost(-1, rpsResult);
	}

	private boolean isWin(String bet, String rpsResult) {
		return (bet.matches("(?i)R(ock)?") && rpsResult.equals(RPS_SCISSORS)) ||
				(bet.matches("(?i)S(cissors?)?") && rpsResult.equals(RPS_PAPER)) ||
				(bet.matches("(?i)P(aper)?") && rpsResult.equals(RPS_ROCK));
	}

	private boolean isDraw(String bet, String rpsResult) {
		char betChar = bet.toLowerCase().charAt(0);
		char resChar = rpsResult.toLowerCase().charAt(0);
		return betChar == resChar;
	}

	private long calcPayout(long wager) {
		double payout = (double) wager * PAYOUT_RATE;
		return Math.max(1, Math.round(payout));
	}
}
