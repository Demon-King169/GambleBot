package com.motorbesitzen.gamblebot.bot.service.entity.gamble;

public class GambleWinInfo {

	private final String name;
	private final double luckyNumber;

	public GambleWinInfo(String name, double luckyNumber) {
		this.name = name;
		this.luckyNumber = luckyNumber;
	}

	public String getName() {
		return name;
	}

	public double getLuckyNumber() {
		return luckyNumber;
	}
}
