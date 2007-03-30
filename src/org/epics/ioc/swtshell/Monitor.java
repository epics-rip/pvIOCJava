/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.swtshell;

import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.swt.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.widgets.*;

import org.epics.ioc.pv.*;
import org.epics.ioc.util.*;
import org.epics.ioc.ca.*;
/**
 * @author mrk
 *
 */
public class Monitor {

    public static void init(Display display) {
        MonitorImpl monitorImpl = new MonitorImpl(display);
        monitorImpl.start();
    }

    private static class MonitorImpl  implements Requestor,Runnable {
        private Display display;
        private MessageQueue messageQueue = MessageQueueFactory.create(3);
        private Shell shell;
        private GetChannelShell getChannelShell;
        private Text consoleText;

        private MonitorImpl(Display display) {
            this.display = display;
        }

        public void start() {
            shell = new Shell(display);
            shell.setText("monitor");
            GridLayout gridLayout = new GridLayout();
            gridLayout.numColumns = 1;
            shell.setLayout(gridLayout);
            Composite shellComposite = new Composite(shell,SWT.BORDER);
            gridLayout = new GridLayout();
            gridLayout.numColumns = 1;
            shellComposite.setLayout(gridLayout);
            GridData gridData = new GridData(GridData.FILL_BOTH);
            shellComposite.setLayoutData(gridData);
            getChannelShell = new GetChannelShell(shellComposite,this);
            new MonitorChannel(shellComposite,this);
            Composite consoleComposite = new Composite(shellComposite,SWT.BORDER);
            gridLayout = new GridLayout();
            gridLayout.numColumns = 1;
            consoleComposite.setLayout(gridLayout);
            GridData consoleCompositeGridData = new GridData(GridData.FILL_BOTH);
            consoleComposite.setLayoutData(consoleCompositeGridData);
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
            GridData textGridData = new GridData(GridData.FILL_BOTH);
            consoleText.setLayoutData(textGridData);
            shell.open();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requestor#getRequestorName()
         */
        public String getRequestorName() {
            return "swtshell.monitor";
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requestor#message(java.lang.String, org.epics.ioc.util.MessageType)
         */
        public void message(final String message, MessageType messageType) {
            boolean syncExec = false;
            messageQueue.lock();
            try {
                if(messageQueue.isEmpty()) syncExec = true;
                if(messageQueue.isFull()) {
                    messageQueue.replaceLast(message, messageType);
                } else {
                    messageQueue.put(message, messageType);
                }
            } finally {
                messageQueue.unlock();
            }
            if(syncExec) {
                display.syncExec(this);
            }
        }
        
        public void run() {
            while(true) {
                String message = null;
                int numOverrun = 0;
                messageQueue.lock();
                try {
                    MessageNode messageNode = messageQueue.get();
                    numOverrun = messageQueue.getClearOverrun();
                    if(messageNode==null && numOverrun==0) break;
                    message = messageNode.message;
                } finally {
                    messageQueue.unlock();
                }
                if(numOverrun>0) {
                    consoleText.append(String.format("%n%d missed messages&n", numOverrun));
                }
                if(message!=null) {
                   consoleText.append(String.format("%s%n",message));
                }
            }
        }

        private static enum MonitorType{ change, absolute, percentage }
        private enum ConnectState {connected,disconnected}
        
        private class MonitorChannel implements
        SelectionListener,
        ChannelMonitorRequestor,
        ChannelFieldGroupListener
        {
            private int queueSize = 3;
            private MonitorType monitorType = MonitorType.change;
            private double deadband = 0.0;
            private Requestor requestor;
            private Button connectButton;          
            private Button propertyButton;          
            private Button changeButton;
            private Button absoluteButton;
            private Button percentageButton;
            private Button startStopButton;
            private Text text;
            
            private ChannelField valueField;
            private boolean propertysOK = false;
            private ChannelFieldGroup channelFieldGroup;
            private String[] propertyNames = null;
            private ChannelMonitor channelMonitor = null;
            private boolean isMonitoring = false;
            
            private Lock lock = new ReentrantLock();
            private Condition waitDone = lock.newCondition();
            private boolean allDone = false;
            private ConnectState connectState = ConnectState.disconnected;
            private String[] connectStateText = {"connect    ","disconnect"};

            public MonitorChannel(Composite parent,Requestor requestor) {
                this.requestor = requestor;
                Composite monitorComposite = new Composite(parent,SWT.BORDER);
                GridLayout gridLayout = new GridLayout();
                gridLayout.numColumns = 1;
                monitorComposite.setLayout(gridLayout);
                GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
                monitorComposite.setLayoutData(gridData);
                
                Composite monitorTypeComposite = new Composite(monitorComposite,SWT.BORDER);
                gridLayout = new GridLayout();
                gridLayout.numColumns = 6;
                monitorTypeComposite.setLayout(gridLayout);
                gridData = new GridData(GridData.FILL_HORIZONTAL);
                monitorTypeComposite.setLayoutData(gridData);
                connectButton = new Button(monitorTypeComposite,SWT.PUSH);                
                connectButton.addSelectionListener(this);
                propertyButton = new Button(monitorTypeComposite,SWT.PUSH);
                propertyButton.setText("property");
                propertyButton.addSelectionListener(this);
                Composite changeComposite = new Composite(monitorTypeComposite,SWT.BORDER);
                gridLayout = new GridLayout();
                gridLayout.numColumns = 3;
                changeComposite.setLayout(gridLayout);
                changeButton = new Button(changeComposite,SWT.RADIO);
                changeButton.setText("onChange");
                changeButton.addSelectionListener(this);
                changeButton.setSelection(true);
                absoluteButton = new Button(changeComposite,SWT.RADIO);
                absoluteButton.setText("onAbsolute");
                absoluteButton.addSelectionListener(this);
                percentageButton = new Button(changeComposite,SWT.RADIO);
                percentageButton.setText("onPercentage");
                changeButton.addSelectionListener(this);
                Composite deadbandComposite = new Composite(monitorTypeComposite,SWT.BORDER);
                gridLayout = new GridLayout();
                gridLayout.numColumns = 2;
                deadbandComposite.setLayout(gridLayout);
                gridData = new GridData(GridData.FILL_HORIZONTAL);
                deadbandComposite.setLayoutData(gridData);
                new Label(deadbandComposite,SWT.NONE).setText("deadband");
                text = new Text(deadbandComposite,SWT.BORDER);
                gridData = new GridData(GridData.FILL_HORIZONTAL);  
                text.setLayoutData(gridData);
                text.addSelectionListener(this);
                               
                Composite startStopComposite = new Composite(monitorComposite,SWT.BORDER);
                gridLayout = new GridLayout();
                gridLayout.numColumns = 1;
                startStopComposite.setLayout(gridLayout);
                startStopButton = new Button(startStopComposite,SWT.PUSH);
                startStopButton.setText("startMonitor");
                startStopButton.addSelectionListener(this);
                setConnectState(ConnectState.disconnected);
                
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
                    switch(connectState) {
                    case disconnected:
                        Channel channel = getChannelShell.getChannel();
                        if(channel==null) {
                            requestor.message(String.format("no record selected%n"),MessageType.error);
                            return;
                        }
                        setConnectState(ConnectState.connected);
                        valueField = getChannelShell.getChannelField();
                        channelMonitor = null;
                        channelFieldGroup = channel.createFieldGroup(this);
                        channelFieldGroup.addChannelField(valueField);
                        Field field = valueField.getField();
                        Type type = field.getType();
                        propertysOK = true;
                        if(type==Type.pvStructure) {
                            propertysOK = false;
                        } else if(type==Type.pvArray){
                            Array array= (Array)field;
                            Type elementType = array.getElementType();
                            if(elementType==Type.pvArray || elementType==Type.pvStructure) {
                                propertysOK = false;
                            }
                        }
                        if(!type.isNumeric()) {
                            monitorType = MonitorType.change;
                            changeButton.setSelection(true);
                        }
                        propertyButton.setEnabled(propertysOK);
                        propertyNames = null;
                        return;
                    case connected:                        
                        setConnectState(ConnectState.disconnected);
                        return;
                    }
                }
                if(object==propertyButton) {
                    if(propertysOK) {
                        GetProperty getProperty = new GetProperty(shell);
                        propertyNames = getProperty.open(valueField.getField());
                    }
                }
                if(object==changeButton) {
                    if(!changeButton.getSelection()) return;
                    monitorType = MonitorType.change;
                    return;
                }
                if(object==absoluteButton) {
                    if(!absoluteButton.getSelection()) return;
                    monitorType = MonitorType.absolute;
                    return;
                }
                if(object==percentageButton) {
                    if(!percentageButton.getSelection()) return;
                    monitorType = MonitorType.percentage;
                    return;
                }
                if(object==text) {
                    String value = text.getText();
                    try {
                        deadband = Double.parseDouble(value);
                    } catch (NumberFormatException e) {
                        requestor.message("Illegal value", MessageType.error);
                    }
                }
                if(object==startStopButton) {
                    if(isMonitoring) {
                        isMonitoring = false;
                        channelMonitor.stop();
                        startStopButton.setText("startMonitor");
                        return;
                    }
                    if(channelMonitor==null) createMonitor();
                    if(channelMonitor==null) {
                        message("no channelMonitor", MessageType.error);
                        return;
                    }
                    isMonitoring = true;
                    startStopButton.setText("stopMonitor");
                    channelMonitor.start(
                            this,queueSize,requestor.getRequestorName(),ScanPriority.low);
                    return;
                }
            }

            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelMonitorRequestor#dataOverrun(int)
             */
            public void dataOverrun(int number) {
                requestor.message(
                    String.format("dataOverrun number = %d", number), MessageType.info);
            }

            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelMonitorRequestor#monitorData(org.epics.ioc.ca.CD)
             */
            public void monitorCD(final CD cD) {
                allDone = false;
                display.syncExec( new Runnable() {
                    public void run() {
                        CDRecordPrint cdRecordPrint = 
                            new CDRecordPrint(cD.getCDRecord(),consoleText); 
                        cdRecordPrint.print();
                        lock.lock();
                        try {
                            allDone = true;
                                waitDone.signal();
                        } finally {
                            lock.unlock();
                        }
                    }

                });
                lock.lock();
                try {
                    while(!allDone) {                       
                        waitDone.await();
                    }
                } catch (InterruptedException ie) {
                    return;
                } finally {
                    lock.unlock();
                }
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.util.Requestor#getRequestorName()
             */
            public String getRequestorName() {
                return requestor.getRequestorName();
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.util.Requestor#message(java.lang.String, org.epics.ioc.util.MessageType)
             */
            public void message(String message, MessageType messageType) {
                requestor.message(message, messageType);   
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelFieldGroupListener#accessRightsChange(org.epics.ioc.ca.Channel, org.epics.ioc.ca.ChannelField)
             */
            public void accessRightsChange(Channel channel, ChannelField channelField) {
                // Nothing to do
            }
            
            private void setConnectState(ConnectState newState) {
                connectState = newState;
                boolean state;
                if(connectState==ConnectState.connected) {
                    state = true;
                    connectButton.setText(connectStateText[1]);
                } else {
                    if(channelMonitor!=null) {
                        Channel channel = getChannelShell.getChannel();
                        channel.destroy(channelMonitor);
                    }
                    channelMonitor = null;
                    state = false;
                    connectButton.setText(connectStateText[0]);
                    isMonitoring = false;
                }
                startStopButton.setText("startMonitor");
                propertyButton.setEnabled(state);
                changeButton.setEnabled(state);
                absoluteButton.setEnabled(state);
                percentageButton.setEnabled(state);
                startStopButton.setEnabled(state);
                text.setEnabled(state);
            }
            
            private void createMonitor() {
                Channel channel = getChannelShell.getChannel();
                if(propertyNames!=null) {
                    for(String fieldName: propertyNames) {
                        channel.findField(null);
                        ChannelFindFieldResult result = channel.findField(fieldName);
                        if(result==ChannelFindFieldResult.thisChannel) {
                            channelFieldGroup.addChannelField(channel.getChannelField());
                        } else {
                            requestor.message(
                                    "monitor remote property not implemented", MessageType.info);
                        }
                    }
                }
                channelMonitor = channel.createChannelMonitor(false,true);
                List<ChannelField> channelFieldList = channelFieldGroup.getList();
                ChannelField channelField = channelFieldList.get(0);
                switch(monitorType) {
                case change:
                    channelMonitor.lookForChange(channelField, true);
                    break;
                case absolute:
                    channelMonitor.lookForAbsoluteChange(channelField, deadband);
                    break;
                case percentage:
                    channelMonitor.lookForPercentageChange(channelField, deadband);
                    break;
                }
                for(int i = 1; i<channelFieldList.size(); i++) {
                    channelField = channelFieldList.get(i);
                    channelMonitor.lookForChange(channelField, false);
                }
            }
            
            
        }
    }
    private static class GetProperty extends Dialog implements SelectionListener {
        private Button doneButton;
        private Button[] propertyButtons;
        private String[] propertyNames = null;
        private Shell shell;
        
        private GetProperty(Shell parent) {
            super(parent,SWT.PRIMARY_MODAL|SWT.DIALOG_TRIM);
        }
        public String[] open(Field field) {
            Property[] propertys = field.getPropertys();
            int length = propertys.length;
            if(length==0) return null;
            shell = new Shell(getParent(),getStyle());
            shell.setText("getProperty");
            GridLayout gridLayout = new GridLayout();
            gridLayout.numColumns = 1;
            shell.setLayout(gridLayout);
            doneButton = new Button(shell,SWT.PUSH);
            doneButton.setText("Done");
            doneButton.addSelectionListener(this);
            propertyButtons = new Button[length];
            for(int i=0; i<length; i++) {
                Button button = new Button(shell,SWT.CHECK);
                button.setText(propertys[i].getPropertyName());
                propertyButtons[i] = button;
            }
            shell.pack();
            shell.open();
            Display display = getParent().getDisplay();
            while(!shell.isDisposed()) {
                if(!display.readAndDispatch()) {
                    display.sleep();
                }
            }
            return propertyNames;
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
            if(object==doneButton) {
                int numSelected = 0;
                for(Button button : propertyButtons) {
                    if(button.getSelection()) numSelected++;
                }
                if(numSelected==0) {
                    propertyNames = null;
                } else {
                    propertyNames = new String[numSelected];
                    int next = 0;
                    for(Button button : propertyButtons) {
                        if(button.getSelection()) {
                            propertyNames[next++] = button.getText();
                        }
                    }
                }
                shell.close();
                return;
            }
        }
    }
}
