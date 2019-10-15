package main.yaannsloot.mediawikibot.sources.endpoints.retrievers;

import java.io.IOException;
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

import main.yaannsloot.mediawikibot.exceptions.WikiSourceNotFoundException;

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
		logger.info("Verifying endpoints...");
		Collection<Future<String>> verifyFutures = new LinkedList<Future<String>>();
		for(String url : wikiLinks) {
			verifyFutures.add(verifyEndpointExistance(url));
		}
		for(Future<String> future : verifyFutures) {
			try {
				result.add(future.get());
			} catch (InterruptedException | ExecutionException e1) {
				e1.printStackTrace();
			}
		}
		logger.info(result.size() + " endpoints verified successfully.");
		return result;
	}

	private Future<List<String>> getWikiLinksFromPage(String pageUrl) {
		return executor.submit(() -> {
			List<String> wikiLinks = new ArrayList<String>();
			logger.info("Retrieving wiki links from " + pageUrl + "...");
			try {
				Document doc = Jsoup.connect(pageUrl).get();
				Elements wikis = new Elements();
				for (Element e : doc.getElementsByClass("wiki")) {
					wikis.addAll(e.getElementsByTag("a"));
				}
				wikis.forEach(e -> wikiLinks.add(e.attr("href")));
				logger.info("Retrieved " + wikiLinks.size() + " wiki links successfully");
			} catch (Exception e) {
				logger.warn("Failed to retrieve wiki links from " + pageUrl);
			}
			return wikiLinks;
		});
	}

}