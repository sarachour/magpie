package magpie.views;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import magpie.analyze.CodeFeatureBuilder;
import magpie.diff.JavaNode;
import magpie.eclipse.ProjectDiffSummary;
import magpie.eclipse.ProjectDiffSummary.Element;
import magpie.util.Logger;
import magpie.util.Pair;
import krasa.formatter.eclipse.JavaCodeFormatterFacade;
import krasa.formatter.settings.Settings;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.*;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.internal.ui.compare.JavaStructureCreator;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.*;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.SWT;
import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.CompareUI;
import org.eclipse.compare.internal.CompareEditor;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.compare.structuremergeviewer.DocumentRangeNode;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;


/**
 * This sample class demonstrates how to plug-in a new
 * workbench view. The view shows data obtained from the
 * model. The sample creates a dummy model on the fly,
 * but a real implementation would connect to the model
 * available either in this or another plug-in (e.g. the workspace).
 * The view is connected to the model using a content provider.
 * <p>
 * The view uses a label provider to define how model
 * objects should be presented in the view. Each
 * view can present the same model objects using
 * different labels and icons, if needed. Alternatively,
 * a single label provider can be shared between views
 * in order to ensure that objects of the same type are
 * presented in the same way everywhere.
 * <p>
 */

public class DiffView extends ViewPart {

	/**
	 * The ID of the view as specified by the extension.
	 */
	public static final String ID = "magpie.views.DiffView";

	public TreeViewer viewer;
	private DrillDownAdapter drillDownAdapter;
	private Action action1;
	private Action action2;
	private Action doubleClickAction;

	/*
	 * The content provider class is responsible for
	 * providing objects to the view. It can wrap
	 * existing objects in adapters or simply return
	 * objects as-is. These objects may be sensitive
	 * to the current input of the view, or ignore
	 * it and always show the same content 
	 * (like Task List, for example).
	 */
	 
	abstract class TreeObject implements IAdaptable {
		private String name;
		private TreeParent parent;
		public String path;
		public TreeObject(String name, String path) {
			this.name = name;
			this.path = path;
		}
		public String getName() {
			return name;
		}
		public void setParent(TreeParent parent) {
			this.parent = parent;
		}
		public TreeParent getParent() {
			return parent;
		}
		public String toString() {
			return getName();
		}
		public Object getAdapter(Class key) {
			return null;
		}
		public abstract double getScore();
	}
	class TreeLeaf extends TreeObject{
		public double left;
		public double right;
		public TreeLeaf(String name, String path, double left, double right) {
			super(name, path);
			this.left = left;
			this.right = right;
			// TODO Auto-generated constructor stub
		}
		public double getScore(){
			return this.left;
		}
		
	}
	class TreeFile extends TreeParent{
		double score;
		String source;
		public TreeFile(String name, String path, double score, String source) {
			super(name, path);
			this.score = score;
			this.source = source;
		}
		public double getScore() {
			return score;
		}
	}
	
	class TreeParent extends TreeObject {
		private List<TreeObject> children;
		public TreeParent(String name, String path) {
			super(name, path);
			children = new LinkedList<TreeObject>();
		}
		
		public TreeObject getChild(String name){
			for(TreeObject child: getChildren()){
				if(child.getName().equals(name)){
					return child;
				}
			}
			return null;
		}
		public boolean hasChild(String name){
			for(TreeObject child: getChildren()){
				if(child.getName().equals(name)){
					return true;
				}
			}
			return false;
		}
		public void addChild(TreeObject child) {
			children.add(child);
			child.setParent(this);
		}
		public void removeChild(TreeObject child) {
			children.remove(child);
			child.setParent(null);
		}
		public TreeObject [] getChildren() {
			return (TreeObject [])children.toArray(new TreeObject[children.size()]);
		}
		public boolean hasChildren() {
			return children.size()>0;
		}
		@Override
		public double getScore() {
			int nchildren = getChildren().length;
			double cscore=0;
			// TODO Auto-generated method stub
			for(TreeObject child : getChildren()){
				cscore += child.getScore();
			}
			return cscore/nchildren;
		}
	}

	class ViewContentProvider implements IStructuredContentProvider, 
										   ITreeContentProvider {
		public TreeParent invisibleRoot;

		public void inputChanged(Viewer v, Object oldInput, Object newInput) {
			initialize();
		}
		public void dispose() {
		}
		public Object[] getElements(Object parent) {
			if (parent.equals(getViewSite())) {
				initialize();
				return getChildren(invisibleRoot);
			}
			return getChildren(parent);
		}
		public Object getParent(Object child) {
			if (child instanceof TreeObject) {
				return ((TreeObject)child).getParent();
			}
			return null;
		}
		public Object [] getChildren(Object parent) {
			if (parent instanceof TreeParent) {
				return ((TreeParent)parent).getChildren();
			}
			return new Object[0];
		}
		public boolean hasChildren(Object parent) {
			if (parent instanceof TreeParent)
				return ((TreeParent)parent).hasChildren();
			return false;
		}
		/*
		 * We will set up a dummy model to initialize tree heararchy.
		 * In a real code, you will connect to a real model and
		 * expose its hierarchy.
		 */
		private TreeParent constructTree(){
			Map<String, Pair<String, List<Element>>> map = ProjectDiffSummary.compares;
			TreeParent root = new TreeParent("root", "");
			
			for(String key : map.keySet()){
				List<Element> elems = map.get(key).e2;
				String path = map.get(key).e1;
				String[] segments = path.split("/");
				TreeParent curr = root;
				//add path
				int depth=0;
				String currpath = "";
				for(String segment : segments){
					TreeParent next;
					currpath += "/"+segment;
					if(depth > 0 && depth < segments.length - 1){
						if(curr.hasChild(segment)){
							next = (TreeParent) curr.getChild(segment);
						}
						else{
							next = new TreeParent(segment, currpath);
							curr.addChild(next);
						}
						curr = next;
					}
					depth++;
					
				}
				double score;
				String source;
				if(elems.size() > 0){
					score = elems.get(0).left;
					source = elems.get(0).name;
				}
				else{
					score = 0;
					source = "No Matches";
				}
				TreeParent lroot = new TreeFile(key, path, score, source);
				curr.addChild(lroot);
				for(int i=0; i < elems.size(); i++){
					Element e = elems.get(i);
					String ctitle = e.name + " ["+e.left + "|"+e.right+"]";
					TreeLeaf lchild = new TreeLeaf(e.name, e.path, e.left, e.right);
					lroot.addChild(lchild);
				}
			}
			return root;
		}
		private void initialize() {
			Logger.info("[DIFFVIEW] Initializing..");
			TreeParent root = constructTree();
			invisibleRoot = new TreeParent("", "");
			invisibleRoot.addChild(root);
		}
	}
	class ViewLabelProvider extends LabelProvider implements IColorProvider, IFontProvider {
		DecimalFormat df = new DecimalFormat("##");
		public String getText(Object obj) {
			if(obj instanceof TreeFile){
				TreeFile tf = (TreeFile) obj;
				String text = tf.getName() + "\t["+df.format(Math.floor(tf.getScore()*100))+"]" + " <"+tf.source+">";
				return text;
			}
			else if(obj instanceof TreeLeaf){
				TreeLeaf tf = (TreeLeaf) obj;
				String text = tf.getName() + "\t["+df.format(Math.floor(tf.getScore()*100))+"]";
				return text;
			}
			else return obj.toString();
		}
		public Image getImage(Object obj) {
			String imageKey = ISharedImages.IMG_OBJ_ELEMENT;
			if (obj instanceof TreeFile)
			   imageKey = ISharedImages.IMG_OBJ_FILE;
			else if(obj instanceof TreeLeaf)
				imageKey = ISharedImages.IMG_OBJ_ELEMENT;
			else if(obj instanceof TreeParent)
			   imageKey = ISharedImages.IMG_OBJ_FOLDER;
			return PlatformUI.getWorkbench().getSharedImages().getImage(imageKey);
		}
		@Override
		public Color getForeground(Object obj) {
			// TODO Auto-generated method stub
			if(obj instanceof TreeObject){
				double score = ((TreeObject) obj).getScore();
				int r =(int) (160 - 100*(score)); // turn less red
				int g =(int) (60 + 100*score);
				int b = 60;
				return new Color(null, r, g, b);
			}
			else
				return new Color(null, 0,0,0);
		}
		@Override
		public Color getBackground(Object obj) {
			// TODO Auto-generated method stub
			return new Color(null, 255,255,255);
		}
		@Override
		public Font getFont(Object obj) {
			FontData fd = new FontData("ariel", 10, SWT.NORMAL);
			
			// TODO Auto-generated method stub
			if(obj instanceof TreeObject){
				double score = ((TreeObject) obj).getScore();
				if(score > 0.999999){
					fd = new FontData("ariel", 10, SWT.BOLD);
				}
			}
			return new Font(null, fd);
		}
	}
	class NameSorter extends ViewerSorter {
		public int category(Object element) {
		    if(element instanceof TreeFile) return 1;
		    else if(element instanceof TreeParent) return 0;
		    if(element instanceof TreeLeaf) return 2;
		    return 3;
		}
	}

	/**
	 * The constructor.
	 */
	public DiffView() {
	}

	/**
	 * This is a callback that will allow us
	 * to create the viewer and initialize it.
	 */
	public void createPartControl(Composite parent) {
		viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		drillDownAdapter = new DrillDownAdapter(viewer);
		viewer.setContentProvider(new ViewContentProvider());
		viewer.setLabelProvider(new ViewLabelProvider());
		viewer.setSorter(new NameSorter());
		viewer.setInput(getViewSite());

		// Create the help context id for the viewer's control
		PlatformUI.getWorkbench().getHelpSystem().setHelp(viewer.getControl(), "Magpie.viewer");
		makeActions();
		hookContextMenu();
		hookDoubleClickAction();
		contributeToActionBars();
	}

	private void hookContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				DiffView.this.fillContextMenu(manager);
			}
		});
		Menu menu = menuMgr.createContextMenu(viewer.getControl());
		viewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, viewer);
	}

	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		fillLocalPullDown(bars.getMenuManager());
		fillLocalToolBar(bars.getToolBarManager());
	}

	private void fillLocalPullDown(IMenuManager manager) {
		manager.add(action1);
		manager.add(new Separator());
		manager.add(action2);
	}

	private void fillContextMenu(IMenuManager manager) {
		manager.add(action1);
		manager.add(action2);
		manager.add(new Separator());
		drillDownAdapter.addNavigationActions(manager);
		// Other plug-ins can contribute there actions here
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}
	
	private void fillLocalToolBar(IToolBarManager manager) {
		manager.add(action1);
		manager.add(action2);
		manager.add(new Separator());
		drillDownAdapter.addNavigationActions(manager);
	}

	private void makeActions() {
		action1 = new Action() {
			public void run() {
				showMessage("Action 1 executed");
			}
		};
		action1.setText("Action 1");
		action1.setToolTipText("Action 1 tooltip");
		action1.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
			getImageDescriptor(ISharedImages.IMG_OBJS_INFO_TSK));
		
		action2 = new Action() {
			public void run() {
				showMessage("Action 2 executed");
			}
		};
		action2.setText("Inspect Source");
		action2.setToolTipText("Inspect Source");
		action2.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
				getImageDescriptor(ISharedImages.IMG_OBJS_INFO_TSK));
		doubleClickAction = new Action() {
			public void run() {
				ISelection selection = viewer.getSelection();
				Object obj = ((IStructuredSelection)selection).getFirstElement();
				if(obj instanceof TreeLeaf){
					TreeLeaf webfile = (TreeLeaf) obj;
					TreeFile file = (TreeFile) webfile.getParent();
					if(webfile.getScore() == 1.0){
						compare(file.path, webfile.path, false);
					}
					else{
						compare(file.path, webfile.path, true);
					}
				}
				else if(obj instanceof TreeFile){
					TreeFile file = (TreeFile) obj;
					TreeObject webfile = file.getChild(file.source);
					if(webfile.getScore() == 1.0){
						compare(file.path, webfile.path, false);
					}
					else{
						compare(file.path, webfile.path, true);
					}
				}
			}
		};
	}
	static CompareConfiguration CurrentConfig = new CompareConfiguration();
	static EditorInput CurrentInput = null;
	public static IEditorPart editor;
	static class EditorInput extends CompareEditorInput{
		DocumentRangeNode left;
		DocumentRangeNode right;
		JavaStructureCreator creator = new JavaStructureCreator();
		public EditorInput(CompareConfiguration configuration) {
			super(configuration);
			// TODO Auto-generated constructor stub
		}
		public void setItems(String left, String right){
			
			DocumentRangeNode c1 = null;
			DocumentRangeNode c2 = null;
				try {
					c1 = (DocumentRangeNode) creator.createStructure(new JavaNode(new Document(left)),null);
					c2 = (DocumentRangeNode) creator.createStructure(new JavaNode(new Document(right)),null);
					
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
			DiffNode diff = (DiffNode) d.findDifferences(false, null, null, null, left, right);
			creator.rewriteTree(d, diff);
			return diff;
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
	protected String readFile(IFile f){
		try {
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
	JavaCodeFormatterFacade javaFormatter=null;
	private void loadFormatter(){
		if(javaFormatter == null){
			
			try {
				Bundle bundle = Platform.getBundle("Magpie");
				Logger.info("[DIFFVIEW] Bundle:" + bundle.getSymbolicName());
				URL url = bundle.getEntry("util/EclipsePrettyPrintConfig.xml");
				Logger.info("[DIFFVIEW] URL:" + url.getPath());
				URL fileURL = FileLocator.toFileURL(url);
				Logger.info("[DIFFVIEW] File URL:" + fileURL.getPath());
				Settings settings = new Settings();
		        settings.setPathToConfigFileJava(fileURL.getPath());
		        settings.setSelectedJavaProfile("Eclipse - Pretty");
				javaFormatter = new JavaCodeFormatterFacade(settings.getJavaProperties());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
	}
	private void compare(String path1, String path2, boolean useFormatter){

		IWorkspace workspace = ResourcesPlugin.getWorkspace(); 
		Logger.info("[DIFFVIEW] path1:" + path1);
		Logger.info("[DIFFVIEW] path2:" + path2);
		IFile f1 = workspace.getRoot().getFile(new Path(path1));
		IFile f2 = workspace.getRoot().getFile(new Path(path2));
		String s1 = CodeFeatureBuilder.removeComments(readFile(f1));
		String s2 = CodeFeatureBuilder.removeComments(readFile(f2));

		loadFormatter();
		if(useFormatter){
			s1=javaFormatter.format(s1, 0, s1.length());
			s2=javaFormatter.format(s2, 0, s2.length());
		}
		
		if(CurrentInput == null){
			CurrentConfig.setProperty(CurrentConfig.IGNORE_WHITESPACE, true);
			CurrentInput = new EditorInput(CurrentConfig);
		}
		CurrentInput.setItems(s1,s2);
		Class[] cls = {EditorInput.class};
		
		IWorkbenchSite site = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePart().getSite();
		editor = findReusableCompareEditor(CurrentInput, site.getPage(), cls);
		CompareUI.reuseCompareEditor(CurrentInput, (IReusableEditor) editor);
		editor = findReusableCompareEditor(CurrentInput, site.getPage(), cls);
		CurrentInput.getNavigator().selectChange(true);
	}

	private void hookDoubleClickAction() {
		viewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				doubleClickAction.run();
			}
		});
	}
	private void showMessage(String message) {
		MessageDialog.openInformation(
			viewer.getControl().getShell(),
			"the diff viewer",
			message);
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus() {
		viewer.getControl().setFocus();
	}
	
}