package com.motorbesitzen.gamblebot.bot.command.impl.coin.shop;

import com.motorbesitzen.gamblebot.bot.command.CommandImpl;
import com.motorbesitzen.gamblebot.data.dao.CoinShopOffer;
import com.motorbesitzen.gamblebot.data.dao.DiscordGuild;
import com.motorbesitzen.gamblebot.data.repo.CoinShopOfferRepo;
import com.motorbesitzen.gamblebot.data.repo.DiscordGuildRepo;
import com.motorbesitzen.gamblebot.util.SlashOptionUtil;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("addoffer")
public class AddShopOffer extends CommandImpl {

	private static final String OFFER_NAME_OPTION_NAME = "offer_name";
	private static final String PRIZE_OPTION_NAME = "prize";
	private static final int SHOP_SIZE = 25;

	private final DiscordGuildRepo guildRepo;
	private final CoinShopOfferRepo offerRepo;

	@Autowired
	private AddShopOffer(final DiscordGuildRepo guildRepo, final CoinShopOfferRepo offerRepo) {
		this.guildRepo = guildRepo;
		this.offerRepo = offerRepo;
	}

	@Override
	public String getName() {
		return "addoffer";
	}

	@Override
	public String getDescription() {
		return "Adds an offer to the shop.";
	}

	@Override
	public boolean isAdminCommand() {
		return true;
	}

	@Override
	public boolean isGlobalCommand() {
		return true;
	}

	@Override
	public void register(JDA jda) {
		jda.upsertCommand(getName(), getDescription())
				.addOptions(
						new OptionData(
								OptionType.INTEGER,
								PRIZE_OPTION_NAME,
								"The prize (in coins) of the product you want to add to the shop.",
								true
						).setRequiredRange(0, 9007199254740991L),
						new OptionData(
								OptionType.STRING,
								OFFER_NAME_OPTION_NAME,
								"The name of the product you want to add to the shop.",
								true
						)
				).queue();
	}

	@Override
	public void execute(SlashCommandEvent event) {
		Guild guild = event.getGuild();
		if (guild == null) {
			return;
		}

		long offerPrice = getOfferPrice(event);
		if (offerPrice <= 0) {
			reply(event, "Please use a valid price (>= 0).");
			return;
		}

		String offerName = getOfferName(event);
		if (offerName == null) {
			reply(event, "Please use a valid name!");
			return;
		}

		final long guildId = guild.getIdLong();
		final DiscordGuild dcGuild = guildRepo.findById(guildId).orElseGet(() -> createNewGuild(guildId));
		if (dcGuild.getShopOffers() != null) {
			if (dcGuild.getShopOffers().size() >= SHOP_SIZE) {
				reply(event, "You can only set " + SHOP_SIZE + " offers in your shop!");
				return;
			}
		}

		final CoinShopOffer offer = new CoinShopOffer(offerName, offerPrice, dcGuild);
		offerRepo.save(offer);
		reply(event, "Added offer to the shop!");
	}

	private long getOfferPrice(SlashCommandEvent event) {
		Long prize = SlashOptionUtil.getIntegerOption(event, PRIZE_OPTION_NAME);
		if (prize == null) {
			prize = -1L;
		}

		return prize;
	}

	private String getOfferName(SlashCommandEvent event) {
		String name = SlashOptionUtil.getStringOption(event, OFFER_NAME_OPTION_NAME);
		if (name == null) {
			return null;
		}

		if (name.isBlank()) {
			return null;
		}

		return name;
	}

	private DiscordGuild createNewGuild(final long guildId) {
		final DiscordGuild dcGuild = DiscordGuild.withGuildId(guildId);
		guildRepo.save(dcGuild);
		return dcGuild;
	}
}
