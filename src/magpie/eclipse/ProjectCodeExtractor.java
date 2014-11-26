package magpie.eclipse;

import java.util.LinkedList;
import java.util.List;

import magpie.util.Code;
import magpie.util.CodeFragment;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.CompilationUnit;
import org.eclipse.jdt.internal.core.JavaProject;
import org.eclipse.jdt.internal.core.PackageFragment;

@SuppressWarnings("restriction")
public class ProjectCodeExtractor {
	private List<Code> files;
	public ProjectCodeExtractor() {
		files = new LinkedList<Code>();
	}
	private List<Code> getChildren(IPackageFragment pf){
		LinkedList<Code> ll = new LinkedList<Code>();
		try {
			IPackageFragmentRoot pfr;
			IJavaProject jp = pf.getJavaProject();
			IPackageFragment[] frags = jp.getPackageFragments();
			for(IPackageFragment frag : frags){
				if(frag.getElementName().startsWith(pf.getElementName())){
					for(IJavaElement elem : frag.getChildren()){
						if(elem instanceof CompilationUnit){
							ll.push(new Code((CompilationUnit) elem));
						}
						else if(elem instanceof IPackageFragment){
							List<Code> cu = getChildren((IPackageFragment) elem);
							ll.addAll(cu);
						}
					}
				}
			}
		} catch (JavaModelException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return ll;
	}
	public List<Code> getFiles(){
		return files;
	}
	public void clearFiles(){
		files.clear();
	}
	public void extractFrom(Object o){
		if(o instanceof CompilationUnit) extractFromCompilationUnit((CompilationUnit) o);
		else if(o instanceof JavaProject) extractFromProject((JavaProject) o);
		else if(o instanceof PackageFragment) extractFromPackage((PackageFragment) o);
		//files.get(0).mark("test123", new CodeFragment(0,0,0,10));
	}
	public void extractFromCompilationUnit(CompilationUnit compUnit){
		files.add(new Code(compUnit));
		System.out.println("File:"+compUnit.getElementName());
		for(Code cu : files){
			System.out.println("		Compilation Unit: "+ cu.getName());
		}

		
	}
	public void extractFromProject(JavaProject jp){
		try {
			IPackageFragmentRoot[] rfrags = jp.getPackageFragmentRoots();
			for(IPackageFragmentRoot rfrag: rfrags){
				//System.out.println(rfrag.getElementName() + ":"+rfrag.getChildren().length);
				if(rfrag.getElementName().equals("src")){
					for(IJavaElement je : rfrag.getChildren()){
						if(je instanceof IPackageFragment){
							IPackageFragment pf = (IPackageFragment) je;
							//System.out.println("	"+pf.getElementName() + ":"+pf.getChildren().length);
							for(IJavaElement je2: pf.getChildren()){
								if(je2 instanceof ICompilationUnit){
									CompilationUnit cu = (CompilationUnit) je2;
									//System.out.println("		"+cu.getElementName() + ":"+cu.getChildren().length);
									files.add(new Code(cu));
								}
							}
						}
					}
				}
			}
			for(Code cu : files){
				System.out.println("		Compilation Unit: "+ cu.getName());
			}
		} catch (JavaModelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public void extractFromPackage(PackageFragment pf){
		files = getChildren(pf);
		System.out.println("	Package Fragment "+pf.getElementName());
		for(Code cu : files){
			System.out.println("		Compilation Unit: "+ cu.getName());
		}
		
	}
}
