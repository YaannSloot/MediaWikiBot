package com.yaannsloot.mediawikibot.discord.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yaannsloot.mediawikibot.core.MediaWikiBot;

import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.ReconnectedEvent;

public class ReconnectEvent {

	private final Logger logger = LoggerFactory.getLogger(ReconnectEvent.class);

	public void ReconnectedEvent(ReconnectedEvent event) {
		logger.info("Shard " + event.getJDA().getShardInfo().getShardString()
				+ " has reconnected to the gateway successfully");
		event.getJDA().getShardManager().setPresence(OnlineStatus.ONLINE,
				Activity.playing(MediaWikiBot.botPrefix + " help"));
	}

}
