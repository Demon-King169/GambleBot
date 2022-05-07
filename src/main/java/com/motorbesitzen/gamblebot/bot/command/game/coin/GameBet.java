package com.motorbesitzen.gamblebot.bot.command.game.coin;

public class GameBet {

	private final long wager;
	private final String betInfo;

	public GameBet(long wager) {
		this.wager = wager;
		this.betInfo = null;
	}

	public GameBet(long wager, String betInfo) {
		this.wager = wager;
		this.betInfo = betInfo;
	}

	public long getWager() {
		return wager;
	}

	public String getBetInfo() {
		return betInfo;
	}
}
