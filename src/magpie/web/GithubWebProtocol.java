package magpie.web;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

public class GithubWebProtocol extends WebProtocol {
	private String BaseURL = "api.github.com";
	private String BaseHTTPURL = "github.com";
	private String BaseRAWURL = "raw.githubusercontent.com";
	private String RepoSearchSuffix =  "/search/repositories";
	private String CodeSearchSuffix =  "/search/code";
	
	GithubWebProtocol(){
		System.setProperty("jsse.enableSNIExtension", "false");
	}
	@Override
	List<String> search(String code) {
		Set<String> urls = new HashSet<String>();
		// TODO Auto-generated method stub
		//https://api.github.com/search/repositories?q=tetris+language:assembly&sort=stars&order=desc
		//String query = BaseSearchURL + "?q="+code+"+in:file+language:java+extension:java";
		//https://github.com/search?q=tetris+extension%3Ajava&type=Code&ref=searchresults
		String request = "l=java&q="+code+"+extension:java&type=Code&ref=searchresults";
		Document rd = toHTML(HTTPRequest("https", BaseHTTPURL, "/search", request));
		Elements elems = rd.select("div.code-list-item");
		for(int i = 0; i< elems.size(); i++){
			Element e= elems.get(i);
			Elements links = e.select("p.title").select("a[href]");
			String fileurl  =links.get(1).attr("href");
			fileurl = fileurl.replaceFirst("/blob/","/");
			urls.add(fileurl);
		}
		//String request = "q="+code+"+language:java";
		//JsonElement repos = toJSON(HTTPRequest(BaseURL, RepoSearchSuffix, request));
		
		return new LinkedList<String>(urls);
	}
	String grab(String url){
		String source = toText(HTTPRequest("https", BaseRAWURL, url, null));
		return source;
	}
/*
	public static void main(String [] args){
		GithubWebProtocol gith = new GithubWebProtocol();
		Set<String> urls = gith.search("public void setTetris(Tetris tetris){this.tetris = tetris;");
		gith.grab(urls);
	}
*/


}
