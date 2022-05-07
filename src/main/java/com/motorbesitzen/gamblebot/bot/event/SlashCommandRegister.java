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

@Service
public class SlashCommandRegister extends ListenerAdapter {

	private final Set<? extends CommandImpl> commands;

	@Autowired
	private SlashCommandRegister(Set<? extends CommandImpl> commands) {
		this.commands = commands;
	}

	@Override
	public void onReady(@NotNull ReadyEvent event) {
		LogUtil.logInfo("Registering commands...");

		JDA jda = event.getJDA();
		for (Command command : commands) {
			command.register(jda);
		}
	}
}
