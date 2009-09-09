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
import org.epics.ca.channelAccess.client.ChannelPutGet;
import org.epics.ca.channelAccess.client.ChannelPutGetRequester;
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
public class PutGetFactory {

    /**
     * Create the shell.
     * @param display The display.
     */
    public static void init(Display display) {
        PutGetImpl channelPutGetImpl = new PutGetImpl(display);
        channelPutGetImpl.start();
    }
    
    private static Executor executor = SwtshellFactory.getExecutor();

    private static class PutGetImpl implements DisposeListener,SelectionListener,ChannelRequester,ConnectChannelRequester,CreateRequestRequester,Runnable 
    {

        private PutGetImpl(Display display) {
            this.display = display;
        }
        
        private enum RunRequest {
            channelStateChange,
            dispose,
            putGetConnect,
            getPutDone,
            putGetDone
        }
        private boolean isDisposed = false;
        private static String windowName = "putGet";
        private ExecutorNode executorNode = executor.createNode(this);
        private Display display;
        private Shell shell = null;
        private Requester requester = null;
        private Button connectButton;
        private Button processButton;
        private Button createPutRequestButton = null;
        private Button createGetRequestButton = null;
        private Button disconnectButton;
        private Button putGetButton;
        private Text consoleText = null;
        private PVStructure pvPutRequest = null;
        private PVStructure pvGetRequest = null;
        private PutGet putGet = new PutGet();
        private boolean isProcess = false;
        private boolean createPutRequest = false;
        private RunRequest runRequest = null;
        private PrintModified printModified = null;
        private Status success;
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
            createPutRequestButton = new Button(composite,SWT.PUSH);
            createPutRequestButton.setText("createPutRequest");
            createPutRequestButton.addSelectionListener(this);               
            createPutRequestButton.setEnabled(false);
            createGetRequestButton = new Button(composite,SWT.PUSH);
            createGetRequestButton.setText("createGetRequest");
            createGetRequestButton.addSelectionListener(this);               
            createGetRequestButton.setEnabled(false);
            disconnectButton = new Button(composite,SWT.PUSH);
            disconnectButton.setText("disconnect");
            disconnectButton.addSelectionListener(this);               
            disconnectButton.setEnabled(false);
            putGetButton = new Button(composite,SWT.NONE);
            putGetButton.setText("putGet");
            putGetButton.addSelectionListener(this);
            putGetButton.setEnabled(false);
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
            runRequest = RunRequest.dispose;
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
            } else if(object==createPutRequestButton) {
                Channel channel = this.channel.get();
                if(channel==null) {
                    message("channel not connected",MessageType.error);
                    return;
                }
                createPutRequest = true;
                CreateRequest createRequest = CreateRequestFactory.create(shell, channel, this);
                createRequest.create();
            } else if(object==createGetRequestButton) {
                Channel channel = this.channel.get();
                if(channel==null) {
                    message("channel not connected",MessageType.error);
                    return;
                }
                createPutRequest = false;
                CreateRequest createRequest = CreateRequestFactory.create(shell, channel, this);
                createRequest.create();
            } else if(object==disconnectButton) {
                putGet.disconnect();
                connectButton.setEnabled(true);
                disconnectButton.setEnabled(false);
                processButton.setEnabled(true);
            } else if(object==putGetButton) {
                GUIData guiData = GUIDataFactory.create(shell);
                guiData.get(putGet.getPutPVStructure(), putGet.getPutBitSet());
                putGet.putGet();
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
         * @see org.epics.ca.channelAccess.client.ChannelRequester#channelStateChange(org.epics.ca.channelAccess.client.Channel, boolean)
         */
        @Override
        public void channelStateChange(Channel c, boolean isConnected) {
            if(!isConnected) {
                message("channel disconnected",MessageType.error);
                return;
            }
            channel.set(c);
            ConnectChannel connectChannel = this.connectChannel.getAndSet(null);
            if(connectChannel!=null) connectChannel.cancelTimeout();
            runRequest = RunRequest.channelStateChange;
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
         * @see org.epics.ioc.ca.ChannelRequester#disconnect(org.epics.ioc.ca.Channel)
         */
        @Override
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
            if(createPutRequest) {
                pvPutRequest = pvRequest;
                createPutRequestButton.setEnabled(false);
                createGetRequestButton.setEnabled(true);
            } else {
                pvGetRequest = pvRequest;
                createGetRequestButton.setEnabled(false);
                processButton.setEnabled(false);
                putGet.connect(isProcess);
            }
        }
        void putGetConnect() {
            runRequest = RunRequest.putGetConnect;
            display.asyncExec(this);
        }
        void putGetCallback(Status success) {
            this.success = success;
            runRequest = RunRequest.putGetDone;
            display.asyncExec(this);
        }
        void getPutCallback(Status success) {
            this.success = success;
            runRequest = RunRequest.getPutDone;
            display.asyncExec(this);
        }
        
        /* (non-Javadoc)
         * @see java.lang.Runnable#run()
         */
        @Override
        public void run() {
            Channel channel = this.channel.get();
            if(channel==null) return;
            switch(runRequest) {
            case channelStateChange:
                boolean isConnected = channel.isConnected();
                if(isConnected) {
                    connectButton.setEnabled(false);
                    disconnectButton.setEnabled(true);
                    createPutRequestButton.setEnabled(true);
                } else {
                    putGet.disconnect();
                    connectButton.setEnabled(true);
                    createPutRequestButton.setEnabled(false);
                    createGetRequestButton.setEnabled(false);
                    disconnectButton.setEnabled(false);
                    processButton.setEnabled(false);
                    putGetButton.setEnabled(false);
                }
                return;
            case dispose:
                channel = this.channel.getAndSet(null);
                if(channel!=null) channel.destroy();
                return;
            case putGetConnect:
                message("putGetConnect",MessageType.info);
                putGetButton.setEnabled(true);
                printModified = PrintModifiedFactory.create(putGet.getGetPVStructure(), putGet.getGetBitSet(), null, consoleText);
                putGet.getPut();
                return;
            case getPutDone:
                if(success.isOK()) {
                    message("getPut done",MessageType.info);
                    putGetButton.setEnabled(true);
                } else {
                	message(success.toString(),MessageType.error);
                }
                return;
            case putGetDone:
                if(success.isOK()) {
                printModified.print();
                } else {
                	message(success.toString(),MessageType.error);
                }
                return;
            }
        }
        
        private enum PutGetRunRequest {
            create,
            disconnect,
            getPut,
            putGet
        }
        
        private class PutGet implements
        Runnable,
        ChannelPutGetRequester
        {
            private ExecutorNode executorNode = executor.createNode(this);
            private AtomicReference<ChannelPutGet> channelPutGet = new AtomicReference<ChannelPutGet>(null);
            private PVStructure pvPutStructure = null;
            private BitSet putBitSet = null;
            private PVStructure pvGetStructure = null;
            private BitSet getBitSet = null;
            private PutGetRunRequest runRequest;
            private boolean process;

            void connect(boolean process) {
                this.process = process;
                runRequest = PutGetRunRequest.create;
                executor.execute(executorNode);
            }
            
            void disconnect() {
                runRequest = PutGetRunRequest.disconnect;
                executor.execute(executorNode);
            }
            
            PVStructure getPutPVStructure() {
                return pvPutStructure;
            }
            
            BitSet getPutBitSet() {
                return putBitSet;
            }
            
            PVStructure getGetPVStructure() {
                return pvGetStructure;
            }
            
            BitSet getGetBitSet() {
                getBitSet.clear();
                getBitSet.set(0);
                return getBitSet;
            }
            
            void putGet() {
                runRequest = PutGetRunRequest.putGet;
                executor.execute(executorNode);
            }
            void getPut() {
                runRequest = PutGetRunRequest.getPut;
                executor.execute(executorNode);
            }
            
            /* (non-Javadoc)
             * @see java.lang.Runnable#run()
             */
            @Override
            public void run() {
                Channel c = channel.get();
                if(c==null) return;
                ChannelPutGet channelPutGet = this.channelPutGet.get();
                switch(runRequest) {
                case create:
                    this.channelPutGet.compareAndSet(null,c.createChannelPutGet(
                            this,pvPutRequest,
                            "arguments", true,pvGetRequest,
                            "result", true, process,null));
                    return;
                case disconnect:
                    c.destroy();
                    channel.set(null);
                    this.channelPutGet.set(null);
                    return;
                case getPut:
                    if(channelPutGet!=null) {
                        channelPutGet.getPut();
                    } else {
                        message("getPut request but channelPutGet is null",MessageType.error);
                    }
                    return;
                case putGet:
                    if(channelPutGet!=null)  {
                        channelPutGet.putGet(false);
                    } else {
                        message("getPut request but channelPutGet is null",MessageType.error);
                    }
                    return;
                   
                }
            }
            /* (non-Javadoc)
             * @see org.epics.ca.channelAccess.client.ChannelPutGetRequester#channelPutGetConnect(Status,org.epics.ca.channelAccess.client.ChannelPutGet, org.epics.pvData.pv.PVStructure, org.epics.pvData.misc.BitSet, org.epics.pvData.pv.PVStructure, org.epics.pvData.misc.BitSet)
             */
            @Override
            public void channelPutGetConnect(Status status,ChannelPutGet channelPutGet,
                    PVStructure pvPutStructure, PVStructure pvGetStructure)
            {
                if (!status.isOK()) {
                	message(status.toString(),MessageType.error);
                	return;
                }
                this.channelPutGet.compareAndSet(null,channelPutGet);
                this.pvPutStructure = pvPutStructure;
                this.pvGetStructure = pvGetStructure;
                putBitSet = new BitSet(pvPutStructure.getNumberFields());
                getBitSet = new BitSet(pvGetStructure.getNumberFields());
                putGetConnect();
            }
            /* (non-Javadoc)
             * @see org.epics.ca.channelAccess.client.ChannelPutGetRequester#getGetDone(Status)
             */
            @Override
            public void getGetDone(Status success) {}

            /* (non-Javadoc)
             * @see org.epics.ca.channelAccess.client.ChannelPutGetRequester#getPutDone(Status)
             */
            @Override
            public void getPutDone(Status success) {
                getPutCallback(success);
            }

            /* (non-Javadoc)
             * @see org.epics.ca.channelAccess.client.ChannelPutGetRequester#putGetDone(Status)
             */
            @Override
            public void putGetDone(Status success) {
                putGetCallback(success);
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
