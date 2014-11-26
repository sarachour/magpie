package magpie.util;

import java.awt.Color;
import java.io.IOException;
import java.net.URL;

import krasa.formatter.eclipse.JavaCodeFormatterFacade;
import krasa.formatter.settings.Settings;
import krasa.formatter.settings.provider.JavaPropertiesProvider;
import magpie.analyze.CodeFeatureBuilder;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.internal.core.CompilationUnit;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.ui.texteditor.AnnotationPreference;
import org.osgi.framework.Bundle;

public class Code {
	public CompilationUnit ast;
	

	public Code(CompilationUnit CompUnit){
		this.ast = CompUnit;
	}
	public String getName(){
		return this.ast.getElementName();
	}
	public void mark(String source, CodeFragment cf){
		try {
			IResource res = ast.getResource();
			for(int line=cf.startLoc.linePos; line <= cf.endLoc.linePos; line++){
				IMarker marker;
				marker = res.createMarker("magpie.source.marker");
				marker.setAttribute("SOURCE", source);
				marker.setAttribute("COLOR", "100,100,100");
				marker.setAttribute(IMarker.LINE_NUMBER, line);
				
				if(line == cf.startLoc.linePos)
					marker.setAttribute(IMarker.CHAR_START, cf.startLoc.charPos);
				if(line == cf.endLoc.linePos)
					marker.setAttribute(IMarker.CHAR_END, cf.endLoc.charPos);
			}
			
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	/*
	public static void main(String[] args){
		String snippet ="package org.xmlrpc.android;\n\n"+
				"import java.util.ArrayList;\n"+
				"public class MethodCall {\n"+
				"private static final int TOPIC = 1;\n"+
				"String methodName;\n"+
				//"ArrayList<Object> params = new ArrayList<Object>();\n\n"+
				"public String getMethodName() { return methodName; }\n"+
				"void setMethodName(String methodName) { this.methodName = methodName; }\n\n"+
				//"public ArrayList<Object> getParams() { return params; }\n"+
				//"void setParams(ArrayList<Object> params) { this.params = params; }\n\n"+
				"public String getTopic() {\n"+
				"	return (String)params.get(TOPIC);\n"+
				"}\n"+
				"}\n";
		Settings settings = new Settings();
        settings.setPathToConfigFileJava("util/EclipsePrettyPrintConfig.xml");
        settings.setSelectedJavaProfile("Eclipse - Pretty");
		JavaCodeFormatterFacade f = new JavaCodeFormatterFacade(settings.getJavaProperties());
		System.out.println(snippet);
		snippet=f.format(snippet, 0, snippet.length());
		System.out.println(snippet);
		
	}
	*/
	
}
