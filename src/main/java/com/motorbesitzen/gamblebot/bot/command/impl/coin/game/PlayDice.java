package com.motorbesitzen.gamblebot.bot.command.impl.coin.game;

import com.motorbesitzen.gamblebot.bot.command.CommandImpl;
import com.motorbesitzen.gamblebot.bot.command.game.coin.GameBet;
import com.motorbesitzen.gamblebot.bot.command.game.coin.GameWinInfo;
import com.motorbesitzen.gamblebot.bot.command.game.coin.impl.DiceGame;
import com.motorbesitzen.gamblebot.data.dao.DiscordGuild;
import com.motorbesitzen.gamblebot.data.dao.DiscordMember;
import com.motorbesitzen.gamblebot.data.repo.DiscordGuildRepo;
import com.motorbesitzen.gamblebot.data.repo.DiscordMemberRepo;
import com.motorbesitzen.gamblebot.util.EnvironmentUtil;
import com.motorbesitzen.gamblebot.util.ParseUtil;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.regex.Pattern;

@Service("dice")
class PlayDice extends CommandImpl {

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
	public String getUsage() {
		return getName() + " <wager>";
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
	public void execute(final GuildMessageReceivedEvent event) {
		final Member author = event.getMember();
		if (author == null) {
			return;
		}

		final Message message = event.getMessage();
		final String content = message.getContentRaw();
		final String prefix = EnvironmentUtil.getEnvironmentVariable("CMD_PREFIX");
		if (!content.matches("(?i)" + Pattern.quote(prefix) + getName() + " [0-9]+[kmb]?")) {
			replyErrorMessage(event.getMessage(), "Please use the correct syntax! Use `" +
					prefix + "help` for a list of valid bets.");
			return;
		}

		final String[] tokens = content.split(" ");
		final String wagerText = tokens[tokens.length - 1];
		final long wager = ParseUtil.safelyParseStringToLong(wagerText);
		if (wager <= 0) {
			replyErrorMessage(event.getMessage(), "Please set a wager of at least 1 coin for your bet!");
			return;
		}

		final long authorId = author.getIdLong();
		final long guildId = event.getGuild().getIdLong();
		final Optional<DiscordGuild> dcGuildOpt = guildRepo.findById(guildId);
		final DiscordGuild dcGuild = dcGuildOpt.orElseGet(() -> createNewGuild(guildId));
		final Optional<DiscordMember> dcMemberOpt = memberRepo.findByDiscordIdAndGuild_GuildId(authorId, guildId);
		final DiscordMember dcMember = dcMemberOpt.orElseGet(() -> createNewMember(dcGuild, authorId));
		if (dcMember.getCoins() < wager) {
			replyErrorMessage(event.getMessage(), "You do not have enough coins for that bet.\n" +
					"You only have **" + dcMember.getCoins() + "** coins right now.");
			return;
		}

		final GameBet bet = new GameBet(wager);
		final GameWinInfo winInfo = diceGame.play(bet);
		if (winInfo.isWin()) {
			final long winAmount = calcTaxedValue(dcGuild, winInfo.getWinAmount());
			dcMember.wonGame(winAmount);
			memberRepo.save(dcMember);
			reply(event.getMessage(), winInfo.getResultText() + " You won **" + winAmount + "** coins!\n" +
					"Your balance: **" + dcMember.getCoins() + "** coins.");
			return;
		}

		dcMember.lostGame(wager);
		memberRepo.save(dcMember);
		reply(event.getMessage(), winInfo.getResultText() + " You lost the bet.\n" +
				"Your balance: **" + dcMember.getCoins() + "** coins.");
	}

	private DiscordMember createNewMember(final DiscordGuild dcGuild, final long memberId) {
		return DiscordMember.createDefault(memberId, dcGuild);
	}

	private DiscordGuild createNewGuild(final long guildId) {
		final DiscordGuild dcGuild = DiscordGuild.withGuildId(guildId);
		guildRepo.save(dcGuild);
		return dcGuild;
	}
}
