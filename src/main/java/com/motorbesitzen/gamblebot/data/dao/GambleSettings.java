package com.motorbesitzen.gamblebot.data.dao;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
public class GambleSettings {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private long settingsId;

	@Min(0)
	private long startTimestampMs;

	@Min(0)
	private long durationMs;

	@Min(0)
	private long cooldownMs;

	@OneToMany(mappedBy = "settings", cascade = CascadeType.ALL)
	private Set<GamblePrize> prizes;

	@NotNull
	@OneToOne
	private DiscordGuild guild;

	protected GambleSettings() {
	}

	public GambleSettings(@Min(0) long startTimestampMs, @Min(0) long durationMs, @Min(0) long cooldownMs, @NotNull DiscordGuild guild) {
		this.startTimestampMs = startTimestampMs;
		this.durationMs = durationMs;
		this.cooldownMs = cooldownMs;
		this.prizes = new HashSet<>();
		this.guild = guild;
	}

	public GambleSettings(@Min(0) long durationMs, @Min(0) long cooldownMs, @NotNull DiscordGuild guild) {
		this.startTimestampMs = 0;
		this.durationMs = durationMs;
		this.cooldownMs = cooldownMs;
		this.prizes = new HashSet<>();
		this.guild = guild;
	}

	public long getStartTimestampMs() {
		return startTimestampMs;
	}

	public void setStartTimestampMs(long startTimestampMs) {
		this.startTimestampMs = startTimestampMs;
	}

	public long getDurationMs() {
		return durationMs;
	}

	public long getCooldownMs() {
		return cooldownMs;
	}

	public Set<GamblePrize> getPrizes() {
		return prizes;
	}

	public void setPrizes(Set<GamblePrize> prizes) {
		this.prizes = prizes;
	}

	public String getPrizeText() {
		StringBuilder sb = new StringBuilder();
		List<GamblePrize> prizeList = new ArrayList<>(prizes);
		prizeList.sort(Comparator.comparingLong(GamblePrize::getPrizeId));
		for (GamblePrize prize : prizeList) {
			sb.append("**").append(prize.getPrizeName()).append("** - ").append(prize.getPrizeChance()).append("%\n");
		}

		return sb.toString();
	}
}
