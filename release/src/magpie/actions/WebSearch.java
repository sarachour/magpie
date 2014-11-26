package magpie.actions;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.TimeUnit;

import magpie.analyze.CodeFeatureBuilder;
import magpie.eclipse.ProjectCodeExtractor;
import magpie.util.Code;
import magpie.util.Logger;
import magpie.util.Pair;
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
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
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
public class WebSearch implements IWorkbenchWindowActionDelegate {
	private IWorkbenchWindow window;
	ProjectCodeExtractor codeExtractor;
	WebCrawler webCrawler;
	public WebSearch(){
		codeExtractor = new ProjectCodeExtractor();
		webCrawler = new WebCrawler();
	}
	/**
	 * The constructor.
	 */
	

	/**
	 * The action has been activated. The argument of the
	 * method represents the 'real' action sitting
	 * in the workbench UI.
	 * @see IWorkbenchWindowActionDelegate#run
	 */

	public class WebSearchAll extends RecursiveAction {
	    List<Code> files;
	    IProgressMonitor prog;
	    WebCrawler webCrawler = new WebCrawler();
	    public static final int chunksize = 8;
	    
	    // Processing window size; should be odd.
	  
	    public WebSearchAll(IProgressMonitor prog, List<Code> files) {
	        this.files = files;
	        this.prog = prog;
	    }
	    public void computeDirect(){
	    	System.out.println("[WEBSEARCH] Chunk = "+files.size() + " files");
	    	for(Code file : files){
		    	System.out.println("[WEBSEARCH]" + file.getName());
				IResource res = file.ast.getResource();
				String name = file.ast.getElementName();
				CompilationUnit source = file.ast;
				webCrawler.crawl(name,  source,0);
				Logger.info("[WEBSEARCH] Crawl Level 1 Completed. <" + file.getName()+">");
				if(!webCrawler.hasURLs(name)){
					Logger.info("[WEBSEARCH] FALLBACK. Crawl Level 2 Starting. <" + file.getName()+">");
					webCrawler.crawl(name,  source,1);
					Logger.info("[WEBSEARCH] Crawl Level 2 Completed. <" + file.getName()+">");
				}
				Logger.info("[WEBSEARCH] Grab starting. <" + file.getName()+">");
				webCrawler.grab(name, source);
				Logger.info("[WEBSEARCH] Search finished. <" + file.getName()+">");
				webCrawler.clear();
				prog.worked(1);
	    	}
	    }
	    protected void compute() {
	        if(files.size() <= chunksize){
	        	computeDirect();
	        }
	        else{
	        	List<Code> firstHalf = new LinkedList<Code>();
	        	List<Code> secondHalf = new LinkedList<Code>();
	        	for(int i=0; i < files.size(); i++){
	        		if(i % 2 == 0) firstHalf.add(files.get(i));
	        		else secondHalf.add(files.get(i));
	        	}
	        	invokeAll(new WebSearchAll(prog, firstHalf),
		                  new WebSearchAll(prog, secondHalf));
	        }
	        
	    }
	}
	public void run(IAction action) {
		Job job = new Job("Seach Web for Source"){
			protected IStatus run(IProgressMonitor monitor){
				ForkJoinPool pool = new ForkJoinPool(4);
				List<Code> files = new LinkedList<Code>(codeExtractor.getFiles());
				Logger.info("[WEBSEARCH] Number Sources: "+files.size());
				monitor.beginTask("Performing Web-Search on "+files.size() + " files...",files.size());
				WebSearchAll fb = new WebSearchAll(monitor, files);
				ForkJoinTask fj = pool.submit(fb);
				
				try {
					while(!fj.isDone()){
						if(monitor.isCanceled()){
							pool.shutdownNow();
							return Status.CANCEL_STATUS;
						}
						else{
							Thread.sleep(1000);
						}
					}
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					try {
						fj.get();
					} catch (InterruptedException e1) {
						// TODO Auto-generated catch block
						return Status.CANCEL_STATUS;
					} catch (ExecutionException e1) {
						// TODO Auto-generated catch block
						return Status.CANCEL_STATUS;
					}
				}
				monitor.done();
				return Status.OK_STATUS;
			}
		};
		System.out.println("Magpie: run");
		job.schedule();
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
			codeExtractor.clearFiles();
			for(TreePath sel:t.getPaths()){
				Object elem = sel.getLastSegment();
				codeExtractor.extractFrom(elem);
				
			}
			System.out.println("Number Extracted: "+codeExtractor.getFiles().size());
			
		}
		else if(selection instanceof TextSelection){
			TextSelection text = (TextSelection) selection;
			
		}
		else{
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