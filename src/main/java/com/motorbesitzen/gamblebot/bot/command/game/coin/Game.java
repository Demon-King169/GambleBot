package com.motorbesitzen.gamblebot.bot.command.game.coin;

public interface Game {

	/**
	 * Play the game with the given bet.
	 *
	 * @param bet The bet the player placed.
	 * @return The {@link GameWinInfo} with the result of the game.
	 */
	GameWinInfo play(GameBet bet);
}
