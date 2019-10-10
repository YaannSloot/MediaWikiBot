package main.yaannsloot.mediawikibot.resolvers;

import main.yaannsloot.mediawikibot.core.entities.QueryResult;
import main.yaannsloot.mediawikibot.core.entities.WikiEndpoint;

public abstract class Resolver {
	
	public abstract QueryResult queryEndpoint(WikiEndpoint endpoint, String query);
	
	public abstract String getResolverId();

}
