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

import java.util.List;
import java.util.Optional;

@Service("deloffer")
public class DelShopOffer extends CommandImpl {

	private static final String SHOP_OPTION_NAME = "shop_id";

	private final DiscordGuildRepo guildRepo;
	private final CoinShopOfferRepo offerRepo;

	@Autowired
	private DelShopOffer(DiscordGuildRepo guildRepo, CoinShopOfferRepo offerRepo) {
		this.guildRepo = guildRepo;
		this.offerRepo = offerRepo;
	}

	@Override
	public String getName() {
		return "deloffer";
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
	public void register(JDA jda) {
		jda.upsertCommand(getName(), getDescription())
				.addOptions(
						new OptionData(
								OptionType.INTEGER,
								SHOP_OPTION_NAME,
								"The ID of the product you want to delete from the shop.",
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

		Long shopId = SlashOptionUtil.getIntegerOption(event, SHOP_OPTION_NAME);
		if (shopId == null) {
			shopId = -1L;
		}

		if (shopId <= 0 || shopId >= Integer.MAX_VALUE) {
			reply(event, "Please use a valid ID! Check the shop for a list of IDs.");
			return;
		}

		int shopIndex = shopId.intValue() - 1; // IDs in the shop start with 1, but we want 0 as first index
		long guildId = event.getGuild().getIdLong();
		Optional<DiscordGuild> dcGuildOpt = guildRepo.findById(guildId);
		dcGuildOpt.ifPresentOrElse(
				dcGuild -> {
					List<CoinShopOffer> offers = offerRepo.findCoinShopOffersByGuild_GuildIdOrderByPriceAsc(guildId);
					CoinShopOffer delOffer = offers.get(shopIndex);
					delOffer.setGuild(null);    // removing link to guild, otherwise cant delete
					offerRepo.save(delOffer);
					offerRepo.delete(delOffer);
					reply(event, "Removed offer from shop!");
				},
				() -> reply(event, "There is no shop for your guild yet!")
		);
	}
}