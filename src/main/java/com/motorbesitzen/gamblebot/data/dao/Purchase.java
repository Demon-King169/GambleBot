package com.motorbesitzen.gamblebot.data.dao;

import javax.persistence.*;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Entity
public class Purchase {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private long purchaseId;

	@Min(1)
	private long price;

	@NotNull
	@NotBlank
	private String product;

	@ManyToOne
	@JoinColumn(name = "buyerId")
	private DiscordMember buyer;

	@ManyToOne
	@JoinColumn(name = "guildId")
	private DiscordGuild guild;

	protected Purchase() {
	}

	public Purchase(long price, String product, DiscordMember buyer, DiscordGuild guild) {
		this.price = price;
		this.product = product;
		this.buyer = buyer;
		this.guild = guild;
	}

	public long getPrice() {
		return price;
	}

	public String getProduct() {
		return product;
	}

	public DiscordMember getBuyer() {
		return buyer;
	}

	public DiscordGuild getGuild() {
		return guild;
	}
}
