package com.motorbesitzen.gamblebot.bot.service;

import com.motorbesitzen.gamblebot.bot.service.entity.GambleWinInfo;
import com.motorbesitzen.gamblebot.data.dao.DiscordGuild;
import com.motorbesitzen.gamblebot.data.dao.DiscordMember;
import com.motorbesitzen.gamblebot.data.dao.GamblePrize;
import com.motorbesitzen.gamblebot.data.dao.GambleSettings;
import com.motorbesitzen.gamblebot.util.LogUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class GambleGame {

	private final Random random;

	@Autowired
	GambleGame(final Random random) {
		this.random = random;
	}

	public GambleWinInfo play(final DiscordMember player) {
		final DiscordGuild dcGuild = player.getGuild();
		final double randomNumber = random.nextDouble() * 100;
		final GambleSettings settings = dcGuild.getGambleSettings();
		final Set<GamblePrize> prizes = settings.getPrizes();
		final List<GamblePrize> prizeList = new ArrayList<>(prizes);
		prizeList.sort(Comparator.comparingLong(GamblePrize::getPrizeId));
		LogUtil.logInfo(player.getDiscordId() + " got a " + randomNumber + " in " + Arrays.toString(prizeList.toArray()));

		double currentPos = 0.0;
		for (GamblePrize prize : prizeList) {
			if (hitsPrize(prize, currentPos, randomNumber)) {
				return new GambleWinInfo(prize.getPrizeName(), randomNumber);
			}

			currentPos += prize.getPrizeChance();
		}

		return new GambleWinInfo(null, randomNumber);
	}

	private boolean hitsPrize(final GamblePrize prize, final double startPos, final double value) {
		final double rangeEnd = startPos + prize.getPrizeChance();
		return Double.compare(startPos, value) <= 0 && Double.compare(value, rangeEnd) < 0;
	}
}
