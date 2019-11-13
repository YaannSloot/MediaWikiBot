package com.yaannsloot.mediawikibot.discord.events;

import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.ReconnectedEvent;
import net.dv8tion.jda.api.events.ResumedEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class Events extends ListenerAdapter {
	
	private LoginEvent loginEvent = new LoginEvent();
	private CommandHandler commandHandler = new CommandHandler();
	private ReconnectEvent reconnectEvent = new ReconnectEvent();
	private ResumeEvent resumeEvent = new ResumeEvent();
	
	@Override
	public void onReady(ReadyEvent event) {
		loginEvent.BotLoginEvent(event);
	}

	@Override
	public void onMessageReceived(MessageReceivedEvent event) {
		commandHandler.MessageReceivedEvent(event);
	}
	
	@Override
	public void onReconnect(ReconnectedEvent event) {
		reconnectEvent.ReconnectedEvent(event);
	}
	
	@Override 
	public void onResume(ResumedEvent event){
		resumeEvent.ResumedEvent(event);
	}
	
}
