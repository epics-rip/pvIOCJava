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
import org.epics.pvaccess.client.ChannelProcess;
import org.epics.pvaccess.client.ChannelProcessRequester;
import org.epics.pvaccess.client.ChannelRequester;
import org.epics.pvdata.pv.MessageType;
import org.epics.pvdata.pv.Requester;
import org.epics.pvdata.pv.Status;

/**
 * Shell for processing a channel.
 * @author mrk
 *
 */
public class ProcessFactory {
    /**
     * Create the process shell.
     * @param display The display.
     */
    public static void init(Display display) {
        ProcessImpl processImpl = new ProcessImpl();
        processImpl.start(display);
    }
    
    private static class ProcessImpl implements DisposeListener,SelectionListener  { 
        // following are global to embedded classes
        private enum State{
            readyForConnect,connecting,
            readyForCreateProcess,creatingProcess,
            ready,processActive
        };
        private StateMachine stateMachine = new StateMachine();
        private ChannelClient channelClient = new ChannelClient();
        private Requester requester = null;
        private boolean isDisposed = false;
        
        private static String windowName = "process";
        private Shell shell = null;
        private Button connectButton = null;
        private Button createProcessButton = null;
        private Button processButton = null;
        private Text consoleText = null; 
        
        void start(Display display) {
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

            createProcessButton = new Button(composite,SWT.PUSH);
            createProcessButton.setText("destroyProcess");
            createProcessButton.addSelectionListener(this);               
            processButton = new Button(composite,SWT.PUSH);
            processButton.setText("process");
            processButton.addSelectionListener(this);               
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
            } else if(object==createProcessButton) {
                State state = stateMachine.getState();
                if(state==State.readyForCreateProcess) {
                    stateMachine.setState(State.creatingProcess);
                    channelClient.createProcess();
                } else {
                    channelClient.destroyProcess();
                    stateMachine.setState(State.readyForCreateProcess);
                }
            } else if(object==processButton) {
                State state = stateMachine.getState();
                if(state==State.ready) {
                    stateMachine.setState(State.processActive);
                    channelClient.process();
                } else {
                    requester.message("process request but not ready",MessageType.error);
                }
                return;
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
                    createProcessButton.setText("createProcess");
                    createProcessButton.setEnabled(false);
                    processButton.setEnabled(false);
                    return;
                case connecting:
                    connectButton.setText("disconnect");
                    createProcessButton.setText("createProcess");
                    createProcessButton.setEnabled(false);
                    processButton.setEnabled(false);
                    return;
                case readyForCreateProcess:
                    connectButton.setText("disconnect");
                    createProcessButton.setText("createProcess");
                    createProcessButton.setEnabled(true);
                    processButton.setEnabled(false);
                    return;
                case creatingProcess:
                    connectButton.setText("disconnect");
                    createProcessButton.setText("destroyProcess");
                    createProcessButton.setEnabled(true);
                    processButton.setEnabled(false);
                    return;
                case ready:
                    connectButton.setText("disconnect");
                    createProcessButton.setText("destroyProcess");
                    createProcessButton.setEnabled(true);
                    processButton.setEnabled(true);
                    return;
                case processActive:
                    connectButton.setText("disconnect");
                    createProcessButton.setText("destroyProcess");
                    createProcessButton.setEnabled(true);
                    processButton.setEnabled(false);
                    return;
                }
                
            }
            State getState() {return state;}
        }
        
        private enum RunCommand{channelConnected,timeout,destroy,channelProcessConnect,processDone};
        private class ChannelClient implements ChannelRequester,ConnectChannelRequester,Runnable,ChannelProcessRequester
        {   
            private Channel channel = null;
            private ConnectChannel connectChannel = null;
            private ChannelProcess channelProcess = null;
            private RunCommand runCommand = null;

            void connect(Shell shell) {
                if(connectChannel!=null) {
                    message("connect in propress",MessageType.error);
                }
                connectChannel = ConnectChannelFactory.create(shell, this,this);
                connectChannel.connect();
            }
            void createProcess() {
                channelProcess = channel.createChannelProcess(this,null);
            }
            
            void destroyProcess() {
                ChannelProcess channelProcess = this.channelProcess;
                if(channelProcess!=null) {
                    this.channelProcess = null;
                    channelProcess.destroy();
                }
            }
            void process() {
                channelProcess.process(false);
            }
            
            void disconnect() {
                Channel channel = this.channel;
                if(channel!=null) {
                    this.channel = null;
                    channel.destroy();
                }
            }
            /* (non-Javadoc)
             * @see org.epics.pvaccess.client.ChannelRequester#channelStateChange(org.epics.pvaccess.client.Channel, org.epics.pvaccess.client.Channel.ConnectionState)
             */
            @Override
            public void channelStateChange(Channel c, ConnectionState state) {
            	
            	if (state == ConnectionState.DESTROYED) {
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
             * @see org.epics.pvaccess.client.ChannelProcessRequester#channelProcessConnect(Status,org.epics.pvaccess.client.ChannelProcess)
             */
            @Override
            public void channelProcessConnect(Status status,ChannelProcess channelProcess) {
                if (!status.isOK()) {
                	message(status.toString(), status.isSuccess() ? MessageType.warning : MessageType.error);
                	if (!status.isSuccess()) return;
                }
                runCommand = RunCommand.channelProcessConnect;
                shell.getDisplay().asyncExec(this);
            }
            /* (non-Javadoc)
             * @see org.epics.pvaccess.client.ChannelProcessRequester#processDone(Status)
             */
            @Override
            public void processDone(Status status) {
                if (!status.isOK()) {
                	message(status.toString(), status.isSuccess() ? MessageType.warning : MessageType.error);
                	if (!status.isSuccess()) return;
                }
                message("processDone succeeded",MessageType.info);
                runCommand = RunCommand.processDone;
                shell.getDisplay().asyncExec(this);
            }
            /* (non-Javadoc)
             * @see java.lang.Runnable#run()
             */
            @Override
            public void run() {
                switch(runCommand) {
                case channelConnected:
                    stateMachine.setState(State.readyForCreateProcess);
                    return;
                case timeout:
                    stateMachine.setState(State.readyForConnect);
                    return;
                case destroy:
                    stateMachine.setState(State.readyForConnect);
                    return;
                case channelProcessConnect:
                    stateMachine.setState(State.ready);
                    return;
                case processDone:
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
