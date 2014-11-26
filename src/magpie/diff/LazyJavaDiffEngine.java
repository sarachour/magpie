package magpie.diff;

import java.util.LinkedList;
import java.util.List;

import magpie.util.Difference;

import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.compare.structuremergeviewer.DocumentRangeNode;
import org.eclipse.compare.structuremergeviewer.IStructureComparator;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.internal.ui.compare.JavaStructureCreator;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;

public class LazyJavaDiffEngine extends TextDiffEngine {
	JavaStructureCreator creator = new JavaStructureCreator();
	TextDiffEngine textdiff = new TextDiffEngine();
	public int getScore(List<Difference> diffs){
		int score=0;
		for(Difference diff : diffs){
			for(String s : diff.otherData){
				score += s.length();
			}
		}
		return score;
	}
	public List<Difference> nodeDiff(IResource thisResource, DocumentRangeNode thisNode, IResource otherResource, DocumentRangeNode otherNode){
		Difference d;
		Position orng;
		IDocument odoc;
		List<DocumentRangeNode> queue = new LinkedList<DocumentRangeNode>();
		int bestScore = -1;
		DocumentRangeNode bestNode = null;
		List<Difference> bestDiffs = new LinkedList<Difference>();
		Position trng = thisNode.getRange();
		
		queue.add(otherNode);
		int nadds = 1;
		while(nadds > 0){
			int len = queue.size();
			nadds = 0;
			for(int i=0; i < len; i++){
				DocumentRangeNode currNode = queue.get(0);
				queue.remove(0);
				if(currNode.getChildren().length == 0){
					queue.add(queue.size(), currNode);
					
				}
				else{
					for(Object child : currNode.getChildren()){
						DocumentRangeNode jchild = (DocumentRangeNode) child;
						queue.add(0,jchild);
						nadds++;
					}
				}
			}
		}
		while(!queue.isEmpty()){
			DocumentRangeNode currNode = queue.get(0);
			queue.remove(0);
			orng = currNode.getRange();
			List<Difference> diffs= takeDiff(thisResource, trng, otherResource, orng);
			int score = getScore(diffs);
			if(bestScore == -1 || score < bestScore){
				bestDiffs = diffs;
				bestNode = currNode;
				bestScore = score;
			}
		}
		return bestDiffs;
		
		
	}
	public List<Difference> traverseNodes(IResource thisResource, DocumentRangeNode thisNode, IResource otherResource, DocumentRangeNode otherNode){
		List<Difference> diffs = new LinkedList<Difference>();
		if(thisNode.getChildren().length == 0){
			Difference d;
			Position rng = thisNode.getRange();
			IDocument doc = thisNode.getDocument();
			List<Difference> differences = nodeDiff(thisResource, thisNode, otherResource, otherNode);
			for(Difference diff : differences){
				try {
					diff.thisPosition.offset += doc.getLineOfOffset(rng.offset);
				} catch (BadLocationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			diffs.addAll(differences);
			
			try {
				String text = doc.get(rng.offset, rng.length);
				System.out.println(text+"\n"+"------");
				for(Difference diff : differences){
					System.out.println("pos:"+diff.thisPosition.offset + ", len:"+diff.thisPosition.length+"\n");
					for(String line : diff.otherData){
						System.out.println(line);
					}
				}
				System.out.println("################");
			} catch (BadLocationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		for(Object child : thisNode.getChildren()){
			DocumentRangeNode jchild = (DocumentRangeNode) child;
			diffs.addAll(traverseNodes(thisResource, jchild, otherResource, otherNode));
		}
		return diffs;
	}
	
	public List<Difference> diff(IResource r1, IResource r2){
		String s1 = readFile(r1);
		String s2 = readFile(r2);
		Position p1 = new Position(0, s1.length());
		Position p2 = new Position(0, s1.length());
		DocumentRangeNode c1;
		DocumentRangeNode c2;
		try {
			c1 = (DocumentRangeNode) creator.createStructure(new JavaNode(new Document(s1)),null);
			c2 = (DocumentRangeNode) creator.createStructure(new JavaNode(new Document(s2)),null);
			if(c1 == null || c2 == null){
				System.out.println("Failed to create structure");
				return null;
			}
			return traverseNodes(r1, c1, r2, c2);
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		
	}
}
