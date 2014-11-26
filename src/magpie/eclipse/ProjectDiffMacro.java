package magpie.eclipse;


import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import magpie.diff.JavaNode;
import magpie.display.SWTList;
import magpie.util.Difference;
import magpie.util.Pair;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.CompareUI;
import org.eclipse.compare.IModificationDate;
import org.eclipse.compare.IStreamContentAccessor;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.compare.structuremergeviewer.DocumentRangeNode;
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
import org.eclipse.jface.text.Document;
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

public class ProjectDiffMacro {
	static class Element{
		public String name;
		public ICompilationUnit left;
		public ICompilationUnit right;
		public Element(String name, ICompilationUnit left, ICompilationUnit right){
			this.name = name;
			this.left = left;
			this.right = right;
		}
	}
	public static Map<String, List<Element>> compares = new HashMap<String, List<Element>>();
	static CompareConfiguration CurrentConfig = new CompareConfiguration();
	static EditorInput CurrentInput = new EditorInput(CurrentConfig);
	
	public static IEditorPart editor;
	public static IWorkbenchSite site;
	public static void closeEditor(String s, IWorkbenchPage wb){
		IEditorReference[] editorRefs = wb.getEditorReferences();
		for (int i = 0; i < editorRefs.length; i++) {
			IEditorPart part = editorRefs[i].getEditor(false);
			if(part instanceof ITextEditor){
				ITextEditor p = (ITextEditor) part;
				String name = p.getEditorInput().getName();
				if(name.equals(s)){
					p.close(false);
				}
			}
		}
	}
	public static IEditorPart findReusableCompareEditor(
			CompareEditorInput input, IWorkbenchPage page,
			Class[] editorInputClasses) {
		IEditorReference[] editorRefs = page.getEditorReferences();
		// first loop looking for an editor with the same input
		for (int i = 0; i < editorRefs.length; i++) {
			IEditorPart part = editorRefs[i].getEditor(false);
			if (part != null && part instanceof IReusableEditor) {
				for (int j = 0; j < editorInputClasses.length; j++) {
					// check if the editor input type 
					// complies with the types given by the caller
					if (editorInputClasses[j].isInstance(part.getEditorInput())&& part.getEditorInput().equals(input))
						return part;
				}
			}
		}

		// no re-usable editor found
		return null;
	}
	 static class EditorInput extends CompareEditorInput{
		DocumentRangeNode left;
		DocumentRangeNode right;
		JavaStructureCreator creator = new JavaStructureCreator();
		public EditorInput(CompareConfiguration configuration) {
			super(configuration);
			// TODO Auto-generated constructor stub
		}
		public void setItems(ICompilationUnit left, ICompilationUnit right){
			
			DocumentRangeNode c1 = null;
			DocumentRangeNode c2 = null;
				try {
					c1 = (DocumentRangeNode) creator.createStructure(new JavaNode(new Document(left.getSource())),null);
					c2 = (DocumentRangeNode) creator.createStructure(new JavaNode(new Document(right.getSource())),null);
				} 
				catch (CoreException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			this.left = c1;
			this.right = c2;	
		}
		@Override
		protected Object prepareInput(IProgressMonitor monitor)
				throws InvocationTargetException, InterruptedException {
			// TODO Auto-generated method stub
			Differencer d = new Differencer();
			Object diff = d.findDifferences(false, null, null, null, left, right);
			return diff;
		}
		
	}
	static boolean hasWebSource(String ref, String web){
		List<Element> cmps = compares.get(ref);
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
	public static void constructDiffModel(IWorkbenchSite wb, ICompilationUnit cuW){
		IProject proj = cuW.getResource().getProject();
		IJavaProject jproj = cuW.getJavaProject();
		String refName = cuW.getElementName();
		String refid = refName.replace(".java", "");
		SortMembersAction memberSort = new SortMembersAction(wb);
		FormatAllAction formatAllAction = new FormatAllAction(wb);
		CompareEditorInput cu;
		EditorInput cinput = null;
		CurrentConfig.setLeftEditable(true);
		CurrentConfig.setRightEditable(true);
		
		if(!compares.containsKey(refName)){
			compares.put(refName, new LinkedList<Element>());
		}
		/*
		JavaContentViewerCreator javaViewerCreator = new JavaContentViewerCreator();
		CompareConfiguration javaViewerConfigurator = new CompareConfiguration();
		JavaMergeViewer javaContentViewer = (JavaMergeViewer) javaViewerCreator.createViewer(wb.getWorkbenchWindow().getShell(), javaViewerConfigurator);
		*/
		MagpieFileHandler.activateCompilation(cuW);
		Pair<IPackageFragment, IPackageFragment> pfs = MagpieFileHandler.trailblazeCompilationUnit(cuW);
		ICompilationUnit refCU= MagpieFileHandler.writeJavaFile(pfs.e1, cuW);
		List<ICompilationUnit> webFiles = MagpieFileHandler.getJavaFiles(pfs.e2);
		boolean isOpen;
		isOpen = refCU.isOpen();
		memberSort.run(new StructuredSelection(refCU));
		formatAllAction.run(new StructuredSelection(refCU));
		if(!isOpen && refCU.isOpen()){
			ProjectDiffMacro.closeEditor(refName,  wb.getPage());
		}
		
		for(ICompilationUnit webCU : webFiles){
			String webName = webCU.getElementName();
			String webid = webName.replace(".java", "");
			if(webid.startsWith(refid)){
				isOpen = webCU.isOpen();
				memberSort.run(new StructuredSelection(webCU));
				formatAllAction.run(new StructuredSelection(webCU));
				if(!isOpen && webCU.isOpen()){
					ProjectDiffMacro.closeEditor(webName,  wb.getPage());
				}
				if(!hasWebSource(refName, webName)){
					compares.get(refName).add(new Element(webName, refCU, webCU));
				}
			}
		}
		site = wb;
		SWTList.create(wb.getShell(), refCU.getElementName(), null);
		MagpieFileHandler.deactivateCompilation(cuW);
		
	}
	public static List<String> getFileList(String ref){
		List<String> files = new LinkedList<String>();
		List<Element> cmps = compares.get(ref);
		for(Element e : cmps){
			files.add(e.name);
		}
		Collections.sort(files);
		return files;
	}
	public static void select(String ref, String web){
		List<Element> cmps = compares.get(ref);
		for(Element e : cmps){
			if(e.name.equals(web)){
				CurrentInput.setItems(e.left, e.right);
			}
		}
		Class[] cls = {EditorInput.class};
		editor = findReusableCompareEditor(CurrentInput, site.getPage(), cls);
		CompareUI.reuseCompareEditor(CurrentInput, (IReusableEditor) editor);
		
	}

	
}
