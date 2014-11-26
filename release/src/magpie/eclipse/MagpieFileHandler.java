package magpie.eclipse;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

import magpie.util.Pair;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

public class MagpieFileHandler {
	public static String MAGPIE_SOURCE_FOLDER = "MAGPIE-SRC";
	 
	private static String readFile(IResource r){
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

	public static List<IFile> getFiles(IPath frag){
		List<IFile> ll = new LinkedList<IFile>();
		IWorkspace workspace = ResourcesPlugin.getWorkspace(); 
		IFolder folder = workspace.getRoot().getFolder(frag);
		if(!folder.exists()) return ll;
		try {
			for(IResource r : folder.members()){
				if(r instanceof IFile){
					IFile f = (IFile) r;
					ll.add(f);
				}
			}
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return ll;
	}
	
	public static void prepare(IFolder folder) {
	    if (!folder.exists()) {
	    	if(folder.getParent() instanceof IFolder)
	    		prepare((IFolder) folder.getParent());
	        try {
				folder.create(true, true, null);
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    }
	}
	
	public static Pair<IPath, IPath> trailblazePath(IResource f){
		IWorkspace workspace = ResourcesPlugin.getWorkspace(); 
		//remove source directory
		IPath rpath = f.getFullPath().removeLastSegments(1);
		IPath wpath = rpath.uptoSegment(1)
			.append(new Path(MAGPIE_SOURCE_FOLDER))
			.append(rpath.removeFirstSegments(2))
			.append(new Path("MAGPIE-WEB"));
		System.out.println("[IO] RPATH:"+rpath.makeRelative().toString());
		System.out.println("[IO] WPATH:"+wpath.makeRelative().toString());
		IFolder wfolder = workspace.getRoot().getFolder(wpath.makeRelative());
		prepare(wfolder);
		return new Pair<IPath,IPath>(rpath, wpath);
	}

	
	public static void writeFile(IPath e2, String filename, String source) {
		// TODO Auto-generated method stub
		IWorkspace workspace = ResourcesPlugin.getWorkspace(); 
		IFolder folder = workspace.getRoot().getFolder(e2);
		IFile file = folder.getFile(filename);
		InputStream is = new ByteArrayInputStream(source.getBytes());
		try {
			if(file.exists()) file.delete(true, null);
			file.create(is, true, null);
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
