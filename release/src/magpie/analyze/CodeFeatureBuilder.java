package magpie.analyze;
/*
 * 
 * 
 * 
 */
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import magpie.util.Pair;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.CompilationUnit;

public class CodeFeatureBuilder {
	LengthComparator lenCompare;
	MembershipComparator memCompare;
	PositionComparator posCompare;
	public CodeFeatureBuilder(){
		lenCompare = new LengthComparator();
		memCompare = new MembershipComparator(null);
		posCompare = new PositionComparator();
	}
	public boolean isSyntax(String token){
		return (token =="if" || token == "else" || token == "while" || token == "return"|| token == "length");
	}
	public class PositionComparator implements java.util.Comparator<Pair<Integer,Integer>> {

	    public int compare(Pair<Integer,Integer> s1, Pair<Integer,Integer> s2) {
	        int dist1 = Math.abs(s1.e1 );
	        int dist2 = Math.abs(s2.e1 );

	        return dist1 - dist2;
	    }
	}
	public class LengthComparator implements java.util.Comparator<String> {

	    public int compare(String s1, String s2) {
	        int dist1 = Math.abs(s1.length() );
	        int dist2 = Math.abs(s2.length() );

	        return dist2 - dist1;
	    }
	}
	public class MembershipComparator implements java.util.Comparator<String> {
		List<String> keywords;
		MembershipComparator(List<String> keywords){
			this.keywords = keywords;
		}
		public void setKeywords(List<String> keywords){
			this.keywords = keywords;
		}
		private int numberKeywords(String s){
			int score=0;
			for(String kw : keywords){
				if(s.contains(kw)){
					score += kw.length();
				}
			}
			return score;
		}
	    public int compare(String s1, String s2) {
	        int score1 =numberKeywords(s1);
	        int score2 =numberKeywords(s2);

	        return score2 - score1;
	    }
	}
	public Pair<String, List<String>> flatten(List<String> kw, int maxlen){
		String query ="";
		List<String> newlist = new LinkedList<String>();
		for(String word : kw){
			if(query.length()+word.length()+1 < maxlen){
				query += " " + word;
			}
			else{
				newlist.add(word);
			}
		}
		return new Pair(query, newlist);
	}
	public static String removeComments(String src){
		String res="";
		for(int i=0; i < src.length(); ){
			if(src.startsWith("/*", i)){
				int next = src.indexOf("*/", i);
				i = next + 2;
			}
			else if(src.startsWith("//",i)){
				int next = src.indexOf("\n", i);
				i = next+1;
			}
			else {
				res += src.charAt(i);
				i++;
			}
		}
		return res;
	}
	public List<String> getBeginning(CompilationUnit source, int MAX_LEN){
		List<String> kwlist = new LinkedList<String>();

		try {
			String src = source.getSource();
			src = removeComments(src);
			int max = src.length() > MAX_LEN ? MAX_LEN : src.length();
			String s = src.substring(0, max);
			kwlist.add(s);
		} catch (JavaModelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return kwlist;
	}
	public List<String> getIdentifyingSignatures(CompilationUnit source){
		List<String> kwlist = new LinkedList<String>();
		try {
			String src = source.getSource();
			IType[] jem = source.getAllTypes();
			for(IType j : jem){
				ISourceRange tsrc = j.getSourceRange();
				int begin = tsrc.getOffset()-1;
				int end = begin + tsrc.getLength();
			}
		} catch (JavaModelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		return kwlist;
	}
	public List<String> getIdentifiers(String id, String source){
		String query="";
		String varRegexp = "[A-Za-z][A-Za-z0-9_]*";
		String badChars = "[^A-Za-z0-9_]";
		Set<String> keywords = new HashSet();
		List<String> kwlist = new LinkedList<String>();

		keywords.add(id);
		String[] tokens = source.split(badChars);
		for(String token : tokens){
			if(token.matches(varRegexp) || isSyntax(token))
				keywords.add(token);
		}
		kwlist.addAll(keywords);
		Collections.sort(kwlist, lenCompare);
		for(String word : kwlist){
			if(query.length()+word.length() < 125){
				query += " " + word;
			}
		}

		return kwlist;
	}
	public List<String> getIdentifyingSnippets(String id, String source, int bufamt){
		List<String> elems = getIdentifiers(id, source);
		String badChars = "[^A-Za-z0-9_]";
		Set<String> snippets = new HashSet<String>(); 
		List<Pair<Integer,Integer>> positions = new LinkedList<Pair<Integer,Integer>>();
		source = source.replaceAll("[\\s]+", " ");
		for(String e : elems){
			int off = 0;
			off = source.indexOf(e, off);
			
			do{
				int begin = Math.max(0, off-bufamt);
				int end = Math.min(source.length()-1, off+bufamt);
				int tmp;
				while(Character.isJavaIdentifierPart(source.charAt(begin))){
					if(begin == source.length()-1 || begin == 0 || begin == end) break;
					else begin++;
				}
				while(Character.isJavaIdentifierPart(source.charAt(end))){
					if(end == source.length()-1 || end == 0 || begin == end) break;
					else end--;
				}
				Pair<Integer,Integer> pos = new Pair(begin, end);
				boolean found=false;
				for(Pair<Integer,Integer> cpos : positions){
					if(Math.min(cpos.e2, pos.e2) -  Math.max(cpos.e1, pos.e1) > 0){
						found = true;
					}
				}
				if(!found) positions.add(pos);
				off = source.indexOf(e, off+1);
			} while(off >= 0);
		}
		Collections.sort(positions, posCompare);
		for(Pair<Integer,Integer> pos : positions){
			snippets.add(source.substring(pos.e1, pos.e2));
		}
		List<String> snippetList = new LinkedList<String>(snippets);
		//memCompare.setKeywords(elems);
		//Collections.sort(snippetList, memCompare);
		
		return snippetList;
	}
	public List<Pair<String,String>> extractFragmentsByIdentifyingSnippets(CompilationUnit cu, int maxlen, int bleedSize){
		List<Pair<String,String>> ll= new LinkedList<Pair<String,String>>();
		String s;
		try {
			List<String> ids = getIdentifyingSnippets(cu.getElementName(), cu.getSource(), bleedSize);
			//ll.add(new Pair(cu.getElementName(), cu.getSource()));
			//deepAdd(cu.getTypes(), ll, maxlen);
			Pair<String, List<String>>  result = new Pair<String, List<String>>("",null);
			int count = 0;
			result.e2 = ids;
			do{
				result = flatten(result.e2, maxlen);
				//System.out.println("-----snippet-----\n"+result.e1+"\n----------\n");
				ll.add(new Pair<String,String>(""+count, result.e1));
				count++;
			}while(result.e2.size() > 0);
		} catch (JavaModelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return ll;
	}
	//less naive
	public List<Pair<String,String>> extractFragmentsByIdentifiers(CompilationUnit cu, int maxlen){
		List<Pair<String,String>> ll= new LinkedList<Pair<String,String>>();
		String s;
		try {
			List<String> ids = getIdentifiers(cu.getElementName(), cu.getSource());
			//ll.add(new Pair(cu.getElementName(), cu.getSource()));
			//deepAdd(cu.getTypes(), ll, maxlen);
			Pair<String, List<String>>  result = new Pair<String, List<String>>("",null);
			int count = 0;
			result.e2 = ids;
			do{
				result = flatten(result.e2, maxlen);
				//System.out.println("-----snippet-----\n"+result.e1+"\n----------\n");
				ll.add(new Pair<String,String>(""+count, result.e1));
				count++;
			}while(result.e2.size() > 0 && result.e1 != "");
		} catch (JavaModelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return ll;
	}
	public List<Pair<String,String>> extractFragmentsBySource(CompilationUnit cu, int maxlen){
		List<Pair<String,String>> ll= new LinkedList<Pair<String,String>>();
		List<String> ids = getIdentifyingSignatures(cu);
		//ll.add(new Pair(cu.getElementName(), cu.getSource()));
		//deepAdd(cu.getTypes(), ll, maxlen);
		int count=0;
		for(String id : ids){
			ll.add(new Pair<String,String>(count+"", id));
			count++;
		}
		return ll;
	}
	public List<Pair<String,String>> extractFragmentsByStart(CompilationUnit cu, int maxlen){
		List<Pair<String,String>> ll= new LinkedList<Pair<String,String>>();
		List<String> ids = getBeginning(cu, maxlen);
		//ll.add(new Pair(cu.getElementName(), cu.getSource()));
		//deepAdd(cu.getTypes(), ll, maxlen);
		int count=0;
		for(String id : ids){
			ll.add(new Pair<String,String>(count+"", id));
			count++;
		}
		return ll;
	}

}
