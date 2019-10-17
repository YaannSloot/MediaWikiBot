package main.yaannsloot.mediawikibot.sources.endpoints.retrievers;

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

import main.yaannsloot.mediawikibot.core.MediaWikiBot;
import main.yaannsloot.mediawikibot.exceptions.WikiSourceNotFoundException;
import main.yaannsloot.mediawikibot.sources.endpoints.WikiEndpoint;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarStyle;

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
	public List<String> extractEndpointUrls(String source, String language)
			throws WikiSourceNotFoundException, IOException {
		List<String> result = new ArrayList<String>();
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
		try (ProgressBar pb = new ProgressBar("Verifying...", wikiLinks.size(), ProgressBarStyle.ASCII)) {
			for (Future<String> future : verifyFutures) {
				try {
					String endpointUrl = future.get();
					if (!endpointUrl.equals("")) {
						result.add(endpointUrl);
					}
				} catch (InterruptedException | ExecutionException e) {
					e.printStackTrace();
				}
			}
		}
		logger.info(result.size() + " endpoints verified successfully.");
		return result;
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
