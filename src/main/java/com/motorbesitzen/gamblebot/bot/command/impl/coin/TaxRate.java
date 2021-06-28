package com.motorbesitzen.gamblebot.bot.command.impl.coin;

import com.motorbesitzen.gamblebot.bot.command.CommandImpl;
import com.motorbesitzen.gamblebot.data.dao.DiscordGuild;
import com.motorbesitzen.gamblebot.data.repo.DiscordGuildRepo;
import com.motorbesitzen.gamblebot.util.ParseUtil;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service("taxrate")
class TaxRate extends CommandImpl {

	private final DiscordGuildRepo guildRepo;

	private TaxRate(final DiscordGuildRepo guildRepo) {
		this.guildRepo = guildRepo;
	}

	@Override
	public String getName() {
		return "taxrate";
	}

	@Override
	public String getUsage() {
		return getName() + " <rateInPercent>";
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
	public void execute(final GuildMessageReceivedEvent event) {
		final long guildId = event.getGuild().getIdLong();
		final Optional<DiscordGuild> dcGuildOpt = guildRepo.findById(guildId);
		final DiscordGuild dcGuild = dcGuildOpt.orElseGet(() -> DiscordGuild.withGuildId(guildId));
		final Message message = event.getMessage();
		final String content = message.getContentRaw();
		final String[] tokens = content.split(" ");
		final String taxRateText = tokens[tokens.length - 1];
		final int taxRate = ParseUtil.safelyParseStringToInt(taxRateText);
		System.out.println("TAX: " + dcGuild.getTaxRate());
		if (taxRate < 0 || taxRate > 100) {
			replyErrorMessage(event.getMessage(), "Please set a valid integer amount of daily coins (0-100)!");
			return;
		}

		dcGuild.setTaxRate(taxRate);
		guildRepo.save(dcGuild);
		answer(event.getChannel(), "Set the tax rate to **" + taxRate + "**%.");
	}
}
