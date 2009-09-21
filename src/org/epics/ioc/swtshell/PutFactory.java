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
import org.epics.ca.client.Channel;
import org.epics.ca.client.ChannelPut;
import org.epics.ca.client.ChannelPutRequester;
import org.epics.ca.client.ChannelRequester;
import org.epics.ca.client.Channel.ConnectionState;
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
public class PutFactory {

    /**
     * Create the shell.
     * @param display The display.
     */
    public static void init(Display display) {
        ChannelPutImpl channelPutImpl = new ChannelPutImpl();
        channelPutImpl.start(display);
    }

    private static class ChannelPutImpl implements DisposeListener,SelectionListener  {
        // following are global to embedded classes
        private enum State{
            readyForConnect,connecting,
            readyForCreateRequest,creatingRequest,
            readyForCreatePut,creatingPut,
            ready,putActive
        };
        private StateMachine stateMachine = new StateMachine();
        private ChannelClient channelClient = new ChannelClient();
        private Requester requester = null;
        private boolean isDisposed = false;
        
        private static String windowName = "put";
        private Shell shell = null;
        private Button connectButton;
        private Button processButton;
        private Button createRequestButton = null;
        private Button createPutButton;
        private Button putButton;
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

            createRequestButton = new Button(composite,SWT.PUSH);
            createRequestButton.setText("createRequest");
            createRequestButton.addSelectionListener(this);
            
            createPutButton = new Button(composite,SWT.PUSH);
            createPutButton.setText("destroyPut");
            createPutButton.addSelectionListener(this);
           
            putButton = new Button(composite,SWT.NONE);
            putButton.setText("put");
            putButton.addSelectionListener(this);

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
            } else if(object==createRequestButton) {
                stateMachine.setState(State.creatingRequest);
                channelClient.createRequest(shell);
            } else if(object==createPutButton) {
                State state = stateMachine.getState();
                if(state==State.readyForCreatePut) {
                    stateMachine.setState(State.creatingPut);
                    channelClient.createPut(isProcess);
                } else {
                    channelClient.destroyPut();
                    stateMachine.setState(State.readyForCreatePut);
                }
            } else if(object==putButton) {
               GUIData guiData = GUIDataFactory.create(shell);
               guiData.get(channelClient.getPVStructure(), channelClient.getBitSet());
               stateMachine.setState(State.putActive);
               channelClient.put();
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
                    createPutButton.setText("createPut");
                    processButton.setEnabled(true);
                    createRequestButton.setEnabled(false);
                    createPutButton.setEnabled(false);
                    putButton.setEnabled(false);
                    return;
                case connecting:
                    connectButton.setText("disconnect");
                    createPutButton.setText("createPut");
                    processButton.setEnabled(true);
                    createRequestButton.setEnabled(false);
                    createPutButton.setEnabled(false);
                    putButton.setEnabled(false);
                    return;
                case readyForCreateRequest:
                    connectButton.setText("disconnect");
                    createPutButton.setText("createPut");
                    processButton.setEnabled(true);
                    createRequestButton.setEnabled(true);
                    createPutButton.setEnabled(false);
                    putButton.setEnabled(false);
                    return;
                case creatingRequest:
                    connectButton.setText("disconnect");
                    createPutButton.setText("createPut");
                    processButton.setEnabled(true);
                    createRequestButton.setEnabled(false);
                    createPutButton.setEnabled(false);
                    putButton.setEnabled(false);
                    return;
                case readyForCreatePut:
                    connectButton.setText("disconnect");
                    createPutButton.setText("createPut");
                    processButton.setEnabled(true);
                    createRequestButton.setEnabled(false);
                    createPutButton.setEnabled(true);
                    putButton.setEnabled(false);
                    return;
                case creatingPut:
                    connectButton.setText("disconnect");
                    createPutButton.setText("destroyPut");
                    processButton.setEnabled(true);
                    createRequestButton.setEnabled(false);
                    createPutButton.setEnabled(true);
                    putButton.setEnabled(false);
                    return;
                case ready:
                    connectButton.setText("disconnect");
                    createPutButton.setText("destroyPut");
                    processButton.setEnabled(true);
                    createRequestButton.setEnabled(false);
                    createPutButton.setEnabled(true);
                    putButton.setEnabled(true);
                    return;
                case putActive:
                    connectButton.setText("disconnect");
                    createPutButton.setText("destroyPut");
                    processButton.setEnabled(true);
                    createRequestButton.setEnabled(false);
                    createPutButton.setEnabled(true);
                    putButton.setEnabled(false);
                    return;
                }
                
            }
            State getState() {return state;}
        }
        
        private enum RunCommand {
            channelConnected,timeout,destroy,channelrequestDone,channelPutConnect,putDone
        }
        
        
       
        
        private class ChannelClient implements
        ChannelRequester,ConnectChannelRequester,CreateRequestRequester,Runnable,ChannelPutRequester
        {
            private Channel channel = null;
            private ConnectChannel connectChannel = null;
            private PVStructure pvRequest = null;
            private boolean isShared = false;
            private ChannelPut channelPut = null;
            private BitSet bitSet = null;
            private RunCommand runCommand;
            private PVStructure pvStructure = null;


            void connect(Shell shell) {
                if(connectChannel!=null) {
                    message("connect in propress",MessageType.error);
                }
                connectChannel = ConnectChannelFactory.create(shell, this,this);
                connectChannel.connect();
            }
            void createRequest(Shell shell) {
                CreateRequest createRequest = CreateRequestFactory.create(shell, channel, this);
                createRequest.create();
            }
            void createPut(boolean process) {
                String structureName = pvRequest.getField().getFieldName();
                channelPut = channel.createChannelPut(this, pvRequest,structureName,isShared,process,null);
                return;
            }
            void destroyPut() {
                ChannelPut channelPut = this.channelPut;
                if(channelPut!=null) {
                    this.channelPut = null;
                    channelPut.destroy();
                }
            }
            void disconnect() {
                Channel channel = this.channel;
                if(channel!=null) {
                    this.channel = null;
                    channel.destroy();
                }
            }
            
            void put() {
                channelPut.put(false);
            }
            
            PVStructure getPVStructure() {
                return pvStructure;
            }
            
            BitSet getBitSet() {
                return bitSet;
            }
            /* (non-Javadoc)
             * @see org.epics.ca.client.ChannelRequester#channelStateChange(org.epics.ca.client.Channel, org.epics.ca.client.Channel.ConnectionState)
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
             * @see org.epics.ca.client.ChannelRequester#channelCreated(org.epics.pvData.pv.Status, org.epics.ca.client.Channel)
             */
            @Override
            public void channelCreated(Status status,Channel c) {
                if (!status.isOK()) {
                    message(status.toString(),MessageType.error);
                    return;
                }
                channel = c;
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
                this.pvRequest = pvRequest;
                this.isShared = isShared;
                runCommand = RunCommand.channelrequestDone;
                shell.getDisplay().asyncExec(this);
            }
            /* (non-Javadoc)
             * @see org.epics.ca.client.ChannelPutRequester#channelPutConnect(Status,org.epics.ca.client.ChannelPut, org.epics.pvData.pv.PVStructure, org.epics.pvData.misc.BitSet)
             */
            @Override
            public void channelPutConnect(Status status,ChannelPut channelPut,PVStructure pvStructure,BitSet bitSet) {
                if (!status.isOK()) {
                	message(status.toString(), status.isSuccess() ? MessageType.warning : MessageType.error);
                	if (!status.isSuccess()) return;
                }
                this.channelPut = channelPut;
                this.pvStructure = pvStructure;
                this.bitSet = bitSet;
                channelPut.get();
            }
            /* (non-Javadoc)
             * @see org.epics.ca.client.ChannelPutRequester#putDone(Status)
             */
            @Override
            public void putDone(Status status) {
                if (!status.isOK()) {
                	message(status.toString(), status.isSuccess() ? MessageType.warning : MessageType.error);
                	if (!status.isSuccess()) return;
                }
                runCommand = RunCommand.putDone;
                shell.getDisplay().asyncExec(this);
            }
            /* (non-Javadoc)
             * @see org.epics.ca.client.ChannelPutRequester#getDone(Status)
             */
            @Override
            public void getDone(Status status) {
                if (!status.isOK()) {
                    message(status.toString(), status.isSuccess() ? MessageType.warning : MessageType.error);
                    if (!status.isSuccess()) return;
                }
                runCommand = RunCommand.channelPutConnect;
                shell.getDisplay().asyncExec(this);
            }
            /* (non-Javadoc)
             * @see java.lang.Runnable#run()
             */
            @Override
            public void run() {
                switch(runCommand) {
                case channelConnected:
                    stateMachine.setState(State.readyForCreateRequest);
                    return;
                case timeout:
                    stateMachine.setState(State.readyForConnect);
                    return;
                case destroy:
                    stateMachine.setState(State.readyForConnect);
                    return;
                case channelrequestDone:
                    stateMachine.setState(State.readyForCreatePut);
                    return;
                case channelPutConnect:
                    stateMachine.setState(State.ready);
                    return;
                case putDone:
                    stateMachine.setState(State.ready);
                    requester.message("put done", MessageType.info);
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
