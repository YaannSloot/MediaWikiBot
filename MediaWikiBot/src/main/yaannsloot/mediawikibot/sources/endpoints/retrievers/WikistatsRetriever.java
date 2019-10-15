package main.yaannsloot.mediawikibot.sources.endpoints.retrievers;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import main.yaannsloot.mediawikibot.exceptions.WikiSourceNotFoundException;

public class WikistatsRetriever extends EndpointRetriever {

	private static final Logger logger = LoggerFactory.getLogger(WikistatsRetriever.class);

	@Override
	public String getRetrieverName() {
		return "wikistats";
	}

	/**
	 * Gets the complete list of mediawiki projects found on
	 * https://wikistats.wmflabs.org
	 * 
	 * @return A list containing the string names of each of the mediawiki projects
	 */
	@Override
	public List<String> getSourceNames() {
		List<String> result = new ArrayList<String>();
		logger.info("Attempting to retrieve the mediawiki project list from https://wikistats.wmflabs.org...");
		Document doc;
		try {
			doc = Jsoup.connect("https://wikistats.wmflabs.org/").get();
			Elements elements = doc.getElementsByAttribute("href");
			Elements sortHeap = new Elements();
			for (Element e : elements) {
				if (e.attr("href").startsWith("display.php?t=")) {
					sortHeap.add(e);
				}
			}
			elements = sortHeap;
			elements.forEach(e -> result.add(e.text()));
			logger.info("Source list retrieved successfully");
		} catch (IOException e1) {
			logger.error("Failed to retrieve source list");
		}
		return result;
	}

	/**
	 * Retrieves all api endpoints related to a mediawiki project
	 * 
	 * @param source   The project to search in
	 * @param language The desired language to retrieve endpoints for
	 * @return A list containing all endpoints related to the project specified
	 * @throws WikiSourceNotFoundException If the mediawiki project is not listed on
	 *                                     https://wikistats.wmflabs.org
	 * @throws IOException                 If an I/O error occurs
	 */
	@Override
	public List<String> extractEndpointUrls(String source, String language)
			throws WikiSourceNotFoundException, IOException {
		List<String> result = new ArrayList<String>();
		List<String> wikiLinks = new ArrayList<String>();
		logger.info("Attempting to retrieve list of wikis from project \"" + source + "\"...");
		URL site = new URL("https://wikistats.wmflabs.org/api.php?action=dump&table=" + source + "&format=csv");
		File temp = File.createTempFile("records", "csv");
		FileUtils.copyURLToFile(site, temp);
		String doc = FileUtils.readFileToString(temp, Charset.defaultCharset());
		temp.delete();
		if (doc.contains("table name not set or unknown")) {
			throw new WikiSourceNotFoundException(source);
		} else {
			CSVParser projectData = CSVParser.parse(doc,
					CSVFormat.RFC4180.withHeader().withDelimiter(',').withRecordSeparator('\n'));
			List<CSVRecord> records = projectData.getRecords();
			logger.info(records.size() + " wikis found");
			if (projectData.getHeaderMap().containsKey("lang")) {
				records = records.stream().filter(rcd -> rcd.get("lang").toLowerCase().equals(language.toLowerCase())
						|| rcd.get("lang").equals("")).collect(Collectors.toList());
				logger.info(records.size() + " wikis either in " + language + " or unspecified");
			} else {
				logger.info("No languages specified in retrieved records so restrictions will be ignored");
			}
			logger.info("Attempting to retrieve " + records.size() + " wiki urls...");
			if (projectData.getHeaderMap().containsKey("prefix")) {
				Document projects = Jsoup.connect("https://wikistats.wmflabs.org/").ignoreContentType(true).get();
				Elements elements = projects.getElementsByAttribute("href");
				String displayUrl = "";
				for (Element e : elements) {
					if (e.attr("href").startsWith("display.php?t=") && e.text().equals(source)) {
						displayUrl = "https://wikistats.wmflabs.org/" + e.attr("href");
						break;
					}
				}
				Document display = Jsoup.connect(displayUrl).get();
				Elements displayLinks = display.getElementsByAttribute("href");
				List<String> prefixes = new ArrayList<String>();
				for (CSVRecord rcd : records) {
					prefixes.add(rcd.get("prefix"));
				}
				String urlTemplate = "";
				for (Element e : displayLinks) {
					if (prefixes.contains(e.text())) {
						try {
							urlTemplate = "http://" + new URL(e.attr("href")).getHost().replace(e.text(), ";;;");
							break;
						} catch (MalformedURLException e1) {
						}
					}
				}
				for (String prefix : prefixes) {
					wikiLinks.add(urlTemplate.replace(";;;", prefix));
				}
			} else if (projectData.getHeaderMap().containsKey("statsurl")) {
				for (CSVRecord rcd : records) {
					try {
						int http = Integer.parseInt(rcd.get("http"));
						if (http == 200 || http == 404)
							wikiLinks.add("http://" + new URL(rcd.get("statsurl")).getHost());
					} catch (MalformedURLException e) {
					}
				}
			}

			logger.info(wikiLinks.size() + " wiki links found. Verifying endpoints...");

			Collection<Future<String>> futures = new LinkedList<Future<String>>();

			for (String wikiLink : wikiLinks) {
				futures.add(verifyEndpointExistance(wikiLink));
			}

			for (Future<String> future : futures) {
				try {
					String endpointUrl = future.get();
					if(!endpointUrl.equals("")) {
						result.add(endpointUrl);
					}
				} catch (InterruptedException | ExecutionException e) {
					e.printStackTrace();
				}
			}

			logger.info(result.size() + " endpoints verified successfully.");
		}
		return result;
	}

}
