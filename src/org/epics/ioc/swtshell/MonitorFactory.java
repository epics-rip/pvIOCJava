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
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.epics.pvData.channelAccess.Channel;
import org.epics.pvData.channelAccess.ChannelMonitor;
import org.epics.pvData.channelAccess.ChannelMonitorRequester;
import org.epics.pvData.factory.PVDataFactory;
import org.epics.pvData.misc.BitSet;
import org.epics.pvData.misc.Executor;
import org.epics.pvData.misc.ExecutorNode;
import org.epics.pvData.pv.Field;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVInt;
import org.epics.pvData.pv.PVDataCreate;
import org.epics.pvData.pv.PVDouble;
import org.epics.pvData.pv.PVString;
import org.epics.pvData.pv.PVStructure;
import org.epics.pvData.pv.Requester;
import org.epics.pvData.pv.ScalarType;

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
    implements DisposeListener,CreateRequestRequester,SelectionListener,Runnable  {

        private MonitorImpl(Display display) {
            this.display = display;
        }

        private static final Executor executor = SwtshellFactory.getExecutor();
        private static final PVDataCreate pvDataCreate = PVDataFactory.getPVDataCreate();
        private static final String windowName = "monitor";
        private ExecutorNode executorNode = executor.createNode(this);
        private Display display;
        private boolean isDisposed = false;
        private Shell shell = null;
        private Requester requester = null;
        private Channel channel = null;
        private Button connectButton;
        private Button createRequestButton = null;
        private Button disconnectButton;
        
        private Text consoleText = null;
        private PVStructure pvRequest = null;
        private PVStructure pvOption = null;
        private PVString pvAlgorithm = null;
        private PVInt pvQueueSize = null;
        private Text queueSizeText = null;
        private PVDouble pvDeadband = null;
        private Text deadbandText;
        private Text simulateDelayText;
        private Monitor monitor = new Monitor();
        
        private int queueSize = 3;
        private double deadband = 0.0;
        private double simulateDelay = 0.0;
        private Button putButton;
        private Button changeButton;
        private Button absoluteButton;
        private Button percentageButton;
        private Button startStopButton;
        
        
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
            composite  = new Composite(shell,SWT.BORDER);
            gridLayout = new GridLayout();
            gridLayout.numColumns = 3;
            composite.setLayout(gridLayout);
            Composite queueComposite = new Composite(composite,SWT.BORDER);
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
            Composite deadbandComposite = new Composite(composite,SWT.BORDER);
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
            
            composite = new Composite(shell,SWT.BORDER);
            gridLayout = new GridLayout();
            gridLayout.numColumns = 1;
            composite.setLayout(gridLayout);
            Composite monitorTypeComposite = new Composite(composite,SWT.BORDER);
            gridLayout = new GridLayout();
            gridLayout.numColumns = 4;
            monitorTypeComposite.setLayout(gridLayout);
            putButton = new Button(monitorTypeComposite,SWT.RADIO);
            putButton.setText("onPut");
            putButton.addSelectionListener(this);
            putButton.setSelection(true);
            changeButton = new Button(monitorTypeComposite,SWT.RADIO);
            changeButton.setText("onChange");
            changeButton.addSelectionListener(this);
            absoluteButton = new Button(monitorTypeComposite,SWT.RADIO);
            absoluteButton.setText("onAbsolute");
            absoluteButton.addSelectionListener(this);
            percentageButton = new Button(monitorTypeComposite,SWT.RADIO);
            percentageButton.setText("onPercentage");
            percentageButton.addSelectionListener(this);
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
                Channel channel = this.channel;
                if(channel!=null) {
                    channel.destroy();
                    this.channel = null;
                }
                ConnectChannel connectChannel = ConnectChannelFactory.create(shell, this);
                connectChannel.connect();
                return;
            }
            if(object==createRequestButton) {
                CreateRequest createRequest = CreateRequestFactory.create(shell, channel, this);
                createRequest.create();
                return;
            }
            if(object==disconnectButton) {
                connectButton.setEnabled(true);
                disconnectButton.setEnabled(false);
                channel.destroy();
                channel = null;
                if(isMonitoring) {
                    isMonitoring = false;
                    startStopButton.setEnabled(false);
                    startStopButton.setText("startMonitor");
                    enableOptions();
                }
                return;
            }
            if(object==putButton) {
                if(!putButton.getSelection()) return;
                pvAlgorithm.put("onPut");
                return;
            }
            if(object==changeButton) {
                if(!changeButton.getSelection()) return;
                pvAlgorithm.put("onChange");
                return;
            }
            if(object==absoluteButton) {
                if(!absoluteButton.getSelection()) return;
                pvAlgorithm.put("onAbsoluteChange");
                return;
            }
            if(object==percentageButton) {
                if(!percentageButton.getSelection()) return;
                pvAlgorithm.put("onPercentChange");
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
         * @see org.epics.ioc.ca.ChannelRequester#channelStateChange(org.epics.ioc.ca.Channel, boolean)
         */
        @Override
        public void channelStateChange(Channel c, boolean isConnected) {
            display.asyncExec(this);
        }
        /* (non-Javadoc)
         * @see org.epics.pvData.channelAccess.ChannelRequester#channelCreated(org.epics.pvData.channelAccess.Channel)
         */
        @Override
        public void channelCreated(Channel channel) {
            this.channel = channel;
            message("channel created",MessageType.info);
            display.asyncExec(this);
        }
        /* (non-Javadoc)
         * @see org.epics.pvData.channelAccess.ChannelRequester#channelNotCreated()
         */
        @Override
        public void channelNotCreated() {
            message("channelNotCreated",MessageType.error);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.swtshell.CreateRequestRequester#request(org.epics.pvData.pv.PVStructure, boolean)
         */
        @Override
        public void request(PVStructure pvRequest, boolean isShared) {
            this.pvRequest = pvRequest;
            createRequestButton.setEnabled(false);
            startStopButton.setEnabled(true);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelRequester#disconnect(org.epics.ioc.ca.Channel)
         */
        @Override
        public void destroy(Channel c) {
            display.asyncExec(this);
        }
        /* (non-Javadoc)
         * @see java.lang.Runnable#run()
         */
        @Override
        public void run() {
            if(isDisposed) {
                if(channel!=null) channel.destroy();
                return;
            }
            if(channel==null) return;
            boolean isConnected = channel.isConnected();
            if(isConnected) {
                connectButton.setEnabled(false);
                disconnectButton.setEnabled(true);
                createRequestButton.setEnabled(true);
                startStopButton.setText("startMonitor");
            } else {
                connectButton.setEnabled(true);
                createRequestButton.setEnabled(false);
                disconnectButton.setEnabled(false);
                startStopButton.setText("startMonitor");
            }
        }
        
        private enum MonitorRunRequest {
            start,
            stop
        }
        
        
        private class Monitor implements
        Runnable,
        ChannelMonitorRequester
        {
            private ExecutorNode executorNode = executor.createNode(this);
            private MonitorRunRequest runRequest;
            private ChannelMonitor channelMonitor = null;
            private PrintModified printModified = null;
            
            void start() {
                runRequest = MonitorRunRequest.start;
                executor.execute(executorNode);
            }
            
            void stop() {
                runRequest = MonitorRunRequest.stop;
                executor.execute(executorNode);
            }
            /* (non-Javadoc)
             * @see java.lang.Runnable#run()
             */
            @Override
            public void run() {
                switch(runRequest) {
                case start:
                    channel.createChannelMonitor(channel, this, pvRequest, pvRequest.getField().getFieldName(), pvOption, executor);
                    break;
                case stop:
                    if(channelMonitor!=null) {
                        channelMonitor.destroy();
                        channelMonitor = null;
                    }
                    break;
                }
                
            }
            /* (non-Javadoc)
             * @see org.epics.pvData.channelAccess.ChannelMonitorRequester#channelMonitorConnect(org.epics.pvData.channelAccess.ChannelMonitor)
             */
            @Override
            public void channelMonitorConnect(ChannelMonitor channelMonitor) {
                this.channelMonitor = channelMonitor;
                if(channelMonitor==null) {
                    display.asyncExec( new Runnable() {
                        public void run() {
                            startStopButton.setText("startMonitor");
                            enableOptions();
                        }

                    });
                } else {
                    channelMonitor.start();
                }
            }
            /* (non-Javadoc)
             * @see org.epics.pvData.channelAccess.ChannelMonitorRequester#monitorEvent(org.epics.pvData.pv.PVStructure, org.epics.pvData.misc.BitSet, org.epics.pvData.misc.BitSet)
             */
            @Override
            public void monitorEvent(PVStructure pvStructure,
                    BitSet changeBitSet, BitSet overrunBitSet)
            {
                printModified = PrintModifiedFactory.create(pvStructure, changeBitSet, overrunBitSet, consoleText);
                display.asyncExec( new Runnable() {
                    public void run() {
                        printModified.print();
                    }
                });
                if(simulateDelay>0.0) {
                    long millis = (long)(simulateDelay*1000.0);
                    try{
                        Thread.sleep(millis, 0);
                    } catch (InterruptedException e) {

                    }
                }
            }

            /* (non-Javadoc)
             * @see org.epics.pvData.channelAccess.ChannelMonitorRequester#unlisten()
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
            
        }

        private void disableOptions() {
            putButton.setEnabled(false);
            changeButton.setEnabled(false);
            absoluteButton.setEnabled(false);
            percentageButton.setEnabled(false);
            deadbandText.setEnabled(false);
            queueSizeText.setEnabled(false);
        }

        private void enableOptions() {
            putButton.setEnabled(true);
            changeButton.setEnabled(true);
            absoluteButton.setEnabled(true);
            percentageButton.setEnabled(true);
            deadbandText.setEnabled(true);
            queueSizeText.setEnabled(true);
        }
    }
}