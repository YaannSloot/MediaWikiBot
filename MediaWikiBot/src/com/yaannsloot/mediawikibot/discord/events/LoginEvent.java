package com.yaannsloot.mediawikibot.discord.events;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yaannsloot.mediawikibot.core.MediaWikiBot;
import com.yaannsloot.mediawikibot.exceptions.WikiSourceNotFoundException;
import com.yaannsloot.mediawikibot.sources.endpoints.retrievers.EndpointRetriever;
import com.yaannsloot.mediawikibot.tools.BotUtils;

import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.ReadyEvent;

public class LoginEvent {

	private static int loginCounter = 0;
	private boolean disableInitialLoad = false;

	private final Logger logger = LoggerFactory.getLogger(LoginEvent.class);

	public void BotLoginEvent(ReadyEvent event) {
		logger.info("Shard " + event.getJDA().getShardInfo().getShardId() + " has logged in.");
		event.getJDA().getShardManager().setPresence(OnlineStatus.ONLINE,
				Activity.playing(MediaWikiBot.botPrefix + " help"));

		loginCounter++;

		if (loginCounter == MediaWikiBot.shardmgr.getShardsTotal() && disableInitialLoad == false) {
			disableInitialLoad = true;
			logger.info("All shards have logged in successfully");

			class commandReader implements Runnable {

				@Override
				public void run() {
					String command = "";
					while (!(command.equals("shutdown"))) {
						try {
							command = MediaWikiBot.lineReader.readLine(">");
							List<String> words = Arrays.asList(BotUtils.normalizeSentence(command).split(" "));
							switch (words.get(0)) {
							case "shutdown":
								System.out.print("Shutdown requested. Shutting down shards...");
								break;
							case "listsources":
								Map<String, List<String>> sourceList = new HashMap<>();
								for (EndpointRetriever retriever : MediaWikiBot.retrieverList) {
									sourceList.put(retriever.getRetrieverName(), retriever.getSourceNames());
								}
								for (String retriever : sourceList.keySet()) {
									System.out.print(retriever + ":");
									sourceList.get(retriever).forEach(source -> System.out.print("   " + source));
								}
								break;
							case "addsource":
								if (words.size() >= 3) {
									boolean autoenable = true;
									if (words.size() >= 4) {
										if (words.get(3).equals("noauto"))
											autoenable = false;
									}
									try {
										if (words.get(1).contains(":")) {
											String[] sourceArgs = words.get(1).split(":");
											if (sourceArgs.length == 2) {
												for (EndpointRetriever retriever : MediaWikiBot.retrieverList) {
													if (retriever.getRetrieverName().equals(sourceArgs[0])) {
														MediaWikiBot.databaseLoader.exportToDatabase(
																retriever.extractEndpoints(sourceArgs[1], words.get(2),
																		autoenable));
														break;
													}
												}
											}
										}
									} catch (WikiSourceNotFoundException e) {
										logger.error("Source not found");
									}
								} else {
									System.out.print("ERROR: Too few arguments");
								}
								break;
							case "reload":
								MediaWikiBot.endpoints = MediaWikiBot.databaseLoader.loadEndpoints();
								break;
							case "status":
								System.out.print("\nBot Stats\n---------------\nShards: "
										+ event.getJDA().getShardManager().getShardsTotal() + "\n" + "Guilds: "
										+ event.getJDA().getShardManager().getGuilds().size() + "\n"
										+ "\nResource usage\n---------------\n" + "Threads: " + Thread.activeCount()
										+ "\n" + "Memory Usage: "
										+ (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())
												/ 1000000
										+ "/" + Runtime.getRuntime().maxMemory() / 1000000 + " MB\n");
								break;
							default:
								System.out.print("ERROR: Command not recognized");
								break;
							}
						} catch (Exception e) {
							System.out.print("ERROR: Command parse error");
						}
					}
					MediaWikiBot.shardmgr.shutdown();
					System.out.print("Bot is shutting down...");
					System.exit(0);
				}
			}

			Thread commandHandler = new Thread(new commandReader());
			commandHandler.start();

		}
	}

}
