package main.yaannsloot.mediawikibot.sources.endpoints;

import java.awt.Color;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabaseManager {

	private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);

	private File databaseFile;

	/**
	 * Sets the target csv database file to export to
	 * 
	 * @param targetFile The target csv file
	 */
	public void setDatabaseFile(File targetFile) {
		this.databaseFile = targetFile;
	}

	/**
	 * Loads endpoints from database file into memory. If multiple endpoints have
	 * the same prefix, only the first occurrence will be loaded.
	 * 
	 * @return A list containing all the endpoints in the database
	 * @throws IOException If an I/O error occurs when loading the database file
	 */
	public List<WikiEndpoint> loadEndpoints() throws IOException {
		logger.info("Loading endpoints from database...");
		List<WikiEndpoint> endpoints = new ArrayList<WikiEndpoint>();
		List<String> repeatingRefIds = new ArrayList<String>();
		List<String> refIds = new ArrayList<String>();
		if (!databaseFile.exists()) {
			FileUtils.forceMkdirParent(databaseFile);
			BufferedWriter writer = Files.newBufferedWriter(Paths.get(databaseFile.toURI()));
			CSVPrinter csvExporter = new CSVPrinter(writer,
					CSVFormat.RFC4180.withHeader("referenceid", "endpoint", "resolverid", "displaycolor"));
			csvExporter.close();
		}
		CSVParser databaseData = CSVParser.parse(databaseFile, Charset.defaultCharset(),
				CSVFormat.RFC4180.withHeader().withDelimiter(',').withRecordSeparator('\n'));
		List<CSVRecord> records = databaseData.getRecords();
		for (CSVRecord record : records) {
			if (refIds.contains(record.get("referenceid")) && !repeatingRefIds.contains(record.get("referenceid"))
					&& !record.get("referenceid").equals("!disabled")) {
				repeatingRefIds.add(record.get("referenceid"));
			} else if (!record.get("referenceid").equals("!disabled")) {
				refIds.add(record.get("referenceid"));
				Color displayColor;
				if (record.get("displaycolor").equals("!random")) {
					displayColor = new Color((int) (Math.random() * 256), (int) (Math.random() * 256),
							(int) (Math.random() * 256));
				} else {
					try {
						displayColor = Color.decode(record.get("displaycolor"));
					} catch (NumberFormatException e) {
						logger.warn("Display color for listing \"" + record.get("referenceid")
								+ "\" is in an unrecognized format");
						displayColor = new Color((int) (Math.random() * 256), (int) (Math.random() * 256),
								(int) (Math.random() * 256));
					}
				}
				endpoints.add(new WikiEndpoint(record.get("referenceid"), record.get("endpoint"),
						record.get("resolverid"), displayColor));
			}
		}
		if (repeatingRefIds.size() > 0) {
			logger.warn(repeatingRefIds.size() + " endpoints have repeating reference ids and were ignored");
		}
		logger.info(endpoints.size() + " endpoints loaded successfully.");
		return endpoints;
	}

	/**
	 * Exports a list of api endpoints to the database csv file
	 * 
	 * @param endpoints The list of endpoint urls to add
	 */
	public void exportToDatabase(List<WikiEndpoint> endpoints) {
		logger.info("Exporting endpoints to database...");
		try {
			FileUtils.forceMkdirParent(databaseFile);
			if (!databaseFile.exists()) {
				databaseFile.createNewFile();
			}
			CSVParser databaseData = CSVParser.parse(databaseFile, Charset.defaultCharset(),
					CSVFormat.RFC4180.withHeader().withDelimiter(',').withRecordSeparator('\n'));
			List<CSVRecord> previousRecords = databaseData.getRecords();
			for (CSVRecord record : previousRecords) {
				for (int i = 0; i < endpoints.size(); i++) {
					if (record.get("endpoint").equals(endpoints.get(i).getApiUrl())) {
						endpoints.remove(i);
						i--;
					}
				}
			}
			logger.info(previousRecords.size() + " previous entries found. " + endpoints.size()
					+ " new endpoints will be added");
			databaseFile.delete();
			databaseFile.createNewFile();
			BufferedWriter writer = Files.newBufferedWriter(Paths.get(databaseFile.toURI()));
			CSVPrinter csvExporter = new CSVPrinter(writer,
					CSVFormat.RFC4180.withHeader("referenceid", "endpoint", "resolverid", "displaycolor"));
			previousRecords.forEach(record -> {
				try {
					csvExporter.printRecord(record.get("referenceid"), record.get("endpoint"), record.get("resolverid"),
							record.get("displaycolor"));
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
			for (WikiEndpoint endpoint : endpoints) {
				String color = "!random";
				if(endpoint.getDisplayColor() != null) {
					color = endpoint.getDisplayColor().toString();
				}
				csvExporter.printRecord(endpoint.getReferenceId(), endpoint.getApiUrl(), endpoint.getResolverId(), color);
			}
			csvExporter.flush();
			csvExporter.close();
			logger.info("Endpoints exported successfully.");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
