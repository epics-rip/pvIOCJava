/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.swtshell;

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
import org.epics.ca.channelAccess.client.ChannelPutGet;
import org.epics.ca.channelAccess.client.ChannelPutGetRequester;
import org.epics.ca.channelAccess.client.ChannelRequester;
import org.epics.ca.channelAccess.client.Channel.ConnectionState;
import org.epics.pvData.misc.BitSet;
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
        PutGetImpl channelPutGetImpl = new PutGetImpl();
        channelPutGetImpl.start(display);
    }
    

    private static class PutGetImpl implements DisposeListener,SelectionListener
    
    {
     // following are global to embedded classes
        private enum State{
            readyForConnect,connecting,
            readyForCreatePutRequest,creatingPutRequest,
            readyForCreateGetRequest,creatingGetRequest,
            readyForCreatePutGet,creatingPutGet,
            ready,putActive
        };
        private StateMachine stateMachine = new StateMachine();
        private ChannelClient channelClient = new ChannelClient();
        private Requester requester = null;
        private boolean isDisposed = false;

        private static String windowName = "putGet";
        private Shell shell = null;
        private Button connectButton;
        private Button processButton;
        private Button createPutRequestButton = null;
        private Button createGetRequestButton = null;
        private Button createPutGetButton = null;
        private Button putGetButton;
        private Text consoleText = null;
        private boolean isProcess = false;
        
        
        private void start(Display display) {
            shell = new Shell(display);
            shell.setText(windowName);
            GridLayout gridLayout = new GridLayout();
            gridLayout.numColumns = 1;
            shell.setLayout(gridLayout);
            Composite composite = new Composite(shell,SWT.BORDER);
            RowLayout rowLayout = new RowLayout(SWT.HORIZONTAL);
            composite.setLayout(rowLayout);
            connectButton = new Button(composite,SWT.PUSH);
            connectButton.setText("disconnect");
            connectButton.addSelectionListener(this);               

            processButton = new Button(composite,SWT.CHECK);
            processButton.setText("process");
            processButton.setSelection(false);
            processButton.addSelectionListener(this);               

            createPutRequestButton = new Button(composite,SWT.PUSH);
            createPutRequestButton.setText("createPutRequest");
            createPutRequestButton.addSelectionListener(this);               
            
            createGetRequestButton = new Button(composite,SWT.PUSH);
            createGetRequestButton.setText("createGetRequest");
            createGetRequestButton.addSelectionListener(this);
            
            createPutGetButton = new Button(composite,SWT.PUSH);
            createPutGetButton.setText("destroyPutGet");
            createPutGetButton.addSelectionListener(this);
           

            putGetButton = new Button(composite,SWT.NONE);
            putGetButton.setText("putGet");
            putGetButton.addSelectionListener(this);

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
            stateMachine.setState(State.readyForConnect);
            shell.open();
            shell.addDisposeListener(this);
        }
        /* (non-Javadoc)
         * @see org.eclipse.swt.events.DisposeListener#widgetDisposed(org.eclipse.swt.events.DisposeEvent)
         */
        public void widgetDisposed(DisposeEvent e) {
            isDisposed = true;
            channelClient.disconnect();
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
                State state = stateMachine.getState();
                if(state==State.readyForConnect) {
                    stateMachine.setState(State.connecting);
                    channelClient.connect(shell);
                } else {
                    channelClient.disconnect();
                    stateMachine.setState(State.readyForConnect);
                }
            } else if(object==processButton) {
                isProcess = processButton.getSelection();
            } else if(object==createPutRequestButton) {
               stateMachine.setState(State.creatingPutRequest);
               channelClient.createPutRequest(shell);
            } else if(object==createGetRequestButton) {
                stateMachine.setState(State.creatingGetRequest);
                channelClient.createGetRequest(shell);
            } else if(object==createPutGetButton) {
                stateMachine.setState(State.creatingPutGet);
                channelClient.createPutGet(isProcess);
            } else if(object==putGetButton) {
                GUIData guiData = GUIDataFactory.create(shell);
                guiData.get(channelClient.getPutPVStructure(),channelClient.getPutBitSet());
                channelClient.putGet();
            }
        }
        
        private class StateMachine {
            private State state = null;
            
            void setState(State newState) {
                if(isDisposed) return;
                state = newState;
                switch(state) {
                case readyForConnect:
                    connectButton.setText("connect");
                    createPutGetButton.setText("createPutGet");
                    processButton.setEnabled(true);
                    createPutRequestButton.setEnabled(false);
                    createGetRequestButton.setEnabled(false);
                    createPutGetButton.setEnabled(false);
                    putGetButton.setEnabled(false);
                    return;
                case connecting:
                    connectButton.setText("disconnect");
                    createPutGetButton.setText("createPutGet");
                    processButton.setEnabled(true);
                    createPutRequestButton.setEnabled(false);
                    createGetRequestButton.setEnabled(false);
                    createPutGetButton.setEnabled(false);
                    putGetButton.setEnabled(false);
                    return;
                case readyForCreatePutRequest:
                    connectButton.setText("disconnect");
                    createPutGetButton.setText("createPutGet");
                    processButton.setEnabled(true);
                    createPutRequestButton.setEnabled(true);
                    createGetRequestButton.setEnabled(false);
                    createPutGetButton.setEnabled(false);
                    putGetButton.setEnabled(false);
                    return;
                case creatingPutRequest:
                    connectButton.setText("disconnect");
                    createPutGetButton.setText("createPutGet");
                    processButton.setEnabled(true);
                    createPutRequestButton.setEnabled(false);
                    createPutGetButton.setEnabled(false);
                    putGetButton.setEnabled(false);
                    return;
                case readyForCreateGetRequest:
                    connectButton.setText("disconnect");
                    createPutGetButton.setText("createPutGet");
                    processButton.setEnabled(true);
                    createPutRequestButton.setEnabled(false);
                    createGetRequestButton.setEnabled(true);
                    createPutGetButton.setEnabled(false);
                    putGetButton.setEnabled(false);
                    return;
                case creatingGetRequest:
                    connectButton.setText("disconnect");
                    createPutGetButton.setText("createPutGet");
                    processButton.setEnabled(true);
                    createPutRequestButton.setEnabled(false);
                    createPutGetButton.setEnabled(false);
                    putGetButton.setEnabled(false);
                    return;
                case readyForCreatePutGet:
                    connectButton.setText("disconnect");
                    createPutGetButton.setText("createPutGet");
                    processButton.setEnabled(true);
                    createPutRequestButton.setEnabled(false);
                    createGetRequestButton.setEnabled(false);
                    createPutGetButton.setEnabled(true);
                    putGetButton.setEnabled(false);
                    return;
                case creatingPutGet:
                    connectButton.setText("disconnect");
                    createPutGetButton.setText("destroyPutGet");
                    processButton.setEnabled(true);
                    createPutRequestButton.setEnabled(false);
                    createPutGetButton.setEnabled(true);
                    putGetButton.setEnabled(false);
                    return;
                case ready:
                    connectButton.setText("disconnect");
                    createPutGetButton.setText("destroyPutGet");
                    processButton.setEnabled(true);
                    createPutRequestButton.setEnabled(false);
                    createGetRequestButton.setEnabled(false);
                    createPutGetButton.setEnabled(true);
                    putGetButton.setEnabled(true);
                    return;
                case putActive:
                    connectButton.setText("disconnect");
                    createPutGetButton.setText("destroyPutGet");
                    processButton.setEnabled(true);
                    createPutRequestButton.setEnabled(false);
                    createGetRequestButton.setEnabled(false);
                    createPutGetButton.setEnabled(true);
                    putGetButton.setEnabled(false);
                    return;
                }
            }
            State getState() {return state;}
        }
        
        private enum RunCommand {
            channelConnected,timeout,destroy,putRequestDone,getRequestDone,getPutDone,putGetDone
        }
       
        
        private class ChannelClient implements
        ChannelRequester,ConnectChannelRequester,CreateRequestRequester,Runnable,ChannelPutGetRequester
        {
            private Channel channel = null;
            private ConnectChannel connectChannel = null;
            private PVStructure pvPutRequest = null;
            private PVStructure pvGetRequest = null;
            private ChannelPutGet channelPutGet = null;
            private PVStructure pvPutStructure = null;
            private BitSet putBitSet = null;
            private PVStructure pvGetStructure = null;
            private BitSet getBitSet = null;
            private RunCommand runCommand = null;
            private boolean isPutRequest = true;
            private boolean putIsShared = false;
            private boolean getIsShared = false;
            private PrintModified printModified = null;

            void connect(Shell shell) {
                if(connectChannel!=null) {
                    message("connect in propress",MessageType.error);
                }
                connectChannel = ConnectChannelFactory.create(shell, this,this);
                connectChannel.connect();
            }
            void createPutRequest(Shell shell) {
                isPutRequest = true;
                CreateRequest createRequest = CreateRequestFactory.create(shell, channel, this);
                createRequest.create();
            }
            void createGetRequest(Shell shell) {
                isPutRequest = false;
                CreateRequest createRequest = CreateRequestFactory.create(shell, channel, this);
                createRequest.create();
            }
            void createPutGet(boolean process) {
                channelPutGet = channel.createChannelPutGet(
                    this,
                    pvPutRequest,"arguments",putIsShared,
                    pvGetRequest,"result", getIsShared, process,null);
                return;
            }
            void destroyPutGet() {
                ChannelPutGet channelPutGet = this.channelPutGet;
                if(channelPutGet!=null) {
                    this.channelPutGet = null;
                    channelPutGet.destroy();
                }
            }
            void disconnect() {
                Channel channel = this.channel;
                if(channel!=null) {
                    this.channel = null;
                    channel.destroy();
                }
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
                channelPutGet.putGet(false);
            }
            
            /* (non-Javadoc)
             * @see org.epics.ca.channelAccess.client.ChannelRequester#channelStateChange(org.epics.ca.channelAccess.client.Channel, org.epics.ca.channelAccess.client.Channel.ConnectionState)
             */
            @Override
            public void channelStateChange(Channel c, ConnectionState state) {
            	
            	if(state == ConnectionState.DESTROYED) {
                    this.channel = null;
                    runCommand = RunCommand.destroy;
                    shell.getDisplay().asyncExec(this);
            	}

            	if(state != ConnectionState.CONNECTED) {
                    message("channel " + state,MessageType.error);
                    return;
                }
            	
                channel = c;
                ConnectChannel connectChannel = this.connectChannel;
                if(connectChannel!=null) {
                    connectChannel.cancelTimeout();
                    this.connectChannel = null;
                }
                runCommand = RunCommand.channelConnected;
                shell.getDisplay().asyncExec(this);
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
                channel = c;;
                c.connect();
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.swtshell.ConnectChannelRequester#timeout()
             */
            @Override
            public void timeout() {
                Channel channel = this.channel;
                if(channel!=null) {
                    this.channel = null;
                    channel.destroy();
                }
                message("channel connect timeout",MessageType.info);
                runCommand = RunCommand.destroy;
                shell.getDisplay().asyncExec(this);
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.swtshell.CreateRequestRequester#request(org.epics.pvData.pv.PVStructure, boolean)
             */
            @Override
            public void request(PVStructure pvRequest, boolean isShared) {
                if(isPutRequest) {
                    pvPutRequest = pvRequest;
                    putIsShared = isShared;
                    runCommand = RunCommand.putRequestDone;
                } else {
                    pvGetRequest = pvRequest;
                    getIsShared = isShared;
                    runCommand = RunCommand.getRequestDone;
                }
                
                shell.getDisplay().asyncExec(this);
            }
            /* (non-Javadoc)
             * @see org.epics.ca.channelAccess.client.ChannelPutGetRequester#channelPutGetConnect(Status,org.epics.ca.channelAccess.client.ChannelPutGet, org.epics.pvData.pv.PVStructure, org.epics.pvData.misc.BitSet, org.epics.pvData.pv.PVStructure, org.epics.pvData.misc.BitSet)
             */
            @Override
            public void channelPutGetConnect(Status status,ChannelPutGet channelPutGet,
                    PVStructure pvPutStructure, PVStructure pvGetStructure)
            {
                if (!status.isOK()) {
                	message(status.toString(), status.isSuccess() ? MessageType.warning : MessageType.error);
                	if (!status.isSuccess()) return;
                }
                this.channelPutGet = channelPutGet;
                this.pvPutStructure = pvPutStructure;
                this.pvGetStructure = pvGetStructure;
                putBitSet = new BitSet(pvPutStructure.getNumberFields());
                getBitSet = new BitSet(pvGetStructure.getNumberFields());
                channelPutGet.getPut();
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
                runCommand = RunCommand.getPutDone;
                shell.getDisplay().asyncExec(this);
            }

            /* (non-Javadoc)
             * @see org.epics.ca.channelAccess.client.ChannelPutGetRequester#putGetDone(Status)
             */
            @Override
            public void putGetDone(Status success) {
                runCommand = RunCommand.putGetDone;
                shell.getDisplay().asyncExec(this);
            }
            /* (non-Javadoc)
             * @see java.lang.Runnable#run()
             */
            @Override
            public void run() {
                switch(runCommand) {
                case channelConnected:
                    stateMachine.setState(State.readyForCreatePutRequest);
                    return;
                case timeout:
                    stateMachine.setState(State.readyForConnect);
                    return;
                case destroy:
                    stateMachine.setState(State.readyForConnect);
                    return;
                case putRequestDone:
                    stateMachine.setState(State.readyForCreateGetRequest);
                    return;
                case getRequestDone:
                    stateMachine.setState(State.readyForCreatePutGet);
                    return;
                case getPutDone:
                    printModified = PrintModifiedFactory.create(
                            channel.getChannelName(),pvGetStructure,getBitSet, null, consoleText);
                    stateMachine.setState(State.ready);
                    return;
                case putGetDone:
                    stateMachine.setState(State.ready);
                    getBitSet.clear();
                    getBitSet.set(0);
                    printModified.print();
                    return;
                }
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
