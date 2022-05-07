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
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service("taxrate")
class TaxRate extends CommandImpl {

	private static final String TAX_OPTION_NAME = "tax_percent";

	private final DiscordGuildRepo guildRepo;

	private TaxRate(final DiscordGuildRepo guildRepo) {
		this.guildRepo = guildRepo;
	}

	@Override
	public String getName() {
		return "taxrate";
	}

	@Override
	public String getDescription() {
		return "Sets a tax rate for each payout in percent.";
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
								TAX_OPTION_NAME,
								"The tax in percent (0-100)."
						).setRequiredRange(0, 100)
				).queue();
	}

	@Override
	public void execute(SlashCommandEvent event) {
		final Guild guild = event.getGuild();
		if (guild == null) {
			return;
		}

		Long taxRate = SlashOptionUtil.getIntegerOption(event, TAX_OPTION_NAME);
		if (taxRate == null) {
			taxRate = -1L;
		}

		if (taxRate < 0 || taxRate > 100) {
			reply(event, "Please set a valid tax percentage (0-100)!");
			return;
		}

		final long guildId = event.getGuild().getIdLong();
		final Optional<DiscordGuild> dcGuildOpt = guildRepo.findById(guildId);
		final DiscordGuild dcGuild = dcGuildOpt.orElseGet(() -> DiscordGuild.withGuildId(guildId));
		dcGuild.setTaxRate(taxRate.intValue());
		guildRepo.save(dcGuild);
		reply(event, "Set the tax rate to **" + taxRate + "**%.");
	}
}
