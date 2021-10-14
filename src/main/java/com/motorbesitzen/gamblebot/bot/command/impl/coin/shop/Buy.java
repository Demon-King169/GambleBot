package com.motorbesitzen.gamblebot.bot.command.impl.coin.shop;

import com.motorbesitzen.gamblebot.bot.command.CommandImpl;
import com.motorbesitzen.gamblebot.data.dao.CoinShopOffer;
import com.motorbesitzen.gamblebot.data.dao.DiscordGuild;
import com.motorbesitzen.gamblebot.data.dao.DiscordMember;
import com.motorbesitzen.gamblebot.data.dao.Purchase;
import com.motorbesitzen.gamblebot.data.repo.CoinShopOfferRepo;
import com.motorbesitzen.gamblebot.data.repo.DiscordGuildRepo;
import com.motorbesitzen.gamblebot.data.repo.DiscordMemberRepo;
import com.motorbesitzen.gamblebot.data.repo.PurchaseRepo;
import com.motorbesitzen.gamblebot.util.ParseUtil;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service("buy")
class Buy extends CommandImpl {

	private final DiscordGuildRepo guildRepo;
	private final DiscordMemberRepo memberRepo;
	private final CoinShopOfferRepo offerRepo;
	private final PurchaseRepo purchaseRepo;

	@Autowired
	private Buy(final DiscordGuildRepo guildRepo, final DiscordMemberRepo memberRepo,
				final CoinShopOfferRepo offerRepo, final PurchaseRepo purchaseRepo) {
		this.guildRepo = guildRepo;
		this.memberRepo = memberRepo;
		this.offerRepo = offerRepo;
		this.purchaseRepo = purchaseRepo;
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
		final Optional<DiscordGuild> dcGuildOpt = guildRepo.findById(guildId);
		final DiscordGuild dcGuild = dcGuildOpt.orElseGet(() -> createDiscordGuild(guildId));
		final Optional<DiscordMember> dcAuthorOpt = memberRepo.findByDiscordIdAndGuild_GuildId(authorId, guildId);
		if (dcAuthorOpt.isEmpty()) {
			replyErrorMessage(event.getMessage(), "You do not have any coins!");
			return;
		}

		final Message message = event.getMessage();
		final String content = message.getContentRaw();
		final String[] tokens = content.split(" ");
		final String shopIdText = tokens[tokens.length - 1];
		final int shopId = ParseUtil.safelyParseStringToInt(shopIdText) - 1; // adjust for index
		if(shopId < 0) {
			replyErrorMessage(event.getMessage(), "Please use a valid ID! Check the shop for a list of IDs.");
			return;
		}

		final DiscordMember dcAuthor = dcAuthorOpt.get();
		final List<CoinShopOffer> offers = offerRepo.findCoinShopOffersByGuild_GuildIdOrderByPriceAsc(guildId);
		if(shopId >= offers.size()) {
			replyErrorMessage(event.getMessage(), "Please use a valid ID! Check the shop for a list of IDs.");
			return;
		}

		final CoinShopOffer boughtOffer = offers.get(shopId);
		if(dcAuthor.getCoins() < boughtOffer.getPrice()) {
			replyErrorMessage(event.getMessage(), "You do not have enough coins for that!\n" +
					"You only have **" + dcAuthor.getCoins() + "** coins right now.");
			return;
		}

		final Purchase purchase = new Purchase(boughtOffer.getPrice(), boughtOffer.getName(), dcAuthor, dcGuild);
		purchaseRepo.save(purchase);
		dcAuthor.spendCoins(boughtOffer.getPrice());
		memberRepo.save(dcAuthor);
		reply(message, "Thanks for your purchase! Your order will be completed as soon as possible.");
		final TextChannel logChannel = event.getGuild().getTextChannelById(dcAuthor.getGuild().getLogChannelId());
		sendLogMessage(logChannel, "<@" + dcAuthor.getDiscordId() + "> bought \"" + boughtOffer.getName() +
				"\" for " + boughtOffer.getPrice() + " coins.");
	}

	private DiscordGuild createDiscordGuild(final long guildId) {
		final DiscordGuild dcGuild = DiscordGuild.withGuildId(guildId);
		guildRepo.save(dcGuild);
		return dcGuild;
	}
}
