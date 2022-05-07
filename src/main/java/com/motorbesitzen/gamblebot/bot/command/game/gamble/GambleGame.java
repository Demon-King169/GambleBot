package com.motorbesitzen.gamblebot.bot.command.game.gamble;

import com.motorbesitzen.gamblebot.data.dao.GamblePrize;
import com.motorbesitzen.gamblebot.data.dao.GambleSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.Set;

@Service
public class GambleGame {

	private final Random random;

	@Autowired
	private GambleGame(Random random) {
		this.random = random;
	}

	public GambleWinInfo play(GambleSettings settings) {
		double randomNumber = random.nextDouble() * 100;
		Set<GamblePrize> prizes = settings.getPrizes();
		List<GamblePrize> prizeList = new ArrayList<>(prizes);
		prizeList.sort(Comparator.comparingLong(GamblePrize::getPrizeId));

		double currentPos = 0.0;
		for (GamblePrize prize : prizeList) {
			if (hitsPrize(prize, currentPos, randomNumber)) {
				return new GambleWinInfo(prize.getPrizeName(), randomNumber);
			}

			currentPos += prize.getPrizeChance();
		}

		return new GambleWinInfo(null, randomNumber);
	}

	private boolean hitsPrize(GamblePrize prize, double startPos, double value) {
		double rangeEnd = startPos + prize.getPrizeChance();
		return Double.compare(startPos, value) <= 0 && Double.compare(value, rangeEnd) < 0;
	}
}
