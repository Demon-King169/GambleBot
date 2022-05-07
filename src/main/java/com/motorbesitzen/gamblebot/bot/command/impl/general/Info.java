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
	private Info(final DiscordGuildRepo guildRepo) {
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
	public void register(final JDA jda) {
		jda.upsertCommand(getName(), getDescription()).queue();
	}

	@Override
	public void execute(SlashCommandEvent event) {
		final Guild guild = event.getGuild();
		if (guild == null) {
			return;
		}

		final long guildId = guild.getIdLong();
		final Optional<DiscordGuild> dcGuildOpt = guildRepo.findById(guildId);
		final DiscordGuild dcGuild = dcGuildOpt.orElseGet(() -> DiscordGuild.withGuildId(guildId));
		final MessageEmbed infoEmbed = buildInfoEmbed(dcGuild, guild);
		reply(event, infoEmbed);
	}

	private MessageEmbed buildInfoEmbed(final DiscordGuild dcGuild, final Guild guild) {
		final TextChannel logChannel = guild.getTextChannelById(dcGuild.getLogChannelId());
		final TextChannel coinChannel = guild.getTextChannelById(dcGuild.getCoinChannelId());
		final String logChannelMention = logChannel == null ? "-" : logChannel.getAsMention();
		final String coinChannelMention = coinChannel == null ? "-" : coinChannel.getAsMention();
		final long dailyCoinsAmount = dcGuild.getDailyCoins();
		final long boosterDailyCoinsAmount = dcGuild.getBoosterDailyBonus();
		final int taxRate = (int) (dcGuild.getTaxRate() * 100);
		final EmbedBuilder eb = new EmbedBuilder();
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
