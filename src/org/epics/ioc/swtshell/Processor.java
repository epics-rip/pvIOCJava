/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.swtshell;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.epics.ioc.db.DBRecord;
import org.epics.ioc.db.IOCDB;
import org.epics.ioc.db.IOCDBFactory;
import org.epics.ioc.process.RecordProcess;
import org.epics.ioc.util.EventScanner;
import org.epics.ioc.util.MessageType;
import org.epics.ioc.util.PeriodicScanner;
import org.epics.ioc.util.*;
/**
 * Provides the following sets of controls:
 * <ul>
 *    <li>Processor - Shows and releases the current record processor.
 *    Also show all scan threads.</li>
 *    <li>Process - Can connect to a channel and process the channel.</li>
 *    <li>Get - Can connect to a field of a channel and get the current values.</li>
 *    <li>PutIt - Can connect to a field of a channel an putIt new values.</li>
 * </ul>
 * For both get an putIt a null field selects a complete record instance.
 * @author mrk
 *
 */
public class Processor {

    public static void init(Display display) {
        ChannelProcessorImpl channelProcessorImpl = new ChannelProcessorImpl();
        channelProcessorImpl.init(display);
    }

    private static class ChannelProcessorImpl extends AbstractChannelShell  {

        private ChannelProcessorImpl() {
            super("processor");
        }

        /**
         * Called by SwtShell after the default constructor has been called.
         * @param display The display.
         */
        public void init(Display display) {
            super.start(display);
        }

        private IOCDB iocdb = IOCDBFactory.getMaster();
        private Composite rowWidget;
        private Button showProcessorButton;
        private Button releaseProcessorButton;
        private Button showThreadsButton;

        public void startClient(Composite parentWidget) {
            rowWidget = new Composite(parentWidget,SWT.BORDER);
            GridLayout gridLayout = new GridLayout();
            gridLayout.numColumns = 4;
            rowWidget.setLayout(gridLayout);
            super.connectButtonCreate(rowWidget);
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
        }
        /* (non-Javadoc)
         * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
         */
        public void widgetSelected(SelectionEvent arg0) {
            Object object = arg0.getSource(); 
            if(object==connectButton) {
                super.connectButtonSelected();
                switch(connectState) {
                case connected:          
                    showProcessorButton.setEnabled(true);
                    releaseProcessorButton.setEnabled(true);
                    return;
                case disconnected:
                    showProcessorButton.setEnabled(false);
                    releaseProcessorButton.setEnabled(false);
                    return;
                }
            }
            if(object==showProcessorButton) {
                String recordName = channel.getChannelName();
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
                message("recordProcessor " + name, MessageType.info);
                return;
            }
            if(object==releaseProcessorButton) {
                if(channel==null) {
                    message(String.format("no record selected%n"),MessageType.error);
                    return;
                }
                String recordName = channel.getChannelName();
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
                return;
            }
            if(object==showThreadsButton) {
                Thread[] threads = ThreadFactory.getThreadCreate().getThreads();
                for(Thread thread : threads) {
                    String name = thread.getName();
                    int priority = thread.getPriority();
                    message(name + " priority " + priority,MessageType.info);
                }
                PeriodicScanner periodicScanner = ScannerFactory.getPeriodicScanner();
                EventScanner eventScanner = ScannerFactory.getEventScanner();
                message(periodicScanner.toString(), MessageType.info);
                message(eventScanner.toString(), MessageType.info);
                return;
            }
        }
    }
}
