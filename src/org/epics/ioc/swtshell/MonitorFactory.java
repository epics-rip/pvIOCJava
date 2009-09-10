/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.swtshell;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.*;
import org.epics.ca.channelAccess.client.Channel;
import org.epics.ca.channelAccess.client.ChannelGet;
import org.epics.ca.channelAccess.client.ChannelRequester;
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
        MonitorImpl monitorImpl = new MonitorImpl(display);
        monitorImpl.start();
    }
    
    private static class MonitorImpl
    implements  DisposeListener,SelectionListener,ChannelRequester,ConnectChannelRequester,CreateRequestRequester,Runnable
    {

        private MonitorImpl(Display display) {
            this.display = display;
        }

        private enum RunRequest {
            channelStateChange,
            dispose,
            monitorConnected
        }
        
        private static final Executor executor = SwtshellFactory.getExecutor();
        private static final PVDataCreate pvDataCreate = PVDataFactory.getPVDataCreate();
        private RunRequest runRequest = null;
        private static final String windowName = "monitor";
        private ExecutorNode executorNode = executor.createNode(this);
        private Display display;
        //private boolean isDisposed = false;
        private Shell shell = null;
        private Requester requester = null;
        private Button connectButton;
        private Button createRequestButton = null;
        private Button disconnectButton;
        private Button statusButton;
        
        private Text consoleText = null;
        private PVStructure pvRequest = null;
        private PVStructure pvOption = null;
        private PVString pvAlgorithm = null;
        private PVInt pvQueueSize = null;
        private Text queueSizeText = null;
        private PVDouble pvDeadband = null;
        private Text deadbandText;
        private Text simulateDelayText;
        private MonitorIt monitor = new MonitorIt();
        
        private int queueSize = 3;
        private double deadband = 0.0;
        private double simulateDelay = 0.0;
        
        private Combo algorithmCombo;
        private Button startStopButton;
        
        private AtomicReference<Channel> channel = new AtomicReference<Channel>(null);
        private AtomicReference<ConnectChannel> connectChannel = new AtomicReference<ConnectChannel>(null);
        
        
        private boolean isMonitoring = false;
        
        private void start() {
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
            connectButton.setText("connect");
            connectButton.addSelectionListener(this);               
            connectButton.setEnabled(true);
            createRequestButton = new Button(composite,SWT.PUSH);
            createRequestButton.setText("createRequest");
            createRequestButton.addSelectionListener(this);               
            createRequestButton.setEnabled(false);
            disconnectButton = new Button(composite,SWT.PUSH);
            disconnectButton.setText("disconnect");
            disconnectButton.addSelectionListener(this);               
            disconnectButton.setEnabled(false);
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
            enableOptions();
            shell.pack();
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
            runRequest = RunRequest.dispose;
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
            Object object = arg0.getSource();
            if(object==connectButton) {
                Channel channel = this.channel.get();
                if(channel!=null) {
                    message("must disconnect first",MessageType.error);
                    return;
                }
                connectChannel.set(ConnectChannelFactory.create(shell,this, this));
                connectChannel.get().connect();
                return;
            }
            if(object==createRequestButton) {
                Channel channel = this.channel.get();
                if(channel==null) {
                    message("channel not connected",MessageType.error);
                    return;
                }
                CreateRequest createRequest = CreateRequestFactory.create(shell, channel, this);
                createRequest.create();
                return;
            }
            if(object==disconnectButton) {
                monitor.disconnect();
                connectButton.setEnabled(true);
                disconnectButton.setEnabled(false);
                if(isMonitoring) {
                    isMonitoring = false;
                    startStopButton.setEnabled(false);
                    startStopButton.setText("startMonitor");
                    enableOptions();
                }
                return;
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
                    message("custom not implemented",MessageType.info);
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
                    message("Illegal value", MessageType.error);
                }
                return;
            }
            if(object==deadbandText) {
                String value = deadbandText.getText();
                try {
                    deadband = Double.parseDouble(value);
                    pvDeadband.put(deadband);
                } catch (NumberFormatException e) {
                    message("Illegal value", MessageType.error);
                }
                return;
            }
            if(object==simulateDelayText) {
                String value = simulateDelayText.getText();
                try {
                    simulateDelay = Double.parseDouble(value);
                } catch (NumberFormatException e) {
                    message("Illegal value", MessageType.error);
                }
                return;
            }
            if(object==startStopButton) {
                if(isMonitoring) {
                    isMonitoring = false;
                    monitor.stop();
                    startStopButton.setText("startMonitor");
                    enableOptions();
                    return;
                }
                isMonitoring = true;
                startStopButton.setText("stopMonitor");
                disableOptions();
                startStopButton.setEnabled(true);
                monitor.start();
                return;
            }
            if(object==statusButton) {
                Channel channel = this.channel.get();
                if(channel==null) {
                    message("not connected",MessageType.info);
                    return;
                }
                String message = "connectionState:" + channel.getConnectionState().toString();
                message(message,MessageType.info);
                message = "provider:" + channel.getProviderName() + " host:" + channel.getRemoteAddress();
                message(message,MessageType.info);
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
            	message(status.toString(), status.isSuccess() ? MessageType.warning : MessageType.error);
            	if (!status.isSuccess()) return;
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
            this.pvRequest = pvRequest;
            createRequestButton.setEnabled(false);
            monitor.create();
        }
        void monitorConnectCallback() {
            runRequest = RunRequest.monitorConnected;
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
                    startStopButton.setText("startMonitor");
                    connectButton.setEnabled(false);
                    disconnectButton.setEnabled(true);
                    createRequestButton.setEnabled(true);
                } else {
                    monitor.disconnect();
                    connectButton.setEnabled(true);
                    createRequestButton.setEnabled(false);
                    disconnectButton.setEnabled(false);
                    startStopButton.setEnabled(false);
                }
                return;
            case dispose:
                channel = this.channel.getAndSet(null);
                if(channel!=null) channel.destroy();
                return;
            case monitorConnected:
                startStopButton.setEnabled(true);
                startStopButton.setText("startMonitor");
                enableOptions();
                return;
            }
        }
        
        
        
        private enum MonitorItRunRequest {
            create,
            disconnect,
            start,
            stop
        }
        
        private class MonitorIt implements
        Runnable,
        MonitorRequester
        {
            private ExecutorNode executorNode = executor.createNode(this);
            private MonitorItRunRequest monitorItRunRequest;
            private AtomicReference<Monitor> monitor = new AtomicReference<Monitor>(null);
            private PrintModified printModified = null;
            private Poll poll = new Poll();
            
            void create() {
                monitorItRunRequest = MonitorItRunRequest.create;
                executor.execute(executorNode);
            }
            void disconnect() {
                monitorItRunRequest = MonitorItRunRequest.disconnect;
                executor.execute(executorNode);
            }
            
            void start() {
                monitorItRunRequest = MonitorItRunRequest.start;
                executor.execute(executorNode);
            }
            
            void stop() {
                monitorItRunRequest = MonitorItRunRequest.stop;
                executor.execute(executorNode);
            }
            /* (non-Javadoc)
             * @see java.lang.Runnable#run()
             */
            @Override
            public void run() {
                Channel c = channel.get();
                if(c==null) return;
                switch(monitorItRunRequest) {
                case create:
                    monitor.compareAndSet(null,c.createMonitor(this, pvRequest,"", pvOption));
                    return;
                case disconnect:
                    c.destroy();
                    channel.set(null);
                    monitor.set(null);
                    return;
                case start:
                    monitor.get().start();
                    return;
                case stop:
                    monitor.get().stop();
                    return;
                
                }
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
                this.monitor.compareAndSet(null, monitor);
                monitorConnectCallback();
            }
            /* (non-Javadoc)
             * @see org.epics.pvData.monitor.MonitorRequester#monitorEvent(org.epics.pvData.monitor.Monitor)
             */
            @Override
            public void monitorEvent(Monitor monitor) {
                poll.poll();
            }

            /* (non-Javadoc)
             * @see org.epics.ca.channelAccess.client.ChannelMonitorRequester#unlisten()
             */
            @Override
            public void unlisten() {
                // TODO Auto-generated method stub
                
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
                    display.asyncExec( new Runnable() {
                        public void run() {
                            while(true) {
                                Monitor theMonitor = monitor.get();
                                if(theMonitor==null) break;
                                MonitorElement monitorElement = theMonitor.poll();
                                if(monitorElement==null) break;
                                PVStructure pvStructure = monitorElement.getPVStructure();
                                if(pvStructure==null) {
                                    consoleText.append("monitor occured");
                                    consoleText.append(String.format("%n"));
                                } else {
                                    BitSet changeBitSet = monitorElement.getChangedBitSet();
                                    BitSet overrunBitSet = monitorElement.getOverrunBitSet();
                                    printModified = PrintModifiedFactory.create(pvStructure, changeBitSet, overrunBitSet, consoleText);
                                    printModified.print();
                                }
                                theMonitor.release(monitorElement);
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
        
        private void disableOptions() {
            algorithmCombo.setEnabled(false);
            deadbandText.setEnabled(false);
            queueSizeText.setEnabled(false);
        }

        private void enableOptions() {
            algorithmCombo.setEnabled(true);
            deadbandText.setEnabled(true);
            queueSizeText.setEnabled(true);
        }
    }
}