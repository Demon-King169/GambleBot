package com.motorbesitzen.gamblebot.bot.command.impl.coin.game;

import com.motorbesitzen.gamblebot.bot.command.CommandImpl;
import com.motorbesitzen.gamblebot.util.SlashOptionUtil;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import org.springframework.stereotype.Service;

@Service
public abstract class PlayCommandImpl extends CommandImpl {

	@Override
	public abstract String getName();

	@Override
	public abstract String getDescription();

	@Override
	public abstract boolean isAdminCommand();

	@Override
	public abstract boolean isGlobalCommand();

	@Override
	public abstract void register(JDA jda);

	@Override
	public abstract void execute(SlashCommandEvent event);

	protected long getWager(SlashCommandEvent event, String wager_option_name) {
		Long wager = SlashOptionUtil.getIntegerOption(event, wager_option_name);
		if (wager == null) {
			wager = -1L;
		}

		return wager;
	}

	protected String getBetString(SlashCommandEvent event, String wagerOptionName) {
		String name = SlashOptionUtil.getStringOption(event, wagerOptionName);
		if (name == null) {
			return null;
		}

		if (name.isBlank()) {
			return null;
		}

		return name;
	}
}
