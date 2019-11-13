package com.yaannsloot.mediawikibot.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import javax.security.auth.login.LoginException;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.PropertyConfigurator;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yaannsloot.mediawikibot.discord.events.Events;
import com.yaannsloot.mediawikibot.sources.endpoints.DatabaseManager;
import com.yaannsloot.mediawikibot.sources.endpoints.WikiEndpoint;
import com.yaannsloot.mediawikibot.sources.endpoints.retrievers.EndpointRetriever;
import com.yaannsloot.mediawikibot.sources.endpoints.retrievers.FandomRetriever;
import com.yaannsloot.mediawikibot.sources.endpoints.retrievers.GamepediaRetriever;
import com.yaannsloot.mediawikibot.sources.endpoints.retrievers.WikistatsRetriever;

import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ShardManager;

public class MediaWikiBot {

	// Logger
	private static final Logger logger = LoggerFactory.getLogger(MediaWikiBot.class);

	// Static variables
	public static List<WikiEndpoint> endpoints;
	public static DatabaseManager databaseLoader;
	public static ShardManager shardmgr;
	public static String botPrefix = "";
	public static List<EndpointRetriever> retrieverList = new ArrayList<EndpointRetriever>(
			Arrays.asList(new WikistatsRetriever(), new GamepediaRetriever(), new FandomRetriever()));

	// Console
	public static Terminal terminal;

	// Line Reader
	public static LineReader lineReader;

	public static void main(String[] args) {

		try {

			terminal = TerminalBuilder.terminal();

			lineReader = LineReaderBuilder.builder().terminal(terminal).build();

			class ModifiedPrintStream extends PrintStream {

				public ModifiedPrintStream(OutputStream out) {
					super(out, true);
				}

				@Override
				public void write(int b) {
					lineReader.printAbove("" + (char) b);
				}

				@Override
				public void write(byte[] b, int off, int len) {
					if ((off | len | (b.length - (len + off)) | (off + len)) < 0)
						throw new IndexOutOfBoundsException();

					String output = "";

					for (int i = 0; i < len; i++) {
						output += (char) b[off + i];
					}

					lineReader.printAbove(output);
				}

				@Override
				public void write(byte[] b) throws IOException {
					String output = "";
					for (byte bt : b) {
						output += (char) bt;
					}
					lineReader.printAbove(output);
				}

			}

			PrintStream originalStream = System.out;

			System.setOut(new ModifiedPrintStream(originalStream));

			// Initial file checks
			boolean doShutdown = false;
			File logProperties = new File("logging/log4j.properties");
			File settingsFile = new File("settings/settings.json");
			File settingsDir = new File("settings");
			File database = new File("database");
			System.out.print("Running initial core file check...");
			System.out.print("Loading log4j.properties file...");
			if (!logProperties.exists()) {
				doShutdown = true;
				System.out.print("Error: log4j.properties file does not exist. Attempting to create a new one...");
				try {
					FileUtils.forceMkdirParent(logProperties);
					logProperties.createNewFile();
					FileWriter fileOut = new FileWriter(logProperties);
					fileOut.write("log4j.rootLogger=INFO, STDOUT\r\n");
					fileOut.write("log4j.logger.deng=ERROR\r\n");
					fileOut.write("log4j.appender.STDOUT=org.apache.log4j.ConsoleAppender\r\n");
					fileOut.write("log4j.appender.STDOUT.layout=org.apache.log4j.PatternLayout\r\n");
					fileOut.write(
							"log4j.appender.STDOUT.layout.ConversionPattern=%d{HH:mm:ss.SSS} [%p][%t][%c:%M] - %m%n\r\n");
					fileOut.close();
					System.out.print("Wrote default settings to log4j.properties file successfully");
				} catch (IOException e) {
					e.printStackTrace();
					System.out.print(
							"Error: Failed to create a new log4j.properties file. Please check the program's file permissions");
					System.exit(1);
				}
			}

			Properties p = new Properties();
			try {
				p.load(new FileInputStream(logProperties));
				PropertyConfigurator.configure(p);
				logger.info("Successfully loaded log4j.properties file");
			} catch (IOException e) {
				e.printStackTrace();
				System.out.print("Error: Could not set " + logProperties.getAbsolutePath() + " as properties file");
				System.exit(1);
			}

			if (!settingsDir.exists()) {
				doShutdown = true;
				try {
					FileUtils.forceMkdir(settingsDir);
					logger.warn("Settings directory did not exist and was created");
				} catch (IOException e) {
					e.printStackTrace();
					logger.error("Settings directory could not be created");
					System.exit(1);
				}
			}

			if (!database.exists()) {
				doShutdown = true;
				try {
					FileUtils.forceMkdir(database);
					logger.warn("Database directory did not exist and was created");
				} catch (IOException e) {
					e.printStackTrace();
					logger.error("Database directory could not be created");
					System.exit(1);
				}
			}

			if (!(new File("start.sh").exists() || new File("start.bat").exists())) {
				doShutdown = true;
				File startScript;
				if (System.getProperty("os.name").toLowerCase().contains("win")) {
					startScript = new File("start.bat");
				} else {
					startScript = new File("start.sh");
				}
				try {
					startScript.createNewFile();
					FileWriter fileOut = new FileWriter(startScript);
					fileOut.write("java -jar mediawikibot.jar");
					fileOut.close();
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(1);
				}
			}

			if (doShutdown) {
				System.exit(0);
			}

			databaseLoader = new DatabaseManager();
			databaseLoader.setDatabaseFile(new File("database/endpoints.csv"));
			endpoints = databaseLoader.loadEndpoints();

			logger.info("Loading bot settings file...");

			JSONObject settings;

			if (!settingsFile.exists()) {
				FileUtils.forceMkdirParent(settingsFile);
				settingsFile.createNewFile();
				logger.info("Settings file was not discovered. Entering setup mode...");
				settings = promptSettings();
				FileUtils.write(settingsFile, settings.toString(), Charset.defaultCharset());
			} else {
				FileReader fileReader = new FileReader(settingsFile);
				int ch;
				String contents = "";
				while ((ch = fileReader.read()) != -1) {
					contents += (char) ch;
				}
				fileReader.close();
				JSONObject preLoad = new JSONObject(contents);
				if (preLoad.has("token") && preLoad.has("prefix")) {
					settings = preLoad;
				} else {
					logger.info("Settings file was not formatted correctly. Entering setup mode...");
					settings = promptSettings();
					FileUtils.write(settingsFile, settings.toString(), Charset.defaultCharset());
				}
			}

			if (settings.has("token") && settings.has("prefix")) {

				// Bot load
				logger.info("Settings loaded successfully");
				botPrefix = settings.getString("prefix");
				logger.info("Core file check complete. Starting bot...");
				logger.info("Loading shards...");

				try {
					shardmgr = new DefaultShardManagerBuilder(settings.getString("token")).setShardsTotal(-1)
							.addEventListeners(new Events()).build();
				} catch (LoginException | IllegalArgumentException | JSONException e) {
					e.printStackTrace();
					if (e instanceof LoginException) {
						logger.error(
								"The token in your settings file may be incorrect. You should try deleting the file and starting the bot again.");
					}
				}

			} else {
				logger.error("Settings failed to load. Bot shutting down...");
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	// Private methods
	private static JSONObject promptSettings() {
		JSONObject result = new JSONObject();
		System.out.print("Please enter your bot's token:");
		String token = lineReader.readLine(">");
		System.out.print("Please enter your desired command prefix:");
		String prefix = lineReader.readLine(">");
		prefix = prefix.trim();
		prefix = prefix.replace(" ", "_");
		result.put("token", token);
		result.put("prefix", prefix);
		return result;
	}

}
