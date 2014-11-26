package magpie.web;

import java.io.InputStream;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class SearchCodeWebProtocol extends WebProtocol {
	private String BaseURL = "searchcode.com";
	private String BaseRAWURL = "searchcode.com";
	private String CodeSearchDir = "/api/codesearch_I";
	
	SearchCodeWebProtocol(){
	}
	@Override
	List<String> search(String code) {
		Set<String> urls = new HashSet<String>();
		// TODO Auto-generated method stub
		//https://api.github.com/search/repositories?q=tetris+language:assembly&sort=stars&order=desc
		//String query = BaseSearchURL + "?q="+code+"+in:file+language:java+extension:java";
		//https://github.com/search?q=tetris+extension%3Ajava&type=Code&ref=searchresults
		String request = "q="+code+"&lang=java";
		InputStream response = HTTPRequest("http", BaseURL, CodeSearchDir, request);
		System.out.println(response);
		JsonObject rd = toJSON(response).getAsJsonObject();
		int count = rd.get("total").getAsInt();
		System.out.println("RESULTS FOUND: "+count);
		JsonArray results = rd.get("results").getAsJsonArray();
		for(JsonElement result : results){
			JsonObject oresult = result.getAsJsonObject();
			String matchtype = oresult.get("modeltype").getAsString();
			String location = oresult.get("location").getAsString();
			System.out.println(matchtype + ":"+location);
		}
		//String request = "q="+code+"+language:java";
		//JsonElement repos = toJSON(HTTPRequest(BaseURL, RepoSearchSuffix, request));
		
		return new LinkedList<String>(urls);
	}
	String grab(String url){
		String source="";
		source = toText(HTTPRequest("https", BaseRAWURL, url, null));
		return source;
	}
}
