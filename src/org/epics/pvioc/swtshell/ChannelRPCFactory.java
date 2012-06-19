/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.swtshell;

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
import org.epics.pvaccess.client.Channel;
import org.epics.pvaccess.client.Channel.ConnectionState;
import org.epics.pvaccess.client.ChannelRPC;
import org.epics.pvaccess.client.ChannelRPCRequester;
import org.epics.pvaccess.client.ChannelRequester;
import org.epics.pvaccess.client.CreateRequestFactory;
import org.epics.pvdata.factory.PVDataFactory;
import org.epics.pvdata.misc.BitSet;
import org.epics.pvdata.pv.MessageType;
import org.epics.pvdata.pv.PVDataCreate;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.Requester;
import org.epics.pvdata.pv.Status;
import org.epics.pvdata.pv.Structure;


/**
 * A shell for making channelRPC requests.
 * @author mrk
 *
 */
public class ChannelRPCFactory {
    /**
     * Create the shell. 
     * @param display The display to which the shell belongs.
     */
    public static void init(Display display) {
        ChannelRPCImpl channelRPCImpl = new ChannelRPCImpl();
        channelRPCImpl.start(display);
    }
    
    
    private static class ChannelRPCImpl implements DisposeListener,SelectionListener
    
    {
        // following are global to embedded classes
        private enum State{
            readyForConnect,connecting,
            readyForCreateChannelRPC,creatingChannelRPC,
            ready,channelRPCActive
        };
        private static final PVDataCreate pvDataCreate = PVDataFactory.getPVDataCreate();
        private StateMachine stateMachine = new StateMachine();
        private ChannelClient channelClient = new ChannelClient();
        private CreateStructure createStructure = null;
        private PVStructure pvRequest = null;
        private PVStructure pvArgument = null;
        private Requester requester = null;
        private boolean isDisposed = false;
        
        private static final String windowName = "channelRPC";
        private Shell shell;
        private Button connectButton;
        private Text argumentNameText = null;
        private Button createArgumentButton;
        private Button createChannelRPCButton;
        private Button channelRPCButton;
        private Text consoleText = null; 
        
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
            
            createArgumentButton = new Button(composite,SWT.PUSH);
            createArgumentButton.setText("createArgument");
            createArgumentButton.addSelectionListener(this);

            argumentNameText = new Text(composite,SWT.BORDER);
            argumentNameText.setText("argument");
            
            createChannelRPCButton = new Button(composite,SWT.PUSH);
            createChannelRPCButton.setText("destroyChannelRPC");
            createChannelRPCButton.addSelectionListener(this);
            
            channelRPCButton = new Button(composite,SWT.NONE);
            channelRPCButton.setText("channelRPC");
            channelRPCButton.addSelectionListener(this);
            
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
            pvRequest = CreateRequestFactory.createRequest("", requester);
            shell.pack();
            createStructure = CreateStructureFactory.create(shell);
            stateMachine.setState(State.readyForConnect);
            shell.open();
            shell.addDisposeListener(this);
        }
        
        /* (non-Javadoc)
         * @see org.eclipse.swt.events.DisposeListener#widgetDisposed(org.eclipse.swt.events.DisposeEvent)
         */
        @Override
        public void widgetDisposed(DisposeEvent e) {
            isDisposed = true;
            channelClient.disconnect();
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
                State state = stateMachine.getState();
                if(state==State.readyForConnect) {
                    stateMachine.setState(State.connecting);
                    channelClient.connect(shell);
                } else {
                    channelClient.disconnect();
                    stateMachine.setState(State.readyForConnect);
                }
            } else if(object==createArgumentButton) {
            	Structure structure = createStructure.create();
            	pvArgument = pvDataCreate.createPVStructure(structure);
            } else if(object==createChannelRPCButton) {
                State state = stateMachine.getState();
                if(state==State.readyForCreateChannelRPC) {
                    stateMachine.setState(State.creatingChannelRPC);
                    channelClient.createChannelRPC();
                } else {
                    channelClient.destroyChannelRPC();
                    stateMachine.setState(State.readyForCreateChannelRPC);
                }
            } else if(object==channelRPCButton) {
            	if(pvArgument!=null) {
            		GUIData guiData = GUIDataFactory.create(shell);
            		BitSet bitSet = new BitSet(pvArgument.getNumberFields());
            		guiData.get(pvArgument,bitSet);
            	}
            	stateMachine.setState(State.channelRPCActive);
            	channelClient.get();
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
                    createChannelRPCButton.setText("createChannelRPC");
                    createChannelRPCButton.setEnabled(false);
                    channelRPCButton.setEnabled(false);
                    return;
                case connecting:
                    connectButton.setText("disconnect");
                    createChannelRPCButton.setText("createChannelRPC");
                    createChannelRPCButton.setEnabled(false);
                    channelRPCButton.setEnabled(false);
                    return;
                case readyForCreateChannelRPC:
                    connectButton.setText("disconnect");
                    createChannelRPCButton.setText("createChannelRPC");
                    createChannelRPCButton.setEnabled(true);
                    channelRPCButton.setEnabled(false);
                    return;
                case creatingChannelRPC:
                    connectButton.setText("disconnect");
                    createChannelRPCButton.setText("destroyChannelRPC");
                    createChannelRPCButton.setEnabled(true);
                    channelRPCButton.setEnabled(false);
                    return;
                case ready:
                    connectButton.setText("disconnect");
                    createChannelRPCButton.setText("destroyChannelRPC");
                    createChannelRPCButton.setEnabled(true);
                    channelRPCButton.setEnabled(true);
                    return;
                case channelRPCActive:
                    connectButton.setText("disconnect");
                    createChannelRPCButton.setText("destroyChannelRPC");
                    createChannelRPCButton.setEnabled(true);
                    channelRPCButton.setEnabled(false);
                    return;
                }
                
            }
            State getState() {return state;}
        }
        
        private enum RunCommand {
            channelConnected,timeout,destroy,channelrequestDone,channelRPCConnect,getDone
        }
        
        private class ChannelClient implements
        ChannelRequester,ConnectChannelRequester,Runnable,ChannelRPCRequester
        {
            private Channel channel = null;
            private ConnectChannel connectChannel = null;
            private ChannelRPC channelRPC = null;
            private RunCommand runCommand;
            private PrintModified printModified = null;
            private PVStructure pvResult = null;
            private BitSet bitSet = null;

            void connect(Shell shell) {
                if(connectChannel!=null) {
                    message("connect in propress",MessageType.error);
                }
                connectChannel = ConnectChannelFactory.create(shell, this,this);
                connectChannel.connect();
            }
            void createChannelRPC() {            	
            	channelRPC = channel.createChannelRPC(this, pvRequest);
                return;
            }
            void destroyChannelRPC() {
                ChannelRPC channelRPC = this.channelRPC;
                if(channelRPC!=null) {
                    this.channelRPC = null;
                    channelRPC.destroy();
                }
            }
            void disconnect() {
                Channel channel = this.channel;
                if(channel!=null) {
                    this.channel = null;
                    channel.destroy();
                }
            }
            
            void get() {
                runCommand = RunCommand.channelRPCConnect;
                channelRPC.request(pvArgument,false);
            }
            
            PVStructure getPVStructure() {
                return pvArgument;
            }
            
            BitSet getBitSet() {
                return bitSet;
            }
            /* (non-Javadoc)
             * @see org.epics.pvaccess.client.ChannelRequester#channelStateChange(org.epics.pvaccess.client.Channel, org.epics.pvaccess.client.Channel.ConnectionState)
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
             * @see org.epics.pvaccess.client.ChannelRequester#channelCreated(org.epics.pvdata.pv.Status, org.epics.pvaccess.client.Channel)
             */
            @Override
            public void channelCreated(Status status,Channel c) {
                if (!status.isOK()) {
                    message(status.toString(),MessageType.error);
                    return;
                }
                channel = c;
            }
            /* (non-Javadoc)
             * @see org.epics.pvioc.swtshell.ConnectChannelRequester#timeout()
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
            @Override
			public void channelRPCConnect(Status status, ChannelRPC channelRPC) {
                if (!status.isOK()) {
                	message(status.toString(), status.isSuccess() ? MessageType.warning : MessageType.error);
                	if (!status.isSuccess()) return;
                }
                this.channelRPC = channelRPC;
                runCommand = RunCommand.channelRPCConnect;
                shell.getDisplay().asyncExec(this);
            }

			/* (non-Javadoc)
			 * @see org.epics.pvaccess.client.ChannelRPCRequester#requestDone(org.epics.pvdata.pv.Status, org.epics.pvdata.pv.PVStructure)
			 */
			@Override
			public void requestDone(Status status, PVStructure pvResponse) {
				pvResult = pvResponse;
				bitSet = new BitSet(pvResponse.getNumberFields());
				bitSet.set(0);
            	BitSet overrunBitSet = new BitSet(pvResponse.getNumberFields());
            	printModified = PrintModifiedFactory.create("rpcResult", pvResponse, bitSet, overrunBitSet, consoleText);
                if (!status.isOK()) {
                	message(status.toString(), status.isSuccess() ? MessageType.warning : MessageType.error);
                	if (!status.isSuccess()) return;
                }
                shell.getDisplay().asyncExec( new Runnable() {
                    public void run() {
                        printModified.print();
                    }

                });
                runCommand = RunCommand.getDone;
                shell.getDisplay().asyncExec(this);
            }
            /* (non-Javadoc)
             * @see java.lang.Runnable#run()
             */
            @Override
            public void run() {
                switch(runCommand) {
                case channelConnected:
                    stateMachine.setState(State.readyForCreateChannelRPC);
                    return;
                case timeout:
                    stateMachine.setState(State.readyForConnect);
                    return;
                case destroy:
                    stateMachine.setState(State.readyForConnect);
                    return;
                case channelrequestDone:
                    stateMachine.setState(State.readyForCreateChannelRPC);
                    return;
                case channelRPCConnect:
                    stateMachine.setState(State.ready);
                    return;
                case getDone:
                    stateMachine.setState(State.ready);
                    return;
                }
            }
            
            /* (non-Javadoc)
             * @see org.epics.pvioc.util.Requester#getRequesterName()
             */
            @Override
            public String getRequesterName() {
                return requester.getRequesterName();
            }
            /* (non-Javadoc)
             * @see org.epics.pvioc.util.Requester#message(java.lang.String, org.epics.pvioc.util.MessageType)
             */
            @Override
            public void message(final String message, final MessageType messageType) {
                requester.message(message, MessageType.info);
            }           
        }
    }
}
