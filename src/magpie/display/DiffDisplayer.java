package magpie.display;

import java.io.InputStream;
import java.util.Map;
import java.util.Scanner;

import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.Position;

import magpie.diff.DiffConsolidator;
import magpie.diff.DiffConsolidator.MatchInfo;
import magpie.util.Difference;
import magpie.util.Difference.MatchType;

public class DiffDisplayer {
		String MARKER_ID = "magpie.source.marker";
		String DELETED_MARKER_ID = "magpie.source.marker.delete";
		String INSERTED_MARKER_ID = "magpie.source.marker.new";
		String CHANGED_MARKER_ID = "magpie.source.marker.different";
		String SAME_MARKER_ID = "magpie.source.marker.same";
		public void mark(IResource resource, Document doc, MatchType type, int offset, int length){
			String MARKER_ID;
			if(type == MatchType.Delete) MARKER_ID = DELETED_MARKER_ID;
			else if(type == MatchType.Insert) MARKER_ID = INSERTED_MARKER_ID;
			else if(type == MatchType.Different) MARKER_ID = CHANGED_MARKER_ID;
			else MARKER_ID = SAME_MARKER_ID;
			
			try {
				try {
					//System.out.println("MARK: "+offset+" ["+length+"] : " + type);
					int line, startchar, endchar;
					int k = offset;
					while(k <= offset + length){
						IRegion reg = doc.getLineInformation(k);
						startchar = reg.getOffset();
						endchar = startchar + reg.getLength();
						line = k+1;
						//System.out.println("	+l:"+line+" o:"+startchar+" e:"+endchar);
						IMarker marker = resource.createMarker(MARKER_ID);
						marker.setAttribute(IMarker.LINE_NUMBER, line);
						marker.setAttribute(IMarker.CHAR_START, startchar);
						marker.setAttribute(IMarker.CHAR_END, endchar);
						marker.setAttribute(IMarker.PRIORITY, IMarker.PRIORITY_HIGH);
						k ++;
					}
					
				} catch (BadLocationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		private Document read(IResource r){
			try {
				IFile f = (IFile) r;
				InputStream i;
				i = f.getContents();
		        String s = new Scanner(i,"UTF-8").useDelimiter("\\A").next();
		        return new Document(s);
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
		}

		public void display(IResource file, DiffConsolidator sources, String filter){
			Map<Position, MatchInfo> matches = sources.getMatches();
			Document doc = read(file);
			try {
				file.deleteMarkers(MARKER_ID, true, IResource.DEPTH_INFINITE);
			} catch (CoreException e) {
			}
			for(Position pos : matches.keySet()){
				int offset = pos.offset;
				int length = pos.length;
				MatchInfo info = matches.get(pos);
				if((filter != null && info.hasSource(filter))){
					mark(file, doc, info.getSource(filter).matchType, offset, length);
				}
			}
		}
}
