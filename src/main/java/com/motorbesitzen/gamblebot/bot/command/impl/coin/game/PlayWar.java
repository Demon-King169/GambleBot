package com.motorbesitzen.gamblebot.bot.command.impl.coin.game;

import com.motorbesitzen.gamblebot.bot.command.CommandImpl;
import com.motorbesitzen.gamblebot.bot.command.game.coin.GameBet;
import com.motorbesitzen.gamblebot.bot.command.game.coin.GameWinInfo;
import com.motorbesitzen.gamblebot.bot.command.game.coin.impl.WarGame;
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

@Service("war")
class PlayWar extends CommandImpl {

	private final DiscordGuildRepo guildRepo;
	private final DiscordMemberRepo memberRepo;
	private final WarGame warGame;

	@Autowired
	private PlayWar(final DiscordGuildRepo guildRepo, final DiscordMemberRepo memberRepo, final WarGame warGame) {
		this.guildRepo = guildRepo;
		this.memberRepo = memberRepo;
		this.warGame = warGame;
	}

	@Override
	public String getName() {
		return "war";
	}

	@Override
	public String getUsage() {
		return getName() + " <wager>";
	}

	@Override
	public String getDescription() {
		return "Whoever has the higher card wins. On a draw it continues until one side wins.";
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
		if (!content.matches("(?i)" + prefix + getName() + " [0-9]+[kmb]?")) {
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
		final Optional<DiscordMember> dcMemberOpt = memberRepo.findByDiscordIdAndGuild_GuildId(authorId, guildId);
		final DiscordMember dcMember = dcMemberOpt.orElseGet(() -> createNewMember(authorId, guildId));
		if (dcMember.getCoins() < wager) {
			replyErrorMessage(event.getMessage(), "You do not have enough coins for that bet.\n" +
					"You only have **" + dcMember.getCoins() + "** coins right now.");
			return;
		}

		final GameBet bet = new GameBet(wager);
		final GameWinInfo winInfo = warGame.play(bet);
		if (winInfo.isWin()) {
			final long winAmount = winInfo.getWinAmount();
			dcMember.wonGame(winAmount);
			memberRepo.save(dcMember);
			reply(event.getMessage(), " You won **" + winAmount + "** coins!\n" + winInfo.getResultText() +
					"\nYour balance: **" + dcMember.getCoins() + "** coins.");
			return;
		}

		dcMember.lostGame(wager);
		memberRepo.save(dcMember);
		reply(event.getMessage(), "You lost the bet.\n" + winInfo.getResultText() +
				"\nYour balance: **" + dcMember.getCoins() + "** coins.");
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
