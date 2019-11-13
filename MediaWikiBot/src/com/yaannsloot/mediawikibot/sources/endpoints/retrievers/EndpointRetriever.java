package com.yaannsloot.mediawikibot.sources.endpoints.retrievers;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.yaannsloot.mediawikibot.exceptions.WikiSourceNotFoundException;
import com.yaannsloot.mediawikibot.sources.endpoints.WikiEndpoint;

/**
 * Scrapes websites for endpoint URLs
 * @author Ian Sloat
 *
 */
public abstract class EndpointRetriever {

	public ExecutorService executor = Executors.newFixedThreadPool(10);
	
	public abstract String getRetrieverName();
	
	public abstract List<String> getSourceNames();
	
	public abstract List<WikiEndpoint> extractEndpoints(String source, String language, boolean autoEnable) throws WikiSourceNotFoundException, IOException;
	
	public abstract List<WikiEndpoint> extractEndpoints(String source, String language) throws WikiSourceNotFoundException, IOException;
	
	public abstract List<WikiEndpoint> extractEndpoints(String language) throws WikiSourceNotFoundException, IOException;
	
	public abstract List<WikiEndpoint> extractEndpoints() throws WikiSourceNotFoundException, IOException;
	
	public String getPrefixFromURL(String sourceURL) {
		String result = "";
		try {
			sourceURL = new URL(sourceURL).getHost();
			int dots = 0;
			for (int i = 0; i < sourceURL.length(); i++) {
				if (sourceURL.charAt(i) == '.') {
					dots++;
				}
			}
			if (dots > 1) {
				result = sourceURL.substring(0, sourceURL.indexOf('.'));
			} else {
				result = "!disabled";
			}
		} catch (MalformedURLException e) {
			result = "!disabled";
		}
		return result;
	}
	
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
				if (connection.getResponseCode() == 200) {
					connection = (HttpURLConnection) new URL(wikiLink + "/api.php")
							.openConnection();
					connection.setRequestMethod("GET");
					connection.setConnectTimeout(60000);
					connection.connect();
					if (connection.getResponseCode() == 200) {
						endpointUrl = wikiLink + "/api.php";
					} else {
						connection = (HttpURLConnection) new URL(wikiLink + "/w/api.php")
								.openConnection();
						connection.setRequestMethod("GET");
						connection.setConnectTimeout(60000);
						connection.connect();
						if (connection.getResponseCode() == 200) {
							endpointUrl = wikiLink + "/w/api.php";
						} else {
							connection = (HttpURLConnection) new URL(wikiLink + "/wiki/api.php")
									.openConnection();
							connection.setRequestMethod("GET");
							connection.setConnectTimeout(60000);
							connection.connect();
							if (connection.getResponseCode() == 200) {
								endpointUrl = wikiLink + "/wiki/api.php";
							}
						}
					}
				}
			} catch (Exception e) {}
			return endpointUrl;
		});
	}
	
}
