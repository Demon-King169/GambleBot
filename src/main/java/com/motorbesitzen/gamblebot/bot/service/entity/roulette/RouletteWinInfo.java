package com.motorbesitzen.gamblebot.bot.service.entity.roulette;

public class RouletteWinInfo {

	private final long winAmount;
	private final int resultNumber;

	public RouletteWinInfo(final long winAmount, final int resultNumber) {
		this.winAmount = winAmount;
		this.resultNumber = resultNumber;
	}

	public boolean isWin() {
		return winAmount > 0;
	}

	public long getWinAmount() {
		return winAmount;
	}

	public int getResultNumber() {
		return resultNumber;
	}
}
