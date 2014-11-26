package magpie.diff;

import java.io.InputStream;
import java.util.List;
import java.util.Scanner;

import magpie.util.Difference;

import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.compare.structuremergeviewer.IStructureComparator;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.internal.ui.compare.*;
import org.eclipse.jface.text.Document;
public class JavaDiffEngine extends DiffEngine<String>{
	Differencer differencer = new Differencer();
	JavaStructureCreator creator = new JavaStructureCreator();
	@Override
	public List<Difference>  diff(String r1, String r2) {
		IStructureComparator c1;
		IStructureComparator c2;
		IResource r;
		try {
			c1 = creator.createStructure(new JavaNode(new Document(r1)),null);
			c2 = creator.createStructure(new JavaNode(new Document(r2)),null);
			if(c1 == null || c2 == null){
				System.out.println("Failed to create structure");
				return null;
			}
			DiffNode df = (DiffNode) differencer.findDifferences(false, null, null, null, c1, c2);
			
			return null;
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		
	}
}
