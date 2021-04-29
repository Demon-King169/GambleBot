package com.motorbesitzen.gamblebot.bot.command.impl.coin.shop;

import com.motorbesitzen.gamblebot.bot.command.CommandImpl;
import com.motorbesitzen.gamblebot.data.dao.CoinShopOffer;
import com.motorbesitzen.gamblebot.data.dao.DiscordGuild;
import com.motorbesitzen.gamblebot.data.repo.CoinShopOfferRepo;
import com.motorbesitzen.gamblebot.data.repo.DiscordGuildRepo;
import com.motorbesitzen.gamblebot.util.DiscordMessageUtil;
import com.motorbesitzen.gamblebot.util.EnvironmentUtil;
import com.motorbesitzen.gamblebot.util.ParseUtil;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;

@Service("addoffer")
public class AddShopOffer extends CommandImpl {

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
	public String getUsage() {
		return getName() + " \"name\" <prizeInCoins>";
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
	public void execute(final GuildMessageReceivedEvent event) {
		final Message message = event.getMessage();
		final String content = message.getContentRaw();
		final String prefix = EnvironmentUtil.getEnvironmentVariable("CMD_PREFIX");
		if (!content.matches("(?i)" + prefix + getName() + " \".*\" [0-9]+[kmb]?")) {
			sendErrorMessage(event.getChannel(), "Please use the correct syntax! Use `" +
					prefix + "help` for a list of valid bets.");
			return;
		}

		final List<String> offerNames = DiscordMessageUtil.getStringsInQuotationMarks(content);
		if(offerNames.size() != 1) {
			sendErrorMessage(event.getChannel(), "Please use the correct syntax! Use `" +
					prefix + "help` for a list of valid bets.");
			return;
		}

		final String offerName = offerNames.get(0);
		final String[] tokens = content.split(" ");
		final String offerPriceText = tokens[tokens.length - 1];
		final long offerPrice = ParseUtil.safelyParseStringToLong(offerPriceText);
		final long guildId = event.getGuild().getIdLong();
		final DiscordGuild dcGuild = guildRepo.findById(guildId).orElseGet(() -> createNewGuild(guildId));
		if(dcGuild.getShopOffers() != null) {
			if(dcGuild.getShopOffers().size() >= 25) {
				sendErrorMessage(event.getChannel(), "You can only set 25 offers in your shop!");
				return;
			}
		}

		final CoinShopOffer offer = new CoinShopOffer(offerName, offerPrice, dcGuild);
		offerRepo.save(offer);
		answer(event.getChannel(), "Added offer to the shop!");
	}

	private DiscordGuild createNewGuild(final long guildId) {
		final DiscordGuild dcGuild = DiscordGuild.withGuildId(guildId);
		guildRepo.save(dcGuild);
		return dcGuild;
	}
}
