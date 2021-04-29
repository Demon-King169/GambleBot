package com.motorbesitzen.gamblebot.bot.command.impl.coin.shop;

import com.motorbesitzen.gamblebot.bot.command.CommandImpl;
import com.motorbesitzen.gamblebot.data.dao.CoinShopOffer;
import com.motorbesitzen.gamblebot.data.repo.CoinShopOfferRepo;
import com.motorbesitzen.gamblebot.util.EnvironmentUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
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
	public String getUsage() {
		return getName();
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
	public void execute(final GuildMessageReceivedEvent event) {
		final long guildId = event.getGuild().getIdLong();
		final List<CoinShopOffer> offers = offerRepo.findCoinShopOffersByGuild_GuildIdOrderByPriceAsc(guildId);
		if (offers.size() == 0) {
			sendErrorMessage(event.getChannel(), "There are no offers in your guild shop yet!");
			return;
		}

		final MessageEmbed embed = buildEmbed(offers);
		answer(event.getChannel(), embed);
	}

	private MessageEmbed buildEmbed(final List<CoinShopOffer> offers) {
		final String prefix = EnvironmentUtil.getEnvironmentVariable("CMD_PREFIX");
		final EmbedBuilder eb = new EmbedBuilder();
		eb.setTitle(":coin: Coin Shop :coin:")
				.setFooter("Use \"" + prefix + "buy <id>\" to buy something from the shop.");
		for (int i = 0; i < Math.min(25, offers.size()); i++) {
			final CoinShopOffer offer = offers.get(i);
			eb.addField("[" + (i + 1) + "] " + offer.getName() + ":", "**" + offer.getPrice() + "** coins", true);
		}
		return eb.build();
	}
}
