package com.motorbesitzen.gamblebot.bot.command.game.coin;

public class GameWinInfo {

	private final GameState gameState;
	private final long winAmount;
	private final String resultText;

	private GameWinInfo(final GameState gameState, final long winAmount, final String resultText) {
		this.gameState = gameState;
		this.winAmount = winAmount;
		this.resultText = resultText;
	}

	public static GameWinInfo won(final long winAmount, final String resultText) {
		return new GameWinInfo(GameState.WIN, winAmount, resultText);
	}

	public static GameWinInfo lost(final long winAmount, final String resultText) {
		return new GameWinInfo(GameState.LOST, winAmount, resultText);
	}

	public static GameWinInfo draw(final long winAmount, final String resultText) {
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

	public boolean isLoss() {
		return gameState == GameState.LOST;
	}
}
