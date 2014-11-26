package magpie.diff;
import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.resource.ImageDescriptor;

import org.eclipse.jface.text.IDocument;

import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.DocumentRangeNode;
import org.eclipse.core.runtime.Assert;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.internal.ui.compare.CompareMessages;

/**
 * Comparable Java elements are represented as JavaNodes.
 * Extends the DocumentRangeNode with method signature information.
 */
public class JavaNode extends DocumentRangeNode implements ITypedElement {

	public static final int CU= 0;
	public static final int PACKAGE= 1;
	public static final int IMPORT_CONTAINER= 2;
	public static final int IMPORT= 3;
	public static final int INTERFACE= 4;
	public static final int CLASS= 5;
	public static final int ENUM= 6;
	public static final int ANNOTATION= 7;
	public static final int FIELD= 8;
	public static final int INIT= 9;
	public static final int CONSTRUCTOR= 10;
	public static final int METHOD= 11;

	private int fInitializerCount= 1;

	private static class JavaCompareUtils{
		private static final char PACKAGEDECLARATION= '%';
		private static final char IMPORTDECLARATION= '#';
		private static final char IMPORT_CONTAINER= '<';
		private static final char FIELD= '^';
		private static final char METHOD= '~';
		private static final char INITIALIZER= '|';
		private static final char COMPILATIONUNIT= '{';
		private static final char TYPE= '[';
		static String buildID(int type, String name) {
			StringBuffer sb= new StringBuffer();
			switch (type) {
			case JavaNode.CU:
				sb.append(COMPILATIONUNIT);
				break;
			case JavaNode.CLASS:
			case JavaNode.INTERFACE:
			case JavaNode.ENUM:
			case JavaNode.ANNOTATION:
				sb.append(TYPE);
				sb.append(name);
				break;
			case JavaNode.FIELD:
				sb.append(FIELD);
				sb.append(name);
				break;
			case JavaNode.CONSTRUCTOR:
			case JavaNode.METHOD:
				sb.append(METHOD);
				sb.append(name);
				break;
			case JavaNode.INIT:
				sb.append(INITIALIZER);
				sb.append(name);
				break;
			case JavaNode.PACKAGE:
				sb.append(PACKAGEDECLARATION);
				break;
			case JavaNode.IMPORT:
				sb.append(IMPORTDECLARATION);
				sb.append(name);
				break;
			case JavaNode.IMPORT_CONTAINER:
				sb.append(IMPORT_CONTAINER);
				break;
			default:
				Assert.isTrue(false);
				break;
			}
			return sb.toString();
		}
	}
	/**
	 * Creates a JavaNode under the given parent.
	 * @param parent the parent node
	 * @param type the Java elements type. Legal values are from the range CU to METHOD of this class.
	 * @param name the name of the Java element
	 * @param start the starting position of the java element in the underlying document
	 * @param length the number of characters of the java element in the underlying document
	 */
	public JavaNode(JavaNode parent, int type, String name, int start, int length) {
		super(parent, type, JavaCompareUtils.buildID(type, name), parent.getDocument(), start, length);
		parent.addChild(this);
	}

	/**
	 * Creates a JavaNode for a CU. It represents the root of a
	 * JavaNode tree, so its parent is null.
	 * @param document the document which contains the Java element
	 */
	public JavaNode(IDocument document) {
		super(CU, JavaCompareUtils.buildID(CU, "root"), document, 0, document.getLength()); //$NON-NLS-1$
	}

	public String getInitializerCount() {
		return Integer.toString(fInitializerCount++);
	}

	/**
	 * Extracts the method name from the signature.
	 * Used for smart matching.
	 */
	public String extractMethodName() {
		String id= getId();
		int pos= id.indexOf('(');
		if (pos > 0)
			return id.substring(1, pos);
		return id.substring(1);
	}

	/**
	 * Extracts the method's arguments name the signature.
	 * Used for smart matching.
	 */
	public String extractArgumentList() {
		String id= getId();
		int pos= id.indexOf('(');
		if (pos >= 0)
			return id.substring(pos+1);
		return id.substring(1);
	}

	/**
	 * Returns a name which is presented in the UI.
	 * @see ITypedElement#getName()
	 */
	public String getName() {

		switch (getTypeCode()) {
		case INIT:
			return CompareMessages.JavaNode_initializer;
		case IMPORT_CONTAINER:
			return CompareMessages.JavaNode_importDeclarations;
		case CU:
			return CompareMessages.JavaNode_compilationUnit;
		case PACKAGE:
			return CompareMessages.JavaNode_packageDeclaration;
		}
		return getId().substring(1);	// we strip away the type character
	}

	/*
	 * @see ITypedElement#getType()
	 */
	public String getType() {
		return "java2"; //$NON-NLS-1$
	}

	/**
	 * Returns a shared image for this Java element.
	 *
	 * see ITypedInput.getImage
	 */
	public Image getImage() {
		/*
		ImageDescriptor id= null;

		switch (getTypeCode()) {
		case CU:
			id= JavaCompareUtilities.getImageDescriptor(IJavaElement.COMPILATION_UNIT);
			break;
		case PACKAGE:
			id= JavaCompareUtilities.getImageDescriptor(IJavaElement.PACKAGE_DECLARATION);
			break;
		case IMPORT:
			id= JavaCompareUtilities.getImageDescriptor(IJavaElement.IMPORT_DECLARATION);
			break;
		case IMPORT_CONTAINER:
			id= JavaCompareUtilities.getImageDescriptor(IJavaElement.IMPORT_CONTAINER);
			break;
		case CLASS:
			id= JavaCompareUtilities.getTypeImageDescriptor(true);
			break;
		case INTERFACE:
			id= JavaCompareUtilities.getTypeImageDescriptor(false);
			break;
		case INIT:
			id= JavaCompareUtilities.getImageDescriptor(IJavaElement.INITIALIZER);
			break;
		case CONSTRUCTOR:
		case METHOD:
			id= JavaCompareUtilities.getImageDescriptor(IJavaElement.METHOD);
			break;
		case FIELD:
			id= JavaCompareUtilities.getImageDescriptor(IJavaElement.FIELD);
			break;
		case ENUM:
			id= JavaCompareUtilities.getEnumImageDescriptor();
			break;
		case ANNOTATION:
			id= JavaCompareUtilities.getAnnotationImageDescriptor();
			break;
		}
		return JavaPlugin.getImageDescriptorRegistry().get(id);
		*/
		return null;
	}

	/*
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return getType() + ": " + getName() //$NON-NLS-1$
				+ "[" + getRange().offset + "+" + getRange().length + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}
}
