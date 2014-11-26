package magpie.eclipse;


import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import magpie.diff.JavaNode;
import magpie.util.Pair;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.CompareUI;
import org.eclipse.compare.IModificationDate;
import org.eclipse.compare.IStreamContentAccessor;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.compare.structuremergeviewer.DocumentRangeNode;
import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.ui.actions.*;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IReusableEditor;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.internal.util.Util;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.jdt.internal.core.PackageFragmentRoot;
import org.eclipse.jdt.internal.ui.compare.JavaContentViewerCreator;
import org.eclipse.jdt.internal.ui.compare.JavaMergeViewer;
import org.eclipse.jdt.internal.ui.compare.JavaStructureCreator;
import org.eclipse.jdt.launching.JavaRuntime;

import difflib.Chunk;
import difflib.Delta;
import difflib.DiffUtils;
import difflib.Patch;

public class ProjectDiffSummary {
	public static class Element{
		public String name;
		public String path;
		public double left;
		public double right;
		public Element(String name, String path, double left, double right){
			this.name = name;
			this.path = path;
			this.left = left;
			this.right = right;
		}
	}
	public static Map<String, Pair<String, List<Element>>> compares = new HashMap<String, Pair<String, List<Element>>>();


	static JavaStructureCreator creator = new JavaStructureCreator();
	public static DiffNode runDifferences(String left, String right) {
			// TODO Auto-generated method stub
		DocumentRangeNode c1 = null;
		DocumentRangeNode c2 = null;
		try {
			c1 = (DocumentRangeNode) creator.createStructure(new JavaNode(new Document(left)),null);
			c2 = (DocumentRangeNode) creator.createStructure(new JavaNode(new Document(right)),null);
		} 
		catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		Differencer d = new Differencer();
		DiffNode rootNode = new DiffNode(Differencer.NO_CHANGE);
		rootNode.setLeft((ITypedElement) c1);
		rootNode.setRight((ITypedElement) c2);
		DiffNode diff =(DiffNode) d.findDifferences(false, null, rootNode, null, c1, c2);
		if(diff != null) creator.rewriteTree(d, diff);
		else return rootNode;
		return diff;
	}

	static boolean hasWebSource(String ref, String web){
		List<Element> cmps = compares.get(ref).e2;
		if(cmps == null){
			return false;
		}
		for(Element cmp : cmps){
			if(cmp.name.equals(web)){
				return true;
			}
		}
		return false;
	}
	enum ScoreKind{
		CHANGE,
		SAME,
		INSERT,
		DELETE
	}
	private static List<String> stringToTokens(String s) {
        List<String> tokens = new LinkedList<String>();
        String [] t = s.split("[\\s\\n]+");
        for(String tok : t){
        	tokens.add(tok);
        }
        
        return tokens;
	}
	public static Pair<Integer,Integer> scoreText(String s1, String s2, ScoreKind kind){
		List<String> original = stringToTokens(s1);
        List<String> revised  = stringToTokens(s2);
        // Compute diff. Get the Patch object. Patch is the container for computed deltas.
        Patch patch = DiffUtils.diff(original, revised);
        
        int lscore = 0; int rscore=0;
        for (Delta delta: patch.getDeltas()) {
        	Chunk orig = delta.getOriginal();
        	Chunk other = delta.getRevised();
        	if(delta.getType() == Delta.TYPE.CHANGE && (kind == ScoreKind.CHANGE || kind == ScoreKind.SAME)){
        		 lscore += orig.size();
        		 rscore += other.size();
        	}
        	else if(delta.getType() == Delta.TYPE.DELETE && kind == ScoreKind.DELETE){
       		 lscore += orig.size();
       		 rscore += other.size();
        	}
        	else if(delta.getType() == Delta.TYPE.INSERT && kind == ScoreKind.INSERT){
          		 lscore += orig.size();
          		 rscore += other.size();
           	}
        }
        if(kind == ScoreKind.SAME){
        	lscore = s1.length() - lscore;
        	rscore = s2.length() - rscore;
        }
        return new Pair(lscore, rscore);
	}
	public static Pair<Integer,Integer> _Score(DiffNode root, ScoreKind cs){
		DocumentRangeNode dr_r = (DocumentRangeNode) root.getRight();
		DocumentRangeNode dr_l = (DocumentRangeNode) root.getLeft();
		Position r = dr_r == null ? new Position(0,0) : dr_r.getRange();
		Position l = dr_l == null ? new Position(0,0) :  dr_l.getRange();
		if(root.getKind() == Differencer.NO_CHANGE && cs == ScoreKind.SAME)
			return new Pair(l.length, r.length);
		else if(root.getKind() == Differencer.ADDITION && cs == ScoreKind.INSERT)
			return new Pair(l.length, r.length);
		else if(root.getKind() == Differencer.DELETION && cs == ScoreKind.DELETE)
			return new Pair(l.length, r.length);
		else if(root.getKind() == Differencer.CHANGE){
			IDiffElement [] els = root.getChildren();
			if(els.length > 0){
				int lc=0; int rc=0;
				for(IDiffElement el : els){
					DiffNode e = (DiffNode) el;
					Pair<Integer,Integer> t = _Score(e, cs);
					lc += t.e1;
					rc += t.e2;
				}
				return new Pair(lc, rc);
			}
			else{
				try {
					String s1 = dr_l.getDocument().get(l.offset, l.length);
					String s2 = dr_r.getDocument().get(r.offset, r.length);
					return scoreText(s1,s2,cs);
				} catch (BadLocationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				};
				
			}
		}
		return new Pair(0,0);
	}
	
	public static Pair<Double, Double> getScore(DiffNode root){
		DocumentRangeNode dr_r = (DocumentRangeNode) root.getRight();
		DocumentRangeNode dr_l = (DocumentRangeNode) root.getLeft();
		Position r = dr_r.getRange();
		Position l = dr_l.getRange();
		try {
			String s1 = dr_l.getDocument().get(l.offset, l.length);
			String s2 = dr_r.getDocument().get(r.offset, r.length);
			Pair<Integer, Integer> ins = _Score(root, ScoreKind.INSERT);
			Pair<Integer, Integer> del = _Score(root, ScoreKind.DELETE);
			Pair<Integer, Integer> chang = _Score(root, ScoreKind.CHANGE);
			int s1len = (s1.length() + ins.e1);
			double scorel =((double) (s1len - ins.e1- del.e1 - chang.e1)) / s1len;
			int s2len = (s2.length() + ins.e2);
			double scorer = ((double) (s2len - ins.e2- del.e2 - chang.e2)) / s2len;
			return new Pair<Double,Double>(scorel, scorer);
		} catch (BadLocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return new Pair<Double,Double>(0.0, 0.0);
	}
	protected static String readFile(IFile f){
		try {
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
	public static void constructDiffModel(IWorkbenchSite wb, ICompilationUnit cuW){
		
		
		try {
			String refName = cuW.getElementName();
			String refid = refName.replace(".java", "");
			String cuWSource = cuW.getSource();
			if(!compares.containsKey(refName)){
				compares.put(refName, new Pair<String, List<Element>>(cuW.getPath().toString(), new LinkedList<Element>()));
			}
			
			Pair<IPath, IPath> paths = MagpieFileHandler.trailblazePath(cuW.getResource());
			List<IFile> webFiles = MagpieFileHandler.getFiles(paths.e2);
			boolean isOpen;

			for(IFile webCU : webFiles){
				String webName = webCU.getName();
				String webid = webName.replace(".java", "");
				if(webid.startsWith(refid)){
					
					if(!hasWebSource(refName, webName)){
						DiffNode d = runDifferences(cuWSource,readFile(webCU));
						Pair<Double,Double> scores = getScore(d);
						compares.get(refName).e2.add(new Element(webName, webCU.getFullPath().toString(), scores.e1, scores.e2));
					}
				}
			}
			

			Collections.sort(compares.get(refName).e2, new Comparator<Element>(){
				@Override
				public int compare(Element o1, Element o2) {
					// TODO Auto-generated method stub
					if(o1.left > o2.left) return -1;
					else if(o1.left < o2.left) return 1;
					else return 0;
				}
				
			});
			for(Element e : compares.get(refName).e2){
				double r = e.left;
				double w = e.right;
				String wname = e.name;
				System.out.println(wname + ": r="+r + "   w="+w);
			}
		} catch (JavaModelException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		
		
	}
	
	
}
