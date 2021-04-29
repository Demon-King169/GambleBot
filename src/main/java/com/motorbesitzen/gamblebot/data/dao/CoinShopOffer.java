package com.motorbesitzen.gamblebot.data.dao;

import javax.persistence.*;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Entity
public class CoinShopOffer {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private long offerId;

	@NotNull
	@NotBlank
	private String name;

	@NotNull
	@Min(1)
	private long price;

	@ManyToOne
	@JoinColumn(name = "guildId")
	private DiscordGuild guild;

	protected CoinShopOffer() {
	}

	public CoinShopOffer(final String name, final long price, final DiscordGuild guild) {
		this.name = name;
		this.price = price;
		this.guild = guild;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public long getPrice() {
		return price;
	}

	public void setPrice(long price) {
		this.price = price;
	}

	public DiscordGuild getGuild() {
		return guild;
	}

	public void setGuild(DiscordGuild guild) {
		this.guild = guild;
	}
}
