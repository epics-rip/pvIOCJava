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
import org.epics.ioc.ca.Channel;
import org.epics.ioc.ca.ChannelField;
import org.epics.ioc.ca.ChannelProcess;
import org.epics.ioc.ca.ChannelProcessRequester;
import org.epics.ioc.ca.ChannelListener;
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
 *    <li>Put - Can connect to a field of a channel an put new values.</li>
 * </ul>
 * For both get an put a null field selects a complete record instance.
 * @author mrk
 *
 */
public class Process {

    public static void init(Display display) {
        ChannelProcessImpl channelProcessImpl = new ChannelProcessImpl();
        channelProcessImpl.init(display);
    }

    private static class ChannelProcessImpl extends AbstractChannelShell  {

        private ChannelProcessImpl() {
            super("process");
        }

        private static IOCExecutor iocExecutor = IOCExecutorFactory.create("swtshell:Get");
        private static ScanPriority scanPriority = ScanPriority.higher;

        /**
         * Called by SwtShell after the default constructor has been called.
         * @param display The display.
         */
        public void init(Display display) {
            super.start(display);
        }

        private Button processButton;

        public void startClient(Composite parentWidget) {
            Composite processWidget = new Composite(parentWidget,SWT.BORDER);
            GridLayout gridLayout = new GridLayout();
            gridLayout.numColumns = 2;
            processWidget.setLayout(gridLayout);
            super.connectButtonCreate(processWidget);
            processButton = new Button(processWidget,SWT.PUSH);
            processButton.setText("process");
            processButton.addSelectionListener(this);               
            processWidget.addDisposeListener(this);
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
                    processButton.setEnabled(true);
                    return;
                case disconnected:
                    processButton.setEnabled(false);
                    return;
                }
            }
            if(object==processButton) {
                ProcessIt process = new ProcessIt(channel,this);
                boolean result = process.connect();
                if(result) {
                    connectState = ConnectState.connected;
                    message(String.format("connected%n"),MessageType.info);
                    connectButton.setText(connectStateText[1]);
                    
                } else {
                    message(String.format("not connected%n"),MessageType.info);
                    process = null;
                }
                process.process();
                process.disconnect();
                return;
            }
        }
        
        private class ProcessIt implements
        Runnable,
        ChannelProcessRequester,ChannelListener
        {   
            private Lock lock = new ReentrantLock();
            private Condition waitDone = lock.newCondition();
            private boolean allDone = false;
            private Channel channel;
            final private Requester requester;
            private ChannelProcess channelProcess;

            private ProcessIt(Channel channel,Requester requester) {
                this.channel = channel;
                this.requester = requester;
            }

            private boolean connect() {
                channelProcess = channel.createChannelProcess(this);
                if(channelProcess==null) return false;
                return true;
            }

            private void disconnect() {
                channelProcess.destroy();
            }

            private void process() {
                if(channelProcess==null) {
                    requester.message("not connected", MessageType.info);
                }
                allDone = false;
                iocExecutor.execute(this, scanPriority);
            }
            /* (non-Javadoc)
             * @see java.lang.Runnable#run()
             */
            public void run() {
                allDone = false;
                channelProcess.process();
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
                requester.message("processComplete", MessageType.info);
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.util.Requester#getRequesterName()
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
             * @see org.epics.ioc.ca.ChannelProcessRequester#processDone(org.epics.ioc.util.RequestResult)
             */
            public void processDone(RequestResult requestResult) {
                lock.lock();
                try {
                    allDone = true;
                    waitDone.signal();
                } finally {
                    lock.unlock();
                }
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelListener#channelStateChange(org.epics.ioc.ca.Channel, boolean)
             */
            public void channelStateChange(Channel c, boolean isConnected) {
                // TODO Auto-generated method stub

            }
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelListener#disconnect(org.epics.ioc.ca.Channel)
             */
            public void disconnect(Channel c) {
                // TODO Auto-generated method stub

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
