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
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.epics.ioc.ca.CD;
import org.epics.ioc.ca.CDFactory;
import org.epics.ioc.ca.CDGet;
import org.epics.ioc.ca.CDGetRequester;
import org.epics.ioc.ca.Channel;
import org.epics.ioc.ca.ChannelField;
import org.epics.ioc.ca.ChannelFieldGroup;
import org.epics.ioc.ca.ChannelFieldGroupListener;
import org.epics.ioc.ca.ChannelListener;
import org.epics.ioc.pv.Type;
import org.epics.ioc.util.IOCExecutor;
import org.epics.ioc.util.IOCExecutorFactory;
import org.epics.ioc.util.MessageType;
import org.epics.ioc.util.RequestResult;
import org.epics.ioc.util.Requester;
import org.epics.ioc.util.ScanPriority;

/**
 * A shell for getting values from a channel.
 * @author mrk
 *
 */
public class GetFactory {
    /**
     * Create the shell. 
     * @param display The display to which the shell belongs.
     */
    public static void init(Display display) {
        GetImpl getImpl = new GetImpl(display);
        getImpl.start();
    }

    private static class GetImpl implements DisposeListener,Requester,ChannelListener,SelectionListener
    {

        private GetImpl(Display display) {
            this.display = display;
        }

        private static IOCExecutor iocExecutor
            = IOCExecutorFactory.create("swtshell:Get",ScanPriority.low);
        private static String windowName = "get";
        private Display display;
        private Shell shell = null;
        private Requester requester = null;
        private Channel channel = null;
        private ChannelConnect channelConnect = null;
        private ChannelField channelField = null;
        private String[] propertyNames = null;
        private Button getButton;
        private Button processButton;
        private Button propertyButton;
        private Text consoleText = null; 
        private GetIt get = null;
        
        /* (non-Javadoc)
         * @see org.eclipse.swt.events.DisposeListener#widgetDisposed(org.eclipse.swt.events.DisposeEvent)
         */
        public void widgetDisposed(DisposeEvent e) {
            if(channel!=null) channel.destroy();
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
        public void message(String message, MessageType messageType) {
            requester.message(message, messageType);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelListener#channelStateChange(org.epics.ioc.ca.Channel, boolean)
         */
        public void channelStateChange(Channel c, boolean isConnected) {
            if(isConnected) {
                channel = channelConnect.getChannel();
                String fieldName = channel.getFieldName();
                channelField = channel.createChannelField(fieldName);
                if(channelField==null) {
                    message(
                        "channelField null fieldName " + fieldName,
                        MessageType.error);
                }
                getButton.setEnabled(true);
                processButton.setEnabled(true);
                propertyButton.setEnabled(true);
                return;
            } else {
                channel = null;
                channelField = null;
                getButton.setEnabled(false);
                processButton.setEnabled(false);
                propertyButton.setEnabled(false);
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
            channelConnect.createWidgets(shell,true,true);
            Composite getWidget = new Composite(shell,SWT.BORDER);
            gridLayout = new GridLayout();
            gridLayout.numColumns = 3;
            getWidget.setLayout(gridLayout);
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
            propertyButton.setEnabled(false);            
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
            shell.addDisposeListener(this);
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
            
            if(object==propertyButton) {
                PropertyGet propertyGet = PropertyGetFactory.create(shell);
                propertyNames = propertyGet.getPropertyNames(channelField);
            } else if(object==getButton) {
                boolean process = processButton.getSelection();
                get = new GetIt(channel,this,process);
                boolean result = get.connect(channelField, propertyNames);
                if(!result) {
                    message(String.format("not connected%n"),MessageType.info);
                    return;
                }
                CD cD = get.get();
                if(cD==null) return;
                CDPrint cdPrint = CDPrintFactory.create(cD.getCDRecord(),consoleText);
                cdPrint.print();
                get.disconnect();
                get = null;
                return;
            }
        }
        
        private class GetIt implements
        Runnable,
        CDGetRequester,
        ChannelFieldGroupListener
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
                getFieldGroup.addChannelField(channelField);
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
                iocExecutor.execute(this);
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
             * @see org.epics.ioc.ca.ChannelFieldGroupListener#accessRightsChange(org.epics.ioc.ca.Channel, org.epics.ioc.ca.ChannelField)
             */
            public void accessRightsChange(Channel channel, ChannelField channelField) {
                // nothing to do
            }
        }
    }
}
