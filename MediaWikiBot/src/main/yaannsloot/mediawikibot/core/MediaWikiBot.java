package main.yaannsloot.mediawikibot.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.FileSystemUtils;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.PropertyConfigurator;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import main.yaannsloot.mediawikibot.exceptions.WikiProjectNotFoundException;
import main.yaannsloot.mediawikibot.tools.StatFetcher;

public class MediaWikiBot {

	private static final Logger logger = LoggerFactory.getLogger(MediaWikiBot.class);

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
					// TODO Auto-generated constructor stub
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
			System.out.println("Running initial core file check...");
			System.out.println("Loading log4j.properties file...");
			if (!logProperties.exists()) {
				doShutdown = true;
				System.out.println("Error: log4j.properties file does not exist. Attempting to create a new one...");
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
					System.out.println("Wrote default settings to log4j.properties file successfully");
				} catch (IOException e) {
					e.printStackTrace();
					System.out.println(
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
				System.out.println("Error: Could not set " + logProperties.getAbsolutePath() + " as properties file");
				System.exit(1);
			}
			
			if(!settingsDir.exists()) {
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
			
			if(!database.exists()) {
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
			
			if(!(new File("start.sh").exists() || new File("start.bat").exists())) {
				doShutdown = true;
				File startScript;
				if(System.getProperty("os.name").toLowerCase().contains("win")) {
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
			
			if(doShutdown) {
				System.exit(0);
			}
			
			StatFetcher test = new StatFetcher();
			
			try {
				List<String> projects = test.getMediaWikis();
				List<String> endpoints = test.getProjectEndpoints("gamepedias", "English");
				endpoints.forEach(pp -> System.out.println(pp));
			} catch (WikiProjectNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}