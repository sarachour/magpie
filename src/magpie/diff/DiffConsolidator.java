package magpie.diff;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

import magpie.util.Difference;
import magpie.util.Difference.MatchType;

import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.compare.structuremergeviewer.DocumentRangeNode;
import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
public class DiffConsolidator {
	public class MatchInfo{
		public List<Difference>  elems = new LinkedList<Difference>();
		public Position thisPosition;
		public IResource thisResource;
		public MatchInfo(Position tPos, IResource tResource){
			this.thisPosition = tPos;
			this.thisResource = tResource;
		}
		public boolean hasSource(String name){
			for(Difference e : elems){
				String oname = e.otherResource.getName();
				if(oname.equals(name)){
					return true;
				}
			}
			return false;
		}
		public Difference getSource(String name){
			for(Difference e : elems){
				String oname = e.otherResource.getName();
				if(oname.equals(name)){
					return e;
				}
			}
			return null;
		}
		public void add(Position oPos, IResource oResource, MatchType matchType){
			Difference e = new Difference(thisPosition,thisResource,oPos,oResource,matchType);
			elems.add(e);
		}
	}
	public DiffConsolidator(){
		Comparator<Position> posComparator = new Comparator<Position>() {
	        @Override 
	        public int compare(Position s1, Position s2) {
	            if(s1.length == s2.length && s1.offset == s2.offset) return 0;
	            else if(s1.offset < s2.offset || (s1.offset == s2.offset && s1.length < s2.length)) return -1;
	            else if(s1.offset > s2.offset || (s1.offset == s2.offset && s1.length > s2.length)) return 1;
	            else return -1;
	        }           
	    };
	    matches = new TreeMap<Position, MatchInfo>(posComparator);
	}
	SortedMap<Position, MatchInfo> matches;
	Set<String> sources = new TreeSet();

	
	private String read(DocumentRangeNode q){

				Position pos = q.getRange();
				IDocument doc = q.getDocument();
				try {
					String text = doc.get(pos.offset, pos.length);
					return "("+pos.offset+","+pos.length+")\n"+text;
				} catch (BadLocationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return "";
	}
	public void consolidate(String name,  List<Difference> ds){
		
		sources.add(name);
		Position pos=null;
		//System.out.println("Consolidating "+name);
		for(Difference d : ds){
			//System.out.println("	+c "+d.thisPosition.offset + " ["+d.thisPosition.length+"]");
			pos = d.thisPosition;
			if(matches.get(pos) == null){
				matches.put(pos, new MatchInfo(d.thisPosition, d.thisResource));
			}
			matches.get(pos).add(d.otherPosition, d.otherResource, d.matchType);	
		}
	}
	public Map<Position,MatchInfo> getMatches(){
		return matches;
	}
	public void clear(){
		matches.clear();
	}
	/*
	 * 
	 private void displayDiffNodes(DiffNode d){
		ITypedElement l = d.getLeft();
		ITypedElement r = d.getRight();
		if(l != null){
			if(l instanceof DocumentRangeNode){
				DocumentRangeNode q = (DocumentRangeNode) l;
				Position pos = q.getRange();
				IDocument doc = q.getDocument();
				try {
					String text = doc.get(pos.offset, pos.length);
					System.out.println(text);
				} catch (BadLocationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		for(IDiffElement c : d.getChildren()){
			displayDiffNodes((DiffNode) c);
		}
		
	}
	 * 
	 */
}
