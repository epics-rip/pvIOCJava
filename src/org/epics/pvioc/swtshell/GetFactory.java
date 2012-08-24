/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS pvData is distributed subject to a Software License Agreement found
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
import org.epics.pvaccess.client.ChannelGet;
import org.epics.pvaccess.client.ChannelGetRequester;
import org.epics.pvaccess.client.ChannelRequester;
import org.epics.pvaccess.client.CreateRequestFactory;
import org.epics.pvdata.misc.BitSet;
import org.epics.pvdata.pv.MessageType;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.Requester;
import org.epics.pvdata.pv.Status;


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
        GetImpl getImpl = new GetImpl();
        getImpl.start(display);
    }
    
    
    private static class GetImpl implements DisposeListener,SelectionListener
    
    {
        // following are global to embedded classes
        private enum State{
            readyForConnect,connecting,
            readyForCreateGet,creatingGet,
            ready,getActive
        };
        private StateMachine stateMachine = new StateMachine();
        private ChannelClient channelClient = new ChannelClient();
        private Requester requester = null;
        private boolean isDisposed = false;
        
        private static final String windowName = "get";
        private static final String defaultRequest = "record[]field(value,alarm,timeStamp)";
        private Shell shell;
        private Button connectButton;
        private Button createRequestButton;
        private Text requestText = null;
        private Button createGetButton;
        private Button getButton;
        private Button dumpButton;
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
            
            Composite requestComposite = new Composite(composite,SWT.BORDER);
            gridLayout = new GridLayout();
            gridLayout.numColumns = 2;
            requestComposite.setLayout(gridLayout);
            createRequestButton = new Button(requestComposite,SWT.PUSH);
            createRequestButton.setText("createRequest");
            createRequestButton.addSelectionListener(this);
            requestText = new Text(requestComposite,SWT.BORDER);
            GridData gridData = new GridData(); 
            gridData.widthHint = 400;
            requestText.setLayoutData(gridData);
            requestText.setText(defaultRequest);
            requestText.addSelectionListener(this);

            createGetButton = new Button(composite,SWT.PUSH);
            createGetButton.setText("destroyGet");
            createGetButton.addSelectionListener(this);
             
            getButton = new Button(composite,SWT.NONE);
            getButton.setText("get");
            getButton.addSelectionListener(this);
            
            dumpButton = new Button(composite,SWT.NONE);
            dumpButton.setText("dump");
            dumpButton.addSelectionListener(this);
            
            Composite consoleComposite = new Composite(shell,SWT.BORDER);
            gridLayout = new GridLayout();
            gridLayout.numColumns = 1;
            consoleComposite.setLayout(gridLayout);
            gridData = new GridData(GridData.FILL_BOTH);
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
            } else if(object==createRequestButton) {
                channelClient.createRequest(shell);
            } else if(object==createGetButton) {
                State state = stateMachine.getState();
                if(state==State.readyForCreateGet) {
                    stateMachine.setState(State.creatingGet);
                    PVStructure pvStructure = CreateRequestFactory.createRequest(requestText.getText(),requester);
                    if(pvStructure==null) return;
                    channelClient.createGet(pvStructure);
                } else {
                    channelClient.destroyGet();
                    stateMachine.setState(State.readyForCreateGet);
                }
            } else if(object==getButton) {
               stateMachine.setState(State.getActive);
               channelClient.get();
            } else if(object==dumpButton) {
                channelClient.dump();
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
                    createGetButton.setText("createGet");
                    createRequestButton.setEnabled(false);
                    createGetButton.setEnabled(false);
                    getButton.setEnabled(false);
                    dumpButton.setEnabled(false);
                    return;
                case connecting:
                    connectButton.setText("disconnect");
                    createGetButton.setText("createGet");
                    createRequestButton.setEnabled(false);
                    createGetButton.setEnabled(false);
                    getButton.setEnabled(false);
                    dumpButton.setEnabled(false);
                    return;
                case readyForCreateGet:
                    connectButton.setText("disconnect");
                    createGetButton.setText("createGet");
                    createRequestButton.setEnabled(true);
                    createGetButton.setEnabled(true);
                    getButton.setEnabled(false);
                    dumpButton.setEnabled(false);
                    return;
                case creatingGet:
                    connectButton.setText("disconnect");
                    createGetButton.setText("destroyGet");
                    createRequestButton.setEnabled(false);
                    createGetButton.setEnabled(true);
                    getButton.setEnabled(false);
                    dumpButton.setEnabled(false);
                    return;
                case ready:
                    connectButton.setText("disconnect");
                    createGetButton.setText("destroyGet");
                    createRequestButton.setEnabled(false);
                    createGetButton.setEnabled(true);
                    getButton.setEnabled(true);
                    dumpButton.setEnabled(true);
                    return;
                case getActive:
                    connectButton.setText("disconnect");
                    createGetButton.setText("destroyGet");
                    createRequestButton.setEnabled(false);
                    createGetButton.setEnabled(true);
                    getButton.setEnabled(false);
                    dumpButton.setEnabled(false);
                    return;
                }
                
            }
            State getState() {return state;}
        }
        
        private enum RunCommand {
            channelConnected,timeout,destroy,channelrequestDone,channelGetConnect,getDone
        }
        
        private class ChannelClient implements
        ChannelRequester,ConnectChannelRequester,CreateFieldRequestRequester,Runnable,ChannelGetRequester
        {
            private Channel channel = null;
            private ConnectChannel connectChannel = null;
            private ChannelGet channelGet = null;
            private BitSet changeBitSet = null;
            private RunCommand runCommand;
            private PrintModified printModified = null;

            void connect(Shell shell) {
                if(connectChannel!=null) {
                    message("connect in propress",MessageType.error);
                }
                connectChannel = ConnectChannelFactory.create(shell, this,this);
                connectChannel.connect();
            }
            void createRequest(Shell shell) {
                CreateFieldRequest createRequest = CreateFieldRequestFactory.create(shell, channel, this);
                createRequest.create();
            }
            void createGet(PVStructure pvRequest) {
                channelGet = channel.createChannelGet(this, pvRequest);
                return;
            }
            void destroyGet() {
                ChannelGet channelGet = this.channelGet;
                if(channelGet!=null) {
                    this.channelGet = null;
                    channelGet.destroy();
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
                channelGet.get(false);
            }
            
            void dump() {
            	changeBitSet.clear();
            	changeBitSet.set(0);
            	printModified.print();
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
			public String getDefault() {
				return "value,alarm,timeStamp";
			}
			/* (non-Javadoc)
			 * @see org.epics.pvioc.swtshell.CreateFieldRequestRequester#request(java.lang.String)
			 */
			@Override
			public void request(String request) {
				requestText.selectAll();
                requestText.clearSelection();
                requestText.setText("record[]field(" + request + ")");
                runCommand = RunCommand.channelrequestDone;
                shell.getDisplay().asyncExec(this);
            }
            /* (non-Javadoc)
             * @see org.epics.pvaccess.client.ChannelGetRequester#channelGetConnect(Status,org.epics.pvaccess.client.ChannelGet, org.epics.pvdata.pv.PVStructure, org.epics.pvdata.misc.BitSet)
             */
            @Override
            public void channelGetConnect(Status status,ChannelGet channelGet,PVStructure pvStructure,BitSet bitSet) {
                if (!status.isOK()) {
                	message(status.toString(), status.isSuccess() ? MessageType.warning : MessageType.error);
                	if (!status.isSuccess()) return;
                }
                this.channelGet = channelGet;
                changeBitSet = bitSet;
                printModified = PrintModifiedFactory.create(channel.getChannelName(),pvStructure, changeBitSet, null, consoleText);
                runCommand = RunCommand.channelGetConnect;
                shell.getDisplay().asyncExec(this);
            }

            /* (non-Javadoc)
             * @see org.epics.pvaccess.client.ChannelGetRequester#getDone(Status)
             */
            @Override
            public void getDone(Status status) {
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
                    stateMachine.setState(State.readyForCreateGet);
                    return;
                case timeout:
                    stateMachine.setState(State.readyForConnect);
                    return;
                case destroy:
                    stateMachine.setState(State.readyForConnect);
                    return;
                case channelrequestDone:
                    stateMachine.setState(State.readyForCreateGet);
                    return;
                case channelGetConnect:
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
