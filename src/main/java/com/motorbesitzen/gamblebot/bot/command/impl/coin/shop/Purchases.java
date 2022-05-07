package com.motorbesitzen.gamblebot.bot.command.impl.coin.shop;

import com.motorbesitzen.gamblebot.bot.command.CommandImpl;
import com.motorbesitzen.gamblebot.data.dao.Purchase;
import com.motorbesitzen.gamblebot.data.repo.PurchaseRepo;
import com.motorbesitzen.gamblebot.util.SlashOptionUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("purchases")
class Purchases extends CommandImpl {

	private static final String USER_OPTION_NAME = "member";

	private final PurchaseRepo purchaseRepo;

	@Autowired
	private Purchases(final PurchaseRepo purchaseRepo) {
		this.purchaseRepo = purchaseRepo;
	}

	@Override
	public String getName() {
		return "purchases";
	}

	@Override
	public String getDescription() {
		return "Displays the last purchases of a user.";
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
				.addOption(
						OptionType.USER,
						USER_OPTION_NAME,
						"The member you want to request the balance of.",
						true
				).queue();
	}

	@Override
	public void execute(SlashCommandEvent event) {
		User user = SlashOptionUtil.getUserOption(event, USER_OPTION_NAME);
		if (user == null) {
			reply(event, "Please mention a user to check the purchases of.");
			return;
		}

		sendPurchaseList(event, user.getIdLong());
	}

	private void sendPurchaseList(final SlashCommandEvent event, final long mentionedUserId) {
		final Guild guild = event.getGuild();
		if (guild == null) {
			return;
		}

		final long guildId = guild.getIdLong();
		final PageRequest pageRequest = PageRequest.of(0, 25);
		final List<Purchase> purchases = purchaseRepo.findAllByGuild_GuildIdAndBuyer_DiscordIdOrderByPurchaseIdDesc(guildId, mentionedUserId, pageRequest);
		if (purchases.size() == 0) {
			reply(event, "That user does not have any purchases yet.");
			return;
		}

		final MessageEmbed purchaseEmbed = buildPurchaseEmbed(purchases);
		reply(event, purchaseEmbed);
	}

	private MessageEmbed buildPurchaseEmbed(final List<Purchase> purchases) {
		final EmbedBuilder eb = new EmbedBuilder();
		eb.setTitle("Last " + purchases.size() + " purchases (newest to oldest):");
		for (Purchase purchase : purchases) {
			eb.addField(purchase.getProduct(), "for " + purchase.getPrice() + " coins", true);
		}

		return eb.build();
	}
}
