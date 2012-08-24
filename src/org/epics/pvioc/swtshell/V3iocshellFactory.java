/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS pvData is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.swtshell;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.epics.pvdata.misc.RunnableReady;
import org.epics.pvdata.misc.ThreadCreate;
import org.epics.pvdata.misc.ThreadCreateFactory;
import org.epics.pvdata.misc.ThreadReady;
import org.epics.pvioc.v3a.AsynOctet;
import org.epics.pvioc.v3a.V3;

/**
 * Call the V3 iocshell
 *       
 * @author mrk
 *
 */
public class V3iocshellFactory {
    
    /**
     * Create a V3 IOC shell.
     * @param display The display.
     */
    public static void init(Display display) {
        Window window = new Window(display);
        window.start();
    }
    
    private static ThreadCreate threadCreate = ThreadCreateFactory.getThreadCreate();
    
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
    
    private static class Invoke implements RunnableReady{
        private String fileName;
        private Invoke (String fileName) {
            this.fileName = fileName;
            threadCreate.create("v3ioc:" + fileName, 2, this);
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.util.RunnableReady#run(org.epics.pvioc.util.ThreadReady)
         */
        public void run(ThreadReady threadReady) {
            threadReady.ready();
            V3.iocsh(fileName);
            AsynOctet asynOctet = new AsynOctet(null);
            asynOctet.readIt();
        }
    }
    
}
