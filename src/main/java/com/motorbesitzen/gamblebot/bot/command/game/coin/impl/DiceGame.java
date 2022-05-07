package com.motorbesitzen.gamblebot.bot.command.game.coin.impl;

import com.motorbesitzen.gamblebot.bot.command.game.coin.Game;
import com.motorbesitzen.gamblebot.bot.command.game.coin.GameBet;
import com.motorbesitzen.gamblebot.bot.command.game.coin.GameWinInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class DiceGame implements Game {

	private final Random random;

	@Autowired
	private DiceGame(Random random) {
		this.random = random;
	}

	@Override
	public GameWinInfo play(GameBet bet) {
		int houseResult = random.nextInt(6) + 1;
		int playerResult = random.nextInt(6) + 1;
		if (playerResult > houseResult) {
			return GameWinInfo.won(bet.getWager(), "You: **" + playerResult + "**, House: **" + houseResult + "** -");
		} else if (playerResult < houseResult) {
			return GameWinInfo.lost(-1, "You: **" + playerResult + "**, House: **" + houseResult + "** -");
		} else {
			int secondPlayerResult = random.nextInt(6) + 1;
			if (secondPlayerResult > playerResult) {
				return GameWinInfo.won(
						bet.getWager(),
						"You: **" + playerResult + "**, House: **" + houseResult + "**, Second Throw: **" + secondPlayerResult + "** -"
				);
			} else {
				return GameWinInfo.lost(
						-1,
						"You: **" + playerResult + "**, House: **" + houseResult + "**, Second Throw: **" + secondPlayerResult + "** -"
				);
			}
		}
	}
}
