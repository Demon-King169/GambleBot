package com.motorbesitzen.gamblebot.bot.command.impl.coin.game;

import com.motorbesitzen.gamblebot.bot.command.game.coin.GameBet;
import com.motorbesitzen.gamblebot.bot.command.game.coin.GameWinInfo;
import com.motorbesitzen.gamblebot.bot.command.game.coin.impl.DiceGame;
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

@Service("dice")
class PlayDice extends PlayCommandImpl {

	private static final String WAGER_OPTION_NAME = "wager";

	private final DiscordGuildRepo guildRepo;
	private final DiscordMemberRepo memberRepo;
	private final DiceGame diceGame;

	@Autowired
	private PlayDice(final DiscordGuildRepo guildRepo, final DiscordMemberRepo memberRepo, final DiceGame diceGame) {
		this.guildRepo = guildRepo;
		this.memberRepo = memberRepo;
		this.diceGame = diceGame;
	}

	@Override
	public String getName() {
		return "dice";
	}

	@Override
	public String getDescription() {
		return "Throws a dice against the house, whoever has the higher result wins. On a draw a second throw gets done" +
				" in which the player needs to have a higher throw than his first one to win.";
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
		jda.upsertCommand(getName(), "Throws a dice against the house, whoever has the higher result wins.")
				.addOptions(
						new OptionData(
								OptionType.INTEGER,
								WAGER_OPTION_NAME,
								"The wager (in coins) you want to bet.",
								true
						).setRequiredRange(0, Integer.MAX_VALUE)
				).queue();
	}

	@Override
	public void execute(SlashCommandEvent event) {
		long wager = getWager(event, WAGER_OPTION_NAME);
		if (wager <= 0) {
			reply(event, "Please set a valid wager (> 0).");
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

		final GameBet bet = new GameBet(wager);
		final GameWinInfo winInfo = diceGame.play(bet);
		if (winInfo.isWin()) {
			final long winAmount = calcTaxedValue(dcGuild, winInfo.getWinAmount());
			dcMember.wonGame(winAmount);
			memberRepo.save(dcMember);
			reply(event, winInfo.getResultText() + " You won **" + winAmount + "** coins!\n" +
					"Your balance: **" + dcMember.getCoins() + "** coins.");
			return;
		}

		dcMember.lostGame(wager);
		memberRepo.save(dcMember);
		reply(event, winInfo.getResultText() + " You lost the bet.\n" +
				"Your balance: **" + dcMember.getCoins() + "** coins.");
	}
}
