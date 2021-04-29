package com.motorbesitzen.gamblebot.bot.command.impl.custom;

import com.motorbesitzen.gamblebot.bot.command.CommandImpl;
import com.motorbesitzen.gamblebot.data.dao.DiscordGuild;
import com.motorbesitzen.gamblebot.data.dao.GambleSettings;
import com.motorbesitzen.gamblebot.data.repo.DiscordGuildRepo;
import com.motorbesitzen.gamblebot.data.repo.GambleSettingsRepo;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
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
	private StopGamble(final DiscordGuildRepo guildRepo, final GambleSettingsRepo settingsRepo) {
		this.guildRepo = guildRepo;
		this.settingsRepo = settingsRepo;
	}

	@Override
	public String getName() {
		return "stopgamble";
	}

	@Override
	public String getUsage() {
		return getName();
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
	public void execute(final GuildMessageReceivedEvent event) {
		final long guildId = event.getGuild().getIdLong();
		final Optional<DiscordGuild> dcGuildOpt = guildRepo.findById(guildId);
		dcGuildOpt.ifPresentOrElse(
				dcGuild -> {
					if (!dcGuild.hasRunningGamble()) {
						sendErrorMessage(event.getChannel(), "There is no running gamble.");
						return;
					}

					final GambleSettings settings = dcGuild.getGambleSettings();
					settings.setStartTimestampMs(System.currentTimeMillis() - settings.getDurationMs());
					settingsRepo.save(settings);
					answer(event.getChannel(), "Stopped the gamble.");
				},
				() -> sendErrorMessage(event.getChannel(), "There is no running gamble.")
		);
	}
}
