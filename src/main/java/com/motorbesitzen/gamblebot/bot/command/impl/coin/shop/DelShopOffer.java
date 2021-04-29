package com.motorbesitzen.gamblebot.bot.command.impl.coin.shop;

import com.motorbesitzen.gamblebot.bot.command.CommandImpl;
import com.motorbesitzen.gamblebot.data.dao.CoinShopOffer;
import com.motorbesitzen.gamblebot.data.dao.DiscordGuild;
import com.motorbesitzen.gamblebot.data.repo.CoinShopOfferRepo;
import com.motorbesitzen.gamblebot.data.repo.DiscordGuildRepo;
import com.motorbesitzen.gamblebot.util.ParseUtil;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service("deloffer")
public class DelShopOffer extends CommandImpl {

	private final DiscordGuildRepo guildRepo;
	private final CoinShopOfferRepo offerRepo;

	@Autowired
	private DelShopOffer(final DiscordGuildRepo guildRepo, final CoinShopOfferRepo offerRepo) {
		this.guildRepo = guildRepo;
		this.offerRepo = offerRepo;
	}

	@Override
	public String getName() {
		return "deloffer";
	}

	@Override
	public String getUsage() {
		return getName() + " <shopID>";
	}

	@Override
	public String getDescription() {
		return "Removes an offer from the shop.";
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
	public void execute(final GuildMessageReceivedEvent event) {
		final Message message = event.getMessage();
		final String content = message.getContentRaw();
		final String[] tokens = content.split(" ");
		final String shopIdText = tokens[tokens.length - 1];
		final int shopId = ParseUtil.safelyParseStringToInt(shopIdText);
		if(shopId <= 0) {
			sendErrorMessage(event.getChannel(), "Please use a valid ID! Check the shop for a list of IDs.");
			return;
		}

		final long guildId = event.getGuild().getIdLong();
		final Optional<DiscordGuild> dcGuildOpt = guildRepo.findById(guildId);
		dcGuildOpt.ifPresentOrElse(
				dcGuild -> {
					final List<CoinShopOffer> offers = offerRepo.findCoinShopOffersByGuild_GuildIdOrderByPriceAsc(guildId);
					final CoinShopOffer delOffer = offers.get(shopId - 1);
					delOffer.setGuild(null);	// removing link to guild, otherwise cant delete
					offerRepo.save(delOffer);
					offerRepo.delete(delOffer);
					answer(event.getChannel(), "Removed offer from shop!");
				},
				() -> sendErrorMessage(event.getChannel(), "There is no shop for your guild yet!")
		);
	}
}