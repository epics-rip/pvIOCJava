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
import org.epics.ioc.ca.CD;
import org.epics.ioc.ca.CDFactory;
import org.epics.ioc.ca.CDGet;
import org.epics.ioc.ca.CDGetRequester;
import org.epics.ioc.ca.Channel;
import org.epics.ioc.ca.ChannelField;
import org.epics.ioc.ca.ChannelFieldGroup;
import org.epics.ioc.ca.ChannelFieldGroupListener;
import org.epics.ioc.ca.ChannelStateListener;
import org.epics.ioc.pv.Type;
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
 *    <li>GetIt - Can connect to a field of a channel and get the current values.</li>
 *    <li>Put - Can connect to a field of a channel an put new values.</li>
 * </ul>
 * For both get an put a null field selects a complete record instance.
 * @author mrk
 *
 */
public class Get {

    public static void init(Display display) {
        ChannelGetImpl channelGetImpl = new ChannelGetImpl();
        channelGetImpl.init(display);
    }

    private static class ChannelGetImpl extends AbstractChannelShell  {

        private ChannelGetImpl() {
            super("get");
        }

        private static IOCExecutor iocExecutor = IOCExecutorFactory.create("swtshell:GetIt");
        private static ScanPriority scanPriority = ScanPriority.higher;

        /**
         * Called by SwtShell after the default constructor has been called.
         * @param display The display.
         */
        public void init(Display display) {
            super.start(display);
        }

        private Button getButton;
        private Button processButton;
        private Button propertyButton;
        private ChannelField channelField = null;
        private String[] propertyNames = null;
        private GetIt get = null;

        public void startClient(Composite parentWidget) {
            Composite getWidget = new Composite(parentWidget,SWT.BORDER);
            GridLayout gridLayout = new GridLayout();
            gridLayout.numColumns = 4;
            getWidget.setLayout(gridLayout);
            super.connectButtonCreate(getWidget);
            getButton = new Button(getWidget,SWT.NONE);
            getButton.setText("get");
            getButton.addSelectionListener(this);
            processButton = new Button(getWidget,SWT.CHECK);
            processButton.setText("process");
            processButton.setSelection(false);
            propertyButton = new Button(getWidget,SWT.PUSH);
            propertyButton.setText("property");
            propertyButton.addSelectionListener(this);
            getButton.setEnabled(false);
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
                        message(String.format("no field selected%n"),MessageType.error);
                        return;
                    }
                    getButton.setEnabled(true);
                    processButton.setEnabled(true);
                    propertyButton.setEnabled(true);
                    return;
                case disconnected:
                    getButton.setEnabled(false);
                    processButton.setEnabled(false);
                    propertyButton.setEnabled(false);
                    return;
                }
            }
            if(object==propertyButton) {
                GetProperty getProperty = new GetProperty(shell);
                propertyNames = getProperty.open(channelField);
            }
            if(object==getButton) {
                boolean process = processButton.getSelection();
                get = new GetIt(channel,this,process);
                boolean result = get.connect(channelField, propertyNames);
                if(result) {
                    connectState = ConnectState.connected;
                    message(String.format("connected%n"),MessageType.info);
                    connectButton.setText(connectStateText[1]);
                } else {
                    message(String.format("not connected%n"),MessageType.info);
                    get = null;
                }
                if(get==null) {
                    message(String.format("not connected%n"),MessageType.info);
                    return;
                }
                CD cD = get.get();
                if(cD==null) return;
                CDRecordPrint cdRecordPrint = new CDRecordPrint(cD.getCDRecord(),consoleText);
                cdRecordPrint.print();
                get.disconnect();
                get = null;
                return;
            }
        }
        
        private class GetIt implements
        Runnable,
        CDGetRequester,
        ChannelStateListener, ChannelFieldGroupListener
        {
            private Lock lock = new ReentrantLock();
            private Condition waitDone = lock.newCondition();
            private boolean allDone = false;
            private Channel channel;
            final private Requester requester;
            private boolean process;
            private CD cd;
            private CDGet cdGet;

            private GetIt(Channel channel,Requester requester,boolean process) {
                this.channel = channel;
                this.requester = requester;
                this.process = process;
            }

            private boolean connect(ChannelField channelField,String[] propertyNames) {
                ChannelFieldGroup getFieldGroup = channel.createFieldGroup(this);
                if(channelField.getField().getType()!=Type.pvStructure
                        || (propertyNames==null || propertyNames.length<=0)) {
                    getFieldGroup.addChannelField(channelField);
                }
                if(propertyNames!=null && propertyNames.length>0) {
                    for(String propertyName: propertyNames) {
                        ChannelField propChannelField = channelField.findProperty(propertyName);
                        if(propChannelField==null) {
                            requester.message(String.format(
                                    "property %s not found%n", propertyName),MessageType.error);
                            continue;
                        }
                        getFieldGroup.addChannelField(propChannelField);
                    }
                }
                cd = CDFactory.createCD(channel, getFieldGroup);
                cdGet = cd.createCDGet(this, process);
                if(cdGet==null) return false;
                return true;
            }

            private void disconnect() {
                cd.destroy(cdGet);
            }

            private CD get() {                
                allDone = false;               
                iocExecutor.execute(this, scanPriority);
                lock.lock();
                try {
                    while(!allDone) {                       
                        waitDone.await();
                    }
                } catch (InterruptedException ie) {
                } finally {
                    lock.unlock();
                }

                return cd;
            }
            /* (non-Javadoc)
             * @see java.lang.Runnable#run()
             */
            public void run() {
                cdGet.get(cd);
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
             * @see org.epics.ioc.ca.CDGetRequester#getDone(org.epics.ioc.util.RequestResult)
             */
            public void getDone(RequestResult requestResult) {
                lock.lock();
                try {
                    allDone = true;
                    waitDone.signal();
                } finally {
                    lock.unlock();
                }
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelStateListener#channelStateChange(org.epics.ioc.ca.Channel, boolean)
             */
            public void channelStateChange(Channel c, boolean isConnected) {
                // TODO Auto-generated method stub

            }
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelStateListener#disconnect(org.epics.ioc.ca.Channel)
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
