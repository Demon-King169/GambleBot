package com.motorbesitzen.gamblebot.data.dao;

import com.motorbesitzen.gamblebot.util.ParseUtil;

import javax.persistence.*;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.math.BigInteger;
import java.util.Set;

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

	@Min(0)
	private long coinsWon;

	@Min(0)
	private long coinsLost;

	@Min(0)
	private long gamesPlayed;

	@Min(0)
	private long gamesWon;

	@Min(0)
	private long gamesLost;

	@Min(0)
	private long coinsSpend;

	@Min(0)
	private long coinsReceived;

	@NotNull
	@ManyToOne
	@JoinColumn(name = "guildId")
	private DiscordGuild guild;

	@OneToMany(mappedBy = "buyer", cascade = CascadeType.ALL)
	private Set<Purchase> purchases;

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

	public void setNextGambleMs(final long nextGambleMs) {
		this.nextGambleMs = nextGambleMs;
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

	public void setCoins(long coins) {
		this.coins = coins;
	}

	public long getCoinsWon() {
		return coinsWon;
	}

	public long getCoinsLost() {
		return coinsLost;
	}

	public long getGamesPlayed() {
		return gamesPlayed;
	}

	public long getGamesWon() {
		return gamesWon;
	}

	public long getGamesLost() {
		return gamesLost;
	}

	public long getCoinsSpend() {
		return coinsSpend;
	}

	public long getCoinsReceived() {
		return coinsReceived;
	}

	public DiscordGuild getGuild() {
		return guild;
	}

	public Set<Purchase> getPurchases() {
		return purchases;
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

	private long safelyAdd(final long a, final long b) {
		final BigInteger bigA = BigInteger.valueOf(a);
		final BigInteger bigB = BigInteger.valueOf(b);
		final BigInteger result = bigA.add(bigB);
		return ParseUtil.safelyParseBigIntToLong(result);
	}

	public void lostGame(final long coinsLost) {
		this.gamesPlayed++;
		this.gamesLost++;
		this.coinsLost = safelyAdd(this.coinsLost, coinsLost);
		this.coins -= coinsLost;
	}

	public void wonGame(final long coinsWon) {
		this.gamesPlayed++;
		this.gamesWon++;
		this.coinsWon = safelyAdd(this.coinsWon, coinsWon);
		this.coins = safelyAdd(this.coins, coinsWon);
	}

	public void spendCoins(final long coins) {
		this.coinsSpend = safelyAdd(this.coinsSpend, coins);
		this.coins -= coins;
	}

	public void receiveCoins(final long coins) {
		this.coinsReceived = safelyAdd(this.coinsReceived, coins);
		this.coins = safelyAdd(this.coins, coins);
	}

	public void giveCoins(final long coins) {
		this.coins = safelyAdd(this.coins, coins);
	}

	public void removeCoins(final long coins) {
		this.coins -= coins;
	}
}
