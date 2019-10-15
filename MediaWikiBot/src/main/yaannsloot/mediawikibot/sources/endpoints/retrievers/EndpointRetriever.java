package main.yaannsloot.mediawikibot.sources.endpoints.retrievers;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import main.yaannsloot.mediawikibot.exceptions.WikiSourceNotFoundException;

/**
 * Scrapes websites for endpoint URLs
 * @author Ian Sloat
 *
 */
public abstract class EndpointRetriever {
	
	private static final Logger logger = LoggerFactory.getLogger(EndpointRetriever.class);

	public ExecutorService executor = Executors.newFixedThreadPool(10);
	
	public abstract String getRetrieverName();
	
	public abstract List<String> getSourceNames();
	
	public abstract List<String> extractEndpointUrls(String source, String language) throws WikiSourceNotFoundException, IOException;
	
	public Future<String> verifyEndpointExistance(String wikiURL) {
		return executor.submit(() -> {
			String endpointUrl = "";
			String wikiLink = wikiURL;
			HttpURLConnection connection;
			try {
				connection = (HttpURLConnection) new URL(wikiLink).openConnection();
				connection.setRequestMethod("GET");
				connection.setConnectTimeout(60000);
				connection.connect();
				if (connection.getResponseCode() != 200) {
					wikiLink =  wikiLink.replace("http://", "https://");
					connection = (HttpURLConnection) new URL(wikiLink).openConnection();
					connection.setRequestMethod("GET");
					connection.setConnectTimeout(60000);
					connection.connect();
				}
				if (connection.getResponseCode() != 200) {
					logger.info("(" + wikiLink + "): Returned " + connection.getResponseCode()
							+ ". Skipping...");
				} else {
					logger.info("(" + wikiLink + ")(Attempt 1): Pinging "
							+ wikiLink + "/api.php...");
					connection = (HttpURLConnection) new URL(wikiLink + "/api.php")
							.openConnection();
					connection.setRequestMethod("GET");
					connection.setConnectTimeout(60000);
					connection.connect();
					if (connection.getResponseCode() == 200) {
						endpointUrl = wikiLink + "/api.php";
						logger.info("(" + wikiLink + "): Endpoint " + wikiLink
								+ "/api.php verified.");
					} else {
						logger.info("(" + wikiLink + ")(Attempt 2): Pinging "
								+ wikiLink + "/w/api.php...");
						connection = (HttpURLConnection) new URL(wikiLink + "/w/api.php")
								.openConnection();
						connection.setRequestMethod("GET");
						connection.setConnectTimeout(60000);
						connection.connect();
						if (connection.getResponseCode() == 200) {
							endpointUrl = wikiLink + "/w/api.php";
							logger.info("(" + wikiLink + "): Endpoint "
									+ wikiLink + "/w/api.php verified.");
						} else {
							logger.info("(" + wikiLink + ")(Attempt 3): Pinging "
									+ wikiLink + "/wiki/api.php...");
							connection = (HttpURLConnection) new URL(wikiLink + "/wiki/api.php")
									.openConnection();
							connection.setRequestMethod("GET");
							connection.setConnectTimeout(60000);
							connection.connect();
							if (connection.getResponseCode() == 200) {
								endpointUrl = wikiLink + "/wiki/api.php";
								logger.info("(" + wikiLink + "): Endpoint "
										+ wikiLink + "/wiki/api.php verified.");
							} else {
								logger.info("(" + wikiLink + "): Endpoint could not be found");
							}
						}
					}
				}
			} catch (Exception e) {
				logger.warn(
						"(" + wikiLink + "): Could not establish a proper connection to host");
			}
			return endpointUrl;
		});
	}
	
}
