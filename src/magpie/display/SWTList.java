package magpie.display;

import magpie.eclipse.ProjectDiffMacro;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;

/**
 * @author Steven Holzner
 * 
 */

public class SWTList {

  public static void create(Shell oshell, final String refName, String selected) {
    Display display = oshell.getDisplay();
    Shell shell = new Shell(display, SWT.MIN);
    java.util.List<String> elements = ProjectDiffMacro.getFileList(refName);
    shell.setText(refName);
    shell.setSize(300, 200);
    shell.setLayout(new FillLayout(SWT.VERTICAL));
    final List list = new List(shell, SWT.BORDER | SWT.V_SCROLL);

    for(String e : elements){
    	list.add(e);
    }

    list.addSelectionListener(new SelectionListener() {
      String ref = refName;
      public void handleSelection(SelectionEvent e){
    	  int[] selections = list.getSelectionIndices();
          String outText = "";
          for (int loopIndex = 0; loopIndex < selections.length; loopIndex++){
          	String selected = list.getItem(selections[loopIndex]);
          	ProjectDiffMacro.select(ref,selected);
          	outText += selected + " ";
          }
          System.out.println("You selected: " + outText);
      }
      public void widgetSelected(SelectionEvent event) {
        handleSelection(event);
      }

      public void widgetDefaultSelected(SelectionEvent event) {
    	handleSelection(event);
      }
    });

    shell.open();
    while (!shell.isDisposed()) {
      if (!display.readAndDispatch())
        display.sleep();
    }
    display.dispose();
  }
}
