/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.swtshell;

import java.util.Date;
import java.util.Calendar;
import java.util.concurrent.locks.*;

import org.eclipse.swt.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.widgets.*;

import org.epics.ioc.pv.*;
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
        }
        start();
    }
    
    // text.setSize does not work. Don't know why. Is it a bug?
    public static void makeBlanks(Text text,int nrows,int ncols) {
        char[] blank = new char[ncols];
        for(int i=0; i<blank.length; i++) blank[i] = 'x';
        String blankString = String.copyValueOf(blank);
        String format = "%s";
        for(int i=0; i<nrows; i++) format += "%n";
        text.setText(String.format(format, blankString));
    }
//  list.setSize does not work. Don't know why. Is it a bug?
    public static void makeBlanks(List list,int nrows,int ncols) {
        char[] blank = new char[ncols];
        for(int i=0; i<blank.length; i++) blank[i] = 'x';
        String blankString = String.copyValueOf(blank);
        String format = "%s";
        for(int i=0; i<nrows; i++) format += "%n";
        list.add(String.format(format, blankString));
    }
    
    public static void makeBlanks(Combo combo,int ncols) {
        char[] blank = new char[ncols];
        for(int i=0; i<blank.length; i++) blank[i] = 'x';
        String blankString = String.copyValueOf(blank);
        combo.add(blankString);
    }
    
    public static String pvDataToString(PVData pvData) {
        String value = null;
        Type type = pvData.getField().getType();
        switch(type) {
        case pvEnum:
            PVEnum pvEnum = (PVEnum)pvData;
            value = pvEnum.getChoices()[pvEnum.getIndex()];
            break;
        case pvMenu:
            PVMenu pvMenu = (PVMenu)pvData;
            value = pvMenu.getChoices()[pvMenu.getIndex()];
            break;
        case pvStructure:
            PVTimeStamp pvTimeStamp = PVTimeStamp.create(pvData);
            if(pvTimeStamp!=null) {
                TimeStamp timeStamp = new TimeStamp();
                pvTimeStamp.get(timeStamp);
                long secondPastEpochs = timeStamp.secondsPastEpoch;
                int nano = timeStamp.nanoSeconds;
                long milliPastEpoch = nano/1000000 + secondPastEpochs*1000;
                Date date = new Date(milliPastEpoch);
                value = String.format("%tF %tT.%tL",date,date,date);
                break;
            }
            // no break
        default:
            value = pvData.toString();
        }
        return value;
    }
    
    private static class SupportCreate implements Requestor{
        private SupportCreate() {
            
        }
        
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
    
    private static void start() {
        ReentrantLock lock = new ReentrantLock();
        Condition done = lock.newCondition();
        try {
            lock.lock();
            try {
                new ThreadInstance(lock,done);
                done.await();
            } finally {
                lock.unlock();
            }
        } catch(InterruptedException e) {}
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
            thread.start();
        } 
        
        public void run() {
            shell();
            lock.lock();
            try {
                done.signal();
            } finally {
                lock.unlock();
            }
        }
    }
    
    private static void shell() {
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
        Button monitorDB = new Button(shell,SWT.PUSH);
        monitorDB.setText("monitor");
        monitorDB.addSelectionListener( new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                Monitor.init();
            }
        });
        Button loadDBD = new Button(shell,SWT.PUSH);
        loadDBD.setText("loadDBD");
        loadDBD.addSelectionListener( new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                LoadDBD.init(display);
            }
        });
        Button loadDB = new Button(shell,SWT.PUSH);
        loadDB.setText("loadDB");
        loadDB.addSelectionListener( new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                LoadDB.init(display);
            }
        });
        Button introspectDBD = new Button(shell,SWT.PUSH);
        introspectDBD.setText("introspectDBD");
        introspectDBD.addSelectionListener( new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                IntrospectDBD.init(display);
            }
        });
        Button introspectDB = new Button(shell,SWT.PUSH);
        introspectDB.setText("introspectDB");
        introspectDB.addSelectionListener( new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                IntrospectDB.init(display);
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
