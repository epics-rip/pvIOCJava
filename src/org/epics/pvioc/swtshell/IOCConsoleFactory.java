/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.swtshell;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.epics.pvdata.pv.MessageType;
import org.epics.pvdata.pv.Requester;
import org.epics.pvioc.database.PVDatabase;
import org.epics.pvioc.database.PVDatabaseFactory;

/**
 * A shell for getting values from a channel.
 * @author mrk
 *
 */
public class IOCConsoleFactory {
    /**
     * Create the shell. 
     * @param display The display to which the shell belongs.
     */
    public static void init(Display display) {
        IOCConsoleImpl iocConsoleImpl = new IOCConsoleImpl(display);
        iocConsoleImpl.start();
    }

    private static final PVDatabase masterPVDatabase = PVDatabaseFactory.getMaster();
    
    private static class IOCConsoleImpl implements DisposeListener,Requester,Runnable
    {

        private IOCConsoleImpl(Display display) {
            this.display = display;
        }

        private boolean isDisposed = false;
        private static String windowName = "iocConsole";
        private static String newLine = String.format("%n");
        private Display display;
        private Shell shell = null;
        private Text consoleText = null; 
        private String consoleMessage = null;
        
        /* (non-Javadoc)
         * @see org.eclipse.swt.events.DisposeListener#widgetDisposed(org.eclipse.swt.events.DisposeEvent)
         */
        public void widgetDisposed(DisposeEvent e) {
            isDisposed = true;
            masterPVDatabase.removeRequester(this);
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.util.Requester#getRequesterName()
         */
        public String getRequesterName() {
            return windowName;
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.util.Requester#message(java.lang.String, org.epics.pvioc.util.MessageType)
         */
        public void message(String message, MessageType messageType) {
            if(isDisposed) return;
            consoleMessage = messageType.toString() + " " + message;
            display.syncExec(this);
        }         
        /* (non-Javadoc)
         * @see java.lang.Runnable#run()
         */
        public void run() {
            if(isDisposed) return;
            consoleText.append(consoleMessage);
            consoleText.append(newLine);
        }
        
        private void start() {
            shell = new Shell(display);
            shell.setText(windowName);
            GridLayout gridLayout = new GridLayout();
            gridLayout.numColumns = 1;
            shell.setLayout(gridLayout);
            
            Composite consoleComposite = new Composite(shell,SWT.BORDER);
            gridLayout = new GridLayout();
            gridLayout.numColumns = 1;
            consoleComposite.setLayout(gridLayout);
            GridData gridData = new GridData(GridData.FILL_BOTH);
            consoleComposite.setLayoutData(gridData);
            Button clearItem = new Button(consoleComposite,SWT.PUSH);
            clearItem.setText("&Clear");
            clearItem.addSelectionListener(new SelectionListener() {
                public void widgetDefaultSelected(SelectionEvent arg0) {
                    widgetSelected(arg0);
                }
                public void widgetSelected(SelectionEvent arg0) {
                    consoleText.selectAll();
                    consoleText.clearSelection();
                    consoleText.setText("");
                }
            });
            consoleText = new Text(consoleComposite,SWT.BORDER|SWT.H_SCROLL|SWT.V_SCROLL|SWT.READ_ONLY);
            gridData = new GridData(GridData.FILL_BOTH);
            gridData.heightHint = 240;
            gridData.widthHint = 780;
            consoleText.setLayoutData(gridData);
            shell.pack();
            shell.open();
            shell.addDisposeListener(this);
            masterPVDatabase.addRequester(this);
        }
    }
}
