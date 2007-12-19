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
import org.epics.ioc.util.MessageType;
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
        SupportCreate supportCreate = new SupportCreate();
        if(!supportCreate.create()) {
            System.out.println("support create failed");
            return;
        }
        new ThreadInstance();
    }

    private static class SupportCreate implements Requester{
        
        private SupportCreate() {}
        
        private void message(String message) {
            System.out.println(message);
        }
        private boolean create() {
            IOCDB iocdb = IOCDBFactory.getMaster();
            SupportCreation supportCreation = SupportCreationFactory.createSupportCreation(iocdb, this);
            boolean gotSupport = supportCreation.createSupport();
            if(!gotSupport) {
                message("Did not find all support.");
                return false;
            }
            boolean readyForStart = supportCreation.initializeSupport();
            if(!readyForStart) {
                message("initializeSupport failed");
                return false;
            }
            boolean ready = supportCreation.startSupport();
            if(!ready) {
                message("startSupport failed");
                return false;
            }
            return true;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requester#getRequesterName()
         */
        public String getRequesterName() {
            return "swtshell";
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requester#message(java.lang.String, org.epics.ioc.util.MessageType)
         */
        public void message(String message, MessageType messageType) {
            System.out.println("swtshell " + message);
        }
    }

    static private class ThreadInstance implements Runnable {
        private Thread thread = null;
        private boolean isRunning = false;
        
        private ThreadInstance() {            
            thread = new Thread(this,"swtshell");
            thread.setPriority(2);
            thread.start();
            while(!isRunning) {
                try {
                Thread.sleep(1);
                } catch(InterruptedException e) {}
            }
        }
        
        
        public void run() {
            isRunning = true;
            runShell();
        }
        
        private void runShell() {
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

