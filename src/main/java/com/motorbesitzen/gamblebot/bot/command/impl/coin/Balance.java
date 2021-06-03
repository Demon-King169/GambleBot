package com.motorbesitzen.gamblebot.bot.command.impl.coin;

import com.motorbesitzen.gamblebot.bot.command.CommandImpl;
import com.motorbesitzen.gamblebot.data.dao.DiscordMember;
import com.motorbesitzen.gamblebot.data.repo.DiscordMemberRepo;
import com.motorbesitzen.gamblebot.util.DiscordMessageUtil;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Shows the coin balance of the user.
 */
@Service("balance")
class Balance extends CommandImpl {

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
	public String getUsage() {
		return getName() + " (@user|id)";
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
	public void execute(final GuildMessageReceivedEvent event) {
		final Message message = event.getMessage();
		final long mentionedUserId = DiscordMessageUtil.getMentionedMemberId(message);
		if (mentionedUserId <= 0) {
			displayOwnBalance(event);
			return;
		}

		displayUserBalance(event, mentionedUserId);
	}

	private void displayUserBalance(final GuildMessageReceivedEvent event, final long mentionedUserId) {
		final long guildId = event.getGuild().getIdLong();
		final Optional<DiscordMember> dcMemberOpt = memberRepo.findByDiscordIdAndGuild_GuildId(mentionedUserId, guildId);
		dcMemberOpt.ifPresentOrElse(
				dcMember -> reply(event.getMessage(), "That user owns **" + dcMember.getCoins() + "** coins."),
				() -> replyErrorMessage(event.getMessage(), "That user does not have any coins.")
		);
	}

	private void displayOwnBalance(final GuildMessageReceivedEvent event) {
		final User author = event.getAuthor();
		final long guildId = event.getGuild().getIdLong();
		final long authorId = author.getIdLong();
		final Optional<DiscordMember> dcMemberOpt = memberRepo.findByDiscordIdAndGuild_GuildId(authorId, guildId);
		dcMemberOpt.ifPresentOrElse(
				dcMember -> reply(event.getMessage(), "You own **" + dcMember.getCoins() + "** coins."),
				() -> sendErrorMessage(event.getChannel(), "You do not own any coins at the moment.")
		);
	}
}
