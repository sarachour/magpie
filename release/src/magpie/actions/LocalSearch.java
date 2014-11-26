package magpie.actions;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.TimeUnit;

import magpie.analyze.CodeFeatureBuilder;
import magpie.eclipse.MagpieFileHandler;
import magpie.eclipse.ProjectCodeExtractor;
import magpie.eclipse.ProjectDiffSummary;
import magpie.util.Code;
import magpie.util.Logger;
import magpie.util.Pair;
import magpie.web.WebCrawler;

import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.core.internal.runtime.Log;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jdt.internal.core.*;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;

/**
 * Our sample action implements workbench action delegate. The action proxy will
 * be created by the workbench and shown in the UI. When the user tries to use
 * the action, this delegate will be created and execution will be delegated to
 * it.
 * 
 * @see IWorkbenchWindowActionDelegate
 */
@SuppressWarnings("restriction")
public class LocalSearch implements IWorkbenchWindowActionDelegate {
	private IWorkbenchWindow window;
	ProjectCodeExtractor codeExtractor;
	WebCrawler webCrawler;

	public LocalSearch() {
		codeExtractor = new ProjectCodeExtractor();
		webCrawler = new WebCrawler();
	}

	/**
	 * The constructor.
	 */

	/**
	 * The action has been activated. The argument of the method represents the
	 * 'real' action sitting in the workbench UI.
	 * 
	 * @see IWorkbenchWindowActionDelegate#run
	 */
	List<Pair<String, String>> localFiles = new LinkedList<Pair<String, String>>();
	ProjectDiffSummary diffSummary = new ProjectDiffSummary();

	protected static String readFile(File file) {
		String entireFileText = "";
		try {
			entireFileText = new Scanner(file).useDelimiter("\\A").next();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return entireFileText;
	}

	public void populateLocalFiles(String path) {
		File[] faFiles = new File(path).listFiles();
		for (File file : faFiles) {
			if (file.isDirectory()) {
				populateLocalFiles(file.getAbsolutePath());
			} else {
				if(file.getName().matches("^(.*?)\\.java")){
					String contents = readFile(file);
					Logger.info(file.getAbsolutePath());
					localFiles.add(new Pair<String, String>(file.getName(), contents));
				}
			}
		}
	}

	public Pair<String, String> findMatch(ICompilationUnit code) {
		try {
			String left = code.getSource();
			Pair<String, String> bestFile = null;
			Pair<Double, Double> bestScore = null;
			for (Pair<String, String> lfile : localFiles) {
				String right = lfile.e2;
				DiffNode d = diffSummary.runDifferences(left, right);
				if(d != null){
					Pair<Double, Double> score = diffSummary.getScore(d);
					System.out.println("["+code.getElementName()+"] SCORE: "+lfile.e1+"="+score.e1);
					if (bestScore == null || score.e1 > bestScore.e1) {
						bestScore = score;
						bestFile = new Pair<String,String>(lfile.e1, lfile.e2);
					}
				}
			}
			return bestFile;
		} catch (JavaModelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	public void searchForFiles(List<Code> files) {
		Logger.info("[LOCALSEARCH] Chunk = " + files.size() + " files");
		for (Code file : files) {
			System.out.println("[LOCALSEARCH]" + file.getName());
			Pair<String, String> match = findMatch(file.ast);
			Logger.info("[LOCALSEARCH] Match Search Completed. <"
					+ file.getName() + ">");
			int i = 0;
			if (match != null) {
				Logger.info("[LOCALSEARCH] Writing File. <"
						+ file.getName() + ">");
				Pair<IPath, IPath> paths = MagpieFileHandler
						.trailblazePath(file.ast.getResource());
				String dfile = "LOCAL_"+match.e1;
				String contents = "/* "+dfile +"*/\n"+ match.e2;
				MagpieFileHandler.writeFile(paths.e2, file.getName().replace(".java", "_LOCAL")+".java", contents);
				Logger.info("[LOCALSEARCH] File Write Completed. <"
						+ file.getName() + ">");
			}
			Logger.info("[LOCALSEARCH] Search Completed. <"
					+ file.getName() + ">");
		}
	}
	
	public void run(IAction action) {
		DirectoryDialog dialog = new DirectoryDialog(window.getShell(),
				SWT.OPEN);
		String path = dialog.open();
		localFiles.clear();
		populateLocalFiles(path);
		List<Code> files = new LinkedList<Code>(codeExtractor.getFiles());
		searchForFiles(files);
		
		// testCompare();
	}

	/**
	 * Selection in the workbench has been changed. We can change the state of
	 * the 'real' action here if we want, but this can only happen after the
	 * delegate has been created.
	 * 
	 * @see IWorkbenchWindowActionDelegate#selectionChanged
	 */

	public void selectionChanged(IAction action, ISelection selection) {

		if (selection instanceof ITreeSelection) {
			ITreeSelection t = (ITreeSelection) selection;
			codeExtractor.clearFiles();
			for (TreePath sel : t.getPaths()) {
				Object elem = sel.getLastSegment();
				codeExtractor.extractFrom(elem);

			}
			System.out.println("Number Extracted: "
					+ codeExtractor.getFiles().size());

		} else if (selection instanceof TextSelection) {
			TextSelection text = (TextSelection) selection;

		} else {
		}
	}

	/**
	 * We can use this method to dispose of any system resources we previously
	 * allocated.
	 * 
	 * @see IWorkbenchWindowActionDelegate#dispose
	 */
	public void dispose() {
	}

	/**
	 * We will cache window object in order to be able to provide parent shell
	 * for the message dialog.
	 * 
	 * @see IWorkbenchWindowActionDelegate#init
	 */
	public void init(IWorkbenchWindow window) {
		this.window = window;
	}
}