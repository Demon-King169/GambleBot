package com.motorbesitzen.gamblebot.data.dao;

import com.motorbesitzen.gamblebot.util.ParseUtil;

import javax.persistence.*;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.math.BigInteger;

@Entity
public class DiscordMember {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private long id;

	@Min(value = 10000000000000000L)
	private long discordId;

	@Min(0)
	private long nextGambleMs;

	@Min(0)
	private long nextDailyCoinsMs;

	@Min(0)
	private long coins;

	@NotNull
	@ManyToOne
	@JoinColumn(name = "guildId")
	private DiscordGuild guild;

	protected DiscordMember() {
	}

	public DiscordMember(@Min(value = 10000000000000000L) long discordId, @Min(0) long nextGambleMs, @NotNull DiscordGuild guild) {
		this.discordId = discordId;
		this.nextGambleMs = nextGambleMs;
		this.guild = guild;
	}

	public static DiscordMember createDefault(final long discordId, final DiscordGuild guild) {
		return new DiscordMember(discordId, 0, guild);
	}

	public long getDiscordId() {
		return discordId;
	}

	public long getNextGambleMs() {
		return nextGambleMs;
	}

	public long getNextDailyCoinsMs() {
		return nextDailyCoinsMs;
	}

	public void setNextDailyCoinsMs(final long nextDailyCoinsMs) {
		this.nextDailyCoinsMs = nextDailyCoinsMs;
	}

	public long getCoins() {
		return coins;
	}

	public void setNextGambleMs(final long nextGambleMs) {
		this.nextGambleMs = nextGambleMs;
	}

	public DiscordGuild getGuild() {
		return guild;
	}

	public boolean canPlay() {
		return nextGambleMs < System.currentTimeMillis();
	}

	public String getTimeToCooldownEndText() {
		final long toEndMs = nextGambleMs - System.currentTimeMillis();
		if (toEndMs <= 0) {
			return "0s";
		}

		return ParseUtil.parseMillisecondsToText(toEndMs);
	}

	public String getTimeToNextDailyText() {
		final long toNextMs = nextDailyCoinsMs - System.currentTimeMillis();
		if (toNextMs <= 0) {
			return "0s";
		}

		return ParseUtil.parseMillisecondsToText(toNextMs);
	}

	public void addCoins(final long coins) {
		this.coins = safelyAdd(this.coins, coins);
	}

	private long safelyAdd(final long a, final long b) {
		final BigInteger bigA = BigInteger.valueOf(a);
		final BigInteger bigB = BigInteger.valueOf(b);
		final BigInteger result = bigA.add(bigB);
		return ParseUtil.safelyParseBigIntToLong(result);
	}

	public void removeCoins(final long coins) {
		this.coins = Math.max(0, this.coins - coins);
	}
}
