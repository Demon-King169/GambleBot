package com.motorbesitzen.gamblebot.bot.command.impl.coin;

import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.motorbesitzen.gamblebot.bot.command.CommandImpl;
import com.motorbesitzen.gamblebot.bot.service.RouletteGame;
import com.motorbesitzen.gamblebot.bot.service.entity.RouletteBet;
import com.motorbesitzen.gamblebot.bot.service.entity.RouletteInfo;
import com.motorbesitzen.gamblebot.bot.service.entity.RouletteWinInfo;
import com.motorbesitzen.gamblebot.data.dao.DiscordGuild;
import com.motorbesitzen.gamblebot.data.dao.DiscordMember;
import com.motorbesitzen.gamblebot.data.repo.DiscordGuildRepo;
import com.motorbesitzen.gamblebot.data.repo.DiscordMemberRepo;
import com.motorbesitzen.gamblebot.util.LogUtil;
import com.motorbesitzen.gamblebot.util.ParseUtil;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service("roulette")
public class PlayRoulette extends CommandImpl {

	private final DiscordMemberRepo memberRepo;
	private final DiscordGuildRepo guildRepo;
	private final EventWaiter eventWaiter;
	private final RouletteGame rouletteGame;

	@Autowired
	private PlayRoulette(final DiscordMemberRepo memberRepo, final DiscordGuildRepo guildRepo,
						 final EventWaiter eventWaiter, final RouletteGame rouletteGame) {
		this.memberRepo = memberRepo;
		this.guildRepo = guildRepo;
		this.eventWaiter = eventWaiter;
		this.rouletteGame = rouletteGame;
	}

	@Override
	public String getName() {
		return "roulette";
	}

	@Override
	public String getUsage() {
		return getName() + " wager";
	}

	@Override
	public String getDescription() {
		return "Starts a round of roulette with the set wager.";
	}

	@Override
	public boolean isAdminCommand() {
		return false;
	}

	@Override
	public void execute(final GuildMessageReceivedEvent event) {
		final Member author = event.getMember();
		if(author == null) {
			return;
		}

		final long authorId = author.getIdLong();
		final long guildId = event.getGuild().getIdLong();
		final Message message = event.getMessage();
		final String content = message.getContentRaw();
		final String[] tokens = content.split(" ");
		final long wager = ParseUtil.safelyParseStringToLong(tokens[tokens.length - 1]);
		if(wager <= 0) {
			sendErrorMessage(event.getChannel(), "Please set a wager of at least 1 coin for your bet!");
			return;
		}

		final Optional<DiscordMember> dcMemberOpt = memberRepo.findByDiscordIdAndGuild_GuildId(authorId, guildId);
		final DiscordMember dcMember = dcMemberOpt.orElseGet(() -> createNewMember(authorId, guildId));
		if(dcMember.getCoins() < wager) {
			sendErrorMessage(event.getChannel(), "You do not have enough coins for that bet.\n" +
					"You only have **" + dcMember.getCoins() + "** coins right now.");
			return;
		}

		final long originalChannelId = event.getChannel().getIdLong();
		final Message instructionMsg = answerPlaceholder(
				event.getChannel(), "Please place your bet! You can set the following bets:\n" +
						"**B** - bet on black\n**R** - bet on red\n**E** - bet on even numbers\n" +
						"**U** - bet on uneven numbers\n**L** - bet on low numbers (1-18)\n" +
						"**H** - bet on high numbers (19-36)\n**0**-**36** - bet on a number between 0 and 36, " +
						"you can bet on up to 6 numbers at once (separate numbers with a comma)\n" +
						"*Betting on multiple numbers decreases your payout!*\n`Example: 3,7,17 would bet on 3, 7 and 17.`"
		);
		eventWaiter.waitForEvent(
				GuildMessageReceivedEvent.class,
				newEvent -> {
					if(isWrongDialog(newEvent, originalChannelId, authorId)) {
						return false;
					}

					final Message newMessage = newEvent.getMessage();
					final String newContent = newMessage.getContentRaw().trim();
					if(!newContent.matches("(?i)([BREULH]|[0-9]{1,2}(,[0-9]{1,2}){0,5})")) {
						sendErrorMessage(newEvent.getChannel(), "Please select one of the mentioned methods!");
						return false;
					}

					return true;
				},
				newEvent -> {
					final Message newMessage = newEvent.getMessage();
					final String newContent = newMessage.getContentRaw().trim();
					final RouletteBet bet = new RouletteBet(wager, newContent);
					final RouletteWinInfo winInfo = rouletteGame.play(bet);
					final String fieldColor = RouletteInfo.getColorEmote(winInfo.getResultNumber());
					if(!winInfo.isWin()) {
						dcMember.removeCoins(wager);
						memberRepo.save(dcMember);
						reply(newMessage, "You lost the bet. Your balance: **" +
								dcMember.getCoins() + "** coins.\n" + fieldColor + " Roulette result: " + winInfo.getResultNumber());
						return;
					}

					final long winAmount = winInfo.getWinAmount();
					dcMember.addCoins(winAmount);
					memberRepo.save(dcMember);
					reply(newMessage, "You won **" + winAmount + "** coins! Your balance: **" +
							dcMember.getCoins() + "** coins.\n" + fieldColor + " Roulette result: " + winInfo.getResultNumber());
				},
				1, TimeUnit.MINUTES,
				() -> instructionMsg.delete().queue(
						v -> LogUtil.logDebug("Deleted roulette instruction message."),
						throwable -> LogUtil.logDebug("Could not delete the roulette instruction message.", throwable)
				)
		);
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
