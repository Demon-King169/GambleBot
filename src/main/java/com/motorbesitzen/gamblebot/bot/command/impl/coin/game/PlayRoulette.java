package com.motorbesitzen.gamblebot.bot.command.impl.coin.game;

import com.motorbesitzen.gamblebot.bot.command.game.coin.GameBet;
import com.motorbesitzen.gamblebot.bot.command.game.coin.GameWinInfo;
import com.motorbesitzen.gamblebot.bot.command.game.coin.impl.RouletteGame;
import com.motorbesitzen.gamblebot.data.dao.DiscordGuild;
import com.motorbesitzen.gamblebot.data.dao.DiscordMember;
import com.motorbesitzen.gamblebot.data.repo.DiscordGuildRepo;
import com.motorbesitzen.gamblebot.data.repo.DiscordMemberRepo;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service("roulette")
class PlayRoulette extends PlayCommandImpl {

	private static final String WAGER_OPTION_NAME = "wager";
	private static final String BET_OPTION_NAME = "bet";

	private final DiscordMemberRepo memberRepo;
	private final DiscordGuildRepo guildRepo;
	private final RouletteGame rouletteGame;

	@Autowired
	private PlayRoulette(DiscordMemberRepo memberRepo, DiscordGuildRepo guildRepo, RouletteGame rouletteGame) {
		this.memberRepo = memberRepo;
		this.guildRepo = guildRepo;
		this.rouletteGame = rouletteGame;
	}

	@Override
	public String getName() {
		return "roulette";
	}

	@Override
	public String getDescription() {
		return "Starts a round of roulette with the set wager. You can choose from the following bets:\n" +
				"**B** - bet on black\n**R** - bet on red\n**E** - bet on even numbers\n" +
				"**U** - bet on uneven numbers\n**L** - bet on low numbers (1-18)\n" +
				"**H** - bet on high numbers (19-36)\n**0**-**36** - bet on a number between 0 and 36, " +
				"you can bet on up to 6 numbers at once (separate numbers with a comma)\n" +
				"*Betting on multiple numbers decreases your payout!*\n`Example: 3,7,17 would bet on 3, 7 and 17.`";
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
		jda.upsertCommand(getName(), "Starts a round of roulette with the set wager.")
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
			reply(event, "Please use a valid bet! Use the help command for further information on what bets exist.");
			return;
		}

		if (!betText.matches("(?i)([BREULH]|[0-9]{1,2}(,[0-9]{1,2}){0,5})")) {
			reply(event, "Please use a valid bet! Use the help command for further information on what bets exist.");
			return;
		}

		Member author = event.getMember();
		if (author == null) {
			return;
		}

		long authorId = author.getIdLong();
		long guildId = author.getGuild().getIdLong();
		Optional<DiscordGuild> dcGuildOpt = guildRepo.findById(guildId);
		DiscordGuild dcGuild = dcGuildOpt.orElseGet(() -> createGuild(guildRepo, guildId));
		Optional<DiscordMember> dcMemberOpt = memberRepo.findByDiscordIdAndGuild_GuildId(authorId, guildId);
		DiscordMember dcMember = dcMemberOpt.orElseGet(() -> createNewMember(dcGuild, authorId));
		if (dcMember.getCoins() < wager) {
			reply(event, "You do not have enough coins for that bet.\n" +
					"You only have **" + dcMember.getCoins() + "** coins right now.");
			return;
		}

		GameBet bet = new GameBet(wager, betText);
		GameWinInfo winInfo = rouletteGame.play(bet);
		if (winInfo.isWin()) {
			long winAmount = calcTaxedValue(dcGuild, winInfo.getWinAmount());
			dcMember.wonGame(winAmount);
			memberRepo.save(dcMember);
			reply(event, "**" + winInfo.getResultText() + "**\n" +
					"You won **" + winAmount + "** coins! Your balance: **" + dcMember.getCoins() + "** coins.");
			return;
		}

		dcMember.lostGame(wager);
		memberRepo.save(dcMember);
		reply(event, "**" + winInfo.getResultText() + "**\n" +
				"You lost the bet. Your balance: **" + dcMember.getCoins() + "** coins.");
	}
}
