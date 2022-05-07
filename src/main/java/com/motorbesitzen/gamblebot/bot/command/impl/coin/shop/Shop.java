package com.motorbesitzen.gamblebot.bot.command.impl.coin.shop;

import com.motorbesitzen.gamblebot.bot.command.CommandImpl;
import com.motorbesitzen.gamblebot.data.dao.CoinShopOffer;
import com.motorbesitzen.gamblebot.data.repo.CoinShopOfferRepo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("shop")
public class Shop extends CommandImpl {

	private final CoinShopOfferRepo offerRepo;

	@Autowired
	private Shop(final CoinShopOfferRepo offerRepo) {
		this.offerRepo = offerRepo;
	}

	@Override
	public String getName() {
		return "shop";
	}

	@Override
	public String getDescription() {
		return "Shows the current offers of the shop.";
	}

	@Override
	public boolean isAdminCommand() {
		return false;
	}

	@Override
	public boolean isGlobalCommand() {
		return false;
	}

	@Override
	public void register(JDA jda) {
		jda.upsertCommand(getName(), getDescription()).queue();
	}

	@Override
	public void execute(SlashCommandEvent event) {
		final Guild guild = event.getGuild();
		if (guild == null) {
			return;
		}

		final long guildId = event.getGuild().getIdLong();
		final List<CoinShopOffer> offers = offerRepo.findCoinShopOffersByGuild_GuildIdOrderByPriceAsc(guildId);
		if (offers.size() == 0) {
			reply(event, "There are no offers in your guild shop yet!");
			return;
		}

		final MessageEmbed embed = buildEmbed(offers);
		reply(event, embed);
	}

	private MessageEmbed buildEmbed(final List<CoinShopOffer> offers) {
		final EmbedBuilder eb = new EmbedBuilder();
		eb.setTitle(":coin: Coin Shop :coin:")
				.setFooter("Use \"/buy <id>\" to buy something from the shop.");
		for (int i = 0; i < Math.min(25, offers.size()); i++) {
			final CoinShopOffer offer = offers.get(i);
			eb.addField("[" + (i + 1) + "] " + offer.getName() + ":", "**" + offer.getPrice() + "** coins", false);
		}
		return eb.build();
	}
}
