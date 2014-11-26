package magpie.analyze;

import java.io.File;
import java.util.HashMap;

import org.eclipse.compare.*;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;

public class CodeComparer {
		
		private void printDiffTree(DiffNode d, String depth){
			String name = d.getName();
			String id = d.getId().toString();
			System.out.println(depth+id+":"+name);
			for(int i=0; i < d.getChildren().length; i++){
				DiffNode e = (DiffNode) d.getChildren()[i];
				printDiffTree(e, depth+" ");
			}
		}
		public void compare(IResource f1, IResource f2){
			ResourceNode r1 = new ResourceNode(f1);
			ResourceNode r2 = new ResourceNode(f2);
			Differencer diff = new Differencer();
			DiffNode difftree = (DiffNode) diff.findDifferences(false, null, null, null, r1, r2);
			printDiffTree(difftree,"");
		}
		/*
		public static void main(String[] args){
			IWorkspaceRoot w = ResourcesPlugin.getWorkspace().getRoot();
			IProject p = w.getProject("Magpie");
			try {
				p.open(null);
				IFile f1 = p.getFile("web/test/source.0.java");
				IFile f2 = p.getFile("web/test/source.1.java");
				CodeComparer c = new CodeComparer();
				c.compare(f1, f2);
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		*/
}
