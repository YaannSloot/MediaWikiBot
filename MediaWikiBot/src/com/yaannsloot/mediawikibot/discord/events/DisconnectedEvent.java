package com.yaannsloot.mediawikibot.discord.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.dv8tion.jda.api.events.DisconnectEvent;

public class DisconnectedEvent {

private final Logger logger = LoggerFactory.getLogger(DisconnectedEvent.class);
	
	public void DisconnectEvent(DisconnectEvent event) {
		logger.error("Shard " + event.getJDA().getShardInfo().getShardString() + " has been disconnected from the Discord Gateway. Reason: " + event.getCloseCode());
	}
	
}
