package magpie.web;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class OhlohWebProtocol extends WebProtocol {
	String BaseHTTPURL = "code.ohloh.net";
	String SearchDir = "/search";
	String FileDir = "/file";
	OhlohWebProtocol(){
		System.setProperty("jsse.enableSNIExtension", "false");
	}
	
	//https://code.ohloh.net/search?s=Quickdroid&pp=0&fl=Java&fe=java&mp=1&ml=1&me=1&md=1&ff=1&filterChecked=true
	@Override
	List<String> search(String code) {
		Set<String> urls = new HashSet<String>();
		// TODO Auto-generated method stub
		int pagenum = 0;
		String request = "s="+code+"&pp="+pagenum+"&fl=Java&fe=java&mp=1&ml=1&me=1&md=1&ff=1&filterChecked=true";
		Document rd = toHTML(HTTPSRequest(BaseHTTPURL, "/search", request));
		Elements elems = rd.select("div.fileNameLabel");
		Pattern matchurl = Pattern.compile("fid=[^&]+&cid=[^&]+&");
		for(int i = 0; i< elems.size(); i++){
			Element e= elems.get(i);
			Elements links = e.select("div").select("a[href]");
			String fileurl  =links.get(0).attr("href");
			//System.out.println(fileurl);
			Matcher matcher = matchurl.matcher(fileurl);
			if(matcher.find()){
				String filesegment = matcher.group(0);
				String dlURL = filesegment+"dl";
				urls.add(dlURL);
			}
		}
		return new LinkedList<String>(urls);
	}

	@Override
	String grab(String url) {
		String source = toText(HTTPSRequest(BaseHTTPURL,FileDir, url));
		return source;
	}

}

//                      /file?fid=6dLEBdMsRujdgcG_s6hWfaNUOvY&amp;cid=MuaZRT7eo6c&amp;s=Quickdroid#L1
//https://code.ohloh.net/file?fid=6dLEBdMsRujdgcG_s6hWfaNUOvY&cid={cid!?html}&dl