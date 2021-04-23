package com.motorbesitzen.gamblebot.bot.command.impl.custom;

import com.motorbesitzen.gamblebot.bot.command.CommandImpl;
import com.motorbesitzen.gamblebot.data.dao.DiscordGuild;
import com.motorbesitzen.gamblebot.data.repo.DiscordGuildRepo;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Prints information about the current gamble. If there is none running it prints the information about the last gamble.
 */
@Service("gambleinfo")
class GambleInfo extends CommandImpl {

	private final DiscordGuildRepo guildRepo;

	@Autowired
	GambleInfo(final DiscordGuildRepo guildRepo) {
		this.guildRepo = guildRepo;
	}

	@Override
	public String getName() {
		return "gambleinfo";
	}

	@Override
	public String getUsage() {
		return getName();
	}

	@Override
	public String getDescription() {
		return "Prints information about the current giveaway. If none is running it prints information about the last one.";
	}

	@Override
	public boolean isAdminCommand() {
		return true;
	}

	@Override
	public boolean isGlobalCommand() {
		return true;
	}

	@Transactional
	@Override
	public void execute(GuildMessageReceivedEvent event) {
		final long guildId = event.getGuild().getIdLong();
		final Optional<DiscordGuild> dcGuildOpt = guildRepo.findById(guildId);
		dcGuildOpt.ifPresentOrElse(
				dcGuild -> {
					if (!dcGuild.hasRunningGamble()) {
						sendErrorMessage(event.getChannel(), "There is no running gamble.");
						return;
					}

					answer(event.getChannel(), buildGambleInfoEmbed(dcGuild));
				},
				() -> sendErrorMessage(event.getChannel(), "There is no running gamble.")
		);
	}


}
