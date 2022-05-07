package com.motorbesitzen.gamblebot.bot.event;

import com.motorbesitzen.gamblebot.bot.command.Command;
import com.motorbesitzen.gamblebot.bot.command.CommandImpl;
import com.motorbesitzen.gamblebot.util.LogUtil;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * On each start the bot registers the slash commands the bot offers on the Discord API so that users can use them in
 * their client. Command registration might need up to an hour to propagate on Discord's end!
 */
@Service
public class SlashCommandRegister extends ListenerAdapter {

	private final Set<? extends CommandImpl> commands;

	@Autowired
	private SlashCommandRegister(Set<? extends CommandImpl> commands) {
		this.commands = commands;
	}

	/**
	 * Gets executed when the Discord bot is ready. Registers the commands on Discord's backend.
	 * However, it does not wait for completion of the requests; it might take a couple seconds.
	 *
	 * @param event The event that gets fired when the bot is ready.
	 */
	@Override
	public void onReady(@NotNull ReadyEvent event) {
		LogUtil.logInfo("Registering commands...");

		JDA jda = event.getJDA();
		for (Command command : commands) {
			command.register(jda);
		}
	}
}
