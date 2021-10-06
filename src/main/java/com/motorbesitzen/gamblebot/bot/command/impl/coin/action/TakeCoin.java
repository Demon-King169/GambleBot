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
 * Take user coins and delete them.
 */
@Service("take")
class TakeCoin extends CommandImpl {

	private final DiscordMemberRepo memberRepo;
	private final DiscordGuildRepo guildRepo;

	@Autowired
	private TakeCoin(final DiscordMemberRepo memberRepo, final DiscordGuildRepo guildRepo) {
		this.memberRepo = memberRepo;
		this.guildRepo = guildRepo;
	}

	@Override
	public String getName() {
		return "take";
	}

	@Override
	public String getUsage() {
		return getName() + " (@user|userid) <coins>";
	}

	@Override
	public String getDescription() {
		return "Takes an amount of coins from a given user.";
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
		final Message message = event.getMessage();
		final long userId = DiscordMessageUtil.getMentionedMemberId(message);
		if (userId <= 100000000000000L) {
			replyErrorMessage(event.getMessage(), "That user seems to be invalid!");
			return;
		}

		takeCoins(event, userId);
	}

	private void takeCoins(final GuildMessageReceivedEvent event, final long userId) {
		final long guildId = event.getGuild().getIdLong();
		final Optional<DiscordMember> dcMemberOpt = memberRepo.findByDiscordIdAndGuild_GuildId(userId, guildId);
		final DiscordMember dcMember = dcMemberOpt.orElseGet(() -> createNewMember(userId, guildId));
		final String content = event.getMessage().getContentRaw();
		final String[] tokens = content.split(" ");
		final String coinText = tokens[tokens.length - 1];
		final long coinAmount = ParseUtil.safelyParseStringToLong(coinText);
		if (coinAmount < 1) {
			replyErrorMessage(event.getMessage(), "Please set a valid coin amount (>= 1)!");
			return;
		}

		if(coinAmount > dcMember.getCoins()) {
			replyErrorMessage(event.getMessage(), "That user does not have that many coins. <@" +
					dcMember.getDiscordId() + "> balance: " + dcMember.getCoins());
			return;
		}

		dcMember.removeCoins(coinAmount);
		memberRepo.save(dcMember);
		answerNoPing(event.getChannel(), "Took **" + coinAmount + "** coins from their balance.");
		LogUtil.logDebug(event.getAuthor().getIdLong() + " took " + coinAmount + " coins from " + dcMember.getDiscordId());
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
