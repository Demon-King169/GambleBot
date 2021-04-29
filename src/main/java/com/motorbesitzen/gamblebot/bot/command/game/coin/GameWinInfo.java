package com.motorbesitzen.gamblebot.bot.command.game.coin;

public class GameWinInfo {

	private final long winAmount;
	private final String resultText;

	public GameWinInfo(final long winAmount, final String resultText) {
		this.winAmount = winAmount;
		this.resultText = resultText;
	}

	public long getWinAmount() {
		return winAmount;
	}

	public String getResultText() {
		return resultText;
	}

	public boolean isWin() {
		return winAmount > 0;
	}
}
