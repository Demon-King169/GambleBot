package com.motorbesitzen.gamblebot.bot.command.impl.coin.action;

import com.motorbesitzen.gamblebot.bot.command.CommandImpl;
import com.motorbesitzen.gamblebot.data.dao.DiscordGuild;
import com.motorbesitzen.gamblebot.data.dao.DiscordMember;
import com.motorbesitzen.gamblebot.data.repo.DiscordGuildRepo;
import com.motorbesitzen.gamblebot.data.repo.DiscordMemberRepo;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Collect the daily coins.
 */
@Service("daily")
class DailyCoins extends CommandImpl {

	private static final int MS_PER_DAY = 86400000;
	private final DiscordMemberRepo memberRepo;
	private final DiscordGuildRepo guildRepo;

	@Autowired
	private DailyCoins(final DiscordMemberRepo memberRepo, final DiscordGuildRepo guildRepo) {
		this.memberRepo = memberRepo;
		this.guildRepo = guildRepo;
	}

	@Override
	public String getName() {
		return "daily";
	}

	@Override
	public String getUsage() {
		return getName();
	}

	@Override
	public String getDescription() {
		return "Collects the amount of daily coins.";
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
		final Member author = event.getMember();
		if (author == null) {
			return;
		}

		final long memberId = author.getIdLong();
		final long guildId = event.getGuild().getIdLong();
		final Optional<DiscordMember> dcMemberOpt = memberRepo.findByDiscordIdAndGuild_GuildId(memberId, guildId);
		final DiscordMember dcMember = dcMemberOpt.orElseGet(() -> createNewMember(memberId, guildId));
		if (dcMember.getNextDailyCoinsMs() >= System.currentTimeMillis()) {
			reply(event.getMessage(), "You can collect your next daily coins in " + dcMember.getTimeToNextDailyText() + ".");
			return;
		}

		final long dailyCoinsAmount = getDailyAmount(author, dcMember.getGuild());
		dcMember.giveCoins(dailyCoinsAmount);
		dcMember.setNextDailyCoinsMs(System.currentTimeMillis() + MS_PER_DAY);
		memberRepo.save(dcMember);
		reply(event.getMessage(), "Added **" + dailyCoinsAmount + "** coins to your balance!\n" +
				"You now have **" + dcMember.getCoins() + "** coins.");
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

	private long getDailyAmount(final Member author, final DiscordGuild dcGuild) {
		final long dailyCoinsAmount = dcGuild.getDailyCoins();
		if(author.getTimeBoosted() != null) {
			final long boosterDailyBonus = dcGuild.getBoosterDailyBonus();
			return dailyCoinsAmount + boosterDailyBonus;
		}

		return dailyCoinsAmount;
	}
}
