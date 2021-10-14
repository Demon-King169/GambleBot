package com.motorbesitzen.gamblebot.bot.command.impl.coin.shop;

import com.motorbesitzen.gamblebot.bot.command.CommandImpl;
import com.motorbesitzen.gamblebot.data.dao.Purchase;
import com.motorbesitzen.gamblebot.data.repo.PurchaseRepo;
import com.motorbesitzen.gamblebot.util.DiscordMessageUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("purchases")
class Purchases extends CommandImpl {

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
	public String getUsage() {
		return getName() + " (@user|id)";
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
	public void execute(final GuildMessageReceivedEvent event) {
		final Message message = event.getMessage();
		final long mentionedUserId = DiscordMessageUtil.getMentionedMemberId(message);
		if (mentionedUserId <= 0) {
			sendErrorMessage(event.getChannel(), "Please mention a user or a user ID to check the purchases of.");
			return;
		}

		sendPurchaseList(event, mentionedUserId);
	}

	private void sendPurchaseList(final GuildMessageReceivedEvent event, final long mentionedUserId) {
		final Guild guild = event.getGuild();
		final long guildId = guild.getIdLong();
		final PageRequest pageRequest = PageRequest.of(0, 25);
		final List<Purchase> purchases = purchaseRepo.findAllByGuild_GuildIdAndBuyer_DiscordIdOrderByPurchaseIdDesc(guildId, mentionedUserId, pageRequest);
		if (purchases.size() == 0) {
			sendErrorMessage(event.getChannel(), "That user does not have any purchases yet.");
			return;
		}

		final MessageEmbed purchaseEmbed = buildPurchaseEmbed(purchases);
		answer(event.getChannel(), purchaseEmbed);
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
