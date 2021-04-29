package com.motorbesitzen.gamblebot.bot.command.game.gamble;

public class GambleWinInfo {

	private final String priceName;
	private final double luckyNumber;

	public GambleWinInfo(final String priceName, final double luckyNumber) {
		this.priceName = priceName;
		this.luckyNumber = luckyNumber;
	}

	public String getPriceName() {
		return priceName;
	}

	public double getLuckyNumber() {
		return luckyNumber;
	}
}
