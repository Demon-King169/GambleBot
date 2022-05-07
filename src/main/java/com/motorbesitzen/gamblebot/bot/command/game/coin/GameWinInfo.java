package com.motorbesitzen.gamblebot.bot.command.game.coin;

public class GameWinInfo {

	private final GameState gameState;
	private final long winAmount;
	private final String resultText;

	private GameWinInfo(GameState gameState, long winAmount, String resultText) {
		this.gameState = gameState;
		this.winAmount = winAmount;
		this.resultText = resultText;
	}

	public static GameWinInfo won(long winAmount, String resultText) {
		return new GameWinInfo(GameState.WIN, winAmount, resultText);
	}

	public static GameWinInfo lost(long winAmount, String resultText) {
		return new GameWinInfo(GameState.LOST, winAmount, resultText);
	}

	public static GameWinInfo draw(long winAmount, String resultText) {
		return new GameWinInfo(GameState.DRAW, winAmount, resultText);
	}

	public long getWinAmount() {
		return winAmount;
	}

	public String getResultText() {
		return resultText;
	}

	public boolean isWin() {
		return gameState == GameState.WIN;
	}

	public boolean isDraw() {
		return gameState == GameState.DRAW;
	}
}
