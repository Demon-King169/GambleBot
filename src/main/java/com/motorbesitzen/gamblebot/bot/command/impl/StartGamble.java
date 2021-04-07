package com.motorbesitzen.gamblebot.bot.command.impl;


import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.motorbesitzen.gamblebot.bot.command.CommandImpl;
import com.motorbesitzen.gamblebot.data.dao.DiscordGuild;
import com.motorbesitzen.gamblebot.data.dao.GamblePrize;
import com.motorbesitzen.gamblebot.data.dao.GambleSettings;
import com.motorbesitzen.gamblebot.data.repo.DiscordGuildRepo;
import com.motorbesitzen.gamblebot.data.repo.GamblePrizeRepo;
import com.motorbesitzen.gamblebot.data.repo.GambleSettingsRepo;
import com.motorbesitzen.gamblebot.util.DiscordMessageUtil;
import com.motorbesitzen.gamblebot.util.ParseUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service("startgamble")
class StartGamble extends CommandImpl {

	private static final int TIMEOUT_MINS = 5;
	private final EventWaiter eventWaiter;
	private final DiscordGuildRepo guildRepo;
	private final GambleSettingsRepo settingsRepo;
	private final GamblePrizeRepo prizeRepo;

	@Autowired
	StartGamble(final EventWaiter eventWaiter, final DiscordGuildRepo guildRepo, final GambleSettingsRepo settingsRepo,
				final GamblePrizeRepo prizeRepo) {
		this.eventWaiter = eventWaiter;
		this.guildRepo = guildRepo;
		this.settingsRepo = settingsRepo;
		this.prizeRepo = prizeRepo;
	}

	@Override
	public String getName() {
		return "startgamble";
	}

	@Override
	public String getUsage() {
		return getName();
	}

	@Override
	public String getDescription() {
		return "Starts the dialog to start a gamble.";
	}

	@Override
	public boolean isAdminCommand() {
		return true;
	}

	@Transactional
	@Override
	public void execute(final GuildMessageReceivedEvent event) {
		final Guild guild = event.getGuild();
		final long guildId = guild.getIdLong();
		final Optional<DiscordGuild> dcGuildOpt = guildRepo.findById(guildId);
		final DiscordGuild dcGuild = dcGuildOpt.orElseGet(() -> DiscordGuild.withGuildId(guildId));
		if (dcGuild.hasRunningGamble()) {
			sendErrorMessage(event.getChannel(), "There is already a running gamble! It ends in " + dcGuild.getTimeToEndText() + ".");
			return;
		}

		final long logChannelId = dcGuild.getLogChannelId();
		final TextChannel logChannel = guild.getTextChannelById(logChannelId);
		final Member author = event.getMember();
		if (author == null) {
			return;
		}

		final long authorId = author.getIdLong();
		final TextChannel senderChannel = event.getChannel();
		if (logChannel == null) {
			requestLogChannel(dcGuild, senderChannel, authorId);
			return;
		}

		if (!logChannel.canTalk()) {
			requestLogChannel(dcGuild, senderChannel, authorId);
			return;
		}

		final GambleSettings settings = dcGuild.getGambleSettings();
		if (settings != null) {
			requestOldSettings(dcGuild, senderChannel, authorId);
			return;
		}

		final GambleSettings newSettings = GambleSettings.createDefault(dcGuild);
		dcGuild.setGambleSettings(newSettings);
		settingsRepo.save(newSettings);
		requestDuration(dcGuild, senderChannel, authorId);
	}

	private void requestLogChannel(final DiscordGuild dcGuild, final TextChannel originalChannel, final long originalAuthorId) {
		final long originalChannelId = originalChannel.getIdLong();
		originalChannel.sendMessage("Please mention the channel to log gamble events in:").queue(
				msg -> eventWaiter.waitForEvent(
						GuildMessageReceivedEvent.class,
						newEvent -> {
							if (!isOriginalDialog(newEvent, originalChannelId, originalAuthorId)) {
								return false;
							}

							final Message message = newEvent.getMessage();
							final List<TextChannel> mentionedChannels = message.getMentionedChannels();
							if (mentionedChannels.size() != 1) {
								sendErrorMessage(newEvent.getChannel(), "Please mention exactly one channel! (ID or name does not work)");
								return false;
							}

							if (!mentionedChannels.get(0).canTalk()) {
								sendErrorMessage(newEvent.getChannel(), "Can not talk in that channel! (read/send messages)");
								return false;
							}

							return true;
						},
						newEvent -> {
							final Message message = newEvent.getMessage();
							final List<TextChannel> mentionedChannels = message.getMentionedChannels();
							final TextChannel logChannel = mentionedChannels.get(0);
							final long logChannelId = logChannel.getIdLong();
							dcGuild.setLogChannelId(logChannelId);
							guildRepo.save(dcGuild);

							GambleSettings settings = dcGuild.getGambleSettings();
							if (settings == null) {
								final GambleSettings newSettings = GambleSettings.createDefault(dcGuild);
								dcGuild.setGambleSettings(newSettings);
								settingsRepo.save(newSettings);
								requestDuration(dcGuild, newEvent.getChannel(), originalAuthorId);
							} else {
								requestOldSettings(dcGuild, newEvent.getChannel(), originalAuthorId);
							}
						},
						TIMEOUT_MINS, TimeUnit.MINUTES,
						() -> sendErrorMessage(originalChannel, "Timeout exceeded. Please start the command again.")
				)
		);
	}

	private boolean isOriginalDialog(final GuildMessageReceivedEvent newEvent, final long originalChannelId, final long originalAuthorId) {
		final TextChannel channel = newEvent.getChannel();
		if (channel.getIdLong() != originalChannelId) {
			return false;
		}

		final Member author = newEvent.getMember();
		if (author == null) {
			return false;
		}

		return author.getIdLong() == originalAuthorId;
	}

	private void requestOldSettings(final DiscordGuild dcGuild, final TextChannel originalChannel, final long originalAuthorId) {
		final long originalChannelId = originalChannel.getIdLong();
		originalChannel.sendMessage(
				"There are old settings available. Do you want to use them again? (Yes/No)\n" +
						"*Answering `Yes` will start the gamble with your old settings!*"
		).embed(getCurrentSettingsEmbed(dcGuild)).queue(
				msg -> eventWaiter.waitForEvent(
						GuildMessageReceivedEvent.class,
						newEvent -> {
							if (!isOriginalDialog(newEvent, originalChannelId, originalAuthorId)) {
								return false;
							}

							final Message message = newEvent.getMessage();
							final String content = message.getContentRaw();
							if (!content.matches("(?i)[yn].*")) {
								sendErrorMessage(newEvent.getChannel(), "Please answer with `Yes` or `No`!");
								return false;
							}

							return true;
						},
						newEvent -> {
							final Message message = newEvent.getMessage();
							final String content = message.getContentRaw();
							if (content.startsWith("n")) {
								requestDuration(dcGuild, newEvent.getChannel(), originalAuthorId);
							} else {
								requestStart(dcGuild, newEvent.getChannel(), originalAuthorId);
							}
						},
						TIMEOUT_MINS, TimeUnit.MINUTES,
						() -> sendErrorMessage(originalChannel, "Timeout exceeded. Please start the command again.")
				)
		);
	}

	private MessageEmbed getCurrentSettingsEmbed(final DiscordGuild dcGuild) {
		final GambleSettings settings = dcGuild.getGambleSettings();
		final EmbedBuilder eb = new EmbedBuilder();
		final String prizeText = settings.getPrizeText();
		eb.setTitle("Current settings:")
				.addField("Duration:", ParseUtil.parseMillisecondsToText(settings.getDurationMs()), true)
				.addField("Cooldown:", ParseUtil.parseMillisecondsToText(settings.getCooldownMs()), true)
				.addBlankField(false)
				.addField("Prizes:", prizeText.substring(0, Math.min(1999, prizeText.length())), false);
		return eb.build();
	}

	private void requestDuration(final DiscordGuild dcGuild, final TextChannel originalChannel, final long originalAuthorId) {
		final GambleSettings settings = GambleSettings.createDefault(dcGuild);
		dcGuild.setGambleSettings(settings);
		settings.setPrizes(new HashSet<>());
		settingsRepo.save(settings);
		final long originalChannelId = originalChannel.getIdLong();
		originalChannel.sendMessage("How long should the gamble last (**d**ays **h**ours, **m**inutes, **s**econds)? Example: `3h 15s`").queue(
				msg -> eventWaiter.waitForEvent(
						GuildMessageReceivedEvent.class,
						newEvent -> {
							if (!isOriginalDialog(newEvent, originalChannelId, originalAuthorId)) {
								return false;
							}

							final Message message = newEvent.getMessage();
							final String content = message.getContentRaw();
							if (content.isBlank()) {
								sendErrorMessage(newEvent.getChannel(), "Please insert a valid duration like `1d 12h 30m 55s`!");
								return false;
							}

							if (!content.matches("(?i)([0-9]+d *)?([0-9]+h *)?([0-9]+m *)?([0-9]+s)?")) {
								sendErrorMessage(newEvent.getChannel(), "Please insert a valid duration like `1d 12h 30m 55s`!");
								return false;
							}

							final long durationMs = ParseUtil.parseTextToMilliseconds(content);
							if (durationMs < 1000 || durationMs > 31556952000L) {
								sendErrorMessage(newEvent.getChannel(), "Please choose a duration above 1 second and below 1 year!");
								return false;
							}

							return true;
						},
						newEvent -> {
							final Message message = newEvent.getMessage();
							final String content = message.getContentRaw();
							final long durationMs = ParseUtil.parseTextToMilliseconds(content);
							final GambleSettings settingsUpdate = dcGuild.getGambleSettings();
							settingsUpdate.setDurationMs(durationMs);
							requestCooldown(dcGuild, newEvent.getChannel(), originalAuthorId);
						},
						TIMEOUT_MINS, TimeUnit.MINUTES,
						() -> sendErrorMessage(originalChannel, "Timeout exceeded. Please start the command again.")
				)
		);
	}

	private void requestCooldown(final DiscordGuild dcGuild, final TextChannel originalChannel, final long originalAuthorId) {
		final long originalChannelId = originalChannel.getIdLong();
		originalChannel.sendMessage(
				"How long should the cooldown last until a user can participate again (**d**ays, **h**ours, **m**inutes, **s**econds)?\n" +
						"*If a high cooldown is chosen and a new gamble starts in the meantime the cooldown will **not** be reset!*"
		).queue(
				msg -> eventWaiter.waitForEvent(
						GuildMessageReceivedEvent.class,
						newEvent -> {
							if (!isOriginalDialog(newEvent, originalChannelId, originalAuthorId)) {
								return false;
							}

							final Message message = newEvent.getMessage();
							final String content = message.getContentRaw();
							if (!content.matches("(?i)([0-9]+d *)?([0-9]+h *)?([0-9]+m *)?([0-9]+s)?")) {
								sendErrorMessage(newEvent.getChannel(), "Please insert a valid cooldown like `1d 12h 30m 55s`!");
								return false;
							}

							final long cooldownMs = ParseUtil.parseTextToMilliseconds(content);
							if (cooldownMs < 1000 || cooldownMs > 31556952000L) {
								sendErrorMessage(newEvent.getChannel(), "Please choose a cooldown above 1 second and below 1 year!");
								return false;
							}

							return true;
						},
						newEvent -> {
							final Message message = newEvent.getMessage();
							final String content = message.getContentRaw();
							final long cooldownMs = ParseUtil.parseTextToMilliseconds(content);
							final GambleSettings settings = dcGuild.getGambleSettings();
							settings.setCooldownMs(cooldownMs);
							requestPrize(dcGuild, newEvent.getChannel(), originalAuthorId);
						},
						TIMEOUT_MINS, TimeUnit.MINUTES,
						() -> sendErrorMessage(originalChannel, "Timeout exceeded. Please start the command again.")
				)
		);
	}

	private void requestPrize(final DiscordGuild dcGuild, final TextChannel originalChannel, final long originalAuthorId) {
		final long originalChannelId = originalChannel.getIdLong();
		final Set<GamblePrize> prizes = dcGuild.getGambleSettings().getPrizes();
		final String request = prizes.size() == 0 ?
				"Add prizes with their chances. A chance has to be greater than 0 but smaller or equal to 100. " +
						"All chances combined are not allowed to surpass 100%!\nTo add a win with its chance " +
						"type `\"win description\" <number>%`.\nReplace <number> with the desired percentage " +
						"(e.g. `\"Fortnite Gift Card $19\" 15.3%`).\n" +
						"If you add \"ban\" or \"kick\" the 'winner' will get kicked or banned if the bot has the needed permissions.\n" +
						"*To start the giveaway type `start`. If your wins do not add up to 100% the free space will be a loss (a blank)!*" :
				"Entered " + prizes.size() + " prize(s), totaling to " + getTotalPercent(prizes) + "%. To start the giveaway type `start`.";

		originalChannel.sendMessage(request).queue(
				msg -> eventWaiter.waitForEvent(
						GuildMessageReceivedEvent.class,
						newEvent -> {
							if (!isOriginalDialog(newEvent, originalChannelId, originalAuthorId)) {
								return false;
							}

							final Message message = newEvent.getMessage();
							final String content = message.getContentRaw();
							if (content.equalsIgnoreCase("start")) {
								return true;
							}

							if (!content.matches("\".*\" [0-9]{1,3}(\\.[0-9]+)?%")) {
								sendErrorMessage(newEvent.getChannel(), "Please use the correct syntax!");
								return false;
							}

							final List<String> wins = DiscordMessageUtil.getStringsInQuotationMarks(content);
							if (wins.size() != 1) {
								sendErrorMessage(newEvent.getChannel(), "Please only define one win per message!");
								return false;
							}

							if (isDuplicatePrize(prizes, wins.get(0))) {
								sendErrorMessage(newEvent.getChannel(), "Please only define one win per message!");
								return false;
							}

							final double chance = getChance(content);
							if (Double.compare(0d, chance) >= 0) {
								sendErrorMessage(newEvent.getChannel(), "Please select a chance greater than 0%!");
								return false;
							}

							if (Double.compare(100d, chance) < 0) {
								sendErrorMessage(newEvent.getChannel(), "Please select a chance smaller than 100%!");
								return false;
							}

							final double current = getTotalPercent(prizes);
							if (Double.compare(100d, current + chance) < 0) {
								sendErrorMessage(newEvent.getChannel(), "You are not allowed to surpass 100%! You have " + (100.0 - current) + "% left.");
								return false;
							}

							return true;
						},
						newEvent -> {
							final GambleSettings settings = dcGuild.getGambleSettings();
							final Message message = newEvent.getMessage();
							final String content = message.getContentRaw();
							if (content.equalsIgnoreCase("start")) {
								settingsRepo.save(settings);
								requestStart(dcGuild, newEvent.getChannel(), originalAuthorId);
								return;
							}

							final List<String> wins = DiscordMessageUtil.getStringsInQuotationMarks(content);
							final String prizeName = wins.get(0);
							final double chance = getChance(content);
							final GamblePrize prize = new GamblePrize(prizeName, chance, settings);
							prizeRepo.save(prize);
							settings.getPrizes().add(prize);
							if (Double.compare(100d, getTotalPercent(prizes)) == 0) {
								answer(newEvent.getChannel(), "Reached 100%! Starting giveaway...");
								settingsRepo.save(settings);
								requestStart(dcGuild, newEvent.getChannel(), originalAuthorId);
								return;
							}

							requestPrize(dcGuild, newEvent.getChannel(), originalAuthorId);
						},
						TIMEOUT_MINS, TimeUnit.MINUTES,
						() -> sendErrorMessage(originalChannel, "Timeout exceeded. Please start the command again.")
				)
		);
	}

	private double getTotalPercent(final Set<GamblePrize> prizes) {
		double total = 0.0;
		for (GamblePrize prize : prizes) {
			total += prize.getPrizeChance();
		}

		return total;
	}

	private boolean isDuplicatePrize(final Set<GamblePrize> prizes, final String prizeName) {
		for (GamblePrize prize : prizes) {
			if (prize.getPrizeName().equalsIgnoreCase(prizeName)) {
				return true;
			}
		}

		return false;
	}

	private double getChance(String content) {
		final String[] tokens = content.split(" ");
		double chance = -1d;
		for (int i = tokens.length - 1; i >= 0; i--) {
			if (tokens[i].matches("[0-9]{1,3}(\\.[0-9]+)?%")) {
				chance = Double.parseDouble(tokens[i].replace("%", ""));
				break;
			}
		}

		return chance;
	}

	private void requestStart(final DiscordGuild dcGuild, final TextChannel originalChannel, final long originalAuthorId) {
		final long originalChannelId = originalChannel.getIdLong();
		originalChannel.sendMessage(
				"If you want to announce the gamble mention a channel to announce the gamble in. If you want to " +
						"ping users append `everyone` or `here` to your message.\n" +
						"*To start the gamble without an announcement just reply with `exit`.*"
		).queue(
				msg -> eventWaiter.waitForEvent(
						GuildMessageReceivedEvent.class,
						newEvent -> {
							if (!isOriginalDialog(newEvent, originalChannelId, originalAuthorId)) {
								return false;
							}

							final Message message = newEvent.getMessage();
							final String content = message.getContentRaw();
							if (content.equalsIgnoreCase("exit")) {
								return true;
							}

							final List<TextChannel> mentionedChannels = message.getMentionedChannels();
							if (mentionedChannels.size() != 1) {
								sendErrorMessage(newEvent.getChannel(), "Please mention exactly one channel!");
								return false;
							}

							final TextChannel announcementChannel = mentionedChannels.get(0);
							if (!announcementChannel.canTalk()) {
								sendErrorMessage(newEvent.getChannel(), "Can not send an announcement in that channel! Update my permissions or choose another channel.");
								return false;
							}

							return true;
						},
						newEvent -> {
							final GambleSettings settings = dcGuild.getGambleSettings();
							settings.setStartTimestampMs(System.currentTimeMillis());
							settingsRepo.save(settings);
							guildRepo.save(dcGuild);
							final Message message = newEvent.getMessage();
							final String content = message.getContentRaw();
							if (content.equalsIgnoreCase("exit")) {
								answer(newEvent.getChannel(), "Started the gamble!");
								return;
							}

							final List<TextChannel> mentionedChannels = message.getMentionedChannels();
							final TextChannel announcementChannel = mentionedChannels.get(0);
							announcementChannel.sendMessage(
									(content.endsWith("everyone") ? "@everyone\n" : "") +
											(content.endsWith("here") ? "@here\n" : "") +
											"A wild gamble appears!"
							).embed(buildGambleInfoEmbed(dcGuild)).queue();
							answer(newEvent.getChannel(), "Started the gamble!");
						},
						TIMEOUT_MINS, TimeUnit.MINUTES,
						() -> sendErrorMessage(originalChannel, "Timeout exceeded. Please start the command again.")
				)
		);
	}
}
