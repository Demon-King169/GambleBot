package com.motorbesitzen.gamblebot.data.dao;

import com.motorbesitzen.gamblebot.util.ParseUtil;
import org.hibernate.annotations.ColumnDefault;

import javax.persistence.*;
import javax.validation.constraints.Min;
import java.util.HashSet;
import java.util.Set;

@Entity
public class DiscordGuild {

	@Id
	@Min(10000000000000000L)
	private long guildId;

	@Min(0)
	private long logChannelId;

	@Min(0)
	private long coinChannelId;

	@Min(0)
	private long dailyCoins;

	@Min(0)
	@ColumnDefault("0")
	private long boosterDailyBonus;

	@OneToOne
	private GambleSettings gambleSettings;

	@OneToMany(mappedBy = "guild", cascade = CascadeType.ALL, fetch = FetchType.EAGER)	// eager as it is limited to 25 anyway
	private Set<CoinShopOffer> shopOffers;

	@OneToMany(mappedBy = "guild", cascade = CascadeType.ALL)
	private Set<DiscordMember> members;

	protected DiscordGuild() {
	}

	private DiscordGuild(@Min(10000000000000000L) final long guildId) {
		this.guildId = guildId;
		this.members = new HashSet<>();
	}

	private DiscordGuild(@Min(10000000000000000L) final long guildId, @Min(0) final long logChannelId) {
		this.guildId = guildId;
		this.logChannelId = logChannelId;
		this.members = new HashSet<>();
	}

	public static DiscordGuild createDefault(final long guildId, final long logChannelId) {
		return new DiscordGuild(guildId, logChannelId);
	}

	public static DiscordGuild withGuildId(final long guildId) {
		return new DiscordGuild(guildId);
	}

	public long getGuildId() {
		return guildId;
	}

	public long getLogChannelId() {
		return logChannelId;
	}

	public void setLogChannelId(final long logChannelId) {
		this.logChannelId = logChannelId;
	}

	public long getCoinChannelId() {
		return coinChannelId;
	}

	public void setCoinChannelId(final long coinChannelId) {
		this.coinChannelId = coinChannelId;
	}

	public long getDailyCoins() {
		return dailyCoins;
	}

	public void setDailyCoins(final long dailyCoins) {
		this.dailyCoins = dailyCoins;
	}

	public long getBoosterDailyBonus() {
		return boosterDailyBonus;
	}

	public void setBoosterDailyBonus(final long boosterDailyCoins) {
		this.boosterDailyBonus = boosterDailyCoins;
	}

	public GambleSettings getGambleSettings() {
		return gambleSettings;
	}

	public void setGambleSettings(final GambleSettings gambleSettings) {
		this.gambleSettings = gambleSettings;
	}

	public Set<CoinShopOffer> getShopOffers() {
		return shopOffers;
	}

	public void setShopOffers(final Set<CoinShopOffer> shopOffers) {
		this.shopOffers = shopOffers;
	}

	public boolean hasRunningGamble() {
		if (gambleSettings == null) {
			return false;
		}

		final long startMs = gambleSettings.getStartTimestampMs();
		final long endMs = startMs + gambleSettings.getDurationMs();
		return System.currentTimeMillis() < endMs;
	}

	public String getTimeToEndText() {
		if (gambleSettings == null) {
			return "∞";
		}

		final long startMs = gambleSettings.getStartTimestampMs();
		final long endMs = startMs + gambleSettings.getDurationMs();
		final long toEndMs = endMs - System.currentTimeMillis();
		if (toEndMs <= 0) {
			return "0s";
		}

		return ParseUtil.parseMillisecondsToText(toEndMs);
	}

	public String getTimeSinceEndText() {
		if (gambleSettings == null) {
			return "∞";
		}

		final long startMs = gambleSettings.getStartTimestampMs();
		final long endMs = startMs + gambleSettings.getDurationMs();
		final long toEndMs = System.currentTimeMillis() - endMs;
		if (toEndMs <= 0) {
			return "0s";
		}

		return ParseUtil.parseMillisecondsToText(toEndMs);
	}
}
