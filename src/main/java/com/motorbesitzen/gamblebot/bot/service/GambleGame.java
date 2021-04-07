package com.motorbesitzen.gamblebot.bot.service;

import com.motorbesitzen.gamblebot.data.dao.DiscordGuild;
import com.motorbesitzen.gamblebot.data.dao.DiscordMember;
import com.motorbesitzen.gamblebot.data.dao.GamblePrize;
import com.motorbesitzen.gamblebot.data.dao.GambleSettings;
import com.motorbesitzen.gamblebot.util.LogUtil;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class GambleGame {

	public GamblePrize play(final DiscordMember player) {
		final DiscordGuild dcGuild = player.getGuild();
		final Random random = new Random();
		final double randomMove = random.nextDouble() * 100;
		final GambleSettings settings = dcGuild.getGambleSettings();
		final Set<GamblePrize> prizes = settings.getPrizes();
		final List<GamblePrize> prizeList = new ArrayList<>(prizes);
		prizeList.sort(Comparator.comparingLong(GamblePrize::getPrizeId));
		LogUtil.logInfo(player.getDiscordId() + " got a " + randomMove + " in " + Arrays.toString(prizeList.toArray()));

		double currentPos = 0.0;
		for (GamblePrize prize : prizeList) {
			if (hitsPrize(prize, currentPos, randomMove)) {
				return prize;
			}

			currentPos += prize.getPrizeChance();
		}

		return null;
	}

	private boolean hitsPrize(final GamblePrize prize, final double startPos, final double value) {
		final double rangeEnd = startPos + prize.getPrizeChance();
		return Double.compare(startPos, value) <= 0 && Double.compare(value, rangeEnd) < 0;
	}
}
