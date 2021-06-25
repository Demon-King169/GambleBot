package com.motorbesitzen.gamblebot.bot.command.game.coin.impl;

import com.motorbesitzen.gamblebot.bot.command.game.coin.Game;
import com.motorbesitzen.gamblebot.bot.command.game.coin.GameBet;
import com.motorbesitzen.gamblebot.bot.command.game.coin.GameWinInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class WarGame implements Game {

	private final Random random;

	private static final String[] DECK = {"2", "3", "4", "5", "6", "7", "8", "9", "J", "Q", "K", "A"};

	@Autowired
	private WarGame(final Random random) {
		this.random = random;
	}

	@Override
	public GameWinInfo play(final GameBet bet) {
		int houseResult = random.nextInt(DECK.length);
		int playerResult = random.nextInt(DECK.length);
		if (playerResult > houseResult) {
			return new GameWinInfo(bet.getWager(),
					"You: **" + DECK[playerResult] + "**, House: **" + DECK[houseResult] + "**");
		} else if (playerResult < houseResult) {
			return new GameWinInfo(-1,
					"You: **" + DECK[playerResult] + "**, House: **" + DECK[houseResult] + "**");
		} else {
			final StringBuilder sb = new StringBuilder();
			int gameCounter = 1;
			appendGameList(sb, gameCounter, playerResult, houseResult);
			do {
				playerResult = random.nextInt(DECK.length);
				houseResult = random.nextInt(DECK.length);
				gameCounter++;
				appendGameList(sb, gameCounter, playerResult, houseResult);
			} while(playerResult == houseResult);

			sb.setLength(sb.length() - 1);
			if (playerResult > houseResult) {
				return new GameWinInfo(bet.getWager(), sb.toString());
			} else {
				return new GameWinInfo(-1, sb.toString());
			}
		}
	}

	private void appendGameList(final StringBuilder sb, final int gameCounter, final int playerResult, final int houseResult) {
		sb.append("**[Game #").append(gameCounter).append("]** You: **")
				. append(DECK[playerResult]).append("**, House: **").append(DECK[houseResult]).append("**\n");
	}
}
