/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.swtshell;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.epics.ioc.db.DBRecord;
import org.epics.ioc.db.IOCDB;
import org.epics.ioc.db.IOCDBFactory;
import org.epics.ioc.support.RecordProcess;
import org.epics.ioc.util.EventScanner;
import org.epics.ioc.util.MessageType;
import org.epics.ioc.util.PeriodicScanner;
import org.epics.ioc.util.Requester;
import org.epics.ioc.util.ScannerFactory;
import org.epics.ioc.util.ThreadCreateFactory;
/**
 * A shell for showing the recordProcessor.
 * It also shows all threads created by org.epics.ioc.util.ThreadCreate.
 * @author mrk
 *
 */
public class ProcessorFactory {
    /** 
     * Create the shell.
     * @param display The display.
     */
    public static void init(Display display) {
        ProcessorImpl processorImpl = new ProcessorImpl(display);
        processorImpl.start();
    }

    private static class ProcessorImpl implements Requester,SelectionListener  {

        private ProcessorImpl(Display display) {
            this.display = display;
        }

        private static String windowName = "processor";
        private Display display;
        private Shell shell = null;
        private Requester requester = null;
        private IOCDB iocdb = IOCDBFactory.getMaster();
        private Composite rowWidget;
        private Button selectRecordButton;
        private Button showProcessorButton;
        private Button releaseProcessorButton;
        private Button showThreadsButton;
        private Text consoleText = null; 
        private String recordName = null;
        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requester#getRequesterName()
         */
        public String getRequesterName() {
            return requester.getRequesterName();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requester#message(java.lang.String, org.epics.ioc.util.MessageType)
         */
        public void message(String message, MessageType messageType) {
            requester.message(message, messageType);
        }
       
        private void start() {
            shell = new Shell(display);
            shell.setText(windowName);
            GridLayout gridLayout = new GridLayout();
            gridLayout.numColumns = 1;
            shell.setLayout(gridLayout);
            rowWidget = new Composite(shell,SWT.BORDER);
            gridLayout = new GridLayout();
            gridLayout.numColumns = 4;
            rowWidget.setLayout(gridLayout);
            selectRecordButton = new Button(rowWidget,SWT.PUSH);
            selectRecordButton.setText("selectRecord");
            selectRecordButton.addSelectionListener(this);
            showProcessorButton = new Button(rowWidget,SWT.PUSH);
            showProcessorButton.setText("showProcessor");
            showProcessorButton.addSelectionListener(this);
            releaseProcessorButton = new Button(rowWidget,SWT.PUSH);
            releaseProcessorButton.setText("releaseProcessor");
            releaseProcessorButton.addSelectionListener(this);
            showThreadsButton = new Button(rowWidget,SWT.PUSH);
            showThreadsButton.setText("showThreads");
            showThreadsButton.addSelectionListener(this);
            showProcessorButton.setEnabled(false);
            releaseProcessorButton.setEnabled(false);
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
            gridData.heightHint = 100;
            gridData.widthHint = 200;
            consoleText.setLayoutData(gridData);
            requester = SWTMessageFactory.create(windowName,display,consoleText);
            shell.pack();
            shell.open();
        }
        /* (non-Javadoc)
         * @see org.eclipse.swt.events.SelectionListener#widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent)
         */
        public void widgetDefaultSelected(SelectionEvent arg0) {
            widgetSelected(arg0);
        }
        /* (non-Javadoc)
         * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
         */
        public void widgetSelected(SelectionEvent arg0) {
            Object object = arg0.getSource(); 
            if(object==selectRecordButton) {
                SelectLocalRecord selectLocalRecord = 
                    SelectLocalRecordFactory.create(shell, requester);
                recordName = selectLocalRecord.getRecordName();
                boolean value = (recordName==null) ? false : true;
                showProcessorButton.setEnabled(value);
                releaseProcessorButton.setEnabled(value);
            } else if(object==showProcessorButton) {
                DBRecord dbRecord = iocdb.findRecord(recordName);
                if(dbRecord==null) {
                    message("channel is not local", MessageType.error);
                    return;
                }
                RecordProcess recordProcess = dbRecord.getRecordProcess();
                if(recordProcess==null) {
                    message("recordProcess is null", MessageType.error);
                    return;
                }
                String name = recordProcess.getRecordProcessRequesterName();
                message(recordName + " recordProcessor " + name, MessageType.info);
            } else if(object==releaseProcessorButton) {
                DBRecord dbRecord = iocdb.findRecord(recordName);
                if(dbRecord==null) {
                    message("channel is not local", MessageType.error);
                    return;
                }
                RecordProcess recordProcess = dbRecord.getRecordProcess();
                if(recordProcess==null) {
                    message("recordProcess is null", MessageType.error);
                    return;
                }
                MessageBox mb = new MessageBox(
                        rowWidget.getShell(),SWT.ICON_WARNING|SWT.YES|SWT.NO);
                mb.setMessage("VERY DANGEROUS. DO YOU WANT TO PROCEED?");
                int rc = mb.open();
                if(rc==SWT.YES) {
                    recordProcess.releaseRecordProcessRequester();
                }
            } else if(object==showThreadsButton) {
                Thread[] threads = ThreadCreateFactory.getThreadCreate().getThreads();
                for(Thread thread : threads) {
                    String name = thread.getName();
                    int priority = thread.getPriority();
                    message(name + " priority " + priority,MessageType.info);
                }
                PeriodicScanner periodicScanner = ScannerFactory.getPeriodicScanner();
                EventScanner eventScanner = ScannerFactory.getEventScanner();
                message(periodicScanner.toString(), MessageType.info);
                message(eventScanner.toString(), MessageType.info);
            }
        }
    }
}
