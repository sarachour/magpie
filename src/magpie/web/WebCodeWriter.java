package magpie.web;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import magpie.eclipse.MagpieFileHandler;
import magpie.util.Pair;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;

public class WebCodeWriter {

		
		public void write(ICompilationUnit reference, String filename, String url, String code){
			String preamble = "/* ====================== FROM ====================\n";
			preamble += "		"+url;
			preamble += "============================================*/ \n";
			
			Pair<IPackageFragment, IPackageFragment> pfs = MagpieFileHandler.trailblazeCompilationUnit(reference);
			MagpieFileHandler.writeJavaFile(pfs.e2, filename, preamble+code);
			System.out.println("WRITE: "+pfs.e2.getPath().toString()+":"+filename);
			/*
			String baseurl = System.getProperty("user.dir");
			System.out.println("BASE: "+baseurl);
			File theDir = new File(baseurl+"/"+"web/"+prefix);
			
			// if the directory does not exist, create it
			if (!theDir.exists()) {
				boolean result = theDir.mkdirs();  
			}
			  
			String filepath = "web/"+prefix+"/"+filename;
			File theFile = new File(baseurl +"/"+filepath);
			if(theFile.exists()){
				theFile.delete();
			}
			
			PrintWriter writer;
			try {
				writer = new PrintWriter(filepath, "UTF-8");
				
				writer.println(preamble);
				writer.println(code);
				writer.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			*/
			
		}
}
