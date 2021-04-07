package com.motorbesitzen.gamblebot.bot.service;

public class WinInfo {

	private final String name;
	private final double luckyNumber;

	public WinInfo(String name, double luckyNumber) {
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
