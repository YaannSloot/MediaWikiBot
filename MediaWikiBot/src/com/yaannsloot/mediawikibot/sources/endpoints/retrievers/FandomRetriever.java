package com.yaannsloot.mediawikibot.sources.endpoints.retrievers;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yaannsloot.mediawikibot.core.MediaWikiBot;
import com.yaannsloot.mediawikibot.exceptions.WikiSourceNotFoundException;
import com.yaannsloot.mediawikibot.sources.endpoints.WikiEndpoint;

import me.tongfei.progressbar.ProgressBar;

public class FandomRetriever extends EndpointRetriever {

	private static final Logger logger = LoggerFactory.getLogger(FandomRetriever.class);

	@Override
	public String getRetrieverName() {
		return "fandom";
	}

	@Override
	public List<String> getSourceNames() {
		return Arrays.asList("<single word search or all>");
	}

	@Override
	public List<WikiEndpoint> extractEndpoints(String source, String language, boolean autoEnable)
			throws WikiSourceNotFoundException, IOException {
		List<WikiEndpoint> result = new ArrayList<WikiEndpoint>();
		if (source.equals("all")) {
			source = "";
		}
		Locale lang = getMatchingLocale(language);
		if (lang == null) {
			logger.error("Specified language not recognized");
		} else {
			logger.info("Looking up term \"" + source + "\" with target language " + lang.getDisplayLanguage());
			Document doc = Jsoup.connect("https://community-search.fandom.com/wiki/Special:Search?search=" + source
					+ "&limit=200&resultsLang=" + lang.getLanguage()).get();
			Elements targetLinks = doc.getElementsByClass("result-link");
			if (targetLinks.size() > 0) {
				logger.info(targetLinks.size() + " results found.");
				boolean hasMorePages = true;
				try {
					Integer.parseInt(doc.getElementsByClass("paginator-page").last().text());
				} catch (Exception e) {
					hasMorePages = false;
					logger.info("No additional pages found.");
				}
				int lastPage = 1;
				List<String> wikiUrls = new ArrayList<String>();
				for (Element e : targetLinks) {
					wikiUrls.add(e.attr("href"));
				}
				if (hasMorePages) {
					logger.info("Checking for additional pages...");
					int nextPage;
					while ((nextPage = Integer.parseInt(Jsoup
							.connect("https://community-search.fandom.com/wiki/Special:Search?search=" + source
									+ "&page=" + lastPage + "&limit=200&resultsLang=" + lang.getLanguage())
							.get().getElementsByClass("paginator-page").last().text())) != lastPage)
						lastPage = nextPage;
					logger.info(lastPage + " additional result pages found. Gathering wiki urls...");
					Collection<Future<List<String>>> futures = new LinkedList<Future<List<String>>>();
					for (int i = 1; i <= lastPage; i++) {
						futures.add(getWikiUrls("https://community-search.fandom.com/wiki/Special:Search?search="
								+ source + "&page=" + i + "&limit=200&resultsLang=" + lang.getLanguage()));
					}
					for (Future<List<String>> future : futures) {
						try {
							wikiUrls.addAll(future.get());
						} catch (InterruptedException | ExecutionException e1) {
							e1.printStackTrace();
						}
					}
				}
				wikiUrls = wikiUrls.stream().filter(url -> url.contains("fandom")).collect(Collectors.toList());
				for (int i = 0; i < wikiUrls.size(); i++) {
					while (wikiUrls.get(i).lastIndexOf("/") == wikiUrls.get(i).length() - 1
							&& wikiUrls.get(i).length() > 0)
						wikiUrls.set(i, wikiUrls.get(i).substring(0, wikiUrls.get(i).length() - 1));
				}
				logger.info("Comparing links with preexisting urls in database...");
				List<WikiEndpoint> endpoints = MediaWikiBot.databaseLoader.loadEndpoints();
				for (WikiEndpoint e : endpoints) {
					wikiUrls = wikiUrls.stream().filter(url -> {
						try {
							return !e.getApiUrl().contains(new URL(url).getHost());
						} catch (MalformedURLException e1) {
							return true;
						}
					}).collect(Collectors.toList());
				}
				logger.info("New size of list to be verified is " + wikiUrls.size());
				logger.info("Verifying endpoints...");
				Collection<Future<String>> futures = new LinkedList<Future<String>>();
				for (String link : wikiUrls) {
					futures.add(verifyEndpointExistance(link));
				}
				try (ProgressBar pb = new ProgressBar("Verifying...", wikiUrls.size())) {
					for (Future<String> future : futures) {
						try {
							String endpointUrl = future.get();
							pb.step();
							if (!endpointUrl.equals("")) {
								if(autoEnable)
									result.add(new WikiEndpoint(getPrefixFromURL(endpointUrl), endpointUrl, "wikia", null));
								else
									result.add(new WikiEndpoint("!disabled", endpointUrl, "wikia", null));
							}
						} catch (InterruptedException | ExecutionException e) {
							e.printStackTrace();
						}
					}
				}
				logger.info(result.size() + " endpoints verified successfully.");
			} else {
				logger.error("Could not find any results based on specified search term and language");
			}
		}
		return result;
	}
	
	@Override
	public List<WikiEndpoint> extractEndpoints(String source, String language)
			throws WikiSourceNotFoundException, IOException {
		return extractEndpoints(source, language, true);
	}
	
	@Override
	public List<WikiEndpoint> extractEndpoints(String language) throws WikiSourceNotFoundException, IOException {
		return extractEndpoints("all", language, true);
	}

	@Override
	public List<WikiEndpoint> extractEndpoints() throws WikiSourceNotFoundException, IOException {
		return extractEndpoints("all", "english", true);
	}

	@Override
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
					connection = (HttpURLConnection) new URL(wikiLink + "/api/v1")
							.openConnection();
					connection.setRequestMethod("GET");
					connection.setConnectTimeout(60000);
					connection.connect();
					if (connection.getResponseCode() == 200) {
						endpointUrl = wikiLink + "/api/v1";
					}
				}
			} catch (Exception e) {}
			return endpointUrl;
		});
	}
	
	private Locale getMatchingLocale(String language) {
		Locale result = null;
		Locale[] locales = Locale.getAvailableLocales();
		for (Locale loc : locales) {
			if (loc.getDisplayLanguage().toLowerCase().equals(language)) {
				result = loc;
				break;
			}
		}
		return result;
	}

	private Future<List<String>> getWikiUrls(String pageUrl) {
		return executor.submit(() -> {
			List<String> result = new ArrayList<String>();
			Document doc = Jsoup.connect(pageUrl).get();
			Elements targetLinks = doc.getElementsByClass("result-link");
			for (Element e : targetLinks) {
				result.add(e.attr("href"));
			}
			return result;
		});
	}

}