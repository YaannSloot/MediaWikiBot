package com.yaannsloot.mediawikibot.resolvers;

import com.yaannsloot.mediawikibot.core.entities.QueryResult;
import com.yaannsloot.mediawikibot.sources.endpoints.WikiEndpoint;

public abstract class Resolver {
	
	public abstract QueryResult queryEndpoint(WikiEndpoint endpoint, String query);
	
	public abstract String getResolverId();

}
