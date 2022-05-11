package com.motorbesitzen.gamblebot.bot.command.impl.coin.game;

import com.motorbesitzen.gamblebot.bot.command.game.coin.GameBet;
import com.motorbesitzen.gamblebot.bot.command.game.coin.GameWinInfo;
import com.motorbesitzen.gamblebot.bot.command.game.coin.impl.RpsGame;
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

@Service("rps")
class PlayRps extends PlayCommandImpl {

	private static final String WAGER_OPTION_NAME = "wager";
	private static final String BET_OPTION_NAME = "bet";

	private final DiscordMemberRepo memberRepo;
	private final DiscordGuildRepo guildRepo;
	private final RpsGame rpsGame;

	@Autowired
	private PlayRps(DiscordMemberRepo memberRepo, DiscordGuildRepo guildRepo, RpsGame rpsGame) {
		this.memberRepo = memberRepo;
		this.guildRepo = guildRepo;
		this.rpsGame = rpsGame;
	}

	@Override
	public String getName() {
		return "rps";
	}

	@Override
	public String getDescription() {
		return "Play a game of rock, paper, scissors against the bot.";
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
								"The bet you want to place (rock/paper/scissors).",
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
			reply(event, "Please use a valid bet (rock/paper/scissors)!");
			return;
		}

		if (!betText.matches("(?i)(R(ock)?|P(aper)?|S(cissors?)?)")) {
			reply(event, "Please use a valid bet (rock/paper/scissors)!");
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
		DiscordMember dcMember = dcMemberOpt.orElseGet(() -> createMember(dcGuild, authorId));
		if (dcMember.getCoins() < wager) {
			reply(event, "You do not have enough coins for that bet.\nYou only have **" + dcMember.getCoins() + "** coins right now.");
			return;
		}

		GameBet bet = new GameBet(wager, betText);
		GameWinInfo winInfo = rpsGame.play(bet);
		if (winInfo.isWin()) {
			long winAmount = calcTaxedValue(dcGuild, winInfo.getWinAmount());
			dcMember.wonGame(winAmount);
			memberRepo.save(dcMember);
			reply(event, "I chose **" + winInfo.getResultText() + "**!\n" +
					"You won **" + winAmount + "** coins! Your balance: **" + dcMember.getCoins() + "** coins.");
			return;
		}

		if (winInfo.isDraw()) {
			reply(event, "I chose **" + winInfo.getResultText() + "**!\n" +
					"It's a draw! Your balance: **" + dcMember.getCoins() + "** coins.");
			return;
		}

		dcMember.lostGame(wager);
		memberRepo.save(dcMember);
		reply(event, "I chose **" + winInfo.getResultText() + "**!\nYou lost the bet. " +
				"Your balance: **" + dcMember.getCoins() + "** coins.");
	}
}
