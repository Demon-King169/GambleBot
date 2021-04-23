package com.motorbesitzen.gamblebot.bot.command.impl.coin;

import com.motorbesitzen.gamblebot.bot.command.CommandImpl;
import com.motorbesitzen.gamblebot.data.dao.DiscordGuild;
import com.motorbesitzen.gamblebot.data.repo.DiscordGuildRepo;
import com.motorbesitzen.gamblebot.util.DiscordMessageUtil;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service("coinchannel")
public class CoinChannel extends CommandImpl {

	private final DiscordGuildRepo guildRepo;

	@Autowired
	private CoinChannel(DiscordGuildRepo guildRepo) {
		this.guildRepo = guildRepo;
	}

	@Override
	public String getName() {
		return "coinchannel";
	}

	@Override
	public String getUsage() {
		return getName() + " (#channel|id)";
	}

	@Override
	public String getDescription() {
		return "Sets a channel as the only channel where coin commands can be used in. If there is no " +
				"coin channel set coin commands can be used in every channel the bot has access to. To clear " +
				"the selected channel use `" + getName() + " 0`";
	}

	@Override
	public boolean isAdminCommand() {
		return true;
	}

	@Override
	public boolean isGlobalCommand() {
		return false;
	}

	@Override
	public void execute(final GuildMessageReceivedEvent event) {
		final Guild guild = event.getGuild();
		final long guildId = guild.getIdLong();
		final Message message = event.getMessage();
		final List<TextChannel> mentionedChannels = message.getMentionedChannels();
		TextChannel mentionedChannel;
		if(mentionedChannels.size() != 0) {
			mentionedChannel = mentionedChannels.get(0);
		} else {
			final long mentionedId = DiscordMessageUtil.getMentionedRawId(message);
			if(mentionedId == 0) {
				clearCoinChannel(guildId);
				answer(event.getChannel(), "Cleared the coin channel selection.");
				return;
			}

			mentionedChannel = guild.getTextChannelById(mentionedId);
		}

		if(mentionedChannel == null) {
			sendErrorMessage(event.getChannel(), "Invalid channel! Please select a valid channel.");
			return;
		}

		if(!mentionedChannel.canTalk()) {
			sendErrorMessage(event.getChannel(), "Invalid channel! Please select a channel where I can read and write in.");
			return;
		}

		setCoinChannel(guildId, mentionedChannel.getIdLong());
		answer(event.getChannel(), "Set the coin channel to " + mentionedChannel.getAsMention() + "!");
	}

	private void clearCoinChannel(final long guildId) {
		setCoinChannel(guildId, 0);
	}

	private void setCoinChannel(final long guildId, final long channelId) {
		final Optional<DiscordGuild> dcGuildOpt = guildRepo.findById(guildId);
		final DiscordGuild dcGuild = dcGuildOpt.orElseGet(() -> DiscordGuild.withGuildId(guildId));
		dcGuild.setCoinChannelId(channelId);
		guildRepo.save(dcGuild);
	}
}
