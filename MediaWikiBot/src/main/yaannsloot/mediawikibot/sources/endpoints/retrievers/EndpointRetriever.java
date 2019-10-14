package main.yaannsloot.mediawikibot.sources.endpoints.retrievers;

import java.io.IOException;
import java.util.List;

import main.yaannsloot.mediawikibot.exceptions.WikiSourceNotFoundException;

/**
 * Scrapes websites for endpoint URLs
 * @author Ian Sloat
 *
 */
public interface EndpointRetriever {

	public abstract String getRetrieverName();
	
	public abstract List<String> getSourceNames();
	
	public abstract List<String> extractEndpointUrls(String source, String language) throws WikiSourceNotFoundException, IOException;
	
}
