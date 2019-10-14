package main.yaannsloot.mediawikibot.sources.endpoints;

import java.awt.Color;

public class WikiEndpoint {

	private String refID;
	private String apiURL;
	private String resolver;
	private Color displayColor;
	
	public WikiEndpoint(String refID, String apiURL, String resolver, Color displayColor) {
		this.refID = refID;
		this.apiURL = apiURL;
		this.resolver = resolver;
		this.displayColor = displayColor;
	}
	
	public String getReferenceId() {
		return refID;
	}
	
	public String getApiUrl() {
		return apiURL;
	}
	
	public String getResolverId() {
		return resolver;
	}
	
	public Color getDisplayColor() {
		return displayColor;
	}
	
}
