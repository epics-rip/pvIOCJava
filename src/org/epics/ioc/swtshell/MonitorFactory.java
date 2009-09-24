/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.swtshell;
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
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.epics.ca.client.Channel;
import org.epics.ca.client.ChannelRequester;
import org.epics.ca.client.Channel.ConnectionState;
import org.epics.pvData.factory.PVDataFactory;
import org.epics.pvData.misc.BitSet;
import org.epics.pvData.misc.Executor;
import org.epics.pvData.misc.ExecutorNode;
import org.epics.pvData.monitor.Monitor;
import org.epics.pvData.monitor.MonitorElement;
import org.epics.pvData.monitor.MonitorRequester;
import org.epics.pvData.pv.Field;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVDataCreate;
import org.epics.pvData.pv.PVDouble;
import org.epics.pvData.pv.PVInt;
import org.epics.pvData.pv.PVString;
import org.epics.pvData.pv.PVStructure;
import org.epics.pvData.pv.Requester;
import org.epics.pvData.pv.ScalarType;
import org.epics.pvData.pv.Status;
import org.epics.pvData.pv.Structure;

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
    
    private static final PVDataCreate pvDataCreate = PVDataFactory.getPVDataCreate();
    private static Executor executor = SwtshellFactory.getExecutor();
    private static class MonitorImpl
    implements  DisposeListener,SelectionListener
    {
        // following are global to embedded classes
        private enum State{
            readyForConnect,connecting,
            readyForCreateRequest,creatingRequest,
            readyForCreateMonitor,creatingMonitor,
            readyForStart, active
        };
        private StateMachine stateMachine = new StateMachine();
        private ChannelClient channelClient = new ChannelClient();
        private Requester requester = null;
        private boolean isDisposed = false;
        
        
        private static final String windowName = "monitor";
        private Shell shell = null;
        private Button connectButton;
        private Button createRequestButton = null;
        private Button createMonitorButton;
        private Button startStopButton;
        private Button statusButton;
        
        private Text consoleText = null;
        //private PVStructure pvRequest = null;
        private PVStructure pvOption = null;
        private PVString pvAlgorithm = null;
        private PVInt pvQueueSize = null;
        private Text queueSizeText = null;
        private PVDouble pvDeadband = null;
        private Text deadbandText;
        private Text simulateDelayText;
        
        private int queueSize = 3;
        private double deadband = 0.0;
        private double simulateDelay = 0.0;
        
        private Combo algorithmCombo; 
        
        private boolean isMonitoring = false;
        
        private void start(Display display) {
            initPVOption();
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
            createRequestButton = new Button(composite,SWT.PUSH);
            createRequestButton.setText("createRequest");
            createRequestButton.addSelectionListener(this);  
            
            createMonitorButton = new Button(composite,SWT.PUSH);
            createMonitorButton.setText("destroyMonitor");
            createMonitorButton.addSelectionListener(this);               

            statusButton = new Button(composite,SWT.PUSH);
            statusButton.setText("serverInfo");
            statusButton.addSelectionListener(this);               
            statusButton.setEnabled(true);
            
            Group group  = new Group(shell,SWT.BORDER);
            group.setText("options for servers that support pvData");
            gridLayout = new GridLayout();
            gridLayout.numColumns = 4;
            group.setLayout(gridLayout);
            Composite queueComposite = new Composite(group,SWT.BORDER);
            gridLayout = new GridLayout();
            gridLayout.numColumns = 2;
            queueComposite.setLayout(gridLayout);
            new Label(queueComposite,SWT.NONE).setText("queueSize");
            queueSizeText = new Text(queueComposite,SWT.BORDER);
            GridData gridData = new GridData(); 
            gridData.widthHint = 25;
            queueSizeText.setLayoutData(gridData);
            queueSizeText.setText(Integer.toString(queueSize));
            queueSizeText.addSelectionListener(this);
            
            Composite simulateDelayComposite = new Composite(group,SWT.BORDER);
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
            
            algorithmCombo = new Combo(group,SWT.DROP_DOWN);
            algorithmCombo.add("onPut");
            algorithmCombo.add("onChange");
            algorithmCombo.add("onAbsolute");
            algorithmCombo.add("onPercentage");
            algorithmCombo.add("custom");
            algorithmCombo.select(0);
            algorithmCombo.addSelectionListener(this);
            Composite deadbandComposite = new Composite(group,SWT.BORDER);
            gridLayout = new GridLayout();
            gridLayout.numColumns = 2;
            deadbandComposite.setLayout(gridLayout);
            new Label(deadbandComposite,SWT.NONE).setText("deadband");
            deadbandText = new Text(deadbandComposite,SWT.BORDER);
            gridData = new GridData(); 
            gridData.widthHint = 75;
            deadbandText.setLayoutData(gridData);
            deadbandText.setText(Double.toString(deadband));
            deadbandText.addSelectionListener(this);
            
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
        
        private void initPVOption() {
            pvOption = pvDataCreate.createPVStructure(null, "pvOption", new Field[0]);
            pvAlgorithm = (PVString)pvDataCreate.createPVScalar(pvOption, "algorithm", ScalarType.pvString);
            pvAlgorithm.put("onPut");
            pvOption.appendPVField(pvAlgorithm);
            pvQueueSize = (PVInt)pvDataCreate.createPVScalar(pvOption, "queueSize", ScalarType.pvInt);
            pvQueueSize.put(queueSize);
            pvOption.appendPVField(pvQueueSize);
            pvDeadband = (PVDouble)pvDataCreate.createPVScalar(pvOption, "deadband", ScalarType.pvDouble);
            pvDeadband.put(deadband);
            pvOption.appendPVField(pvDeadband);
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
            }
            if(object==createRequestButton) {
                stateMachine.setState(State.creatingRequest);
                channelClient.createRequest(shell);
            }
            if(object==createMonitorButton) {
                State state = stateMachine.getState();
                if(state==State.readyForCreateMonitor) {
                    stateMachine.setState(State.creatingMonitor);
                    channelClient.createMonitor();
                } else {
                    channelClient.destroyMonitor();
                    stateMachine.setState(State.readyForCreateMonitor);
                }
            }
            if(object==algorithmCombo) {
                int item = algorithmCombo.getSelectionIndex();
                if(item==0) {
                    pvAlgorithm.put("onPut");
                } else if(item==1) {
                    pvAlgorithm.put("onChange");
                } else if(item==2) {
                    pvAlgorithm.put("onAbsoluteChange");
                } else if(item==3) {
                    pvAlgorithm.put("onPercentChange");
                } else if(item==4) {
                    requester.message("custom not implemented",MessageType.info);
                    algorithmCombo.select(0);
                }
                return;
            }
            
            if(object==queueSizeText) {
                String value = queueSizeText.getText();
                try {
                    queueSize = Integer.decode(value);
                    pvQueueSize.put(queueSize);
                } catch (NumberFormatException e) {
                    requester.message("Illegal value", MessageType.error);
                }
                return;
            }
            if(object==deadbandText) {
                String value = deadbandText.getText();
                try {
                    deadband = Double.parseDouble(value);
                    pvDeadband.put(deadband);
                } catch (NumberFormatException e) {
                    requester.message("Illegal value", MessageType.error);
                }
                return;
            }
            if(object==simulateDelayText) {
                String value = simulateDelayText.getText();
                try {
                    simulateDelay = Double.parseDouble(value);
                } catch (NumberFormatException e) {
                    requester.message("Illegal value", MessageType.error);
                }
                return;
            }
            if(object==startStopButton) {
                if(isMonitoring) {
                    isMonitoring = false;
                    channelClient.stop();
                    stateMachine.setState(State.readyForStart);
                    return;
                }
                isMonitoring = true;
                stateMachine.setState(State.active);
                channelClient.start();
                return;
            }
            if(object==statusButton) {
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
            private State state = null;
            
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
                case readyForCreateRequest:
                    connectButton.setText("disconnect");
                    createMonitorButton.setText("createMonitor");
                    startStopButton.setText("start");
                    createRequestButton.setEnabled(true);
                    createMonitorButton.setEnabled(false);
                    startStopButton.setEnabled(false);
                    return;
                case creatingRequest:
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
                    createRequestButton.setEnabled(false);
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
        ChannelRequester,ConnectChannelRequester,CreateRequestRequester,
        Runnable,
        MonitorRequester
        {
            private Channel channel = null;
            private ConnectChannel connectChannel = null;
            private PVStructure pvRequest = null;
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
                CreateRequest createRequest = CreateRequestFactory.create(shell, channel, this);
                createRequest.create();
            }
            void disconnect() {
                Channel channel = this.channel;
                if(channel!=null) {
                    this.channel = null;
                    channel.destroy();
                }
            }
            
            void createMonitor() {
                monitor = channel.createMonitor(this, pvRequest, "", pvOption);
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
             * @see org.epics.ca.client.ChannelRequester#channelStateChange(org.epics.ca.client.Channel, org.epics.ca.client.Channel.ConnectionState)
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
             * @see org.epics.ca.client.ChannelRequester#channelCreated(org.epics.pvData.pv.Status, org.epics.ca.client.Channel)
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
                runCommand = RunCommand.channelrequestDone;
                shell.getDisplay().asyncExec(this);
            }
            /* (non-Javadoc)
             * @see org.epics.pvData.monitor.MonitorRequester#monitorConnect(Status,org.epics.pvData.monitor.Monitor, org.epics.pvData.pv.Structure)
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
             * @see org.epics.pvData.monitor.MonitorRequester#monitorEvent(org.epics.pvData.monitor.Monitor)
             */
            @Override
            public void monitorEvent(Monitor monitor) {
                poll.poll();
            }

            /* (non-Javadoc)
             * @see org.epics.ca.client.ChannelMonitorRequester#unlisten()
             */
            @Override
            public void unlisten() {
                // TODO Auto-generated method stub
                
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
                    stateMachine.setState(State.readyForCreateMonitor);
                    return;
                case monitorConnect:
                    stateMachine.setState(State.readyForStart);
                    return;
                }
            }
            
            /* (non-Javadoc)
             * @see org.epics.ioc.util.Requester#getRequesterName()
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
                    more = true;
                    shell.getDisplay().asyncExec( new Runnable() {
                        public void run() {
                            while(true) {
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
                    if(simulateDelay>0.0) {
                        long millis = (long)(simulateDelay*1000.0);
                        try{
                            Thread.sleep(millis, 0);
                        } catch (InterruptedException e) {

                        }
                    }
                }
            }   
        }
    }
}