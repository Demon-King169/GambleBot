package com.motorbesitzen.gamblebot.bot.command.impl.general;

import com.motorbesitzen.gamblebot.bot.command.Command;
import com.motorbesitzen.gamblebot.bot.command.CommandImpl;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Sends a help message with information about all available commands to the channel where the help was requested.
 */
@Service("help")
class Help extends CommandImpl {

	private static final int FIELDS_PER_EMBED = 25;
	private final Map<String, Command> commandMap;

	@Autowired
	private Help(Map<String, Command> commandMap) {
		this.commandMap = commandMap;
	}

	@Override
	public String getName() {
		return "help";
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
	public String getDescription() {
		return "Shows a list of commands that can be used.";
	}

	@Override
	public void register(JDA jda) {
		jda.upsertCommand(getName(), getDescription()).queue();
	}

	/**
	 * Sends a help message.
	 *
	 * @param event The event provided by JDA that a guild message got received.
	 */
	@Override
	public void execute(SlashCommandEvent event) {
		Member author = event.getMember();
		if (author != null) {
			sendHelpMessage(event, author);
		}
	}

	/**
	 * Sends the help message in the channel where the help got requested.
	 *
	 * @param event  The slash command event.
	 * @param author The author of the command.
	 */
	private void sendHelpMessage(SlashCommandEvent event, Member author) {
		List<Command> commands = new ArrayList<>(commandMap.values());
		if (commands.size() == 0) {
			reply(event, "No commands found!");
			return;
		}

		List<Command> fittingCommands = new ArrayList<>();
		for (Command command : commands) {
			if (command.isAdminCommand() && !author.hasPermission(Permission.ADMINISTRATOR)) {
				continue;
			}

			fittingCommands.add(command);
		}

		if (fittingCommands.size() == 0) {
			reply(event, "No commands found!");
			return;
		}

		int pages = (fittingCommands.size() / FIELDS_PER_EMBED) + 1;
		List<MessageEmbed> embeds = buildPages(pages, fittingCommands);
		replyMultipleEmbeds(event, embeds);
	}

	private List<MessageEmbed> buildPages(int pages, List<Command> fittingCommands) {
		List<MessageEmbed> embeds = new ArrayList<>();
		EmbedBuilder eb = buildEmbedPage(1, pages);
		for (int i = 0; i < fittingCommands.size(); i++) {
			if (i > 0 && i % 25 == 0) {
				embeds.add(eb.build());
				eb = buildEmbedPage((i / FIELDS_PER_EMBED) + 1, pages);
			}

			Command command = fittingCommands.get(i);
			addHelpEntry(eb, command);
		}

		embeds.add(eb.build());
		return embeds;
	}

	/**
	 * Creates a numerated page for help entries. Can have up to 25 command fields.
	 *
	 * @param page       The current page number.
	 * @param totalPages The total pages needed to display all commands
	 * @return An {@code EmbedBuilder} with page identification if needed.
	 */
	private EmbedBuilder buildEmbedPage(int page, int totalPages) {
		return new EmbedBuilder().setTitle(
				page == 1 && totalPages == 1 ?
						"Commands and their variations" :
						"Commands and their variations [" + page + "/" + totalPages + "]"
		).setDescription(
				"A list of all commands you can use and what they do. " +
						"Note that \"(a|b|c)\" means that a, b or c can be chosen. \"<...>\" " +
						"requires you to add a value. Do not keep the \"<\", \">\"!"
		);
	}

	/**
	 * Adds an entry for a command.
	 *
	 * @param eb      The {@code EmbedBuilder} to which each commands help information gets.
	 * @param command The command to add to the help page.
	 */
	private void addHelpEntry(EmbedBuilder eb, Command command) {
		String title = "/" + command.getName();
		eb.addField(title, command.getDescription(), false);
	}
}
