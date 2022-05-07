package com.motorbesitzen.gamblebot.bot.command.impl.custom;

import com.motorbesitzen.gamblebot.bot.command.CommandImpl;
import com.motorbesitzen.gamblebot.bot.command.game.gamble.GambleGame;
import com.motorbesitzen.gamblebot.bot.command.game.gamble.GambleWinInfo;
import com.motorbesitzen.gamblebot.data.dao.DiscordGuild;
import com.motorbesitzen.gamblebot.data.dao.DiscordMember;
import com.motorbesitzen.gamblebot.data.dao.GambleSettings;
import com.motorbesitzen.gamblebot.data.repo.DiscordGuildRepo;
import com.motorbesitzen.gamblebot.data.repo.DiscordMemberRepo;
import com.motorbesitzen.gamblebot.util.LogUtil;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Play the currently available gamble if there is one.
 */
@Service("gamble")
class PlayGamble extends CommandImpl {

	private final DiscordGuildRepo guildRepo;
	private final DiscordMemberRepo memberRepo;
	private final GambleGame gambleGame;

	@Autowired
	PlayGamble(DiscordGuildRepo guildRepo, GambleGame gambleGame, DiscordMemberRepo memberRepo) {
		this.guildRepo = guildRepo;
		this.gambleGame = gambleGame;
		this.memberRepo = memberRepo;
	}

	@Override
	public String getName() {
		return "gamble";
	}

	@Override
	public String getDescription() {
		return "Participate in the gamble.";
	}

	@Override
	public boolean isAdminCommand() {
		return false;
	}

	@Override
	public boolean isGlobalCommand() {
		return true;
	}

	@Override
	public void register(JDA jda) {
		jda.upsertCommand(getName(), getDescription()).queue();
	}

	@Transactional
	@Override
	public void execute(SlashCommandEvent event) {
		Guild guild = event.getGuild();
		if (guild == null) {
			return;
		}

		long guildId = guild.getIdLong();
		Optional<DiscordGuild> dcGuildOpt = guildRepo.findById(guildId);
		dcGuildOpt.ifPresentOrElse(
				dcGuild -> {
					if (!dcGuild.hasRunningGamble()) {
						String timeSinceEndText = dcGuild.getTimeSinceEndText();
						reply(event, "The gamble ended " + timeSinceEndText + " ago.", true);
						return;
					}

					Member member = event.getMember();
					if (member == null) {
						return;
					}

					long memberId = member.getIdLong();
					Optional<DiscordMember> memberOpt = memberRepo.findByDiscordIdAndGuild_GuildId(memberId, guildId);
					DiscordMember player = memberOpt.orElseGet(() -> DiscordMember.createDefault(memberId, dcGuild));
					if (!player.canPlay()) {
						reply(event, "You are on cooldown. You can play again in " + player.getTimeToCooldownEndText() + ".", true);
						return;
					}

					player.setNextGambleMs(System.currentTimeMillis() + dcGuild.getGambleSettings().getCooldownMs());
					memberRepo.save(player);
					playGamble(event, player, member);
				},
				() -> reply(event, "There is no running gamble.", true)
		);
	}

	private void playGamble(SlashCommandEvent event, DiscordMember player, Member member) {
		DiscordGuild dcGuild = player.getGuild();
		GambleSettings settings = dcGuild.getGambleSettings();
		GambleWinInfo gambleWinInfo = gambleGame.play(settings);
		LogUtil.logInfo(
				player.getDiscordId() + " got a " + gambleWinInfo.getLuckyNumber() + " in " +
						Arrays.toString(settings.getPrizes().toArray()) + " -> " + gambleWinInfo.getPriceName()
		);

		NumberFormat nf = generateNumberFormat();
		TextChannel logChannel = member.getGuild().getTextChannelById(dcGuild.getLogChannelId());
		if (gambleWinInfo.getPriceName() == null) {
			handleBlank(event, member, nf, gambleWinInfo);
			return;
		}

		if (gambleWinInfo.getPriceName().equalsIgnoreCase("ban") || gambleWinInfo.getPriceName().toLowerCase().startsWith("ban ")) {
			String banResult = handleBan(event, member, nf, gambleWinInfo);
			logResult(logChannel, banResult);
			return;
		}

		if (gambleWinInfo.getPriceName().equalsIgnoreCase("kick") || gambleWinInfo.getPriceName().toLowerCase().startsWith("kick ")) {
			String kickResult = handleKick(event, member, nf, gambleWinInfo);
			logResult(logChannel, kickResult);
			return;
		}

		handleWin(event, member, nf, gambleWinInfo);
		logResult(logChannel, member.getAsMention() + " won \"" + gambleWinInfo.getPriceName() + "\"!");
	}

	private NumberFormat generateNumberFormat() {
		NumberFormat nf = NumberFormat.getNumberInstance();
		nf.setMinimumFractionDigits(1);
		nf.setMaximumFractionDigits(4);
		nf.setRoundingMode(RoundingMode.HALF_UP);
		return nf;
	}

	private void handleBlank(SlashCommandEvent event, Member member, NumberFormat nf, GambleWinInfo gambleWinInfo) {
		reply(
				event,
				member.getAsMention() + ", you drew a blank! You did not win anything.\n" +
						"Your (rounded) unlucky number: " + nf.format(gambleWinInfo.getLuckyNumber())
		);
	}

	private String handleBan(SlashCommandEvent event, Member member, NumberFormat nf, GambleWinInfo gambleWinInfo) {
		Member self = member.getGuild().getSelfMember();
		if (self.canInteract(member)) {
			reply(
					event,
					"Unlucky " + member.getAsMention() + "! You won a ban. Enforcing ban in a few seconds...\n" +
							"Your (rounded) unlucky number: " + nf.format(gambleWinInfo.getLuckyNumber())
			);
			member.ban(0, "'Won' a ban in the gamble.").queueAfter(
					10, TimeUnit.SECONDS,
					b -> event.getHook().sendMessage("Enforced ban of " + member.getAsMention() + ". Rip in pieces :poop:").queue()
			);
			return member.getAsMention() + " won a ban. Enforcing ban in the next 10 seconds.";
		}

		reply(
				event,
				"Unlucky " + member.getAsMention() + "! You won a ban. Be glad that I can not ban you myself. Reporting to authorities...\n" +
						"Your (rounded) unlucky number: " + nf.format(gambleWinInfo.getLuckyNumber())
		);
		return member.getAsMention() + " won a ban. However, I can not ban that user.";
	}

	private String handleKick(SlashCommandEvent event, Member member, NumberFormat nf, GambleWinInfo gambleWinInfo) {
		Member self = member.getGuild().getSelfMember();
		if (self.canInteract(member)) {
			reply(
					event,
					"Unlucky " + member.getAsMention() + "! You won a kick. Enforcing kick in a few seconds...\n" +
							"Your (rounded) unlucky number: " + nf.format(gambleWinInfo.getLuckyNumber())
			);
			member.kick("'Won' a kick in the gamble.").queueAfter(
					5, TimeUnit.SECONDS,
					k -> event.getHook().sendMessage("Enforced kick of " + member.getAsMention() + ". Hopefully it is a ban next time :smiling_imp:").queue()
			);
			return member.getAsMention() + " won a kick. Enforcing kick in the next 5 seconds.";
		}

		reply(
				event,
				"Unlucky " + member.getAsMention() + "! You won a kick. Be glad that I can not kick you myself. Reporting to authorities...\n" +
						"Your (rounded) unlucky number: " + nf.format(gambleWinInfo.getLuckyNumber())
		);
		return member.getAsMention() + " won a kick. However, I can not kick that user.";
	}

	private void handleWin(SlashCommandEvent event, Member member, NumberFormat nf, GambleWinInfo gambleWinInfo) {
		reply(
				event,
				member.getAsMention() + " you won \"" + gambleWinInfo.getPriceName() + "\"!\n" +
						"Your (rounded) lucky number: " + nf.format(gambleWinInfo.getLuckyNumber())
		);
	}

	private void logResult(TextChannel logChannel, String message) {
		sendLogMessage(logChannel, message);
	}
}
