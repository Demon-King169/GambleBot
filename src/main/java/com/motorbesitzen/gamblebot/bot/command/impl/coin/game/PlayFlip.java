package com.motorbesitzen.gamblebot.bot.command.impl.coin.game;

import com.motorbesitzen.gamblebot.bot.command.game.coin.GameBet;
import com.motorbesitzen.gamblebot.bot.command.game.coin.GameWinInfo;
import com.motorbesitzen.gamblebot.bot.command.game.coin.impl.FlipGame;
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

@Service("flip")
public class PlayFlip extends PlayCommandImpl {

	private static final String WAGER_OPTION_NAME = "wager";
	private static final String BET_OPTION_NAME = "bet";

	private final DiscordMemberRepo memberRepo;
	private final DiscordGuildRepo guildRepo;
	private final FlipGame flipGame;

	@Autowired
	private PlayFlip(DiscordMemberRepo memberRepo, DiscordGuildRepo guildRepo, FlipGame flipGame) {
		this.memberRepo = memberRepo;
		this.guildRepo = guildRepo;
		this.flipGame = flipGame;
	}

	@Override
	public String getName() {
		return "flip";
	}

	@Override
	public String getDescription() {
		return "Play a game of coin flip against the bot.";
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
								"The bet you want to place (head/tail).",
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

		if (!betText.matches("(?i)(H(eads?)?|T(ails?)?)")) {
			reply(event, "Please choose a valid bet (head/tail).");
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
		GameWinInfo winInfo = flipGame.play(bet);
		if (winInfo.isWin()) {
			long winAmount = calcTaxedValue(dcGuild, winInfo.getWinAmount());
			dcMember.wonGame(winAmount);
			memberRepo.save(dcMember);
			reply(event, "It is **" + winInfo.getResultText() + "**! You won **" + winAmount + "** coins!\n" +
					"Your balance: **" + dcMember.getCoins() + "** coins.");
			return;
		}

		dcMember.lostGame(wager);
		memberRepo.save(dcMember);
		reply(event, "It is **" + winInfo.getResultText() + "**! You lost the bet.\n" +
				"Your balance: **" + dcMember.getCoins() + "** coins.");
	}
}
