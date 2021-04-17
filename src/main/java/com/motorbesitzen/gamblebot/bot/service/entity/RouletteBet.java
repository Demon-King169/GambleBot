package com.motorbesitzen.gamblebot.bot.service.entity;

public class RouletteBet {

	private final long wager;
	private final String betInfo;

	public RouletteBet(final long wager, final String betInfo) {
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
