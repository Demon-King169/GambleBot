package com.motorbesitzen.gamblebot.bot.command.impl.coin;

import com.motorbesitzen.gamblebot.bot.command.CommandImpl;
import com.motorbesitzen.gamblebot.data.dao.DiscordGuild;
import com.motorbesitzen.gamblebot.data.repo.DiscordGuildRepo;
import com.motorbesitzen.gamblebot.util.SlashOptionUtil;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Sets a channel where coin commands are allowed in.
 */
@Service("coinchannel")
class CoinChannel extends CommandImpl {

	private static final String OPTION_CHANNEL_NAME = "channel";

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
	public String getDescription() {
		return "Sets a channel as the only channel where coin commands can be used in. If there is no " +
				"coin channel set coin commands can be used in every channel the bot has access to. To clear " +
				"the selected channel use an invalid channel like a category or a voice channel.";
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
	public void register(JDA jda) {
		jda.upsertCommand(getName(), "Sets a channel as the only channel where coin commands can be used in.")
				.addOptions(
						new OptionData(
								OptionType.CHANNEL,
								OPTION_CHANNEL_NAME,
								"The channel you want to allow coin messages in. Check the help command for further information.",
								true
						).setChannelTypes(ChannelType.TEXT)
				).queue();
	}

	@Override
	public void execute(SlashCommandEvent event) {
		final Guild guild = event.getGuild();
		if (guild == null) {
			return;
		}

		final long guildId = guild.getIdLong();
		GuildChannel channel = SlashOptionUtil.getGuildChannelOption(event, OPTION_CHANNEL_NAME);
		if (isInvalidGuildChannel(channel)) {
			clearCoinChannel(guildId);
			reply(event, "Cleared the coin channel selection.");
			return;
		}

		TextChannel mentionedChannel = channel.getGuild().getTextChannelById(channel.getIdLong());
		if (isInvalidTextChannel(mentionedChannel)) {
			reply(event, "Invalid channel! Please select a channel where I can read and write in.");
			return;
		}

		setCoinChannel(guildId, mentionedChannel.getIdLong());
		reply(event, "Set the coin channel to " + mentionedChannel.getAsMention() + "!");
	}

	private boolean isInvalidGuildChannel(GuildChannel channel) {
		if (channel == null) {
			return true;
		}

		return channel.getType() != ChannelType.TEXT;
	}

	private void clearCoinChannel(final long guildId) {
		setCoinChannel(guildId, 0);
	}

	private boolean isInvalidTextChannel(TextChannel channel) {
		if (channel == null) {
			return true;
		}

		return !channel.canTalk();
	}

	private void setCoinChannel(final long guildId, final long channelId) {
		final Optional<DiscordGuild> dcGuildOpt = guildRepo.findById(guildId);
		final DiscordGuild dcGuild = dcGuildOpt.orElseGet(() -> DiscordGuild.withGuildId(guildId));
		dcGuild.setCoinChannelId(channelId);
		guildRepo.save(dcGuild);
	}
}
