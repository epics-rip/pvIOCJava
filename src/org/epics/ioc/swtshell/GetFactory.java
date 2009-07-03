/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.swtshell;

import java.util.BitSet;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.epics.pvData.channelAccess.Channel;
import org.epics.pvData.channelAccess.ChannelGet;
import org.epics.pvData.channelAccess.ChannelGetRequester;
import org.epics.pvData.misc.Executor;
import org.epics.pvData.misc.ExecutorNode;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVStructure;
import org.epics.pvData.pv.Requester;


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
    
    private static Executor executor = SwtshellFactory.getExecutor();
    
    private static class GetImpl implements DisposeListener,CreateRequestRequester,SelectionListener,Runnable
    {

        private GetImpl(Display display) {
            this.display = display;
        }
        
        private boolean isDisposed = false;
        private static String windowName = "get";
        private Display display;
        private Shell shell = null;
        private Requester requester = null;
        private Channel channel = null;
        private Button connectButton;
        private Button processButton;
        private Button createRequestButton = null;
        private Button disconnectButton;
        private Button getButton;
        private Text consoleText = null; 
        private PVStructure pvRequest = null;
        private Get get = new Get();
        private boolean isProcess = false;
        
        private void start() {
            shell = new Shell(display);
            shell.setText(windowName);
            GridLayout gridLayout = new GridLayout();
            gridLayout.numColumns = 1;
            shell.setLayout(gridLayout);
            Composite composite = new Composite(shell,SWT.BORDER);
            RowLayout rowLayout = new RowLayout(SWT.HORIZONTAL);
            composite.setLayout(rowLayout);
            connectButton = new Button(composite,SWT.PUSH);
            connectButton.setText("connect");
            connectButton.addSelectionListener(this);               
            connectButton.setEnabled(true);
            processButton = new Button(composite,SWT.CHECK);
            processButton.setText("process");
            processButton.setSelection(false);
            processButton.addSelectionListener(this);               
            processButton.setEnabled(true);
            createRequestButton = new Button(composite,SWT.PUSH);
            createRequestButton.setText("createRequest");
            createRequestButton.addSelectionListener(this);               
            createRequestButton.setEnabled(false);
            disconnectButton = new Button(composite,SWT.PUSH);
            disconnectButton.setText("disconnect");
            disconnectButton.addSelectionListener(this);               
            disconnectButton.setEnabled(false);
            getButton = new Button(composite,SWT.NONE);
            getButton.setText("get");
            getButton.addSelectionListener(this);
            getButton.setEnabled(false);          
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
            shell.addDisposeListener(this);
            shell.pack();
            shell.open();
        }
        
        /* (non-Javadoc)
         * @see org.eclipse.swt.events.DisposeListener#widgetDisposed(org.eclipse.swt.events.DisposeEvent)
         */
        @Override
        public void widgetDisposed(DisposeEvent e) {
            isDisposed = true;
        }
        /* (non-Javadoc)
         * @see org.eclipse.swt.events.SelectionListener#widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent)
         */
        @Override
        public void widgetDefaultSelected(SelectionEvent arg0) {
            widgetSelected(arg0);
        }
        /* (non-Javadoc)
         * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
         */
        @Override
        public void widgetSelected(SelectionEvent arg0) {
            if(isDisposed) return;
            Object object = arg0.getSource(); 
            if(object==connectButton) {
                ConnectChannel connectChannel = ConnectChannelFactory.create(shell, this);
                connectChannel.connect();
            } else if(object==processButton) {
                isProcess = processButton.getSelection();
            } else if(object==createRequestButton) {
                CreateRequest createRequest = CreateRequestFactory.create(shell, channel, this);
                createRequest.create();
            } else if(object==disconnectButton) {
                get.disconnect();
                connectButton.setEnabled(true);
                disconnectButton.setEnabled(false);
                processButton.setEnabled(true);
                channel = null;
            } else if(object==getButton) {
                get.get();
                return;
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requester#getRequesterName()
         */
        @Override
        public String getRequesterName() {
            return requester.getRequesterName();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requester#message(java.lang.String, org.epics.ioc.util.MessageType)
         */
        @Override
        public void message(String message, MessageType messageType) {
            requester.message(message, messageType);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelRequester#channelStateChange(org.epics.ioc.ca.Channel, boolean)
         */
        @Override
        public void channelStateChange(Channel c, boolean isConnected) {
            display.asyncExec(this);
        }
        /* (non-Javadoc)
         * @see org.epics.pvData.channelAccess.ChannelRequester#channelCreated(org.epics.pvData.channelAccess.Channel)
         */
        @Override
        public void channelCreated(Channel channel) {
            this.channel = channel;
            message("channel created",MessageType.info);
            display.asyncExec(this);
        }
        /* (non-Javadoc)
         * @see org.epics.pvData.channelAccess.ChannelRequester#channelNotCreated()
         */
        @Override
        public void channelNotCreated() {
            message("channelNotCreated",MessageType.error);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.swtshell.CreateRequestRequester#request(org.epics.pvData.pv.PVStructure, boolean)
         */
        @Override
        public void request(PVStructure pvRequest, boolean isShared) {
            this.pvRequest = pvRequest;
            get.connect(isShared, isProcess);
            getButton.setEnabled(true);
            createRequestButton.setEnabled(false);
            processButton.setEnabled(false);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelRequester#disconnect(org.epics.ioc.ca.Channel)
         */
        @Override
        public void destroy(Channel c) {
            display.asyncExec(this);
        }
        /* (non-Javadoc)
         * @see java.lang.Runnable#run()
         */
        @Override
        public void run() {
            if(isDisposed) {
                if(channel!=null) channel.destroy();
                return;
            }
            if(channel==null) return;
            boolean isConnected = channel.isConnected();
            if(isConnected) {
                connectButton.setEnabled(false);
                disconnectButton.setEnabled(true);
                createRequestButton.setEnabled(true);
            } else {
                get.disconnect();
                connectButton.setEnabled(true);
                createRequestButton.setEnabled(false);
                disconnectButton.setEnabled(false);
                processButton.setEnabled(false);
                getButton.setEnabled(false);
            }
        }
        
        private enum GetRunRequest {
            connect,
            disconnect,
            get
        }
        
        private class Get implements
        Runnable,
        ChannelGetRequester
        {
            boolean isConnected = false;
            private ExecutorNode executorNode = executor.createNode(this);
            private ChannelGet channelGet = null;
            private PVStructure pvStructure = null;
            private BitSet changeBitSet = null;
            private GetRunRequest runRequest;
            private boolean isShared;
            private boolean process;

            private void connect(boolean isShared,boolean process) {
                if(isConnected) {
                    requester.message("already connected", MessageType.warning);
                    return;
                }
                this.isShared = isShared;
                this.process = process;
                runRequest = GetRunRequest.connect;
                executor.execute(executorNode);
            }
            
            private void disconnect() {
                if(!isConnected) {
                    requester.message("not connected", MessageType.warning);
                    return;
                }
                runRequest = GetRunRequest.disconnect;
                executor.execute(executorNode);
            }
            
            private void get() {
                runRequest = GetRunRequest.get;
                executor.execute(executorNode);
            }
            /* (non-Javadoc)
             * @see java.lang.Runnable#run()
             */
            @Override
            public void run() {
                switch(runRequest) {
                case connect:
                    channel.createChannelGet(this, pvRequest, pvRequest.getField().getFieldName(),isShared,process);
                    break;
                case disconnect:
                    channelGet.destroy();
                    channel.destroy();
                    isConnected = false;
                    break;
                case get:
                    channelGet.get(false);
                    break;
                }
                
            }
            /* (non-Javadoc)
             * @see org.epics.pvData.channelAccess.ChannelGetRequester#channelGetConnect(org.epics.pvData.channelAccess.ChannelGet, org.epics.pvData.pv.PVStructure)
             */
            @Override
            public void channelGetConnect(ChannelGet channelGet,PVStructure pvStructure) {
                this.channelGet = channelGet;
                this.pvStructure = pvStructure;
                changeBitSet = channelGet.getBitSet();
                isConnected = true;
            }

            /* (non-Javadoc)
             * @see org.epics.pvData.channelAccess.ChannelGetRequester#getDone(boolean)
             */
            @Override
            public void getDone(boolean success) {
                display.asyncExec( new Runnable() {
                    public void run() {
                        PrintModified printModified = PrintModifiedFactory.create(pvStructure, changeBitSet, null, consoleText);
                        printModified.print();
                    }

                });
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.util.Requester#getRequesterName()
             */
            @Override
            public String getRequesterName() {
                return requester.getRequesterName();
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.util.Requester#message(java.lang.String, org.epics.ioc.util.MessageType)
             */
            @Override
            public void message(final String message, final MessageType messageType) {
                requester.message(message, MessageType.info);
            }           
        }
    }
}
