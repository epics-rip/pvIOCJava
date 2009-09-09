/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.swtshell;

import java.util.concurrent.atomic.AtomicReference;

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
import org.epics.ca.channelAccess.client.Channel;
import org.epics.ca.channelAccess.client.ChannelGet;
import org.epics.ca.channelAccess.client.ChannelPut;
import org.epics.ca.channelAccess.client.ChannelPutRequester;
import org.epics.ca.channelAccess.client.ChannelRequester;
import org.epics.pvData.misc.BitSet;
import org.epics.pvData.misc.Executor;
import org.epics.pvData.misc.ExecutorNode;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVStructure;
import org.epics.pvData.pv.Requester;
import org.epics.pvData.pv.Status;

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

    private static class ChannelPutImpl implements DisposeListener,SelectionListener,ChannelRequester,ConnectChannelRequester,CreateRequestRequester,Runnable  {

        private ChannelPutImpl(Display display) {
            this.display = display;
        }

        private boolean isDisposed = false;
        private static String windowName = "put";
        private ExecutorNode executorNode = executor.createNode(this);
        private Display display;
        private Shell shell = null;
        private Requester requester = null;
        private Button connectButton;
        private Button processButton;
        private Button createRequestButton = null;
        private Button disconnectButton;
        private Button putButton;
        private Text consoleText = null;
        private PVStructure pvRequest = null;
        private Put put = new Put();
        private boolean isProcess = false;
        private AtomicReference<Channel> channel = new AtomicReference<Channel>(null);
        private AtomicReference<ConnectChannel> connectChannel = new AtomicReference<ConnectChannel>(null);
        
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
            executor.execute(executorNode);
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
                Channel channel = this.channel.get();
                if(channel!=null) {
                    message("must disconnect first",MessageType.error);
                    return;
                }
                connectChannel.set(ConnectChannelFactory.create(shell,this, this));
                connectChannel.get().connect();
            } else if(object==processButton) {
                isProcess = processButton.getSelection();
            } else if(object==createRequestButton) {
                Channel channel = this.channel.get();
                if(channel==null) {
                    message("channel not connected",MessageType.error);
                    return;
                }
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
            if(!isConnected) {
                message("channel disconnected",MessageType.error);
                return;
            }
            channel.set(c);
            ConnectChannel connectChannel = this.connectChannel.getAndSet(null);
            if(connectChannel!=null) connectChannel.cancelTimeout();
            display.asyncExec(this);
        }
        /* (non-Javadoc)
         * @see org.epics.ca.channelAccess.client.ChannelRequester#channelCreated(org.epics.pvData.pv.Status, org.epics.ca.channelAccess.client.Channel)
         */
        @Override
        public void channelCreated(Status status,Channel c) {
            if (!status.isOK()) {
                message(status.toString(),MessageType.error);
                return;
            }
            channel.set(c);
            c.connect();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelListener#disconnect(org.epics.ioc.ca.Channel)
         */
        public void destroy(Channel c) {
            display.asyncExec(this);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.swtshell.ConnectChannelRequester#timeout()
         */
        @Override
        public void timeout() {
            Channel channel = this.channel.getAndSet(null);
            if(channel!=null) channel.destroy();
            message("channel connect timeout",MessageType.info);
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
         * @see java.lang.Runnable#run()
         */
        @Override
        public void run() {
            Channel channel = null;
            if(isDisposed) {
                channel = this.channel.getAndSet(null);
                if(channel!=null) channel.destroy();
                return;
            }
            channel = this.channel.get();
            if(channel==null) return;
            boolean isConnected = channel.isConnected();
            if(isConnected) {
                connectButton.setEnabled(false);
                disconnectButton.setEnabled(true);
                if(ConnectChannelFactory.pvDataCompatible(channel)) {
                    createRequestButton.setEnabled(true);
                } else {
                    put.connect(false, false);
                    putButton.setEnabled(true);
                }
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
            create,
            disconnect,
            put
        }
        
        private class Put implements
        Runnable,
        ChannelPutRequester
        {
            private ExecutorNode executorNode = executor.createNode(this);
            private AtomicReference<ChannelPut> channelPut = new AtomicReference<ChannelPut>(null);
            private PVStructure pvStructure = null;
            private BitSet changeBitSet = null;
            private PutRunRequest runRequest;
            private boolean isShared;
            private boolean process;

            void connect(boolean isShared,boolean process) {
                this.isShared = isShared;
                this.process = process;
                runRequest = PutRunRequest.create;
                executor.execute(executorNode);
            }
            
            void disconnect() {
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
                Channel c = channel.get();
                if(c==null) return;
                switch(runRequest) {
                case create:
                    String structureName = "";
                    if(pvRequest!=null) structureName = pvRequest.getField().getFieldName();
                    channelPut.compareAndSet(null,c.createChannelPut(this, pvRequest, structureName,isShared,process,null));
                    return;
                case disconnect:
                    c.destroy();
                    channel.set(null);
                    channelPut.set(null);
                    return;
                case put:
                    ChannelPut channelPut = this.channelPut.get();
                    if(channelPut!=null) channelPut.put(false);
                    return;
                }
                
            }
            /* (non-Javadoc)
             * @see org.epics.ca.channelAccess.client.ChannelPutRequester#channelPutConnect(Status,org.epics.ca.channelAccess.client.ChannelPut, org.epics.pvData.pv.PVStructure, org.epics.pvData.misc.BitSet)
             */
            @Override
            public void channelPutConnect(Status status,ChannelPut channelPut,PVStructure pvStructure,BitSet bitSet) {
                if (!status.isOK()) {
                	message(status.toString(),MessageType.error);
                	return;
                }
                this.channelPut.compareAndSet(null, channelPut);
                this.pvStructure = pvStructure;
                changeBitSet = bitSet;
                channelPut.get();
            }
            /* (non-Javadoc)
             * @see org.epics.ca.channelAccess.client.ChannelPutRequester#putDone(Status)
             */
            @Override
            public void putDone(Status status) {
                if (!status.isOK()) {
                	message(status.toString(),MessageType.error);
                	return;
                }
                display.asyncExec( new Runnable() {
                    public void run() {
                        PrintModified printModified = PrintModifiedFactory.create(pvStructure, changeBitSet, null, consoleText);
                        printModified.print();
                    }

                });
            }
            /* (non-Javadoc)
             * @see org.epics.ca.channelAccess.client.ChannelPutRequester#getDone(Status)
             */
            @Override
            public void getDone(Status status) {
                if (!status.isOK()) {
                	message(status.toString(),MessageType.error);
                	return;
                }
                // TODO what to do?
            }
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
