package main.yaannsloot.mediawikibot.resolvers;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import main.yaannsloot.mediawikibot.core.entities.QueryResult;
import main.yaannsloot.mediawikibot.core.entities.WikiEndpoint;

public class GenericResolver extends Resolver {

	@Override
	public QueryResult queryEndpoint(WikiEndpoint endpoint, String query) {
		QueryResult result = null;
		try {
			Document doc;
			doc = Jsoup
					.connect(
							endpoint.getApiUrl() + "?action=query&srsearch=" + query + "&srprop&list=search&format=xml")
					.ignoreContentType(true).get();
			JSONObject parsedResult = XML.toJSONObject(doc.toString());
			int hits = parsedResult.getJSONObject("api").getJSONObject("query").getJSONObject("searchinfo")
					.getInt("totalhits");
			if (hits > 0) {
				JSONObject resultPage = parsedResult.getJSONObject("api").getJSONObject("query").getJSONObject("search")
						.getJSONArray("p").getJSONObject(0);
				String title = resultPage.getString("title");
				int pageid = resultPage.getInt("pageid");
				doc = Jsoup.connect(endpoint.getApiUrl() + "?action=query&pageids=" + pageid
						+ "&prop=extracts|info|pageimages&pithumbsize=800&inprop=url&exintro&explaintext&exchars=1000&format=xml")
						.ignoreContentType(true).get();
				parsedResult = XML.toJSONObject(doc.toString());
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
