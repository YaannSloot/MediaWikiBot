package com.yaannsloot.mediawikibot.resolvers;

import java.io.IOException;
import java.net.URLEncoder;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;

import com.yaannsloot.mediawikibot.core.entities.QueryResult;
import com.yaannsloot.mediawikibot.sources.endpoints.WikiEndpoint;

public class GenericResolver extends Resolver {

	@Override
	public QueryResult queryEndpoint(WikiEndpoint endpoint, String query) {
		QueryResult result = null;
		HttpClient client = HttpClientBuilder.create().disableRedirectHandling().build();
		try {
			HttpGet req = new HttpGet(endpoint.getApiUrl() + "?action=query&srsearch="
					+ URLEncoder.encode(query, "UTF-8") + "&srprop&list=search&format=xml");
			HttpResponse resp = client.execute(req);
			JSONObject parsedResult = XML.toJSONObject(EntityUtils.toString(resp.getEntity(), "UTF-8"));
			boolean hasResults = false;
			if (parsedResult.getJSONObject("api").getJSONObject("query").has("search")) {
				if (parsedResult.getJSONObject("api").getJSONObject("query").get("search") instanceof JSONObject) {
					hasResults = parsedResult.getJSONObject("api").getJSONObject("query").getJSONObject("search")
							.has("p");
				}
			}

			if (hasResults) {
				JSONObject resultPage = parsedResult.getJSONObject("api").getJSONObject("query").getJSONObject("search")
						.getJSONArray("p").getJSONObject(0);
				String title = resultPage.getString("title");
				String queryURL = (endpoint.getApiUrl() + "?action=query&titles=" + URLEncoder.encode(title, "UTF-8")
						+ "&prop=extracts|info|pageimages&pithumbsize=800&inprop=url&exintro&explaintext&exchars=1200&format=xml")
								.replace("|", "%7C");
				req = new HttpGet(queryURL);
				resp = client.execute(req);
				parsedResult = XML.toJSONObject(EntityUtils.toString(resp.getEntity(), "UTF-8"));
				resultPage = parsedResult.getJSONObject("api").getJSONObject("query").getJSONObject("pages")
						.getJSONObject("page");
				String summary = resultPage.getJSONObject("extract").getString("content").replace("....", ".");
				String pageUrl = resultPage.getString("fullurl");
				String imageUrl;
				try {
					imageUrl = resultPage.getJSONObject("thumbnail").getString("source");
				} catch (JSONException e) {
					imageUrl = "";
				}
				result = new QueryResult(title, summary, imageUrl, pageUrl);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}

	@Override
	public String getResolverId() {
		return "generic";
	}

}
