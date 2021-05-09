package com.motorbesitzen.gamblebot.bot.command.impl.coin.action;

import com.motorbesitzen.gamblebot.bot.command.CommandImpl;
import com.motorbesitzen.gamblebot.data.dao.DiscordGuild;
import com.motorbesitzen.gamblebot.data.dao.DiscordMember;
import com.motorbesitzen.gamblebot.data.repo.DiscordGuildRepo;
import com.motorbesitzen.gamblebot.data.repo.DiscordMemberRepo;
import com.motorbesitzen.gamblebot.util.DiscordMessageUtil;
import com.motorbesitzen.gamblebot.util.LogUtil;
import com.motorbesitzen.gamblebot.util.ParseUtil;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Pay an amount of coins to a user.
 */
@Service("pay")
class PayCoins extends CommandImpl {

	private final DiscordMemberRepo memberRepo;
	private final DiscordGuildRepo guildRepo;

	@Autowired
	private PayCoins(final DiscordMemberRepo memberRepo, final DiscordGuildRepo guildRepo) {
		this.memberRepo = memberRepo;
		this.guildRepo = guildRepo;
	}

	@Override
	public String getName() {
		return "pay";
	}

	@Override
	public String getUsage() {
		return getName() + " (@user|id) <amount>";
	}

	@Override
	public String getDescription() {
		return "Transfers the specified amount of coins to the mentioned user.";
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
		final long authorId = event.getAuthor().getIdLong();
		final long guildId = event.getGuild().getIdLong();
		final Optional<DiscordMember> dcAuthorOpt = memberRepo.findByDiscordIdAndGuild_GuildId(authorId, guildId);
		if (dcAuthorOpt.isEmpty()) {
			sendErrorMessage(event.getChannel(), "You do not have any coins!");
			return;
		}

		final Message message = event.getMessage();
		final long userId = DiscordMessageUtil.getMentionedMemberId(message);
		if (userId == authorId) {
			sendErrorMessage(event.getChannel(), "You can not pay coins to yourself!");
			return;
		}

		if (userId <= 100000000000000L) {
			sendErrorMessage(event.getChannel(), "That user seems to be invalid!");
			return;
		}

		event.getGuild().retrieveMemberById(userId).queue(
				member -> payCoins(event, dcAuthorOpt.get(), member),
				throwable -> sendErrorMessage(event.getChannel(), "That user is not in this guild!")
		);
	}

	private void payCoins(final GuildMessageReceivedEvent event, final DiscordMember author, final Member member) {
		final long guildId = event.getGuild().getIdLong();
		final Optional<DiscordMember> dcMemberOpt = memberRepo.findByDiscordIdAndGuild_GuildId(member.getIdLong(), guildId);
		final DiscordMember dcMember = dcMemberOpt.orElseGet(() -> createNewMember(member.getIdLong(), guildId));
		final String content = event.getMessage().getContentRaw();
		final String[] tokens = content.split(" ");
		final String coinText = tokens[tokens.length - 1];
		final long coinAmount = ParseUtil.safelyParseStringToLong(coinText);
		if (coinAmount < 1) {
			sendErrorMessage(event.getChannel(), "Please choose a coin amount of at least 1!");
			return;
		}

		final long authorCoins = author.getCoins();
		if (coinAmount > authorCoins) {
			final String errorMsg = authorCoins > 0 ?
					"Please set a valid coin amount (1 - " + dcMember.getCoins() + ")!" :
					"You do not have enough coins for that!\nYou only have **" + authorCoins + "** coins right now.";
			sendErrorMessage(event.getChannel(), errorMsg);
			return;
		}

		author.spendCoins(coinAmount);
		memberRepo.save(author);
		dcMember.receiveCoins(coinAmount);
		memberRepo.save(dcMember);
		answer(event.getChannel(), "Added **" + coinAmount + "** coins to the balance of " + member.getAsMention() + ".");
		LogUtil.logDebug(author.getDiscordId() + " paid " + coinAmount + " coins to " + dcMember.getDiscordId());
	}

	private DiscordMember createNewMember(final long memberId, final long guildId) {
		final Optional<DiscordGuild> dcGuildOpt = guildRepo.findById(guildId);
		final DiscordGuild dcGuild = dcGuildOpt.orElseGet(() -> createNewGuild(guildId));
		return DiscordMember.createDefault(memberId, dcGuild);
	}

	private DiscordGuild createNewGuild(final long guildId) {
		final DiscordGuild dcGuild = DiscordGuild.withGuildId(guildId);
		guildRepo.save(dcGuild);
		return dcGuild;
	}
}
