package com.motorbesitzen.gamblebot.bot.command.impl.coin.game;

import com.motorbesitzen.gamblebot.bot.command.CommandImpl;
import com.motorbesitzen.gamblebot.bot.command.game.coin.GameBet;
import com.motorbesitzen.gamblebot.bot.command.game.coin.GameWinInfo;
import com.motorbesitzen.gamblebot.bot.command.game.coin.impl.RouletteGame;
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

@Service("roulette")
class PlayRoulette extends CommandImpl {

	private final DiscordMemberRepo memberRepo;
	private final DiscordGuildRepo guildRepo;
	private final RouletteGame rouletteGame;

	@Autowired
	private PlayRoulette(final DiscordMemberRepo memberRepo, final DiscordGuildRepo guildRepo,
						 final RouletteGame rouletteGame) {
		this.memberRepo = memberRepo;
		this.guildRepo = guildRepo;
		this.rouletteGame = rouletteGame;
	}

	@Override
	public String getName() {
		return "roulette";
	}

	@Override
	public String getUsage() {
		return getName() + " <wager> bet";
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
	public void execute(final GuildMessageReceivedEvent event) {
		final Member author = event.getMember();
		if (author == null) {
			return;
		}

		final long authorId = author.getIdLong();
		final long guildId = event.getGuild().getIdLong();
		final Message message = event.getMessage();
		final String content = message.getContentRaw();
		final String prefix = EnvironmentUtil.getEnvironmentVariable("CMD_PREFIX");
		if (!content.matches("(?i)" + Pattern.quote(prefix) + getName() + " [0-9]+[kmb]? ([BREULH]|[0-9]{1,2}(,[0-9]{1,2}){0,5})")) {
			replyErrorMessage(event.getMessage(), "Please use the correct syntax! Use `" +
					prefix + "help` for a list of valid bets.");
			return;
		}

		final String[] tokens = content.split(" ");
		final String wagerText = tokens[tokens.length - 2];
		final long wager = ParseUtil.safelyParseStringToLong(wagerText);
		if (wager <= 0) {
			replyErrorMessage(event.getMessage(), "Please set a wager of at least 1 coin for your bet!");
			return;
		}

		final String betText = tokens[tokens.length - 1];
		if (!betText.matches("(?i)([BREULH]|[0-9]{1,2}(,[0-9]{1,2}){0,5})")) {
			replyErrorMessage(event.getMessage(), "Please choose a valid bet! Use `" +
					prefix + "help` for a list of valid bets.");
			return;
		}

		final Optional<DiscordGuild> dcGuildOpt = guildRepo.findById(guildId);
		final DiscordGuild dcGuild = dcGuildOpt.orElseGet(() -> createNewGuild(guildId));
		final Optional<DiscordMember> dcMemberOpt = memberRepo.findByDiscordIdAndGuild_GuildId(authorId, guildId);
		final DiscordMember dcMember = dcMemberOpt.orElseGet(() -> createNewMember(dcGuild, authorId));
		if (dcMember.getCoins() < wager) {
			replyErrorMessage(event.getMessage(), "You do not have enough coins for that bet.\n" +
					"You only have **" + dcMember.getCoins() + "** coins right now.");
			return;
		}

		final GameBet bet = new GameBet(wager, betText);
		final GameWinInfo winInfo = rouletteGame.play(bet);
		if (winInfo.isWin()) {
			final long winAmount = calcTaxedValue(dcGuild, winInfo.getWinAmount());
			dcMember.wonGame(winAmount);
			memberRepo.save(dcMember);
			reply(event.getMessage(), "**" + winInfo.getResultText() + "**\n" +
					"You won **" + winAmount + "** coins! Your balance: **" + dcMember.getCoins() + "** coins.");
			return;
		}

		dcMember.lostGame(wager);
		memberRepo.save(dcMember);
		reply(event.getMessage(), "**" + winInfo.getResultText() + "**\n" +
				"You lost the bet. Your balance: **" + dcMember.getCoins() + "** coins.");
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
