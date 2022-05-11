package com.motorbesitzen.gamblebot.bot.command.impl.coin.action;

import com.motorbesitzen.gamblebot.bot.command.CommandImpl;
import com.motorbesitzen.gamblebot.data.dao.DiscordGuild;
import com.motorbesitzen.gamblebot.data.dao.DiscordMember;
import com.motorbesitzen.gamblebot.data.repo.DiscordGuildRepo;
import com.motorbesitzen.gamblebot.data.repo.DiscordMemberRepo;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
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
	private DailyCoins(DiscordMemberRepo memberRepo, DiscordGuildRepo guildRepo) {
		this.memberRepo = memberRepo;
		this.guildRepo = guildRepo;
	}

	@Override
	public String getName() {
		return "daily";
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
	public void register(JDA jda) {
		jda.upsertCommand(getName(), getDescription()).queue();
	}

	@Override
	public void execute(SlashCommandEvent event) {
		Member author = event.getMember();
		if (author == null) {
			return;
		}

		long memberId = author.getIdLong();
		long guildId = author.getGuild().getIdLong();
		Optional<DiscordMember> dcMemberOpt = memberRepo.findByDiscordIdAndGuild_GuildId(memberId, guildId);
		DiscordMember dcMember = dcMemberOpt.orElseGet(() -> createMember(guildRepo, guildId, memberId));
		if (dcMember.getNextDailyCoinsMs() >= System.currentTimeMillis()) {
			reply(event, "You can collect your next daily coins in " + dcMember.getTimeToNextDailyText() + ".", true);
			return;
		}

		long dailyCoinsAmount = getDailyAmount(author, dcMember.getGuild());
		dcMember.giveCoins(dailyCoinsAmount);
		dcMember.setNextDailyCoinsMs(System.currentTimeMillis() + MS_PER_DAY);
		memberRepo.save(dcMember);
		reply(event, "Added **" + dailyCoinsAmount + "** coins to your balance!\n" +
				"You now have **" + dcMember.getCoins() + "** coins.", true);
	}

	private long getDailyAmount(Member author, DiscordGuild dcGuild) {
		long dailyCoinsAmount = dcGuild.getDailyCoins();
		if (author.getTimeBoosted() != null) {
			long boosterDailyBonus = dcGuild.getBoosterDailyBonus();
			return dailyCoinsAmount + boosterDailyBonus;
		}

		return dailyCoinsAmount;
	}
}
