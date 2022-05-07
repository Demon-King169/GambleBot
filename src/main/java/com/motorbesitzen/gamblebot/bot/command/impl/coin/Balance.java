package com.motorbesitzen.gamblebot.bot.command.impl.coin;

import com.motorbesitzen.gamblebot.bot.command.CommandImpl;
import com.motorbesitzen.gamblebot.data.dao.DiscordMember;
import com.motorbesitzen.gamblebot.data.repo.DiscordMemberRepo;
import com.motorbesitzen.gamblebot.util.SlashOptionUtil;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Shows the coin balance of the user.
 */
@Service("balance")
class Balance extends CommandImpl {

	private static final String USER_OPTION_NAME = "member";

	private final DiscordMemberRepo memberRepo;

	@Autowired
	private Balance(final DiscordMemberRepo memberRepo) {
		this.memberRepo = memberRepo;
	}

	@Override
	public String getName() {
		return "balance";
	}

	@Override
	public String getDescription() {
		return "Displays the balance of a user. Shows your own balance if you do not include a user.";
	}

	@Override
	public boolean isAdminCommand() {
		return false;
	}

	@Override
	public boolean isGlobalCommand() {
		return false;
	}

	@Override
	public void register(JDA jda) {
		jda.upsertCommand(getName(), getDescription())
				.addOption(
						OptionType.USER,
						USER_OPTION_NAME,
						"The member you want to request the balance of. Do not provide a member to see your own balance."
				).queue();
	}

	@Override
	public void execute(SlashCommandEvent event) {
		User user = SlashOptionUtil.getUserOption(event, USER_OPTION_NAME);
		if (user == null) {
			displayOwnBalance(event);
			return;
		}

		displayUserBalance(event, user.getIdLong());
	}

	private void displayUserBalance(final SlashCommandEvent event, final long mentionedUserId) {
		final Guild guild = event.getGuild();
		if (guild == null) {
			return;
		}

		final long guildId = guild.getIdLong();
		final Optional<DiscordMember> dcMemberOpt = memberRepo.findByDiscordIdAndGuild_GuildId(mentionedUserId, guildId);
		dcMemberOpt.ifPresentOrElse(
				dcMember -> reply(event, "That user owns **" + dcMember.getCoins() + "** coins.", true),
				() -> reply(event, "That user does not have any coins.", true)
		);
	}

	private void displayOwnBalance(final SlashCommandEvent event) {
		final Guild guild = event.getGuild();
		if (guild == null) {
			return;
		}

		final User author = event.getUser();
		final long guildId = guild.getIdLong();
		final long authorId = author.getIdLong();
		final Optional<DiscordMember> dcMemberOpt = memberRepo.findByDiscordIdAndGuild_GuildId(authorId, guildId);
		dcMemberOpt.ifPresentOrElse(
				dcMember -> reply(event, "You own **" + dcMember.getCoins() + "** coins.", true),
				() -> reply(event, "You do not own any coins at the moment.", true)
		);
	}
}
