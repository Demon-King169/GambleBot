package com.motorbesitzen.gamblebot.bot.command.impl.coin;

import com.motorbesitzen.gamblebot.bot.command.CommandImpl;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.springframework.stereotype.Service;

@Service("redeem")
public class Redeem extends CommandImpl {

	@Override
	public String getName() {
		return "redeem";
	}

	@Override
	public String getUsage() {
		return getName();
	}

	@Override
	public String getDescription() {
		return "Starts a redeem dialog to trade coins for prizes.";
	}

	@Override
	public boolean isAdminCommand() {
		return false;
	}

	@Override
	public boolean isGlobalCommand() {
		return false;
	}

	/*
	 * If win contains a role mention the user should get that role (-> image perms, embed perms, some other perms)
	 * change nickname
	 *
	 */

	@Override
	public void execute(final GuildMessageReceivedEvent event) {
		// get guild coin prizes

		// print list
			// request index

		// subtract coins, give win if possible, otherwise log
	}
}
