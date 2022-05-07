package com.motorbesitzen.gamblebot.bot.command.impl.coin;

import com.motorbesitzen.gamblebot.bot.command.CommandImpl;
import com.motorbesitzen.gamblebot.data.dao.DiscordGuild;
import com.motorbesitzen.gamblebot.data.repo.DiscordGuildRepo;
import com.motorbesitzen.gamblebot.util.SlashOptionUtil;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Sets the amount of daily coins for the guild.
 */
@Service("setdaily")
class DailyCoinAmount extends CommandImpl {

	private static final String AMOUNT_OPTION_NAME = "amount";

	private final DiscordGuildRepo guildRepo;

	@Autowired
	private DailyCoinAmount(DiscordGuildRepo guildRepo) {
		this.guildRepo = guildRepo;
	}

	@Override
	public String getName() {
		return "setdaily";
	}

	@Override
	public String getDescription() {
		return "Sets the amount of coins a user can get each day.";
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
								AMOUNT_OPTION_NAME,
								"The amount of coins which the daily command should provide.",
								true
						).setMinValue(0)
				).queue();
	}

	@Override
	public void execute(SlashCommandEvent event) {
		Guild guild = event.getGuild();
		if (guild == null) {
			return;
		}

		Long coinAmount = SlashOptionUtil.getIntegerOption(event, AMOUNT_OPTION_NAME);
		if (coinAmount == null) {
			coinAmount = -1L;
		}

		if (coinAmount < 0) {
			reply(event, "Please set a valid amount of daily coins (>= 0)!");
			return;
		}

		long guildId = event.getGuild().getIdLong();
		Optional<DiscordGuild> dcGuildOpt = guildRepo.findById(guildId);
		DiscordGuild dcGuild = dcGuildOpt.orElseGet(() -> DiscordGuild.withGuildId(guildId));
		dcGuild.setDailyCoins(coinAmount);
		guildRepo.save(dcGuild);
		reply(event, "Set the daily coins amount to **" + coinAmount + "** coins.");
	}
}
