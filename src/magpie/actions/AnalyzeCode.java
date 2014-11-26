package magpie.actions;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import magpie.analyze.CodeComparer;
import magpie.analyze.CodeFeatureBuilder;
import magpie.eclipse.MagpieFileHandler;
import magpie.eclipse.ProjectCodeExtractor;
import magpie.eclipse.ProjectDiffHandler;
import magpie.eclipse.ProjectDiffMacro;
import magpie.eclipse.ProjectDiffSummary;
import magpie.util.Code;
import magpie.util.Pair;
import magpie.views.DiffView;
import magpie.web.WebCrawler;

import org.eclipse.core.internal.runtime.Log;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jdt.internal.core.*;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;
/**
 * Our sample action implements workbench action delegate.
 * The action proxy will be created by the workbench and
 * shown in the UI. When the user tries to use the action,
 * this delegate will be created and execution will be 
 * delegated to it.
 * @see IWorkbenchWindowActionDelegate
 */
@SuppressWarnings("restriction")
public class AnalyzeCode implements IWorkbenchWindowActionDelegate {
	private IWorkbenchWindow window;
	ProjectCodeExtractor codeExtractor = new ProjectCodeExtractor();
	ProjectDiffHandler diffHandler = new ProjectDiffHandler();
	ProjectDiffMacro diffMacro = new ProjectDiffMacro();
	/**
	 * The constructor.
	 */
	

	/**
	 * The action has been activated. The argument of the
	 * method represents the 'real' action sitting
	 * in the workbench UI.
	 * @see IWorkbenchWindowActionDelegate#run
	 */

	public static IViewPart getView(String id) {
		IWorkbench w = PlatformUI.getWorkbench();
		if(w == null) return null;
		IWorkbenchWindow wi = w.getActiveWorkbenchWindow();
		if(wi == null) return null;
		IWorkbenchPage wp = wi.getActivePage();
		IViewReference viewReferences[] = wp.getViewReferences();
		for (int i = 0; i < viewReferences.length; i++) {
			if (id.equals(viewReferences[i].getId())) {
				return viewReferences[i].getView(false);
			}
		}
		return null;
	}
	
	public void run(IAction action) {

		List<Code> files = new LinkedList<Code>(codeExtractor.getFiles());
		System.out.println("FILES: "+files.size());
		if(files.size() > 0){
			
			for(Code file: files){
				IResource res = file.ast.getResource();
				String name = file.ast.getElementName();
				System.out.println("DIFF:"+res.getName());
				IWorkbenchPage wp = window.getActivePage();
				IViewPart wv= wp.getViewReferences()[0].getView(true);
				IWorkbenchSite site = wv.getSite();
				//ProjectDiffMacro.constructDiffModel(site, file.ast);
				ProjectDiffSummary.constructDiffModel(site, file.ast);
				//diffHandler.constructDiffModel(res);
			}
		}
		DiffView diffView = (DiffView)getView(DiffView.ID);
		if(diffView != null)
			diffView.viewer.refresh();
		//testCompare();
	}

	/**
	 * Selection in the workbench has been changed. We 
	 * can change the state of the 'real' action here
	 * if we want, but this can only happen after 
	 * the delegate has been created.
	 * @see IWorkbenchWindowActionDelegate#selectionChanged
	 */
	
	
	public void selectionChanged(IAction action, ISelection selection) {
		if(selection instanceof ITreeSelection){
			ITreeSelection t = (ITreeSelection) selection;
			System.out.println("Tree Selected");
			codeExtractor.clearFiles();
			
			for(TreePath sel:t.getPaths()){
				Object elem = sel.getLastSegment();
				codeExtractor.extractFrom(elem);
				
			}
			
		}
		else if(selection instanceof TextSelection){
			System.out.println("Text Selected: Unsupported.");
			
		}
		else{
			System.out.println("Other Selected: "+selection.getClass().toString());
		}
	}

	/**
	 * We can use this method to dispose of any system
	 * resources we previously allocated.
	 * @see IWorkbenchWindowActionDelegate#dispose
	 */
	public void dispose() {
	}

	/**
	 * We will cache window object in order to
	 * be able to provide parent shell for the message dialog.
	 * @see IWorkbenchWindowActionDelegate#init
	 */
	public void init(IWorkbenchWindow window) {
		this.window = window;
	}
}