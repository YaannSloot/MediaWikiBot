package com.yaannsloot.mediawikibot.discord.commands;

import com.yaannsloot.mediawikibot.exceptions.NoMatchException;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public abstract class Command {

	public abstract boolean CheckForCommandMatch(Message command);

	public abstract void execute(MessageReceivedEvent event) throws NoMatchException;

	public abstract String getHelpSnippet();

}
