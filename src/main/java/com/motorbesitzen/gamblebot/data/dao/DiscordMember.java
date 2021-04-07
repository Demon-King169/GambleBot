package com.motorbesitzen.gamblebot.data.dao;

import com.motorbesitzen.gamblebot.util.ParseUtil;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Entity
public class DiscordMember {

	@Id
	@Min(value = 10000000000000000L)
	private long discordId;

	@Min(0)
	private long nextGambleMs;

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
}
