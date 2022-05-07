package com.motorbesitzen.gamblebot.bot.command.impl.general;

import com.motorbesitzen.gamblebot.bot.command.CommandImpl;
import com.motorbesitzen.gamblebot.data.dao.DiscordGuild;
import com.motorbesitzen.gamblebot.data.repo.DiscordGuildRepo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Sends an info message about specific guild settings.
 */
@Service("info")
class Info extends CommandImpl {

	private final DiscordGuildRepo guildRepo;

	@Autowired
	private Info(DiscordGuildRepo guildRepo) {
		this.guildRepo = guildRepo;
	}

	@Override
	public String getName() {
		return "info";
	}

	@Override
	public String getDescription() {
		return "Shows some information about the guild settings.";
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
		jda.upsertCommand(getName(), getDescription()).queue();
	}

	@Override
	public void execute(SlashCommandEvent event) {
		Guild guild = event.getGuild();
		if (guild == null) {
			return;
		}

		long guildId = guild.getIdLong();
		Optional<DiscordGuild> dcGuildOpt = guildRepo.findById(guildId);
		DiscordGuild dcGuild = dcGuildOpt.orElseGet(() -> DiscordGuild.withGuildId(guildId));
		MessageEmbed infoEmbed = buildInfoEmbed(dcGuild, guild);
		reply(event, infoEmbed);
	}

	private MessageEmbed buildInfoEmbed(DiscordGuild dcGuild, Guild guild) {
		TextChannel logChannel = guild.getTextChannelById(dcGuild.getLogChannelId());
		TextChannel coinChannel = guild.getTextChannelById(dcGuild.getCoinChannelId());
		String logChannelMention = logChannel == null ? "-" : logChannel.getAsMention();
		String coinChannelMention = coinChannel == null ? "-" : coinChannel.getAsMention();
		long dailyCoinsAmount = dcGuild.getDailyCoins();
		long boosterDailyCoinsAmount = dcGuild.getBoosterDailyBonus();
		int taxRate = (int) (dcGuild.getTaxRate() * 100);
		EmbedBuilder eb = new EmbedBuilder();
		eb.setTitle("Info for \"" + guild.getName() + "\":")
				.addField("Log channel:", logChannelMention, true)
				.addField("Coin channel:", coinChannelMention, true)
				.addBlankField(true)
				.addField("Daily coins:", dailyCoinsAmount + " coins", true)
				.addField("Booster daily bonus:", boosterDailyCoinsAmount + " coins", true)
				.addField("Tax rate:", taxRate + "%", true)
				.setFooter("Coin related commands can be used everywhere if no coin channel is set!");
		return eb.build();
	}
}
