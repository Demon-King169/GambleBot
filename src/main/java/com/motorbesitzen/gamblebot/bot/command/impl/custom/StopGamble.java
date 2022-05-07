package com.motorbesitzen.gamblebot.bot.command.impl.custom;

import com.motorbesitzen.gamblebot.bot.command.CommandImpl;
import com.motorbesitzen.gamblebot.data.dao.DiscordGuild;
import com.motorbesitzen.gamblebot.data.dao.GambleSettings;
import com.motorbesitzen.gamblebot.data.repo.DiscordGuildRepo;
import com.motorbesitzen.gamblebot.data.repo.GambleSettingsRepo;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Stops the currently running gamble of a guild if there is one.
 */
@Service("stopgamble")
public class StopGamble extends CommandImpl {

	private final DiscordGuildRepo guildRepo;
	private final GambleSettingsRepo settingsRepo;

	@Autowired
	private StopGamble(DiscordGuildRepo guildRepo, GambleSettingsRepo settingsRepo) {
		this.guildRepo = guildRepo;
		this.settingsRepo = settingsRepo;
	}

	@Override
	public String getName() {
		return "stopgamble";
	}

	@Override
	public String getDescription() {
		return "Stops the currently running gamble.";
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
		dcGuildOpt.ifPresentOrElse(
				dcGuild -> {
					if (!dcGuild.hasRunningGamble()) {
						reply(event, "There is no running gamble.");
						return;
					}

					GambleSettings settings = dcGuild.getGambleSettings();
					settings.setStartTimestampMs(System.currentTimeMillis() - settings.getDurationMs());
					settingsRepo.save(settings);
					reply(event, "Stopped the gamble.");
				},
				() -> reply(event, "There is no running gamble.")
		);
	}
}
