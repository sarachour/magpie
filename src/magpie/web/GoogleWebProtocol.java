package magpie.web;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.customsearch.*;
import com.google.api.services.customsearch.model.Result;
import com.google.api.services.customsearch.model.Search;
public class GoogleWebProtocol extends WebProtocol {
	//https://www.googleapis.com/customsearch/v1?key=INSERT_YOUR_API_KEY&cx=017576662512468239146:omuauf_lfve&q=lectures
	private String ApiKey = "XXX-YOUR-API-KEY-HERE";
	private String SiteKey = "XXX-YOUR-SITE-KEY-HERE";
	GoogleWebProtocol(){
		
	}
	
	public InputStream nakedHTML(String path){
		
		try {
			URL url;
			url = new URL(path);
			URLConnection conn;
			conn = url.openConnection();
			InputStream istream = conn.getInputStream();
			return istream;
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	public String translate(String s){
		//https://code.google.com/p/yuku-android-util/source/browse/GreenDroid/src/greendroid/widget/QuickAction.java?name=master
		//https://yuku-android-util.googlecode.com/git-history/master/GreenDroid/src/greendroid/widget/QuickAction.java
		if(s.startsWith("https://code.google.com") || s.startsWith("http://code.google.com")){
			Document istream = toHTML(nakedHTML(s));
			if(istream == null) return s;
			//System.out.println(s);
			Elements sources = istream.getElementsContainingOwnText("View raw file");
			//System.out.println(sources.get(0).toString());
			if(sources.size() > 0){
				String news  ="http:"+sources.get(0).attr("href");
				return news;
			}
			else{
				return s;
			}
			//System.out.println(news);
			
		}
		else if(s.startsWith("https://github.com")||s.startsWith("http://github.com")){
			Document istream = toHTML(nakedHTML(s));
			if(istream == null) return s;
			//raw url
			Element source = istream.getElementById("raw-url");
			String news = "https://github.com" + source.attr("href");
			return news;
		}
		else{
			return s;
		}
	}
	@Override
	List<String> search(String code) {
		
		List<String> urls = new LinkedList<String>();
		HttpRequestInitializer httpRequestInitializer = new HttpRequestInitializer() {

		    @Override
		    public void initialize(HttpRequest request) throws IOException {

		    }
		};
		JsonFactory jsonFactory = new JacksonFactory();

		Customsearch custom = new Customsearch(new NetHttpTransport(), jsonFactory, httpRequestInitializer);
	    Customsearch.Cse.List list;
		try {
			list = custom.cse().list(code);
			list.setFileType("java");
			list.setCx(SiteKey);
		    list.setKey(ApiKey);
		    Search results = list.execute();
		    if(results.getItems() == null) return urls;
		    for(Result result : results.getItems()){
		    	System.out.println("[GRABBER]"+result.getLink()+" / "+result.getFileFormat()+" / "+results.getItems().size());
		    	urls.add(result.getLink());
		    }
		   
		} catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}
	    
	    
		return urls;
	}
	String grab(String path){
		try {
			System.out.println("[GRABBER] grabbing: "+path);
			URI uri;
			uri = new URI(path);
			URL url = uri.toURL();
			URLConnection conn;
			conn = url.openConnection();
			InputStream istream = conn.getInputStream();
			// open the stream and put it into BufferedReader
			String s = new Scanner(istream,"UTF-8").useDelimiter("\\A").next();
			return s;
			
		}
		catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "";
		

	}
	/*
	public static void main(String[] args){
		GoogleWebProtocol gp = new GoogleWebProtocol();
		//String r = gp.translate("https://github.com/cyrilmottier/GreenDroid/blob/master/GreenDroid/src/greendroid/widget/QuickAction.java");
		String r = gp.translate("http://code.google.com/p/android-xmlrpc/source/browse/trunk/XMLRPC/src/org/xmlrpc/android/MethodCall.java?r=17"); 

		System.out.println(r);
	}
	*/
	
	
}

/*

final HttpTransport TRANSPORT = new NetHttpTransport();
final JsonFactory JSON_FACTORY = new JacksonFactory();
// TODO Auto-generated method stub
//you can also setAccessToken
final List<String> SCOPE = Arrays.asList(
	    "https://www.googleapis.com/auth/plus.me");

//final String REDIRECT_URI = "urn:ietf:wg:oauth:2.0:oob";

GoogleAuthorizationCodeFlow flow;
flow= new GoogleAuthorizationCodeFlow.Builder(
	    new NetHttpTransport(),
	    new JacksonFactory(),
	    CLIENT_ID, // This comes from your APIs console project
	    CLIENT_SECRET, // This, as well
	    SCOPE)
	    .setApprovalPrompt("force")
	    .setAccessType("offline").build();
GoogleTokenResponse tokenResponse=null;
try {
	tokenResponse = flow.newTokenRequest(code).execute();
} catch (IOException e2) {
	// TODO Auto-generated catch block
	e2.printStackTrace();
}
GoogleCredential credential = new GoogleCredential.Builder()
.setTransport(new NetHttpTransport())
.setJsonFactory(new JacksonFactory())
//clieentid, client secret
.setClientSecrets(CLIENT_ID, CLIENT_SECRET)
.addRefreshListener(new CredentialRefreshListener() {
	  @Override
	  public void onTokenResponse(Credential credential, TokenResponse tokenResponse) {
		  // Handle success.
		  System.out.println("Credential was refreshed successfully.");
	  }
	
		@Override
		public void onTokenErrorResponse(Credential arg0,
				TokenErrorResponse arg1) throws IOException {
			// TODO Auto-generated method stub
		System.out.println("Credential failed.");
	}
	}).build();
// Set authorized credentials.
credential.setFromTokenResponse(tokenResponse);
// Though not necessary when first created, you can manually refresh the
// token, which is needed after 60 minutes.
try {
	credential.refreshToken();
} catch (IOException e1) {
	// TODO Auto-generated catch block
	e1.printStackTrace();
}
Customsearch custom = new Customsearch.Builder(TRANSPORT, JSON_FACTORY, credential).build();
Customsearch.Cse search = custom.cse();
HttpRequest request;
try {
	request = search.list(code).setFileType("java")
			   .setCx(SiteKey)
				   .buildHttpRequestUsingHead();
	HttpResponse response = request.execute();
} catch (IOException e) {
	// TODO Auto-generated catch block
	e.printStackTrace();
}

Customsearch custom = new Customsearch.Builder(TRANSPORT, JSON_FACTORY, credential).build();
Customsearch.Cse search = custom.cse();
HttpRequest request = search.list(code).setFileType("java")
									   .setCx(SiteKey)
										   .setKey(APIKey)
										   .buildHttpRequestUsingHead();
	HttpResponse response = request.execute();
	InputStream in = response.getContent();
	String s = new Scanner(in,"UTF-8").useDelimiter("\\A").next();
	System.out.println(s);
 */

/*
String request = "key="+APIKey+"&cx="+SiteKey+"&q="+code+"&filetype=java";
Document rd = toHTML(HTTPSRequest(BaseURL, SearchSuffix, request));
System.out.println(rd.toString());
*/
//String request = "q="+code+"+language:java";
//JsonElement repos = toJSON(HTTPRequest(BaseURL, RepoSearchSuffix, request));
