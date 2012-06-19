/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.swtshell;
import java.util.concurrent.locks.Condition;
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
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.epics.pvaccess.client.Channel;
import org.epics.pvaccess.client.Channel.ConnectionState;
import org.epics.pvaccess.client.ChannelRequester;
import org.epics.pvaccess.client.CreateRequestFactory;
import org.epics.pvdata.misc.BitSet;
import org.epics.pvdata.misc.Executor;
import org.epics.pvdata.misc.ExecutorFactory;
import org.epics.pvdata.misc.ExecutorNode;
import org.epics.pvdata.misc.ThreadPriority;
import org.epics.pvdata.monitor.Monitor;
import org.epics.pvdata.monitor.MonitorElement;
import org.epics.pvdata.monitor.MonitorRequester;
import org.epics.pvdata.pv.MessageType;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.Requester;
import org.epics.pvdata.pv.Status;
import org.epics.pvdata.pv.Structure;

/**
 * A shell for monitoring a channel.
 * @author mrk
 *
 */
public class MonitorFactory {

    /**
     * Create the monitor shell.
     * @param display The display.
     */
    public static void init(Display display) {
        MonitorImpl monitorImpl = new MonitorImpl();
        monitorImpl.start(display);
    }
    
    private static Executor executor = ExecutorFactory.create("swtshell:display",ThreadPriority.low);
    private static class MonitorImpl
    implements  DisposeListener,SelectionListener
    {
        // following are global to embedded classes
        private enum State{
            readyForConnect,connecting,
            readyForCreateMonitor,creatingMonitor,
            readyForStart, active
        };
        private StateMachine stateMachine = new StateMachine();
        private ChannelClient channelClient = new ChannelClient();
        private Requester requester = null;
        private boolean isDisposed = false;
        
        
        private static final String windowName = "monitor";
        private static final String defaultRequest = "record[queueSize=2]field(value,alarm,timeStamp)";
        private Shell shell = null;
        private Button connectButton;
        private Button createRequestButton = null;
        private Text requestText = null;
        private Button createMonitorButton;
        private Button startStopButton;
        private Button statusButton;
        
        private Text consoleText = null;
        private Text simulateDelayText;
        
        private double simulateDelay = 0.0; 
        
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
            connectButton.setEnabled(true);
            
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
             
            createMonitorButton = new Button(composite,SWT.PUSH);
            createMonitorButton.setText("destroyMonitor");
            createMonitorButton.addSelectionListener(this);               

            statusButton = new Button(composite,SWT.PUSH);
            statusButton.setText("serverInfo");
            statusButton.addSelectionListener(this);               
            statusButton.setEnabled(true);
             
            Composite simulateDelayComposite = new Composite(composite,SWT.BORDER);
            gridLayout = new GridLayout();
            gridLayout.numColumns = 2;
            simulateDelayComposite.setLayout(gridLayout);
            new Label(simulateDelayComposite,SWT.NONE).setText("simulateDelay");
            simulateDelayText = new Text(simulateDelayComposite,SWT.BORDER);
            gridData = new GridData(); 
            gridData.widthHint = 75;
            simulateDelayText.setLayoutData(gridData);
            simulateDelayText.setText(Double.toString(simulateDelay));
            simulateDelayText.addSelectionListener(this);
            
            Composite startStopComposite = new Composite(shell,SWT.BORDER);
            gridLayout = new GridLayout();
            gridLayout.numColumns = 1;
            startStopComposite.setLayout(gridLayout);
            startStopButton = new Button(startStopComposite,SWT.PUSH);
            startStopButton.setText("startMonitor");
            startStopButton.addSelectionListener(this);
            startStopButton.setEnabled(false);
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
            State state = stateMachine.getState();
            if(object==connectButton) {
                if(state==State.readyForConnect) {
                    stateMachine.setState(State.connecting);
                    channelClient.connect(shell);
                } else {
                    channelClient.disconnect();
                    stateMachine.setState(State.readyForConnect);
                }
            } else if(object==createRequestButton) {
                channelClient.createRequest(shell);
            } else if(object==createMonitorButton) {
                if(state==State.readyForCreateMonitor) {
                    stateMachine.setState(State.creatingMonitor);
                    PVStructure pvStructure = CreateRequestFactory.createRequest(requestText.getText(),requester);
                    if(pvStructure==null) return;
                    channelClient.createMonitor(pvStructure);
                } else {
                    channelClient.destroyMonitor();
                    stateMachine.setState(State.readyForCreateMonitor);
                }
            } else if(object==simulateDelayText) {
                String value = simulateDelayText.getText();
                try {
                    simulateDelay = Double.parseDouble(value);
                } catch (NumberFormatException e) {
                    requester.message("Illegal value", MessageType.error);
                }
                return;
            } else if(object==startStopButton) {
                if(state==State.active) {
                    channelClient.stop();
                    stateMachine.setState(State.readyForStart);
                    return;
                } else if(state==State.readyForStart) {
                    stateMachine.setState(State.active);
                    channelClient.start();
                }
                return;
            } else if(object==statusButton) {
                Channel channel = channelClient.getChannel();
                if(channel==null) {
                    requester.message("not connected",MessageType.info);
                    return;
                }
                String message = "connectionState:" + channel.getConnectionState().toString();
                requester.message(message,MessageType.info);
                message = "provider:" + channel.getProvider().getProviderName() + " host:" + channel.getRemoteAddress();
                requester.message(message,MessageType.info);
                return;
            }
        }
        
        private class StateMachine {
            private State state = State.readyForConnect;;

            void setState(State newState) {
                if(isDisposed) return;
                state = newState;
                switch(state) {
                case readyForConnect:
                    connectButton.setText("connect");
                    createMonitorButton.setText("createMonitor");
                    createRequestButton.setEnabled(false);
                    createMonitorButton.setEnabled(false);
                    startStopButton.setEnabled(false);
                    return;
                case connecting:
                    connectButton.setText("disconnect");
                    createMonitorButton.setText("createMonitor");
                    startStopButton.setText("start");
                    createRequestButton.setEnabled(false);
                    createMonitorButton.setEnabled(false);
                    startStopButton.setEnabled(false);
                    return;
                case readyForCreateMonitor:
                    connectButton.setText("disconnect");
                    createMonitorButton.setText("createMonitor");
                    startStopButton.setText("start");
                    createRequestButton.setEnabled(true);
                    createMonitorButton.setEnabled(true);
                    startStopButton.setEnabled(false);
                    return;
                case creatingMonitor:
                    connectButton.setText("disconnect");
                    createMonitorButton.setText("destroyMonitor");
                    startStopButton.setText("start");
                    createRequestButton.setEnabled(false);
                    createMonitorButton.setEnabled(true);
                    startStopButton.setEnabled(false);
                    return;
                case readyForStart:
                    connectButton.setText("disconnect");
                    createMonitorButton.setText("destroyMonitor");
                    startStopButton.setText("start");
                    createRequestButton.setEnabled(false);
                    createMonitorButton.setEnabled(true);
                    startStopButton.setEnabled(true);
                    return;
                case active:
                    connectButton.setText("disconnect");
                    createMonitorButton.setText("destroyMonitor");
                    startStopButton.setText("stop");
                    createRequestButton.setEnabled(false);
                    createMonitorButton.setEnabled(true);
                    startStopButton.setEnabled(true);
                    return;
                }
                
            }
            State getState() {return state;}
        }
        
        private enum RunCommand {
            channelConnected,timeout,destroy,channelrequestDone,monitorConnect
        }
         
        private class ChannelClient implements
        ChannelRequester,ConnectChannelRequester,CreateFieldRequestRequester,
        Runnable,
        MonitorRequester
        {
            private Channel channel = null;
            private ConnectChannel connectChannel = null;
            private RunCommand runCommand;
            private Monitor monitor = null;
            private PrintModified printModified = null;
            private Poll poll = new Poll();
            
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
            void disconnect() {
            	if(monitor!=null) monitor.stop();
                if(channel!=null) channel.destroy();
            }
            
            void createMonitor(PVStructure pvRequest) {
                monitor = channel.createMonitor(this, pvRequest);
            }

            void destroyMonitor() {
                Monitor monitor = this.monitor;
                if(monitor!=null) {
                    this.monitor = null;
                    monitor.destroy();
                }
            }
            
            void start() {
                monitor.start();
            }
            
            void stop() {
                monitor.stop();
            }
            
            Channel getChannel() {
                return channel;
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
                requestText.setText("record[queueSize=2]field(" + request + ")");
                runCommand = RunCommand.channelrequestDone;
                shell.getDisplay().asyncExec(this);
            }
            /* (non-Javadoc)
             * @see org.epics.pvdata.monitor.MonitorRequester#monitorConnect(Status,org.epics.pvdata.monitor.Monitor, org.epics.pvdata.pv.Structure)
             */
            @Override
            public void monitorConnect(Status status,Monitor monitor, Structure structure) {
                if (!status.isOK()) {
                	message(status.toString(), status.isSuccess() ? MessageType.warning : MessageType.error);
                	if (!status.isSuccess()) return;
                }
                this.monitor = monitor;
                runCommand = RunCommand.monitorConnect;
                shell.getDisplay().asyncExec(this);
            }
            /* (non-Javadoc)
             * @see org.epics.pvdata.monitor.MonitorRequester#monitorEvent(org.epics.pvdata.monitor.Monitor)
             */
            @Override
            public void monitorEvent(Monitor monitor) {
                poll.poll();
            }
            /* (non-Javadoc)
             * @see org.epics.pvdata.monitor.MonitorRequester#unlisten(org.epics.pvdata.monitor.Monitor)
             */
            @Override
            public void unlisten(Monitor monitor) {
            	// What to do???
            } 
            /* (non-Javadoc)
             * @see java.lang.Runnable#run()
             */
            @Override
            public void run() {
                switch(runCommand) {
                case channelConnected:
                    stateMachine.setState(State.readyForCreateMonitor);
                    return;
                case timeout:
                    stateMachine.setState(State.readyForConnect);
                    return;
                case destroy:
                    stateMachine.setState(State.readyForConnect);
                    return;
                case channelrequestDone:
                    stateMachine.setState(State.readyForCreateMonitor);
                    return;
                case monitorConnect:
                    stateMachine.setState(State.readyForStart);
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
            
            private class Poll implements Runnable {
                private ExecutorNode executorNode = executor.createNode(this);
                private ReentrantLock lock = new ReentrantLock();
                private Condition moreWork = lock.newCondition();
                private volatile boolean more = true;

                void poll() {
                    executor.execute(executorNode);
                }

                /* (non-Javadoc)
                 * @see java.lang.Runnable#run()
                 */
                @Override
                public void run() {
                	if(simulateDelay>0.0) {
                        long millis = (long)(simulateDelay*1000.0);
                        try{
                            Thread.sleep(millis, 0);
                        } catch (InterruptedException e) {

                        }
                    }
                    more = true;
                    shell.getDisplay().asyncExec( new Runnable() {
                        public void run() {
                            while(true) {
                                if(monitor==null) return;
                                MonitorElement monitorElement = monitor.poll();
                                if(monitorElement==null) break;
                                PVStructure pvStructure = monitorElement.getPVStructure();
                                if(pvStructure==null) {
                                    consoleText.append("monitor occured");
                                    consoleText.append(String.format("%n"));
                                } else {
                                    BitSet changeBitSet = monitorElement.getChangedBitSet();
                                    BitSet overrunBitSet = monitorElement.getOverrunBitSet();
                                    printModified = PrintModifiedFactory.create(channel.getChannelName(),pvStructure, changeBitSet, overrunBitSet, consoleText);
                                    printModified.print();
                                }
                                monitor.release(monitorElement);
                            }
                            lock.lock();
                            try {
                                more = false;
                                moreWork.signal();
                                return;
                            } finally {
                                lock.unlock();
                            }
                        }
                    });
                    lock.lock();
                    try {
                        while(more) {
                            try {
                                moreWork.await();
                            } catch(InterruptedException e) {

                            }
                        }
                    }finally {
                        lock.unlock();
                    }
                }
            }   
        }
    }
}
