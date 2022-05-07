package com.motorbesitzen.gamblebot.data.dao;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
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

	public CoinShopOffer(String name, long price, DiscordGuild guild) {
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
