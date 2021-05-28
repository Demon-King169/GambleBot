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
 * Give a user coins without taking them from your balance.
 */
@Service("give")
class GiveCoin extends CommandImpl {

	private final DiscordMemberRepo memberRepo;
	private final DiscordGuildRepo guildRepo;

	@Autowired
	private GiveCoin(final DiscordMemberRepo memberRepo, final DiscordGuildRepo guildRepo) {
		this.memberRepo = memberRepo;
		this.guildRepo = guildRepo;
	}

	@Override
	public String getName() {
		return "give";
	}

	@Override
	public String getUsage() {
		return getName() + " (@user|userid) <coins>";
	}

	@Override
	public String getDescription() {
		return "Adds an amount of coins to a given user.";
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
			sendErrorMessage(event.getChannel(), "That user seems to be invalid!");
			return;
		}

		event.getGuild().retrieveMemberById(userId).queue(
				member -> addCoins(event, member),
				throwable -> sendErrorMessage(event.getChannel(), "That user is not in this guild!")
		);
	}

	private void addCoins(final GuildMessageReceivedEvent event, final Member member) {
		if(member.getUser().isBot()) {
			sendErrorMessage(event.getChannel(), "You can not give coins to a bot!");
			return;
		}

		final long guildId = event.getGuild().getIdLong();
		final Optional<DiscordMember> dcMemberOpt = memberRepo.findByDiscordIdAndGuild_GuildId(member.getIdLong(), guildId);
		final DiscordMember dcMember = dcMemberOpt.orElseGet(() -> createNewMember(member.getIdLong(), guildId));
		final String content = event.getMessage().getContentRaw();
		final String[] tokens = content.split(" ");
		final String coinText = tokens[tokens.length - 1];
		final long coinAmount = ParseUtil.safelyParseStringToLong(coinText);
		if (coinAmount < 1) {
			sendErrorMessage(event.getChannel(), "Please set a valid coin amount (>= 1)!");
			return;
		}

		dcMember.giveCoins(coinAmount);
		memberRepo.save(dcMember);
		answer(event.getChannel(), "Added **" + coinAmount + "** coins to the balance of " + member.getAsMention() + ".");
		LogUtil.logDebug(event.getAuthor().getIdLong() + " gave " + coinAmount + " coins to " + dcMember.getDiscordId());
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
