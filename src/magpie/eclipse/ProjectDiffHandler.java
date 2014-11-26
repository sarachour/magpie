package magpie.eclipse;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Scanner;

import magpie.diff.DiffConsolidator;
import magpie.diff.DiffEngine;
import magpie.diff.JavaDiffEngine;
import magpie.diff.LazyJavaDiffEngine;
import magpie.diff.TextDiffEngine;

import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.compare.structuremergeviewer.DocumentRangeNode;
import org.eclipse.compare.structuremergeviewer.IDiffContainer;
import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.internal.core.JavaProject;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;

import magpie.diff.JavaNode;
import magpie.diff.DiffConsolidator.MatchInfo;
import magpie.display.DiffDisplayer;
import magpie.util.Difference;
public class ProjectDiffHandler {
	String WebSourceDir = "web/";
	TextDiffEngine textDiffEngine = new TextDiffEngine();
	LazyJavaDiffEngine javaDiffEngine = new LazyJavaDiffEngine();
	DiffConsolidator consolidator = new DiffConsolidator();
	DiffDisplayer displayer = new DiffDisplayer();
	

	private String readFile(IResource r){
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

	public void constructDiffModel(IResource resource){
		IProject proj = resource.getProject();
		String resourceName = resource.getName();
		String webSourceDir = WebSourceDir + "/" + resourceName;
		IFolder webFolder = proj.getFolder(webSourceDir);
		IResource[] resources;
		consolidator.clear();
		try {
			resources = webFolder.members();
			for(IResource res : resources){
				System.out.println("	+"+res.getName());
				List<Difference> droot;
				String s1 = readFile(resource);
				String s2 = readFile(res);
				if(resourceName.endsWith(".java")){
					droot = textDiffEngine.diff(resource,  res);
				}
				else{
					droot = textDiffEngine.diff(resource,  res);
				}
				consolidator.consolidate(res.getName(), droot);
				//javaDiffEngine.diff(compileFile(resource), compileFile(res));
				
			}
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		displayer.display(resource, consolidator, "result0.java");
		
	}
}
