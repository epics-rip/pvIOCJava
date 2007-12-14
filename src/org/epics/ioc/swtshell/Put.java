/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.swtshell;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.epics.ioc.ca.CD;
import org.epics.ioc.ca.CDFactory;
import org.epics.ioc.ca.CDPut;
import org.epics.ioc.ca.CDPutRequester;
import org.epics.ioc.ca.CDRecord;
import org.epics.ioc.ca.Channel;
import org.epics.ioc.ca.ChannelField;
import org.epics.ioc.ca.ChannelFieldGroup;
import org.epics.ioc.ca.ChannelFieldGroupListener;
import org.epics.ioc.util.IOCExecutor;
import org.epics.ioc.util.IOCExecutorFactory;
import org.epics.ioc.util.MessageType;
import org.epics.ioc.util.RequestResult;
import org.epics.ioc.util.Requester;
import org.epics.ioc.util.ScanPriority;
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
public class Put {

    public static void init(Display display) {
        ChannelPutImpl channelPutImpl = new ChannelPutImpl();
        channelPutImpl.init(display);
    }

    private static class ChannelPutImpl extends AbstractChannelShell  {

        private ChannelPutImpl() {
            super("put");
        }

        private static IOCExecutor iocExecutor = IOCExecutorFactory.create("swtshell:putt");
        private static ScanPriority scanPriority = ScanPriority.higher;

        /**
         * Called by SwtShell after the default constructor has been called.
         * @param display The display.
         */
        public void init(Display display) {
            super.start(display);
        }
        
        private Button putButton;
        private Button processButton;
        private ChannelField channelField = null;
        private PutIt putIt = null;

        public void startClient(Composite parentWidget) {
            Composite putComposite = new Composite(parentWidget,SWT.BORDER);
            GridLayout gridLayout = new GridLayout();
            gridLayout.numColumns = 3;
            putComposite.setLayout(gridLayout);
            super.connectButtonCreate(putComposite);
            putButton = new Button(putComposite,SWT.NONE);
            putButton.setText("put");
            putButton.addSelectionListener(this);
            processButton = new Button(putComposite,SWT.CHECK);
            processButton.setText("process");
            processButton.setSelection(false);
            putButton.setEnabled(false);
            processButton.setEnabled(false);
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
                    GetChannelField getChannelField = new GetChannelField(shell,this,channel);
                    channelField = getChannelField.getChannelField();
                    if(channelField==null) {
                        super.message(String.format("no field selected%n"),MessageType.error);
                        return;
                    }
                    putButton.setEnabled(true);
                    processButton.setEnabled(true);
                    return;
                case disconnected:
                    putButton.setEnabled(false);
                    processButton.setEnabled(false);
                    return;
                }
                return;
            }
            if(object==putButton) {
                boolean process = processButton.getSelection();
                putIt = new PutIt(channel,this,process,shell);
                boolean result = putIt.connect(channelField);
                if(result) {
                    connectState = ConnectState.connected;
                    super.message(String.format("connected%n"),MessageType.info);
                    connectButton.setText(connectStateText[1]);

                } else {
                    super.message(String.format("not connected%n"),MessageType.info);
                    putIt = null;
                }
                if(putIt==null) {
                    super.message(String.format("not connected%n"),MessageType.info);
                    return;
                }
                putIt.put();
                putIt.disconnect();
                putIt = null;
                return;
            }
        }
        private class PutIt implements
        Runnable,
        CDPutRequester,
        ChannelFieldGroupListener
        {
            private Shell shell;
            private Lock lock = new ReentrantLock();
            private Condition waitDone = lock.newCondition();
            private Channel channel;
            final private Requester requester;
            private boolean process;
            private boolean allDone = false;
            private RequestResult requestResult;

            private CD cd;
            private CDPut cdPut;

            private PutIt(Channel channel,Requester requester,boolean process,Shell shell) {
                this.shell = shell;
                this.channel = channel;
                this.requester = requester;
                this.process = process;
            }

            private boolean connect(ChannelField channelField) {
                ChannelFieldGroup putFieldGroup = channel.createFieldGroup(this);
                putFieldGroup.addChannelField(channelField);
                cd = CDFactory.createCD(channel, putFieldGroup);
                cdPut = cd.createCDPut(this, process);
                if(cdPut==null) return false;
                return true;
            }

            private void disconnect() {
                cd.destroy(cdPut);
            }

            private void put() {
                allDone = false;
                cdPut.get(cd);
                lock.lock();
                try {
                    while(!allDone) {                       
                        waitDone.await();
                    }
                } catch (InterruptedException ie) {
                    return;
                } finally {
                    lock.unlock();
                }
                if(requestResult!=RequestResult.success) {
                    message("get failed", MessageType.error);
                }
                allDone = false;
                CDRecord cdRecord = cd.getCDRecord();
                cdRecord.getCDStructure().clearNumPuts();
                GetCDValue getCDValue = new GetCDValue(shell);
                getCDValue.getValue(cdRecord);
                iocExecutor.execute(this, scanPriority);
                lock.lock();
                try {
                    while(!allDone) {                       
                        waitDone.await();
                    }
                } catch (InterruptedException ie) {
                    return;
                } finally {
                    lock.unlock();
                }
                if(requestResult!=RequestResult.success) {
                    message("get failed", MessageType.error);
                }
            }
            /* (non-Javadoc)
             * @see java.lang.Runnable#run()
             */
            public void run() {
                cdPut.put(cd);
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.util.Requester#putRequesterName()
             */
            public String getRequesterName() {
                return requester.getRequesterName();
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.util.Requester#message(java.lang.String, org.epics.ioc.util.MessageType)
             */
            public void message(final String message, final MessageType messageType) {
                requester.message(message, MessageType.info);
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.CDPutRequester#getDone(org.epics.ioc.util.RequestResult)
             */
            public void getDone(RequestResult requestResult) {
                lock.lock();
                try {
                    this.requestResult = requestResult;
                    allDone = true;
                    waitDone.signal();
                } finally {
                    lock.unlock();
                }
            }

            /* (non-Javadoc)
             * @see org.epics.ioc.ca.CDPutRequester#putDone(org.epics.ioc.util.RequestResult)
             */
            public void putDone(RequestResult requestResult) {
                lock.lock();
                try {
                    this.requestResult = requestResult;
                    allDone = true;
                    waitDone.signal();
                } finally {
                    lock.unlock();
                }
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelFieldGroupListener#accessRightsChange(org.epics.ioc.ca.Channel, org.epics.ioc.ca.ChannelField)
             */
            public void accessRightsChange(Channel channel, ChannelField channelField) {
                // TODO Auto-generated method stub

            }
        }
    }
}
