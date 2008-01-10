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
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.epics.ioc.ca.Channel;
import org.epics.ioc.ca.ChannelField;
import org.epics.ioc.ca.ChannelListener;
import org.epics.ioc.ca.ChannelProcess;
import org.epics.ioc.ca.ChannelProcessRequester;
import org.epics.ioc.util.IOCExecutor;
import org.epics.ioc.util.IOCExecutorFactory;
import org.epics.ioc.util.MessageType;
import org.epics.ioc.util.RequestResult;
import org.epics.ioc.util.Requester;
import org.epics.ioc.util.ScanPriority;
/**
 * Shell for processing a channel.
 * @author mrk
 *
 */
public class ProcessFactory {

    /**
     * Create the process shell.
     * @param display The display.
     */
    public static void init(Display display) {
        ProcessImpl processImpl = new ProcessImpl(display);
        processImpl.start();
    }

    private static class ProcessImpl implements Requester,ChannelListener,SelectionListener  {

        private ProcessImpl(Display display) {
            this.display = display;
        }

        private static IOCExecutor iocExecutor = IOCExecutorFactory.create("swtshell:Get");
        private static ScanPriority scanPriority = ScanPriority.higher;
        private static String windowName = "process";
        private Display display;
        private Shell shell = null;
        private Requester requester = null;
        private Channel channel = null;
        private ChannelConnect channelConnect = null;
        private Button processButton;

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
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelListener#channelStateChange(org.epics.ioc.ca.Channel, boolean)
         */
        public void channelStateChange(Channel c, boolean isConnected) {
            if(isConnected) {
                channel = channelConnect.getChannel();
                processButton.setEnabled(true);
                return;
            } else {
                channel = null;
                processButton.setEnabled(false);
                return;
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelListener#disconnect(org.epics.ioc.ca.Channel)
         */
        public void destroy(Channel c) {
            channelStateChange(c,false);
        }
        
        private void start() {
            shell = new Shell(display);
            shell.setText(windowName);
            GridLayout gridLayout = new GridLayout();
            gridLayout.numColumns = 1;
            shell.setLayout(gridLayout);
            channelConnect = ChannelConnectFactory.create(this,this);
            channelConnect.createWidgets(shell);
            
        
            processButton = new Button(shell,SWT.PUSH);
            processButton.setText("process");
            processButton.addSelectionListener(this);               
            processButton.setEnabled(false);
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
            if(object==processButton) {
                ProcessIt process = new ProcessIt(channel,this);
                boolean result = process.connect();
                if(result) {
                    process.process();
                    process.disconnect();
                    message(String.format("processed%n"),MessageType.info);
                } else {
                    message(String.format("process request failed%n"),MessageType.info);
                    process = null;
                }
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
            public void destroy(Channel c) {
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