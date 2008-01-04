/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.swtshell;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.epics.ioc.db.IOCDB;
import org.epics.ioc.db.IOCDBFactory;
import org.epics.ioc.process.SupportCreation;
import org.epics.ioc.process.SupportCreationFactory;
import org.epics.ioc.util.*;
import org.epics.ioc.util.Requester;

/**
 * A GUI iocshell implemented via Eclipse SWT (Standard Widget Toolkit).
 * The iocshell itype filter texts executed in a low priority thread so that it has low priority.
 * @author mrk
 *
 */
public class Swtshell {
    
    /**
     * 
     */
    public static void swtshell() {
        new ThreadInstance();
    }
   
    static private ThreadCreate threadCreate = ThreadFactory.getThreadCreate();

    static private class ThreadInstance implements ReadyRunnable {
        
        private ThreadInstance() {  
            threadCreate.create("swtshell", 2, this);
            
        }
        
        private boolean isReady = false;
        /* (non-Javadoc)
         * @see org.epics.ioc.util.ReadyRunnable#isReady()
         */
        public boolean isReady() {
            return isReady;
        }

        public void run() {
            isReady = true;
            final Display display = new Display();
            Shell shell = new Shell(display);
            shell.setText("iocshell");
            GridLayout layout = new GridLayout();
            layout.numColumns = 1;
            layout.makeColumnsEqualWidth = true;
            shell.setLayout(layout);
            Button getDB = new Button(shell,SWT.PUSH);
            getDB.setText("get");
            getDB.addSelectionListener( new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    Get.init(display);
                }
            });
            Button putDB = new Button(shell,SWT.PUSH);
            putDB.setText("put");
            putDB.addSelectionListener( new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    Put.init(display);
                }
            });
            Button processDB = new Button(shell,SWT.PUSH);
            processDB.setText("process");
            processDB.addSelectionListener( new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    Process.init(display);
                }
            });
            Button monitorDB = new Button(shell,SWT.PUSH);
            monitorDB.setText("monitor");
            monitorDB.addSelectionListener( new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    Monitor.init(display);
                }
            });
            Button processorDB = new Button(shell,SWT.PUSH);
            processorDB.setText("processor");
            processorDB.addSelectionListener( new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    Processor.init(display);
                }
            });
            Button loadDatabase = new Button(shell,SWT.PUSH);
            loadDatabase.setText("loadDatabase");
            loadDatabase.addSelectionListener( new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    LoadDatabase.init(display);
                }
            });
            Button introspectDatabase = new Button(shell,SWT.PUSH);
            introspectDatabase.setText("introspectDatabase");
            introspectDatabase.addSelectionListener( new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    IntrospectDatabase.init(display);
                }
            });
            Button v3iocshellDB = new Button(shell,SWT.PUSH);
            v3iocshellDB.setText("v3iocshell");
            v3iocshellDB.addSelectionListener( new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    V3iocshell.init(display);
                }
            });
            shell.pack();
            shell.open();
            while(!shell.isDisposed()) {
                if(!display.readAndDispatch()) display.sleep();
            }
            display.dispose();
        }
    }
}

