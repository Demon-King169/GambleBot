package com.motorbesitzen.gamblebot.bot.command.impl.coin.game;

import com.motorbesitzen.gamblebot.bot.command.game.coin.GameBet;
import com.motorbesitzen.gamblebot.bot.command.game.coin.GameWinInfo;
import com.motorbesitzen.gamblebot.bot.command.game.coin.impl.LotteryGame;
import com.motorbesitzen.gamblebot.data.dao.DiscordGuild;
import com.motorbesitzen.gamblebot.data.dao.DiscordMember;
import com.motorbesitzen.gamblebot.data.repo.DiscordGuildRepo;
import com.motorbesitzen.gamblebot.data.repo.DiscordMemberRepo;
import com.motorbesitzen.gamblebot.util.ParseUtil;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Optional;

@Service("lottery")
class PlayLottery extends PlayCommandImpl {

	private static final String WAGER_OPTION_NAME = "wager";
	private static final String BET_OPTION_NAME = "bet";

	private final DiscordMemberRepo memberRepo;
	private final DiscordGuildRepo guildRepo;
	private final LotteryGame lotteryGame;

	@Autowired
	private PlayLottery(final DiscordMemberRepo memberRepo, final DiscordGuildRepo guildRepo,
						final LotteryGame lotteryGame) {
		this.memberRepo = memberRepo;
		this.guildRepo = guildRepo;
		this.lotteryGame = lotteryGame;
	}

	@Override
	public String getName() {
		return "lottery";
	}

	@Override
	public String getDescription() {
		return "Plays the lottery (6 of 49) with the set wager. You have to select six different numbers from 1 to 49." +
				"Your \"super number\" will be chosen at random. To win you need to have at least two hits and a " +
				"matching super number or three hits and more.\n" +
				"`Example: 13,7,17,42,6,9 would bet on 13, 7, 17, 42, 6 and 9.`";
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
		jda.upsertCommand(getName(), "Plays the lottery (6 of 49) with the set wager.")
				.addOptions(
						new OptionData(
								OptionType.INTEGER,
								WAGER_OPTION_NAME,
								"The wager (in coins) you want to bet.",
								true
						).setRequiredRange(0, Integer.MAX_VALUE),
						new OptionData(
								OptionType.STRING,
								BET_OPTION_NAME,
								"The bet you want to place. Use the help command for further information.",
								true
						)
				).queue();
	}

	@Override
	public void execute(SlashCommandEvent event) {
		long wager = getWager(event, WAGER_OPTION_NAME);
		if (wager <= 0) {
			reply(event, "Please set a valid wager (> 0).");
			return;
		}

		String betText = getBetString(event, BET_OPTION_NAME);
		if (betText == null) {
			reply(event, "Please choose a valid bet of six different numbers from 1 to 49.");
			return;
		}

		if (!betText.matches("[0-9]{1,2}(,[0-9]{1,2}){5}")) {
			reply(event, "Please choose a valid bet of six different numbers from 1 to 49.");
			return;
		}

		final HashSet<Integer> bets = new HashSet<>();
		final String[] betTokens = betText.split(",");
		for (String betToken : betTokens) {
			final int betNumber = ParseUtil.safelyParseStringToInt(betToken);
			if (betNumber < 1 || betNumber > 49) {
				reply(event, "Please choose a valid bet of six different numbers from 1 to 49.");
				return;
			}
			bets.add(betNumber);
		}

		if (bets.size() != 6) {
			reply(event, "Please choose a valid bet of six different numbers from 1 to 49.");
			return;
		}

		final Member author = event.getMember();
		if (author == null) {
			return;
		}

		final long authorId = author.getIdLong();
		final long guildId = author.getGuild().getIdLong();
		final Optional<DiscordGuild> dcGuildOpt = guildRepo.findById(guildId);
		final DiscordGuild dcGuild = dcGuildOpt.orElseGet(() -> createNewGuild(guildRepo, guildId));
		final Optional<DiscordMember> dcMemberOpt = memberRepo.findByDiscordIdAndGuild_GuildId(authorId, guildId);
		final DiscordMember dcMember = dcMemberOpt.orElseGet(() -> createNewMember(dcGuild, authorId));
		if (dcMember.getCoins() < wager) {
			reply(event, "You do not have enough coins for that bet.\n" +
					"You only have **" + dcMember.getCoins() + "** coins right now.");
			return;
		}

		final GameBet bet = new GameBet(wager, betText);
		final GameWinInfo winInfo = lotteryGame.play(bet);
		if (winInfo.isWin()) {
			final long winAmount = calcTaxedValue(dcGuild, winInfo.getWinAmount());
			dcMember.spendCoins(wager);        // price needs to get paid no matter what
			dcMember.wonGame(winAmount);
			memberRepo.save(dcMember);
			reply(event, winInfo.getResultText() + "\n" +
					"You won **" + winAmount + "** coins! Your balance: **" + dcMember.getCoins() + "** coins.");
			return;
		}

		dcMember.lostGame(wager);
		memberRepo.save(dcMember);
		reply(event, winInfo.getResultText() + "\n" +
				"You lost the bet. Your balance: **" + dcMember.getCoins() + "** coins.");
	}
}
