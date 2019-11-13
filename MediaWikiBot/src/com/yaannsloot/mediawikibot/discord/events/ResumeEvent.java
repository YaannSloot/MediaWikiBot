package com.yaannsloot.mediawikibot.discord.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yaannsloot.mediawikibot.core.MediaWikiBot;

import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.ResumedEvent;

public class ResumeEvent {

	private final Logger logger = LoggerFactory.getLogger(ResumeEvent.class);

	public void ResumedEvent(ResumedEvent event) {
		logger.info(
				"Shard " + event.getJDA().getShardInfo().getShardString() + " has resumed its session successfully");
		event.getJDA().getShardManager().setPresence(OnlineStatus.ONLINE,
				Activity.playing(MediaWikiBot.botPrefix + " help"));
	}

}
