/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.swtshell;

import org.eclipse.swt.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.layout.*;

import org.epics.ioc.v3a.*;

/**
 * Call the V3 iocshell
 *       
 * @author mrk
 *
 */
public class V3iocshell {
    
    /**
     * Called by SwtShell after the default constructor has been called.
     * @param display The display.
     */
    public static void init(Display display) {
        Window window = new Window(display);
        window.start();
    }
    
    private static class Window implements SelectionListener {
        private Display display;
        private Shell shell;
        private Button fileNameButton;
        private Text fileNameText;
        
        private Window(Display display) {
            this.display = display;
        }
        
        private void start() {
            shell = new Shell(display);
            shell.setText("iocshell");
            GridLayout gridLayout = new GridLayout();
            gridLayout.numColumns = 2;
            shell.setLayout(gridLayout);
            
            fileNameButton = new Button(shell,SWT.PUSH);
            fileNameButton.setText("fileName");
            fileNameButton.addSelectionListener(this);
            fileNameText = new Text(shell,SWT.BORDER);
            GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
            gridData.minimumWidth = 500;
            fileNameText.setLayoutData(gridData);
            fileNameText.addSelectionListener(this);
            shell.pack();
            shell.open();
        }
        /* (non-Javadoc)
         * @see org.eclipse.swt.events.SelectionListener#widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent)
         */
        public void widgetDefaultSelected(SelectionEvent e) {
            widgetSelected(e);
        }
        /* (non-Javadoc)
         * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
         */
        public void widgetSelected(SelectionEvent e) {
            String fileName = null;
            if(e.getSource()==fileNameButton) {
                FileDialog fd = new FileDialog(shell, SWT.OPEN);
                fd.setText("Open");
                String[] filterExt = { "*.cmd"};
                fd.setFilterExtensions(filterExt);
                fileName = fd.open();
            }
            if(e.getSource()==fileNameText) {
                fileName = fileNameText.getText();
            }
            new Invoke(fileName);
        }       
    }
    
    private static class Invoke implements Runnable{
        private String fileName;
        private Thread thread;
        private Invoke (String fileName) {
            this.fileName = fileName;
            thread = new Thread(this);
            thread.setPriority(2);
            thread.start();
        }
        
        public void run() {
            V3.iocsh(fileName);
            AsynOctet asynOctet = new AsynOctet(null);
            asynOctet.readIt();
        }
    }
    
}
