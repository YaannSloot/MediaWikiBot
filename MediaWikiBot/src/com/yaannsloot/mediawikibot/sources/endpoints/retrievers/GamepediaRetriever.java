package com.yaannsloot.mediawikibot.sources.endpoints.retrievers;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
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

public class GamepediaRetriever extends EndpointRetriever {

	private static final Logger logger = LoggerFactory.getLogger(GamepediaRetriever.class);

	@Override
	public String getRetrieverName() {
		return "gamepedia";
	}

	@Override
	public List<String> getSourceNames() {
		return Arrays.asList("gamepedia");
	}
	
	@Override
	public List<WikiEndpoint> extractEndpoints(String source, String language, boolean autoEnable)
			throws WikiSourceNotFoundException, IOException {
		List<WikiEndpoint> result = new ArrayList<WikiEndpoint>();
		logger.info("Retrieving endpoint urls from https://www.gamepedia.com...");
		Document doc = Jsoup.connect("https://www.gamepedia.com/wikis").get();
		int wikiPageCount = Integer.parseInt(new Elements(doc.getElementsByClass("b-pagination-item").stream()
				.filter(e -> e.tagName().equals("a")).collect(Collectors.toList())).last().text());
		List<String> wikiLinks = new ArrayList<String>();
		Collection<Future<List<String>>> futures = new LinkedList<Future<List<String>>>();
		logger.info("Gathering wiki urls...");
		for (int i = 1; i <= wikiPageCount; i++) {
			futures.add(getWikiLinksFromPage("https://www.gamepedia.com/wikis?page=" + i));
		}
		for (Future<List<String>> future : futures) {
			try {
				wikiLinks.addAll(future.get());
			} catch (InterruptedException | ExecutionException e1) {
				e1.printStackTrace();
			}
		}
		logger.info("Retrieved " + wikiLinks.size() + " wiki links successfully");
		logger.info("Comparing links with preexisting urls in database...");
		List<WikiEndpoint> endpoints = MediaWikiBot.databaseLoader.loadEndpoints();
		for (WikiEndpoint e : endpoints) {
			wikiLinks = wikiLinks.stream().filter(url -> {
				try {
					return !e.getApiUrl().contains(new URL(url).getHost());
				} catch (MalformedURLException e1) {
					return true;
				}
			}).collect(Collectors.toList());
		}
		logger.info("New size of list to be verified is " + wikiLinks.size());
		logger.info("Verifying endpoints...");
		Collection<Future<String>> verifyFutures = new LinkedList<Future<String>>();
		for (String url : wikiLinks) {
			verifyFutures.add(verifyEndpointExistance(url));
		}
		try (ProgressBar pb = new ProgressBar("Verifying...", wikiLinks.size())) {
			for (Future<String> future : verifyFutures) {
				try {
					String endpointUrl = future.get();
					pb.step();
					if (!endpointUrl.equals("")) {
						if(autoEnable)
							result.add(new WikiEndpoint(getPrefixFromURL(endpointUrl), endpointUrl, "generic", null));
						else
							result.add(new WikiEndpoint("!disabled", endpointUrl, "generic", null));
					}
				} catch (InterruptedException | ExecutionException e) {
					e.printStackTrace();
				}
			}
		}
		logger.info(result.size() + " endpoints verified successfully.");
		return result;
	}

	@Override
	public List<WikiEndpoint> extractEndpoints(String source, String language)
			throws WikiSourceNotFoundException, IOException {
		return extractEndpoints(null, null, true);
	}
	
	@Override
	public List<WikiEndpoint> extractEndpoints(String language) throws WikiSourceNotFoundException, IOException {
		return extractEndpoints(null, null, true);
	}

	@Override
	public List<WikiEndpoint> extractEndpoints() throws WikiSourceNotFoundException, IOException {
		return extractEndpoints(null, null, true);
	}

	private Future<List<String>> getWikiLinksFromPage(String pageUrl) {
		return executor.submit(() -> {
			List<String> wikiLinks = new ArrayList<String>();
			try {
				Document doc = Jsoup.connect(pageUrl).get();
				Elements wikis = new Elements();
				for (Element e : doc.getElementsByClass("wiki")) {
					wikis.addAll(e.getElementsByTag("a"));
				}
				wikis.forEach(e -> wikiLinks.add(e.attr("href")));
			} catch (Exception e) {
				logger.warn("Failed to retrieve wiki links from " + pageUrl);
			}
			return wikiLinks;
		});
	}

}
