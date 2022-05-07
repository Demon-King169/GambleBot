package com.motorbesitzen.gamblebot.bot.command.impl.custom;


import com.motorbesitzen.gamblebot.bot.command.CommandImpl;
import com.motorbesitzen.gamblebot.data.dao.DiscordGuild;
import com.motorbesitzen.gamblebot.data.dao.GamblePrize;
import com.motorbesitzen.gamblebot.data.dao.GambleSettings;
import com.motorbesitzen.gamblebot.data.repo.DiscordGuildRepo;
import com.motorbesitzen.gamblebot.data.repo.GamblePrizeRepo;
import com.motorbesitzen.gamblebot.data.repo.GambleSettingsRepo;
import com.motorbesitzen.gamblebot.util.LogUtil;
import com.motorbesitzen.gamblebot.util.ParseUtil;
import com.motorbesitzen.gamblebot.util.SlashOptionUtil;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Starts a custom gamble dialog.
 */
@Service("startgamble")
class StartGamble extends CommandImpl {

	private static final String DURATION_OPTION_NAME = "duration";
	private static final String COOLDOWN_OPTION_NAME = "cooldown";
	private static final String PRIZES_OPTION_NAME = "prize_list";
	private static final String ANNOUNCEMENT_PING_OPTION_NAME = "ping";
	private static final String ANNOUNCEMENT_CHANNEL_OPTION_NAME = "channel";

	private final DiscordGuildRepo guildRepo;
	private final GambleSettingsRepo settingsRepo;
	private final GamblePrizeRepo prizeRepo;

	@Autowired
	StartGamble(DiscordGuildRepo guildRepo, GambleSettingsRepo settingsRepo, GamblePrizeRepo prizeRepo) {
		this.guildRepo = guildRepo;
		this.settingsRepo = settingsRepo;
		this.prizeRepo = prizeRepo;
	}

	@Override
	public String getName() {
		return "startgamble";
	}

	@Override
	public String getDescription() {
		return "Starts a gamble. Uses the last settings if options are left blank.\nThe list of prizes needs to be in the following format: `[price1name;percent],[price2name;percent], ...`\nAll percentages need to add up to 100 or lower. If they do not reach 100% the rest will be a blank (no win). Make sure to only use the semicolon as seperator and not in the name of your prize.\nExample: `[1000 coins:20.5],[Ban:5],[Kick:10.5]` would result in a 64% chance of a blank.";
	}

	@Override
	public boolean isAdminCommand() {
		return true;
	}

	@Override
	public boolean isGlobalCommand() {
		return true;
	}

	@Override
	public void register(JDA jda) {
		jda.upsertCommand(getName(), "Starts a gamble. Uses the last settings if options are left blank.")
				.addOptions(
						new OptionData(
								OptionType.INTEGER,
								DURATION_OPTION_NAME,
								"The duration in minutes."
						).setRequiredRange(1, 525960),    // max 1 year
						new OptionData(
								OptionType.INTEGER,
								COOLDOWN_OPTION_NAME,
								"The cooldown until a user can participate again in seconds."
						).setRequiredRange(0, 31536000),    // max 1 year
						new OptionData(
								OptionType.STRING,
								PRIZES_OPTION_NAME,
								"The list of prizes. Check the help command for the correct format."
						),
						new OptionData(
								OptionType.CHANNEL,
								ANNOUNCEMENT_CHANNEL_OPTION_NAME,
								"The channel to announce the gamble in. Leave blank to post no announcement."
						).setChannelTypes(ChannelType.TEXT),
						new OptionData(
								OptionType.STRING,
								ANNOUNCEMENT_PING_OPTION_NAME,
								"Use \"everyone\" or \"here\" to ping users in the announcement. Leave blank for no ping."
						)
				).queue();
	}

	@Transactional
	@Override
	public void execute(SlashCommandEvent event) {
		Guild guild = event.getGuild();
		if (guild == null) {
			return;
		}

		Member author = event.getMember();
		if (author == null) {
			return;
		}

		long guildId = guild.getIdLong();
		Optional<DiscordGuild> dcGuildOpt = guildRepo.findById(guildId);
		DiscordGuild dcGuild = dcGuildOpt.orElseGet(() -> DiscordGuild.withGuildId(guildId));
		if (dcGuild.hasRunningGamble()) {
			reply(event, "There is already a running gamble! It ends in " + dcGuild.getTimeToEndText() + ".");
			return;
		}

		long logChannelId = dcGuild.getLogChannelId();
		TextChannel logChannel = guild.getTextChannelById(logChannelId);
		if (logChannel == null) {
			reply(event, "Please set a log channel before starting a gamble!");
			return;
		}

		if (!logChannel.canTalk()) {
			reply(event, "Please make sure I can send messages in the log channel!");
			return;
		}

		GambleSettings settings = createSettings(event, dcGuild);
		if (settings == null) {
			reply(event, "Invalid settings!");
			return;
		}

		settingsRepo.save(settings);
		Set<GamblePrize> prizes;
		try {
			prizes = createPrizes(event, settings);
		} catch (IllegalArgumentException e) {
			reply(event, e.getMessage());
			return;
		}

		settings.setPrizes(prizes);
		startGamble(dcGuild, settings);
		reply(event, "Started the gamble!");
		if (shouldAnnounce(event)) {
			announce(event, dcGuild);
		}
	}

	private GambleSettings createSettings(SlashCommandEvent event, DiscordGuild dcGuild) {
		GambleSettings settings = dcGuild.getGambleSettings();
		Long durationValue = SlashOptionUtil.getIntegerOption(event, DURATION_OPTION_NAME);
		long durationMs = getDuration(settings, durationValue) * 60 * 1000;
		if (durationMs <= 0) {
			reply(event, "Please set a valid duration!");
			return null;
		}

		Long cooldownValue = SlashOptionUtil.getIntegerOption(event, COOLDOWN_OPTION_NAME);
		long cooldownMs = getCooldown(settings, cooldownValue) * 1000;
		if (cooldownMs < 0) {
			reply(event, "Please set a valid cooldown!");
			return null;
		}

		return new GambleSettings(durationMs, cooldownMs, dcGuild);
	}

	private long getDuration(GambleSettings settings, Long duration) {
		if (settings == null) {
			return duration;
		}

		if (settings.getDurationMs() == 0) {
			return duration;
		}

		if (duration == null) {
			return settings.getDurationMs();
		}

		return duration;
	}

	private long getCooldown(GambleSettings settings, Long cooldown) {
		if (settings == null) {
			return cooldown;
		}

		if (settings.getCooldownMs() == 0) {
			return cooldown;
		}

		if (cooldown == null) {
			return settings.getCooldownMs();
		}

		return cooldown;
	}

	private Set<GamblePrize> createPrizes(SlashCommandEvent event, GambleSettings settings) {
		String prizeText = SlashOptionUtil.getStringOption(event, PRIZES_OPTION_NAME);
		if (prizeText == null) {
			return settings.getPrizes();
		}

		if (prizeText.isBlank()) {
			return settings.getPrizes();
		}

		prizeText = prizeText.trim();
		if (!prizeText.matches("(\\[.+;\\d{1,3}(\\.\\d+)?%?],?)+")) {
			LogUtil.logDebug("Prize list does not match regex!");
			throw new IllegalArgumentException("Please follow the exact syntax! Check the help command for further information.");
		}

		List<String> prizeInfos = filterPrizeInfo(prizeText);
		Set<GamblePrize> prizes = buildPrizes(settings, prizeInfos);
		if (hasInvalidChances(prizes)) {
			throw new IllegalArgumentException("The sum of chances cannot be higher than 100!");
		}

		return prizes;
	}

	private List<String> filterPrizeInfo(String prizeText) {
		List<String> prizeInfos = new ArrayList<>();
		Pattern pattern = Pattern.compile("\\[[^]]+;\\d{0,3}(\\.\\d+)?%?]");
		Matcher matcher = pattern.matcher(prizeText);
		while (matcher.find()) {
			prizeInfos.add(matcher.group());
		}

		return prizeInfos;
	}

	private Set<GamblePrize> buildPrizes(GambleSettings settings, List<String> prizeInfos) {
		Set<GamblePrize> prizes = new HashSet<>();
		for (String prizeInfo : prizeInfos) {
			if (hasInvalidSeparation(prizeInfo)) {
				throw new IllegalArgumentException("Please follow the exact syntax! Check the help command for further information.");
			}

			String prizeText = prizeInfo.substring(1, prizeInfo.length() - 1);    // remove [ and ]
			String prizeName = prizeText.split(";")[0];
			if (prizeName.isBlank()) {
				throw new IllegalArgumentException("Please set valid names for your prizes!");
			}

			String prizeChanceText = prizeText.split(";")[1].replace("%", "");
			double prizeChance = ParseUtil.safelyParseStringToDouble(prizeChanceText);
			if (Double.compare(0d, prizeChance) >= 0) {
				throw new IllegalArgumentException("All chances need to be greater than 0!");
			}

			GamblePrize prize = new GamblePrize(prizeName, prizeChance, settings);
			prizeRepo.save(prize);
			prizes.add(prize);
		}

		return prizes;
	}

	private boolean hasInvalidSeparation(String prizeInfo) {
		String noSeparator = prizeInfo.replaceAll(";", "");
		return prizeInfo.length() - 1 != noSeparator.length();
	}

	private boolean hasInvalidChances(Set<GamblePrize> prizes) {
		return Double.compare(100d, getTotalPercent(prizes)) < 0;
	}

	private double getTotalPercent(Set<GamblePrize> prizes) {
		double total = 0.0;
		for (GamblePrize prize : prizes) {
			total += prize.getPrizeChance();
		}

		return total;
	}

	private void startGamble(DiscordGuild dcGuild, GambleSettings settings) {
		settings.setStartTimestampMs(System.currentTimeMillis());
		settingsRepo.save(settings);
		dcGuild.setGambleSettings(settings);
		guildRepo.save(dcGuild);
	}

	private boolean shouldAnnounce(SlashCommandEvent event) {
		GuildChannel channel = SlashOptionUtil.getGuildChannelOption(event, ANNOUNCEMENT_CHANNEL_OPTION_NAME);
		return channel != null;
	}

	private void announce(SlashCommandEvent event, DiscordGuild dcGuild) {
		TextChannel channel = (TextChannel) SlashOptionUtil.getGuildChannelOption(event, ANNOUNCEMENT_CHANNEL_OPTION_NAME);
		if (channel == null) {
			return;
		}

		if (!channel.canTalk()) {
			event.getChannel().sendMessage("I cannot send an announcement in the given channel! The gamble started anyway, but you need to post the announcement yourself.").queue();
			return;
		}

		String ping = SlashOptionUtil.getStringOption(event, ANNOUNCEMENT_PING_OPTION_NAME);
		String text = getFittingMention(ping) + "A wild gamble appears!";
		channel.sendMessage(text).setEmbeds(buildGambleInfoEmbed(dcGuild)).queue();
	}

	private String getFittingMention(String ping) {
		if (ping == null) {
			return "";
		}

		if (ping.equalsIgnoreCase("everyone") || ping.equalsIgnoreCase("here")) {
			return "@" + ping + "\n";
		}

		return "";
	}
}
