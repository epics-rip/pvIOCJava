/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.swtshell;

import java.util.BitSet;
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
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.epics.ioc.channelAccess.*;


import org.epics.ioc.util.RequestResult;
import org.epics.pvData.misc.Executor;
import org.epics.pvData.misc.ExecutorFactory;
import org.epics.pvData.misc.ExecutorNode;
import org.epics.pvData.misc.ThreadPriority;
import org.epics.pvData.pv.*;
import org.epics.pvData.channelAccess.*;

/*
 * A shell for channelGet.
 * @author mrk
 *
 */
public class PutFactory {

    /**
     * Create the shell.
     * @param display The display.
     */
    public static void init(Display display) {
        ChannelPutImpl channelPutImpl = new ChannelPutImpl(display);
        channelPutImpl.start();
    }
    
    private static Executor executor = SwtshellFactory.getExecutor();

    private static class ChannelPutImpl implements DisposeListener,Requester,CreateRequestRequester,SelectionListener,Runnable  {

        private ChannelPutImpl(Display display) {
            this.display = display;
        }

        private boolean isDisposed = false;
        private static String windowName = "put";
        private Display display;
        private Shell shell = null;
        private Requester requester = null;
        private Channel channel = null;
        private Button connectButton;
        private Button processButton;
        private Button createRequestButton = null;
        private Button disconnectButton;
        private Button putButton;
        private Text consoleText = null;
        private PVStructure pvRequest = null;
        private Put put = new Put();
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
            putButton = new Button(composite,SWT.NONE);
            putButton.setText("put");
            putButton.addSelectionListener(this);
            putButton.setEnabled(false);
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
         * @see org.eclipse.swt.events.DisposeListener#widgetDisposed(org.eclipse.swt.events.DisposeEvent)
         */
        public void widgetDisposed(DisposeEvent e) {
            isDisposed = true;
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
                put.disconnect();
                connectButton.setEnabled(true);
                disconnectButton.setEnabled(false);
                processButton.setEnabled(true);
                channel = null;
            } else if(object==putButton) {
                GUIData guiData = GUIDataFactory.create(shell);
                guiData.get(put.getPVStructure(), put.getBitSet());
                put.put();
                return;
            }
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
            display.asyncExec(this);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelListener#disconnect(org.epics.ioc.ca.Channel)
         */
        public void destroy(Channel c) {
            display.asyncExec(this);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.swtshell.CreateRequestRequester#request(org.epics.pvData.pv.PVStructure, boolean)
         */
        @Override
        public void request(PVStructure pvRequest, boolean isShared) {
            this.pvRequest = pvRequest;
            put.connect(isShared, isProcess);
            putButton.setEnabled(true);
            createRequestButton.setEnabled(false);
            processButton.setEnabled(false);
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
                put.disconnect();
                connectButton.setEnabled(true);
                createRequestButton.setEnabled(false);
                disconnectButton.setEnabled(false);
                processButton.setEnabled(false);
                putButton.setEnabled(false);
            }
        }
        
        private enum PutRunRequest {
            connect,
            disconnect,
            put
        }
        
        private class Put implements
        Runnable,
        ChannelPutRequester
        {
            boolean isConnected = false;
            private ExecutorNode executorNode = executor.createNode(this);
            private ChannelPut channelPut = null;
            private PVStructure pvStructure = null;
            private BitSet changeBitSet = null;
            private PutRunRequest runRequest;
            private boolean isShared;
            private boolean process;

            void connect(boolean isShared,boolean process) {
                if(isConnected) {
                    requester.message("already connected", MessageType.warning);
                    return;
                }
                this.isShared = isShared;
                this.process = process;
                runRequest = PutRunRequest.connect;
                executor.execute(executorNode);
            }
            
            void disconnect() {
                if(!isConnected) {
                    requester.message("not connected", MessageType.warning);
                    return;
                }
                runRequest = PutRunRequest.disconnect;
                executor.execute(executorNode);
            }
            
            PVStructure getPVStructure() {
                return pvStructure;
            }
            
            BitSet getBitSet() {
                return changeBitSet;
            }
            
            private void put() {
                runRequest = PutRunRequest.put;
                executor.execute(executorNode);
            }
            /* (non-Javadoc)
             * @see java.lang.Runnable#run()
             */
            @Override
            public void run() {
                switch(runRequest) {
                case connect:
                    channel.createChannelPut(this, pvRequest, pvRequest.getField().getFieldName(),isShared,process);
                    break;
                case disconnect:
                    channelPut.destroy();
                    channel.destroy();
                    isConnected = false;
                    break;
                case put:
                    channelPut.put(false);
                    break;
                }
                
            }
            /* (non-Javadoc)
             * @see org.epics.pvData.channelAccess.ChannelPutRequester#channelPutConnect(org.epics.pvData.channelAccess.ChannelPut, org.epics.pvData.pv.PVStructure)
             */
            @Override
            public void channelPutConnect(ChannelPut channelPut,PVStructure pvStructure) {
                this.channelPut = channelPut;
                this.pvStructure = pvStructure;
                changeBitSet = channelPut.getBitSet();
                isConnected = true;
                channelPut.get();
            }
            /* (non-Javadoc)
             * @see org.epics.pvData.channelAccess.ChannelPutRequester#putDone(boolean)
             */
            @Override
            public void putDone(boolean success) {
                display.asyncExec( new Runnable() {
                    public void run() {
                        PrintModified printModified = PrintModifiedFactory.create(pvStructure, changeBitSet, null, consoleText);
                        printModified.print();
                    }

                });
            }
            /* (non-Javadoc)
             * @see org.epics.pvData.channelAccess.ChannelPutRequester#getDone(boolean)
             */
            @Override
            public void getDone(boolean success) {}

            /* (non-Javadoc)
             * @see org.epics.ioc.util.Requester#putRequesterName()
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
