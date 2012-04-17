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
import org.epics.pvaccess.client.ChannelPutGet;
import org.epics.pvaccess.client.ChannelPutGetRequester;
import org.epics.pvaccess.client.ChannelRequester;
import org.epics.pvaccess.client.CreateRequestFactory;
import org.epics.pvaccess.client.Channel.ConnectionState;
import org.epics.pvdata.misc.BitSet;
import org.epics.pvdata.pv.MessageType;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.Requester;
import org.epics.pvdata.pv.Status;

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
            readyForCreatePutGet,creatingPutGet,
            ready,putGetActive
        };
        private StateMachine stateMachine = new StateMachine();
        private ChannelClient channelClient = new ChannelClient();
        private Requester requester = null;
        private boolean isDisposed = false;

        private static String windowName = "putGet";
        private static final String defaultRequest = "record[process=true]putField(arguments)getField(result)";
        private Shell shell = null;
        private Button connectButton;
        private Button createPutRequestButton = null;
        private Button createGetRequestButton = null;
        private Text requestText = null;
        private Button createPutGetButton = null;
        private Button putGetButton;
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
            gridLayout.numColumns = 3;
            requestComposite.setLayout(gridLayout);
            createPutRequestButton = new Button(requestComposite,SWT.PUSH);
            createPutRequestButton.setText("createPutRequest");
            createPutRequestButton.addSelectionListener(this);
            createGetRequestButton = new Button(requestComposite,SWT.PUSH);
            createGetRequestButton.setText("createGetRequest");
            createGetRequestButton.addSelectionListener(this);
            requestText = new Text(requestComposite,SWT.BORDER);
            GridData gridData = new GridData(); 
            gridData.widthHint = 400;
            requestText.setLayoutData(gridData);
            requestText.setText(defaultRequest);
            requestText.addSelectionListener(this);
            
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
            } else if(object==createPutRequestButton) {
               channelClient.createPutRequest(shell);
            } else if(object==createGetRequestButton) {
                channelClient.createGetRequest(shell);
            } else if(object==createPutGetButton) {
                stateMachine.setState(State.creatingPutGet);
                PVStructure pvStructure = CreateRequestFactory.createRequest(requestText.getText(),requester);
                channelClient.createPutGet(pvStructure);
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
                    createPutRequestButton.setEnabled(false);
                    createGetRequestButton.setEnabled(false);
                    createPutGetButton.setEnabled(false);
                    putGetButton.setEnabled(false);
                    return;
                case connecting:
                    connectButton.setText("disconnect");
                    createPutGetButton.setText("createPutGet");
                    createPutRequestButton.setEnabled(false);
                    createGetRequestButton.setEnabled(false);
                    createPutGetButton.setEnabled(false);
                    putGetButton.setEnabled(false);
                    return;
                case readyForCreatePutGet:
                    connectButton.setText("disconnect");
                    createPutGetButton.setText("createPutGet");
                    createPutRequestButton.setEnabled(true);
                    createGetRequestButton.setEnabled(true);
                    createPutGetButton.setEnabled(true);
                    putGetButton.setEnabled(false);
                    return;
                case creatingPutGet:
                    connectButton.setText("disconnect");
                    createPutGetButton.setText("destroyPutGet");
                    createPutRequestButton.setEnabled(false);
                    createGetRequestButton.setEnabled(false);
                    createPutGetButton.setEnabled(true);
                    putGetButton.setEnabled(false);
                    return;
                case ready:
                    connectButton.setText("disconnect");
                    createPutGetButton.setText("destroyPutGet");
                    createPutRequestButton.setEnabled(false);
                    createGetRequestButton.setEnabled(false);
                    createPutGetButton.setEnabled(true);
                    putGetButton.setEnabled(true);
                    return;
                case putGetActive:
                    connectButton.setText("disconnect");
                    createPutGetButton.setText("destroyPutGet");
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
            channelConnected,timeout,destroy,requestDone,getPutDone,putGetDone
        }
       
        
        private class ChannelClient implements
        ChannelRequester,ConnectChannelRequester,CreateFieldRequestRequester,Runnable,ChannelPutGetRequester
        {
            private Channel channel = null;
            private ConnectChannel connectChannel = null;
            
            private ChannelPutGet channelPutGet = null;
            private PVStructure pvPutStructure = null;
            private BitSet putBitSet = null;
            private PVStructure pvGetStructure = null;
            private BitSet getBitSet = null;
            private RunCommand runCommand = null;
            private boolean isPutRequest = true;
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
                CreateFieldRequest createRequest = CreateFieldRequestFactory.create(shell, channel, this);
                createRequest.create();
            }
            void createGetRequest(Shell shell) {
                isPutRequest = false;
                CreateFieldRequest createRequest = CreateFieldRequestFactory.create(shell, channel, this);
                createRequest.create();
            }
            void createPutGet(PVStructure pvRequest) {
                channelPutGet = channel.createChannelPutGet(this,pvRequest);
                return;
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
            
            void putGet() {
                channelPutGet.putGet(false);
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
            /* (non-Javadoc)
             * @see org.epics.pvioc.swtshell.CreateFieldRequestRequester#getDefault()
             */
            @Override
			public String getDefault() {
			    if(isPutRequest) {
			    	return "argument";
			    } else {
			    	return "result";
			    }
			}
			/* (non-Javadoc)
			 * @see org.epics.pvioc.swtshell.CreateFieldRequestRequester#request(java.lang.String)
			 */
			@Override
			public void request(String request) {
                if(isPutRequest) {
                    String text = requestText.getText();
                    int start = text.indexOf("putField(");
                    int end = text.indexOf(')', start);
                    String prefix = text.substring(0, start + 9);
                    String postfix =  text.substring(end);
                    text = prefix + request + postfix;
                    requestText.selectAll();
                    requestText.clearSelection();
                    requestText.setText(text);
                } else {
                	String text = requestText.getText();
                    int start = text.indexOf("getField(");
                    String prefix = text.substring(0, start + 9);
                    text = prefix + request + ")";
                    requestText.selectAll();
                    requestText.clearSelection();
                    requestText.setText(text);
                }
                runCommand = RunCommand.requestDone;
                shell.getDisplay().asyncExec(this);
            }
            /* (non-Javadoc)
             * @see org.epics.pvaccess.client.ChannelPutGetRequester#channelPutGetConnect(Status,org.epics.pvaccess.client.ChannelPutGet, org.epics.pvdata.pv.PVStructure, org.epics.pvdata.misc.BitSet, org.epics.pvdata.pv.PVStructure, org.epics.pvdata.misc.BitSet)
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
             * @see org.epics.pvaccess.client.ChannelPutGetRequester#getGetDone(Status)
             */
            @Override
            public void getGetDone(Status success) {}

            /* (non-Javadoc)
             * @see org.epics.pvaccess.client.ChannelPutGetRequester#getPutDone(Status)
             */
            @Override
            public void getPutDone(Status success) {
                runCommand = RunCommand.getPutDone;
                shell.getDisplay().asyncExec(this);
            }

            /* (non-Javadoc)
             * @see org.epics.pvaccess.client.ChannelPutGetRequester#putGetDone(Status)
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
                    stateMachine.setState(State.readyForCreatePutGet);
                    return;
                case timeout:
                    stateMachine.setState(State.readyForConnect);
                    return;
                case destroy:
                    stateMachine.setState(State.readyForConnect);
                    return;
                case requestDone:
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
             * @see org.epics.pvioc.util.Requester#putRequesterName()
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
