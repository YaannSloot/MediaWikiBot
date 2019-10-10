package main.yaannsloot.mediawikibot.discord.commands;

import java.awt.Color;

import main.yaannsloot.mediawikibot.core.MediaWikiBot;
import main.yaannsloot.mediawikibot.exceptions.NoMatchException;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class HelpCommand extends Command {

	@Override
	public boolean CheckForCommandMatch(Message command) {
		return command.getContentRaw().equals(MediaWikiBot.botPrefix + " help");
	}

	@Override
	public void execute(MessageReceivedEvent event) throws NoMatchException {
		if (!(CheckForCommandMatch(event.getMessage()))) {
			throw new NoMatchException();
		}
		EmbedBuilder message = new EmbedBuilder();
		String help = "";
		for (Command command : CommandRegistry.DefaultInstances) {
			help += command.getHelpSnippet() + "\n";
		}
		message.addField("Commands:", help, false);
		message.setColor(Color.decode("#00cc99"));
		event.getAuthor().openPrivateChannel().queue(channel -> channel.sendMessage(message.build()).queue());
		if(event.isFromGuild())
			event.getChannel().sendMessage("Sent a list of commands to you via DM").queue();
	}

	@Override
	public String getHelpSnippet() {
		return MediaWikiBot.botPrefix + " help - Lists commands you can use";
	}

}
