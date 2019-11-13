package com.yaannsloot.mediawikibot.core.entities;

public class QueryResult {

	private String title = "";
	private String summary = "";
	private String thumbnailUrl = "";
	private String pageUrl = "";
	
	public QueryResult(String title, String summary, String thumbnailUrl, String pageUrl) {
		this.title = title;
		this.summary = summary;
		this.thumbnailUrl = thumbnailUrl;
		this.pageUrl = pageUrl;
	}
	
	public String getTitle() {
		return title;
	}
	
	public String getSummary() {
		return summary;
	}
	
	public String getThumbnailUrl() {
		return thumbnailUrl;
	}
	
	public String getPageUrl() {
		return pageUrl;
	}
	
}
