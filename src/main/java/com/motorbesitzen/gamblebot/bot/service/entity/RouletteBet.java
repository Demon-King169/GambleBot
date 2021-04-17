package com.motorbesitzen.gamblebot.bot.service.entity;

public class RouletteBet {

	private final long userId;
	private final long wager;
	private final String betInfo;

	public RouletteBet(long userId, long wager, String betInfo) {
		this.userId = userId;
		this.wager = wager;
		this.betInfo = betInfo;
	}

	public long getUserId() {
		return userId;
	}

	public long getWager() {
		return wager;
	}

	public String getBetInfo() {
		return betInfo;
	}
}
