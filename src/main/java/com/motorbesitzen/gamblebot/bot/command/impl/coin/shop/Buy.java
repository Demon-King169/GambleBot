package com.motorbesitzen.gamblebot.bot.command.impl.coin.shop;

import com.motorbesitzen.gamblebot.bot.command.CommandImpl;
import com.motorbesitzen.gamblebot.data.dao.CoinShopOffer;
import com.motorbesitzen.gamblebot.data.dao.DiscordMember;
import com.motorbesitzen.gamblebot.data.repo.CoinShopOfferRepo;
import com.motorbesitzen.gamblebot.data.repo.DiscordMemberRepo;
import com.motorbesitzen.gamblebot.util.ParseUtil;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service("buy")
public class Buy extends CommandImpl {

	private final DiscordMemberRepo memberRepo;
	private final CoinShopOfferRepo offerRepo;

	@Autowired
	private Buy(final DiscordMemberRepo memberRepo, final CoinShopOfferRepo offerRepo) {
		this.memberRepo = memberRepo;
		this.offerRepo = offerRepo;
	}

	@Override
	public String getName() {
		return "buy";
	}

	@Override
	public String getUsage() {
		return getName() + " <shopID>";
	}

	@Override
	public String getDescription() {
		return "Buy a product in the shop with your coins.";
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
		final long authorId = event.getAuthor().getIdLong();
		final long guildId = event.getGuild().getIdLong();
		final Optional<DiscordMember> dcAuthorOpt = memberRepo.findByDiscordIdAndGuild_GuildId(authorId, guildId);
		if (dcAuthorOpt.isEmpty()) {
			sendErrorMessage(event.getChannel(), "You do not have any coins!");
			return;
		}

		final Message message = event.getMessage();
		final String content = message.getContentRaw();
		final String[] tokens = content.split(" ");
		final String shopIdText = tokens[tokens.length - 1];
		final int shopId = ParseUtil.safelyParseStringToInt(shopIdText);
		if(shopId <= 0) {
			sendErrorMessage(event.getChannel(), "Please use a valid ID! Check the shop for a list of IDs.");
			return;
		}

		final DiscordMember dcAuthor = dcAuthorOpt.get();
		final List<CoinShopOffer> offers = offerRepo.findCoinShopOffersByGuild_GuildIdOrderByPriceAsc(guildId);
		final CoinShopOffer boughtOffer = offers.get(shopId - 1);
		if(dcAuthor.getCoins() < boughtOffer.getPrice()) {
			sendErrorMessage(event.getChannel(), "You do not have enough coins for that!");
			return;
		}

		dcAuthor.spendCoins(boughtOffer.getPrice());
		memberRepo.save(dcAuthor);
		reply(message, "Thanks for your purchase! Your order will be completed as soon as possible.");
		final TextChannel logChannel = event.getGuild().getTextChannelById(dcAuthor.getGuild().getLogChannelId());
		sendLogMessage(logChannel, "<@" + dcAuthor.getDiscordId() + "> bought \"" + boughtOffer.getName() +
				"\" for " + boughtOffer.getPrice() + " coins.");
	}
}
