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
import com.motorbesitzen.gamblebot.util.SlashOptionUtil;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service("buy")
class Buy extends CommandImpl {

	private static final String SHOP_OPTION_NAME = "shop_id";

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
	public void register(JDA jda) {
		jda.upsertCommand(getName(), getDescription())
				.addOptions(
						new OptionData(
								OptionType.INTEGER,
								SHOP_OPTION_NAME,
								"The ID of the product you want to buy in the shop.",
								true
						).setRequiredRange(1, 25)
				).queue();
	}

	@Override
	public void execute(SlashCommandEvent event) {
		Guild guild = event.getGuild();
		if (guild == null) {
			return;
		}

		final long guildId = event.getGuild().getIdLong();
		final long authorId = event.getUser().getIdLong();
		final Optional<DiscordGuild> dcGuildOpt = guildRepo.findById(guildId);
		final DiscordGuild dcGuild = dcGuildOpt.orElseGet(() -> createDiscordGuild(guildId));
		final Optional<DiscordMember> dcAuthorOpt = memberRepo.findByDiscordIdAndGuild_GuildId(authorId, guildId);
		if (dcAuthorOpt.isEmpty()) {
			reply(event, "You do not have any coins!");
			return;
		}

		Long shopId = SlashOptionUtil.getIntegerOption(event, SHOP_OPTION_NAME);
		if (shopId == null) {
			shopId = -1L;
		}

		if (shopId <= 0 || shopId >= Integer.MAX_VALUE) {
			reply(event, "Please use a valid ID! Check the shop for a list of IDs.");
			return;
		}

		int shopIndex = shopId.intValue() - 1; // IDs in the shop start with 1, but we want 0 as first index
		final DiscordMember dcAuthor = dcAuthorOpt.get();
		final List<CoinShopOffer> offers = offerRepo.findCoinShopOffersByGuild_GuildIdOrderByPriceAsc(guildId);
		if (shopIndex >= offers.size()) {
			reply(event, "Please use a valid ID! Check the shop for a list of IDs.");
			return;
		}

		final CoinShopOffer boughtOffer = offers.get(shopIndex);
		if (dcAuthor.getCoins() < boughtOffer.getPrice()) {
			reply(event, "You do not have enough coins for that!\n" +
					"You only have **" + dcAuthor.getCoins() + "** coins right now.");
			return;
		}

		final Purchase purchase = new Purchase(boughtOffer.getPrice(), boughtOffer.getName(), dcAuthor, dcGuild);
		purchaseRepo.save(purchase);
		dcAuthor.spendCoins(boughtOffer.getPrice());
		memberRepo.save(dcAuthor);
		reply(event, "Thanks for your purchase! Your order will be completed as soon as possible.");
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
