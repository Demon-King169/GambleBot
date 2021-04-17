package com.motorbesitzen.gamblebot.bot.command.impl.coin;

import com.motorbesitzen.gamblebot.bot.command.CommandImpl;
import com.motorbesitzen.gamblebot.data.dao.DiscordMember;
import com.motorbesitzen.gamblebot.data.repo.DiscordMemberRepo;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service("coins")
public class CoinInfo extends CommandImpl {

	private final DiscordMemberRepo memberRepo;

	@Autowired
	private CoinInfo(final DiscordMemberRepo memberRepo) {
		this.memberRepo = memberRepo;
	}

	@Override
	public String getName() {
		return "coins";
	}

	@Override
	public String getUsage() {
		return getName();
	}

	@Override
	public String getDescription() {
		return "Displays the amount of coins you currently own.";
	}

	@Override
	public boolean isAdminCommand() {
		return false;
	}

	@Override
	public void execute(final GuildMessageReceivedEvent event) {
		final Member author = event.getMember();
		if (author == null) {
			return;
		}

		final long guildId = event.getGuild().getIdLong();
		final long authorId = author.getIdLong();
		final Optional<DiscordMember> dcMemberOpt = memberRepo.findByDiscordIdAndGuild_GuildId(authorId, guildId);
		dcMemberOpt.ifPresentOrElse(
				dcMember -> reply(event.getMessage(), "You own **" + dcMember.getCoins() + "** coins."),
				() -> sendErrorMessage(event.getChannel(), "You do not own any coins at the moment.")
		);
	}
}
