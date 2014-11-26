package magpie.web;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.internal.core.CompilationUnit;

import magpie.analyze.CodeFeatureBuilder;
import magpie.util.Pair;

public class WebCrawler {
	private int SLEEP_GRAB = 0;
	private int SLEEP_OHLOH = 100;
	
	CodeFeatureBuilder featureExtractor = new CodeFeatureBuilder();
	GithubWebProtocol git = new GithubWebProtocol();
	GoogleWebProtocol google;
	OhlohWebProtocol ohloh = new OhlohWebProtocol();
	WebCodeWriter writer = new WebCodeWriter();
	Map<String, Pair<WebProtocol, List<String>>> urls;
	public WebCrawler(){
		urls = new HashMap<String, Pair<WebProtocol, List<String>>>();
		google = new GoogleWebProtocol();
	}
	private void crawlWebProtocol(String filename, CompilationUnit source, WebProtocol web, List<Pair<String,String>> features, int sleep){
		//List<Pair<String,String>> features = featureExtractor.extractFragmentsByIdentifiers(source, 125);
		System.out.println("Count: "+features.size());
		int i=0;
		for(Pair<String,String> feature : features){
			String currFilename = "res."+filename+ "#"+ feature.e1;
			System.out.println("FEATURE ["+i+"]:");
			List<String> results = web.search(feature.e2);
			if(urls.containsKey(currFilename)){
				urls.get(currFilename).e2.addAll(results);
			}
			else{
				urls.put(currFilename, new Pair<WebProtocol, List<String>>(web,results));
			}
			try {
				Thread.sleep(sleep);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			i++;
		}
	}
	private void crawlOhloh(String filename, CompilationUnit source, int intensity){
		//List<Pair<String,String>> features = featureExtractor.extractFragmentsByIdentifyingSnippets(source, 500,200);
		List<Pair<String,String>> features;
		if(intensity == 0){
			features = featureExtractor.extractFragmentsByStart(source, 1000);
		}
		else{
			features = featureExtractor.extractFragmentsByIdentifyingSnippets(source, 500,200);
		}
		//set sleeping time
		crawlWebProtocol(filename, source, ohloh, features, SLEEP_OHLOH);
	}
	/*
	private void crawlGit(String filename, CompilationUnit source){
		List<Pair<String,String>> features = featureExtractor.extractFragmentsByIdentifyingSnippets(source, 125,40);
		crawlWebProtocol(filename, source, git, features, 10000);
		
	}
	private void crawlGoogle(String filename, CompilationUnit source){
		List<Pair<String,String>> features = featureExtractor.extractFragmentsByStart(source, 500);
		crawlWebProtocol(filename, source, google, features, 1000);
		
	}
	*/
	public void crawl(String filename, CompilationUnit source, int intensity){
		//crawlGit(filename, source);
		//crawlGoogle(filename, source);
		crawlOhloh(filename, source, intensity);
	}
	public class LengthComparator implements java.util.Comparator<Element> {
	    public int compare(Element s1, Element s2) {
	        int dist1 = s1.freq;
	        int dist2 = s2.freq;

	        return dist2 - dist1;
	    }


	}
	class Element {
		public int freq=0;
		public String url = "";
		public WebProtocol protocol;
		public Element(WebProtocol p, String e){
			protocol = p;
			url  =e;
			freq=1;
		}
		public void increaseFreq(){
			freq+=1;
		}
	}
	public void grab(String filename, ICompilationUnit ref){
		final int MAX = 10;
		List<Pair<WebProtocol, String>> allurls = new LinkedList<Pair<WebProtocol, String>>();
		LengthComparator len = new LengthComparator();

		for(String key : urls.keySet()){
			if(key.contains("."+filename+"#")){
				System.out.println(key + ": " + urls.get(key).e2.size());
				for(String p : urls.get(key).e2){
					allurls.add(new Pair<WebProtocol, String>(urls.get(key).e1, p));
				}
			}
		}
		Map<String, Element> urlmap = new HashMap<String,Element>();
		List<Element> urlset = new LinkedList<Element>();
		for(Pair<WebProtocol, String> url : allurls){
			if(!urlmap.containsKey(url.e2)) urlmap.put(url.e2, new Element(url.e1, url.e2));
			else urlmap.get(url.e2).increaseFreq();
		}
		for(String key : urlmap.keySet()){
			urlset.add(urlmap.get(key));
		}
		Collections.sort(urlset, len);
		for(Element  url : urlset){
			System.out.println(url.freq + ":" +url.url);
		}
		for(int i=0; i < urlset.size() && i < MAX; i++){
			String source = urlset.get(i).protocol.grab(urlset.get(i).url);
			writer.write(ref, filename.replace(".java", "_")+i+".java", urlset.get(i).url,  source);
			try {
				Thread.sleep(SLEEP_GRAB);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	public boolean hasURLs(String filename){
		int nurls=0;
		for(String key : urls.keySet()){
			if(key.contains("."+filename+"#")){
				nurls += (urls.get(key).e2.size());
			}
		}
		return (nurls > 0);
	}
	public void clear(){
		urls.clear();
	}
	/*
	public static void main(String [] args){
		WebCrawler gith = new WebCrawler();
		gith.crawl("test","public void setTetris(Tetris tetris){this.tetris = tetris;");
	}
	*/
}
