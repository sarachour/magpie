package magpie.web;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.xml.sax.SAXException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

public abstract class WebProtocol {
	public class Verifier implements HostnameVerifier {

	    public boolean verify(String arg0, SSLSession arg1) {
	            return true;   // mark everything as verified
	    }
	}
	InputStream HTTPSRequest(String base, String suffix, String query){
		try {
			URI uri;
			if(query != null)
				uri= new URI("https", base, suffix,query, null);
			else
				uri= new URI("https", base, suffix, null);
			URL url = uri.toURL();
			HttpsURLConnection con = (HttpsURLConnection)url.openConnection();
			con.setConnectTimeout(60000);
		    con.setReadTimeout(60000);
		    con.addRequestProperty("User-Agent", "Mozilla/5.0");
			con.setHostnameVerifier(new Verifier());
	        InputStream in = con.getInputStream();
	        
	        return in;
	        
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
        
		
	}
	InputStream HTTPRequest(String protocol, String base, String suffix, String query){
		try {
			URI uri;
			if(query != null)
				uri= new URI(protocol, base, suffix,query, null);
			else
				uri= new URI(protocol, base, suffix, null);
			URL url = uri.toURL();
			URLConnection urlc = url.openConnection();
			
	        InputStream in = urlc.getInputStream();
	        return in;
	        
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
        
		
	}
	JsonElement toJSON(InputStream in){
		BufferedReader bf = new BufferedReader(new InputStreamReader(in));
		Gson gson = new GsonBuilder().create();
        JsonElement je = gson.fromJson(bf , JsonElement.class);
        return je;
	}
	Document toHTML(InputStream is){
		Document d =Jsoup.parse(toText(is));
		return d;
	}
	String toText(InputStream in){
		if(in == null) return "";
		BufferedReader bf = new BufferedReader(new InputStreamReader(in));
		String txt = "";
		String buf;
		 try {
			while ((buf = bf.readLine()) != null) {
				txt += buf+"\n";
			}
			bf.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return txt;
		
	}
	abstract List<String> search(String code);
	abstract String grab(String urls);
}
