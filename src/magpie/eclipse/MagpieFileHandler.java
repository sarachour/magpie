package magpie.eclipse;

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
	public static void deactivateCompilation(ICompilationUnit cuW){
		IProject proj = cuW.getResource().getProject();
		IJavaProject jproj = cuW.getJavaProject();
		List<IClasspathEntry> buildPaths = new LinkedList<IClasspathEntry>();
		try {
			for(IClasspathEntry en: jproj.getRawClasspath()){
				String cep = en.getPath().toString();
				System.out.println(cep);
				if(!cep.contains(MAGPIE_SOURCE_FOLDER)){
					buildPaths.add(en);
				}
			}
			jproj.setRawClasspath(buildPaths.toArray(new IClasspathEntry[buildPaths.size()]), true, null);
		} catch (JavaModelException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	public static void activateCompilation(ICompilationUnit cuW){
		IProject proj = cuW.getResource().getProject();
		IJavaProject jproj = cuW.getJavaProject();
		List<IClasspathEntry> buildPaths = new LinkedList<IClasspathEntry>();
		buildPaths.add(JavaCore.newSourceEntry(proj.getFullPath().append(MAGPIE_SOURCE_FOLDER)));
		
		try {
			for(IClasspathEntry en: jproj.getRawClasspath())
				buildPaths.add(en);
			
			jproj.setRawClasspath(buildPaths.toArray(new IClasspathEntry[buildPaths.size()]), true, null);
		} catch (JavaModelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public static ICompilationUnit writeJavaFile(IPackageFragment pf, ICompilationUnit cuW){
		String sW = readFile(cuW.getResource());
		return writeJavaFile(pf, cuW.getElementName(), sW);
	}
	public static ICompilationUnit writeJavaFile(IPackageFragment pf, String filename, String content){
		try {
			return pf.createCompilationUnit(filename, content, true, null);
		} catch (JavaModelException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}
		return null;
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
	
	public static Pair<IPath, IPath> trailblazePath(IResource f){
		IWorkspace workspace = ResourcesPlugin.getWorkspace(); 
		//remove source directory
		IPath rpath = f.getFullPath().removeLastSegments(1);
		IPath wpath = rpath.uptoSegment(1)
			.append(new Path(MAGPIE_SOURCE_FOLDER))
			.append(rpath.removeFirstSegments(2))
			.append(new Path("WEB"));
		System.out.println("[IO] RPATH:"+rpath.toString());
		System.out.println("[IO] WPATH:"+wpath.toString());
		IFolder wfolder = workspace.getRoot().getFolder(wpath);
		if(!wfolder.exists()){
			try {
				wfolder.create(true, true, null);
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return new Pair<IPath,IPath>(rpath, wpath);
	}
	public static List<ICompilationUnit> getJavaFiles(IPackageFragment pf){
		List<ICompilationUnit> cus =  new LinkedList<ICompilationUnit>();
		try {
			ICompilationUnit[] icus;
			icus = pf.getCompilationUnits();
			for(ICompilationUnit cu: icus){
				cus.add(cu);
			}
		} catch (JavaModelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return cus;
	}
	
	public static Pair<IPackageFragment,IPackageFragment> trailblazeCompilationUnit(ICompilationUnit cuW){
		IProject proj = cuW.getResource().getProject();
		IJavaProject jproj = cuW.getJavaProject();
		
		String packagePath=null;
		String webPackagePath=null;
		IFolder folder = proj.getFolder(MAGPIE_SOURCE_FOLDER);

		packagePath =cuW.getParent().getElementName();
		webPackagePath = packagePath + ".WEB";
		System.out.println("IO: File Path "+packagePath);
		System.out.println("IO: WEB Path "+webPackagePath);

		if(!folder.exists()){
			try {
				folder.create(true, true, null);
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if(!jproj.getPackageFragmentRoot(folder).exists()){
			List<IClasspathEntry> buildPaths = new LinkedList<IClasspathEntry>();
			buildPaths.add(JavaCore.newSourceEntry(proj.getFullPath().append(MAGPIE_SOURCE_FOLDER)));
			
			try {
				for(IClasspathEntry en: jproj.getRawClasspath())
					buildPaths.add(en);
				
				//jproj.setRawClasspath(buildPaths.toArray(new IClasspathEntry[buildPaths.size()]), true, null);
			} catch (JavaModelException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		IPackageFragmentRoot proot = jproj.getPackageFragmentRoot(folder);
		if(!proot.getPackageFragment(packagePath).exists()){
			try {
				proot.createPackageFragment(packagePath, true, null);
			} catch (JavaModelException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		IPackageFragment pf = proot.getPackageFragment(packagePath);
		if(!proot.getPackageFragment(webPackagePath).exists()){
			try {
				proot.createPackageFragment(webPackagePath, true, null);
			} catch (JavaModelException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		IPackageFragment wbf = proot.getPackageFragment(webPackagePath);

		return new Pair<IPackageFragment, IPackageFragment>(pf, wbf);
	}
}
