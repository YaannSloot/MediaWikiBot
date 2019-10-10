package main.yaannsloot.mediawikibot.discord.events;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import main.yaannsloot.mediawikibot.core.MediaWikiBot;
import main.yaannsloot.mediawikibot.exceptions.WikiProjectNotFoundException;
import main.yaannsloot.mediawikibot.tools.BotUtils;
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
								System.out.println("Shutdown requested. Shutting down shards...");
								break;
							case "listprojects":
								List<String> projects = MediaWikiBot.statLoader.getMediaWikis();
								projects.forEach(project -> System.out.println(project));
								break;
							case "addproject":
								if (words.size() >= 3) {
									boolean autoenable = true;
									if (words.size() >= 4) {
										if (words.get(3).equals("noauto"))
											autoenable = false;
									}
									try {
										MediaWikiBot.statLoader.exportToDatabase(
												MediaWikiBot.statLoader.getProjectEndpoints(words.get(1), words.get(2)),
												autoenable);
									} catch (WikiProjectNotFoundException e) {
										logger.error("Project not found");
									}
								} else {
									System.out.println("ERROR: Too few arguments");
								}
								break;
							case "reload":
								MediaWikiBot.endpoints = MediaWikiBot.statLoader.loadEndpoints();
								break;
							case "status":
								System.out.println("\nBot Stats\n---------------\nShards: "
										+ event.getJDA().getShardManager().getShardsTotal() + "\n" + "Guilds: "
										+ event.getJDA().getShardManager().getGuilds().size() + "\n"
										+ "\nResource usage\n---------------\n" + "Threads: " + Thread.activeCount()
										+ "\n" + "Memory Usage: "
										+ (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())
												/ 1000000
										+ "/" + Runtime.getRuntime().maxMemory() / 1000000 + " MB\n");
								break;
							default:
								System.out.println("ERROR: Command not recognized");
								break;
							}
						} catch (Exception e) {
							System.out.println("ERROR: Command parse error");
						}
					}
					MediaWikiBot.shardmgr.shutdown();
					System.out.println("Bot is shutting down...");
					System.exit(0);
				}
			}

			Thread commandHandler = new Thread(new commandReader());
			commandHandler.start();

		}
	}

}
