package magpie.diff;

import difflib.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;


import magpie.util.Difference;
import magpie.util.Difference.MatchType;

import org.eclipse.compare.contentmergeviewer.*;
import org.eclipse.compare.rangedifferencer.RangeDifference;
import org.eclipse.compare.rangedifferencer.RangeDifferencer;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.compare.structuremergeviewer.DocumentRangeNode;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.Position;

public class TextDiffEngine extends DiffEngine<IResource> {

	private static List<String> stringToLines(String s) {
	        List<String> lines = new LinkedList<String>();
	        String line = "";
	        try {
	                BufferedReader in = new BufferedReader(new StringReader(s));
	                while ((line = in.readLine()) != null) {
	                        lines.add(line);
	                }
	        } catch (IOException e) {
	                e.printStackTrace();
	        }
	        return lines;
	}
	protected String readFile(IResource r){
		try {
			IFile f = (IFile) r;
			InputStream i;
			i = f.getContents();
	        String s = new Scanner(i,"UTF-8").useDelimiter("\\A").next();
	        return s;
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "";
	}
	public Difference getDifference(List<Difference> deltas, int soff){
		for(Difference delta : deltas){
			if(soff == delta.thisPosition.offset){
				return delta;
			}
		}
		return null;
	}
	public List<Difference> getSimilarities(List<Difference> diff, int l1, int l2){
		
		
		return null;
	}
	public Position getPosition(Chunk c, List<String> s){
		int loff = c.getPosition();
		int llen = c.size();
		/*
		int off=0;
		for(int i=0; i < loff; i++){
			off += s.get(i).length();
		}
		int len=0;
		for(int i=loff; i < llen+loff; i++){
			len += s.get(i).length();
		}
		*/
		return new Position(loff,llen);
		
	}

	protected List<Difference> takeDiff(IResource r1, Position o1, IResource r2, Position o2) {
			String s1 = readFile(r1).substring(o1.offset, o1.offset+o1.length);
			String s2 = readFile(r2).substring(o2.offset, o2.offset+o2.length);;
	        List<Difference> diff = new LinkedList<Difference>();
			List<String> original = stringToLines(s1);
	        List<String> revised  = stringToLines(s2);
	        // Compute diff. Get the Patch object. Patch is the container for computed deltas.
	        Patch patch = DiffUtils.diff(original, revised);
	        
	        for (Delta delta: patch.getDeltas()) {
	        	Chunk orig = delta.getOriginal();
	        	Chunk other = delta.getRevised();
	        	Position origPos = getPosition(orig, original);
	        	Position newPos = getPosition(other, revised);
	        	List<String> newData = (List<String>)other.getLines();
	        	Difference d=null;
	        	if(delta.getType() == Delta.TYPE.CHANGE){
		        	 d = new Difference(origPos, r1, newPos, r2, MatchType.Different, newData);
	        	}
	        	else if(delta.getType() == Delta.TYPE.INSERT){
	        		d = new Difference(origPos, r1, newPos, r2, MatchType.Delete, newData);
	        	}
	        	else if(delta.getType() == Delta.TYPE.DELETE){
	        		d = new Difference(origPos, r1, newPos, r2, MatchType.Insert, newData);
	        	}
	        	if(d != null) diff.add(d);
	        }
	        List<Difference> sims= getSimilarities(diff, s1.length(), s2.length());
			return diff;
	}

	public List<Difference> diff(IResource r1, IResource r2){
		String s1 = readFile(r1);
		String s2 = readFile(r2);
		Position p1 = new Position(0, s1.length());
		Position p2 = new Position(0, s1.length());
		return takeDiff(r1,p1, r2, p2);
	}
}
