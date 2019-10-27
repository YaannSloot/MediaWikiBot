package main.yaannsloot.mediawikibot.resolvers;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.json.JSONObject;

import main.yaannsloot.mediawikibot.core.entities.QueryResult;
import main.yaannsloot.mediawikibot.sources.endpoints.WikiEndpoint;
import main.yaannsloot.mediawikibot.tools.BotUtils;

public class WikiaResolver extends Resolver {

	@Override
	public QueryResult queryEndpoint(WikiEndpoint endpoint, String query) {
		QueryResult result = null;
		try {
			query = query.trim().replace(" ", "%20");
			File tempJSON = File.createTempFile("query", ".json");
			FileUtils.copyURLToFile(new URL(endpoint.getApiUrl() + "/Search/List?query=" + query
					+ "&limit=1&minArticleQuality=1&batch=1&namespaces=0%2C14"), tempJSON);
			JSONObject queryResult = new JSONObject(FileUtils.readFileToString(tempJSON, StandardCharsets.UTF_8));
			tempJSON.delete();
			if (queryResult.getInt("total") != 0) {
				int pageId = queryResult.getJSONArray("items").getJSONObject(0).getInt("id");
				tempJSON = File.createTempFile("query", ".json");
				FileUtils.copyURLToFile(new URL(endpoint.getApiUrl() + "/Articles/AsSimpleJson?id=" + pageId),
						tempJSON);
				queryResult = new JSONObject(FileUtils.readFileToString(tempJSON, StandardCharsets.UTF_8));
				tempJSON.delete();
				boolean useAbstract = false;
				String summary = "";
				if(!queryResult.getJSONArray("sections").getJSONObject(0).getJSONArray("content").isEmpty())
					summary = queryResult.getJSONArray("sections").getJSONObject(0).getJSONArray("content").getJSONObject(0).getString("text");
				else
					useAbstract = true;
				String thumbUrl = "";
				tempJSON = File.createTempFile("query", ".json");
				FileUtils.copyURLToFile(new URL(endpoint.getApiUrl() + "/Articles/Details?ids=" + pageId + "&abstract=500"),
						tempJSON);
				queryResult = new JSONObject(FileUtils.readFileToString(tempJSON, StandardCharsets.UTF_8));
				tempJSON.delete();
				String pageUrl = queryResult.getString("basepath") + queryResult.getJSONObject("items").getJSONObject("" + pageId).getString("url");
				String title = queryResult.getJSONObject("items").getJSONObject("" + pageId).getString("title");
				if(queryResult.getJSONObject("items").getJSONObject("" + pageId).get("thumbnail") instanceof String) {
					thumbUrl = queryResult.getJSONObject("items").getJSONObject("" + pageId).getString("thumbnail");
				}
				if(useAbstract) {
					summary = queryResult.getJSONObject("items").getJSONObject("" + pageId).getString("abstract");
					if(BotUtils.lastIndexOfRegex(summary, "([A-z].*?\\.{1})") != 0) {
						summary = summary.substring(0, BotUtils.lastIndexOfRegex(summary, "([A-z].*?\\.{1})"));
					}
				}
				result = new QueryResult(title, summary, thumbUrl, pageUrl);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}

	@Override
	public String getResolverId() {
		return "wikia";
	}

}
