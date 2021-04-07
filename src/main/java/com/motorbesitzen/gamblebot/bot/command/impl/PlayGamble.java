package com.motorbesitzen.gamblebot.bot.command.impl;

import com.motorbesitzen.gamblebot.bot.command.CommandImpl;
import com.motorbesitzen.gamblebot.bot.service.GambleGame;
import com.motorbesitzen.gamblebot.data.dao.DiscordGuild;
import com.motorbesitzen.gamblebot.data.dao.DiscordMember;
import com.motorbesitzen.gamblebot.data.dao.GamblePrize;
import com.motorbesitzen.gamblebot.data.repo.DiscordGuildRepo;
import com.motorbesitzen.gamblebot.data.repo.DiscordMemberRepo;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.SelfUser;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service("gamble")
class PlayGamble extends CommandImpl {

	private final DiscordGuildRepo guildRepo;
	private final DiscordMemberRepo memberRepo;
	private final GambleGame gambleGame;

	@Autowired
	PlayGamble(final DiscordGuildRepo guildRepo, final GambleGame gambleGame, final DiscordMemberRepo memberRepo) {
		this.guildRepo = guildRepo;
		this.gambleGame = gambleGame;
		this.memberRepo = memberRepo;
	}

	@Override
	public String getName() {
		return "gamble";
	}

	@Override
	public String getUsage() {
		return getName();
	}

	@Override
	public String getDescription() {
		return "Participate in the gamble.";
	}

	@Override
	public boolean isAdminCommand() {
		return false;
	}

	@Transactional
	@Override
	public void execute(final GuildMessageReceivedEvent event) {
		final Guild guild = event.getGuild();
		final long guildId = guild.getIdLong();
		final Optional<DiscordGuild> dcGuildOpt = guildRepo.findById(guildId);
		dcGuildOpt.ifPresentOrElse(
				dcGuild -> {
					if (!dcGuild.hasRunningGamble()) {
						sendErrorMessage(event.getChannel(), "There is no running gamble.");
						return;
					}

					final Member member = event.getMember();
					if (member == null) {
						return;
					}

					final long memberId = member.getIdLong();
					final Optional<DiscordMember> memberOpt = memberRepo.findById(memberId);
					final DiscordMember player = memberOpt.orElseGet(() -> DiscordMember.createDefault(memberId, dcGuild));
					if (!player.canPlay()) {
						sendErrorMessage(event.getChannel(), "You are on cooldown. You can play again in " + player.getTimeToCooldownEndText() + ".");
						return;
					}

					player.setNextGambleMs(System.currentTimeMillis() + dcGuild.getGambleSettings().getCooldownMs());
					memberRepo.save(player);
					playGamble(event.getChannel(), player, member);
				},
				() -> sendErrorMessage(event.getChannel(), "There is no running gamble.")
		);
	}

	private void playGamble(final TextChannel channel, final DiscordMember player, final Member member) {
		final GamblePrize prize = gambleGame.play(player);
		if (prize == null) {
			answer(channel, "You drew a blank! You did not win anything.");
			return;
		}

		final String playerMention = "<@" + player.getDiscordId() + ">";
		final DiscordGuild dcGuild = player.getGuild();
		final TextChannel logChannel = channel.getGuild().getTextChannelById(dcGuild.getLogChannelId());
		if(prize.getPrizeName().equalsIgnoreCase("ban")) {
			final Member self = channel.getGuild().getSelfMember();
			if(self.canInteract(member)) {
				answer(channel,"Unlucky " + playerMention + "! You won a ban. Enforcing ban in a few seconds...");
				member.ban(0, "'Won' a ban in the gamble.").queueAfter(
						10, TimeUnit.SECONDS,
						b -> answer(channel, "Enforced ban of " + playerMention + ". Rip in pieces :poop:")
				);
				if(logChannel != null) {
					answer(logChannel, playerMention + " won a ban. Enforcing ban in the next 10 seconds.");
				}
			} else {
				answer(channel,"Unlucky " + playerMention + "! You won a ban. Be glad that I can not ban you myself. Reporting to authorities...");
				if(logChannel != null) {
					answer(logChannel, playerMention + " won a ban. However, I can not ban that user.");
				}
			}
			return;
		}

		if(prize.getPrizeName().equalsIgnoreCase("kick")) {
			final Member self = channel.getGuild().getSelfMember();
			if(self.canInteract(member)) {
				answer(channel,"Unlucky " + playerMention + "! You won a kick. Enforcing kick in a few seconds...");
				member.kick("'Won' a kick in the gamble.").queueAfter(
						5, TimeUnit.SECONDS,
						k -> answer(channel, "Enforced kick of " + playerMention + ". Hopefully it is a ban next time :smiling_imp:")
				);
				if(logChannel != null) {
					answer(logChannel, playerMention + " won a kick. Enforcing kick in the next 5 seconds.");
				}
			} else {
				answer(channel,"Unlucky " + playerMention + "! You won a kick. Be glad that I can not kick you myself. Reporting to authorities...");
				if(logChannel != null) {
					answer(logChannel, playerMention + " won a kick. However, I can not kick that user.");
				}
			}
			return;
		}

		answer(channel, playerMention + " you won \"" + prize.getPrizeName() + "\"!");
		if (logChannel != null) {
			answer(logChannel, playerMention + " won \"" + prize.getPrizeName() + "\"!");
		}
	}
}
