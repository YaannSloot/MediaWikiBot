package com.yaannsloot.mediawikibot.discord.events;

import com.yaannsloot.mediawikibot.core.MediaWikiBot;
import com.yaannsloot.mediawikibot.discord.commands.Command;
import com.yaannsloot.mediawikibot.discord.commands.CommandRegistry;
import com.yaannsloot.mediawikibot.exceptions.NoMatchException;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class CommandHandler {
	
	public void MessageReceivedEvent(MessageReceivedEvent event) {
		if (event.getMessage().getContentRaw().toLowerCase().startsWith(MediaWikiBot.botPrefix + " ")) {
			for(Command c : CommandRegistry.DefaultInstances) {
				if(c.CheckForCommandMatch(event.getMessage())) {
					try {
						c.execute(event);
					} catch (NoMatchException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

}
