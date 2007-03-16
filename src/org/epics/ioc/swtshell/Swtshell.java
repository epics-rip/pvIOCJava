/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.swtshell;

import java.util.concurrent.locks.*;

import org.eclipse.swt.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.widgets.*;

import org.epics.ioc.db.*;
import org.epics.ioc.process.*;
import org.epics.ioc.util.*;

/**
 * A GUI iocshell implemented via Eclipse SWT (Standard Widget Toolkit).
 * The iocshell itype filter texts executed in a low priority thread so that it has low priority.
 * @author mrk
 *
 */
public class Swtshell {
    
    public static void swtshell() {
        SupportCreate supportCreate = new SupportCreate();
        if(!supportCreate.create()) {
            System.out.println("support create failed");
            return;
        }
        ReentrantLock lock = new ReentrantLock();
        Condition done = lock.newCondition();
        ThreadInstance threadInstance = new ThreadInstance(lock,done);
        try {
            lock.lock();
            try {
                threadInstance.start();
                done.await();
            } finally {
                lock.unlock();
            }
        } catch(InterruptedException e) {}
    }

    private static class SupportCreate implements Requestor{
        
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
         * @see org.epics.ioc.util.Requestor#getRequestorName()
         */
        public String getRequestorName() {
            return "swtshell";
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requestor#message(java.lang.String, org.epics.ioc.util.MessageType)
         */
        public void message(String message, MessageType messageType) {
            System.out.println("swtshell " + message);
        }
    }

    static private class ThreadInstance implements Runnable {
        private Thread thread = null;
        private ReentrantLock lock;
        private Condition done;

        private ThreadInstance(ReentrantLock lock,Condition done) {
            this.lock = lock;
            this.done = done;
            thread = new Thread(this,"swtshell");
            thread.setPriority(2);
        }
        
        private void start() {
            thread.start();
        }
        
        public void run() {
            runShell();
            lock.lock();
            try {
                done.signal();
            } finally {
                lock.unlock();
            }
        }
        
        private void runShell() {
            final Display display = new Display();
            Shell shell = new Shell(display);
            shell.setText("iocshell");
            GridLayout layout = new GridLayout();
            layout.numColumns = 1;
            layout.makeColumnsEqualWidth = true;
            shell.setLayout(layout);
            Button probeDB = new Button(shell,SWT.PUSH);
            probeDB.setText("probe");
            probeDB.addSelectionListener( new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    Probe.init(display);
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
            Button monitorDB = new Button(shell,SWT.PUSH);
            monitorDB.setText("monitor");
            monitorDB.addSelectionListener( new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    Monitor.init(display);
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

