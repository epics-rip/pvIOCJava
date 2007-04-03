/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.swtshell;

import java.util.concurrent.locks.*;

import org.eclipse.swt.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.widgets.*;

import org.epics.ioc.pv.*;
import org.epics.ioc.util.*;
import org.epics.ioc.ca.*;
import org.epics.ioc.db.*;
import org.epics.ioc.process.*;
/**
 * @author mrk
 *
 */
public class Probe {
    static private IOCDB iocdb = IOCDBFactory.getMaster();
    private static IOCExecutor iocExecutor = IOCExecutorFactory.create("swtshell:Probe");
    private static ScanPriority scanPriority = ScanPriority.higher;
    public static void init(Display display) {
        ProbeImpl probeImpl = new ProbeImpl(display);
        probeImpl.start();
    }

    private enum ConnectState {connected,disconnected}

    private static class ProbeImpl  implements Requestor, Runnable{
        private Display display;
        private Shell shell;
        private Text consoleText;
        private MessageQueue messageQueue = MessageQueueFactory.create(3);

        private ProbeImpl(Display display) {
            this.display = display;
        }

        public void start() {
            shell = new Shell(display);
            shell.setText("probe");
            GridLayout gridLayout = new GridLayout();
            gridLayout.numColumns = 1;
            shell.setLayout(gridLayout);
            Composite shellComposite = new Composite(shell,SWT.NONE);
            gridLayout = new GridLayout();
            gridLayout.numColumns = 1;
            shellComposite.setLayout(gridLayout);
            GridData gridData = new GridData(GridData.FILL_BOTH);
            shellComposite.setLayoutData(gridData);   
            Composite processorComposite = new Composite(shellComposite,SWT.NONE);
            gridLayout = new GridLayout();
            gridLayout.numColumns = 1;
            processorComposite.setLayout(gridLayout);
            gridData = new GridData(GridData.FILL_HORIZONTAL);
            processorComposite.setLayoutData(gridData);
            new ProcessorShell(processorComposite,this);
            Composite processGetPutComposite = new Composite(shellComposite,SWT.NONE);
            gridLayout = new GridLayout();
            gridLayout.numColumns = 3;
            processGetPutComposite.setLayout(gridLayout);
            gridData = new GridData(GridData.FILL_HORIZONTAL);
            processGetPutComposite.setLayoutData(gridData);
            new ProcessShell(processGetPutComposite,this);      
            new GetShell(processGetPutComposite,this);
            new PutShell(processGetPutComposite,this);
            Composite consoleComposite = new Composite(shellComposite,SWT.BORDER);
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
            consoleText.setLayoutData(gridData);
            shell.open();
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requestor#getRequestorName()
         */
        public String getRequestorName() {
            return "swtshell.probe";
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
        
        private abstract class ShellBase implements SelectionListener,ChannelStateListener,DisposeListener {
            protected Channel channel = null;
            protected Requestor requestor;
            
            protected ShellBase(Requestor requestor) {
                this.requestor = requestor;
            }
            /* (non-Javadoc)
             * @see org.eclipse.swt.events.SelectionListener#widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent)
             */
            public void widgetDefaultSelected(SelectionEvent arg0) {
                widgetSelected(arg0);
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelStateListener#channelStateChange(org.epics.ioc.ca.Channel, boolean)
             */
            public void channelStateChange(Channel c, boolean isConnected) {
                // TODO Auto-generated method stub
                
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelStateListener#disconnect(org.epics.ioc.ca.Channel)
             */
            public void disconnect(Channel c) {
                // TODO 
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
             * @see org.eclipse.swt.events.DisposeListener#widgetDisposed(org.eclipse.swt.events.DisposeEvent)
             */
            public void widgetDisposed(DisposeEvent e) {
                if(channel!=null) channel.destroy();
            }
        }

        private class ProcessShell extends ShellBase
        {
            private Button connectButton;
            private Button processButton;
            private Process process = null;
            private Channel channel = null;
            private ConnectState connectState = ConnectState.disconnected;
            private String[] connectStateText = {"connect    ","disconnect"};

            private ProcessShell(Composite parentWidget,Requestor requestor) {
                super(requestor);
                Composite processWidget = new Composite(parentWidget,SWT.BORDER);
                GridLayout gridLayout = new GridLayout();
                gridLayout.numColumns = 3;
                processWidget.setLayout(gridLayout);
                Label label = new Label(processWidget,SWT.NONE);
                label.setText("Process");
                connectButton = new Button(processWidget,SWT.PUSH);
                connectButton.setText(connectStateText[0]);
                connectButton.addSelectionListener(this);
                processButton = new Button(processWidget,SWT.PUSH);
                processButton.setText("process");
                processButton.addSelectionListener(this);               
                processWidget.addDisposeListener(this);
                processButton.setEnabled(false);
            }
            /* (non-Javadoc)
             * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
             */
            public void widgetSelected(SelectionEvent arg0) {
                Object object = arg0.getSource(); 
                if(object==connectButton) {
                    switch(connectState) {
                    case disconnected:
                        GetChannel getChannel = new GetChannel(shell,requestor,this);
                        channel = getChannel.getChannel();
                        if(channel==null) {
                            requestor.message(String.format("no record selected%n"),MessageType.error);
                            return;
                        }
                        process = new Process(channel,requestor);
                        boolean result = process.connect();
                        if(result) {
                            connectState = ConnectState.connected;
                            requestor.message(String.format("connected%n"),MessageType.info);
                            connectButton.setText(connectStateText[1]);
                            processButton.setEnabled(true);
                        } else {
                            requestor.message(String.format("not connected%n"),MessageType.info);
                            process = null;
                        }
                        return;
                    case connected:
                        process.disconnect();
                        process = null;
                        connectState = ConnectState.disconnected;
                        connectButton.setText(connectStateText[0]);
                        processButton.setEnabled(false);
                        return;
                    }
                }
                if(object==processButton) {
                    if(process==null) {
                        requestor.message(String.format("not connected%n"),MessageType.error);
                        return;
                    }
                    process.process();
                    return;
                }
            }
        }
        
        private class ProcessorShell extends ShellBase {
            private Composite rowWidget;
            private Button connectButton;
            private Button showProcessorButton;
            private Button releaseProcessorButton;
            private Button showThreadsButton;
            private ConnectState connectState = ConnectState.disconnected;
            private String[] connectStateText = {"connect    ","disconnect"};
            
            private Channel channel = null;

            private ProcessorShell(Composite parentWidget,Requestor requestor) {
                super(requestor);
                rowWidget = new Composite(parentWidget,SWT.BORDER);
                GridLayout gridLayout = new GridLayout();
                gridLayout.numColumns = 5;
                rowWidget.setLayout(gridLayout);
                Label label = new Label(rowWidget,SWT.NONE);
                label.setText("Processor");
                connectButton = new Button(rowWidget,SWT.PUSH);
                connectButton.setText(connectStateText[0]);
                connectButton.addSelectionListener(this);         
                showProcessorButton = new Button(rowWidget,SWT.PUSH);
                showProcessorButton.setText("showProcessor");
                showProcessorButton.addSelectionListener(this);
                releaseProcessorButton = new Button(rowWidget,SWT.PUSH);
                releaseProcessorButton.setText("releaseProcessor");
                releaseProcessorButton.addSelectionListener(this);
                showThreadsButton = new Button(rowWidget,SWT.PUSH);
                showThreadsButton.setText("showThreads");
                showThreadsButton.addSelectionListener(this);
                showProcessorButton.setEnabled(false);
                releaseProcessorButton.setEnabled(false);
            }
            /* (non-Javadoc)
             * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
             */
            public void widgetSelected(SelectionEvent arg0) {
                Object object = arg0.getSource(); 
                if(object==connectButton) {
                    switch(connectState) {
                    case disconnected:
                        GetChannel getChannel = new GetChannel(shell,requestor,this);
                        channel = getChannel.getChannel();
                        if(channel==null) {
                            requestor.message(String.format("no record selected%n"),MessageType.error);
                            return;
                        }
                        connectState = ConnectState.connected;
                        requestor.message(String.format("connected%n"),MessageType.info);
                        connectButton.setText(connectStateText[1]);
                        showProcessorButton.setEnabled(true);
                        releaseProcessorButton.setEnabled(true);
                        return;
                    case connected:
                        connectState = ConnectState.disconnected;
                        connectButton.setText(connectStateText[0]);
                        showProcessorButton.setEnabled(false);
                        releaseProcessorButton.setEnabled(false);
                        return;
                    }
                }
                if(object==showProcessorButton) {
                    String recordName = channel.getChannelName();
                    DBRecord dbRecord = iocdb.findRecord(recordName);
                    if(dbRecord==null) {
                        requestor.message("channel is not local", MessageType.error);
                        return;
                    }
                    RecordProcess recordProcess = dbRecord.getRecordProcess();
                    if(recordProcess==null) {
                        requestor.message("recordProcess is null", MessageType.error);
                        return;
                    }
                    String name = recordProcess.getRecordProcessRequestorName();
                    requestor.message("recordProcessor " + name, MessageType.info);
                    return;
                }
                if(object==releaseProcessorButton) {
                    if(channel==null) {
                        requestor.message(String.format("no record selected%n"),MessageType.error);
                        return;
                    }
                    String recordName = channel.getChannelName();
                    DBRecord dbRecord = iocdb.findRecord(recordName);
                    if(dbRecord==null) {
                        requestor.message("channel is not local", MessageType.error);
                        return;
                    }
                    RecordProcess recordProcess = dbRecord.getRecordProcess();
                    if(recordProcess==null) {
                        requestor.message("recordProcess is null", MessageType.error);
                        return;
                    }
                    MessageBox mb = new MessageBox(
                            rowWidget.getShell(),SWT.ICON_WARNING|SWT.YES|SWT.NO);
                    mb.setMessage("VERY DANGEROUS. DO YOU WANT TO PROCEED?");
                    int rc = mb.open();
                    if(rc==SWT.YES) {
                        recordProcess.releaseRecordProcessRequestor();
                    }
                    return;
                }
                if(object==showThreadsButton) {
                    PeriodicScanner periodicScanner = ScannerFactory.getPeriodicScanner();
                    EventScanner eventScanner = ScannerFactory.getEventScanner();
                    requestor.message(periodicScanner.toString(), MessageType.info);
                    requestor.message(eventScanner.toString(), MessageType.info);
                    return;
                }
            }
        }
        
        private class GetShell extends ShellBase {
            private Button connectButton;
            private Button getButton;
            private Button processButton;
            private Button propertyButton;
            private ConnectState connectState = ConnectState.disconnected;
            private String[] connectStateText = {"connect    ","disconnect"};
            private Channel channel = null;
            private Get get = null;

            private GetShell(Composite parentWidget,Requestor requestor) {
                super(requestor);
                Composite getWidget = new Composite(parentWidget,SWT.BORDER);
                GridLayout gridLayout = new GridLayout();
                gridLayout.numColumns = 5;
                getWidget.setLayout(gridLayout);
                Label label = new Label(getWidget,SWT.NONE);
                label.setText("Get");
                connectButton = new Button(getWidget,SWT.PUSH);
                connectButton.setText(connectStateText[0]);
                connectButton.addSelectionListener(this);
                getButton = new Button(getWidget,SWT.NONE);
                getButton.setText("get");
                getButton.addSelectionListener(this);
                processButton = new Button(getWidget,SWT.CHECK);
                processButton.setText("process");
                processButton.setSelection(false);
                propertyButton = new Button(getWidget,SWT.CHECK);
                propertyButton.setText("properties");
                propertyButton.setSelection(true);
                getButton.setEnabled(false);
                processButton.setEnabled(true);
                propertyButton.setEnabled(true);
            }
            /* (non-Javadoc)
             * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
             */
            public void widgetSelected(SelectionEvent arg0) {
                Object object = arg0.getSource(); 
                if(object==connectButton) {
                    switch(connectState) {
                    case disconnected:
                        GetChannel getChannel = new GetChannel(shell,requestor,this);
                        channel = getChannel.getChannel();
                        if(channel==null) {
                            requestor.message(String.format("no record selected%n"),MessageType.error);
                            return;
                        }
                        GetChannelField getChannelField = new GetChannelField(shell,requestor,channel);
                        ChannelField channelField = getChannelField.getChannelField(channel);
                        if(channelField==null) {
                            requestor.message(String.format("no field selected%n"),MessageType.error);
                            return;
                        }
                        boolean process = processButton.getSelection();
                        Property[] properties = null;
                        Field field = channelField.getField();
                        Type type = field.getType();
                        boolean propertysOK = true;
                        if(type==Type.pvStructure) {
                            propertysOK = false;
                        } else if(type==Type.pvArray){
                            Array array= (Array)field;
                            Type elementType = array.getElementType();
                            if(elementType==Type.pvArray || elementType==Type.pvStructure) {
                                propertysOK = false;
                            }
                        }
                        
                        if(!propertysOK) propertyButton.setSelection(false);
                        boolean getProperties = propertyButton.getSelection();
                        if(getProperties) properties = channelField.getField().getPropertys();
                        get = new Get(channel,requestor,process);
                        boolean result = get.connect(channelField, properties);
                        if(result) {
                            getButton.setEnabled(true);
                            processButton.setEnabled(false);
                            propertyButton.setEnabled(false);
                            connectState = ConnectState.connected;
                            requestor.message(String.format("connected%n"),MessageType.info);
                            connectButton.setText(connectStateText[1]);
                        } else {
                            requestor.message(String.format("not connected%n"),MessageType.info);
                            get = null;
                        }
                        return;
                    case connected:
                        get.disconnect();
                        get = null;
                        connectState = ConnectState.disconnected;
                        connectButton.setText(connectStateText[0]);
                        getButton.setEnabled(false);
                        processButton.setEnabled(true);
                        propertyButton.setEnabled(true);
                        return;
                    }
                }
                if(object==getButton) {
                    if(get==null) {
                        requestor.message(String.format("not connected%n"),MessageType.info);
                        return;
                    }
                    CD cD = get.get();
                    if(cD==null) return;
                    CDRecordPrint cdRecordPrint = new CDRecordPrint(cD.getCDRecord(),consoleText);
                    cdRecordPrint.print();
                    return;
                }
            }
        }

        private class PutShell extends ShellBase {
            private Button connectButton;
            private Button putButton;
            private Button processButton;
            private ConnectState connectState = ConnectState.disconnected;
            private String[] connectStateText = {"connect    ","disconnect"};
            private Channel channel;
            private Put put = null;

            private PutShell(Composite parentWidput,Requestor requestor) {
                super(requestor);
                Composite putComposite = new Composite(parentWidput,SWT.BORDER);
                GridLayout gridLayout = new GridLayout();
                gridLayout.numColumns = 4;
                putComposite.setLayout(gridLayout);
                Label label = new Label(putComposite,SWT.NONE);
                label.setText("Put");
                connectButton = new Button(putComposite,SWT.PUSH);
                connectButton.setText(connectStateText[0]);
                connectButton.addSelectionListener(this);
                putButton = new Button(putComposite,SWT.NONE);
                putButton.setText("put");
                putButton.addSelectionListener(this);
                processButton = new Button(putComposite,SWT.CHECK);
                processButton.setText("process");
                processButton.setSelection(false);
                putButton.setEnabled(false);
                processButton.setEnabled(true);
            }
            /* (non-Javadoc)
             * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
             */
            public void widgetSelected(SelectionEvent arg0) {
                Object object = arg0.getSource(); 
                if(object==connectButton) {
                    switch(connectState) {
                    case disconnected:
                        GetChannel getChannel = new GetChannel(shell,requestor,this);
                        channel = getChannel.getChannel();
                        if(channel==null) {
                            requestor.message(String.format("no record selected%n"),MessageType.error);
                            return;
                        }
                        GetChannelField getChannelField = new GetChannelField(shell,requestor,channel);
                        ChannelField channelField = getChannelField.getChannelField(channel);
                        if(channelField==null) {
                            requestor.message(String.format("no field selected%n"),MessageType.error);
                            return;
                        }
                        boolean process = processButton.getSelection();
                        put = new Put(channel,requestor,process);
                        boolean result = put.connect(channelField);
                        if(result) {
                            connectState = ConnectState.connected;
                            requestor.message(String.format("connected%n"),MessageType.info);
                            connectButton.setText(connectStateText[1]);
                            putButton.setEnabled(true);
                            processButton.setEnabled(false);
                        } else {
                            requestor.message(String.format("not connected%n"),MessageType.info);
                            put = null;
                        }
                        return;
                    case connected:
                        put.disconnect();
                        put = null;
                        connectState = ConnectState.disconnected;
                        connectButton.setText(connectStateText[0]);
                        putButton.setEnabled(false);
                        processButton.setEnabled(true);
                        return;
                    }
                    return;
                }
                if(object==putButton) {
                    if(put==null) {
                        requestor.message(String.format("not connected%n"),MessageType.info);
                        return;
                    }
                    put.put();
                    return;
                }
            }
        }
        
        private class Process implements
        Runnable,
        ChannelProcessRequestor,ChannelStateListener
        {   
            private Lock lock = new ReentrantLock();
            private Condition waitDone = lock.newCondition();
            private boolean allDone = false;
            private Channel channel;
            final private Requestor requestor;
            private ChannelProcess channelProcess;

            private Process(Channel channel,Requestor requestor) {
                this.channel = channel;
                this.requestor = requestor;
            }

            private boolean connect() {
                channelProcess = channel.createChannelProcess(this,true);
                if(channelProcess==null) return false;
                return true;
            }

            private void disconnect() {
                channel.destroy(channelProcess);
            }

            private void process() {
                if(channelProcess==null) {
                    requestor.message("not connected", MessageType.info);
                }
                allDone = false;
                iocExecutor.execute(this, scanPriority);
            }
            /* (non-Javadoc)
             * @see java.lang.Runnable#run()
             */
            public void run() {
                allDone = false;
                channelProcess.process();
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
                requestor.message("processComplete", MessageType.info);
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
            public void message(final String message, final MessageType messageType) {
                requestor.message(message, MessageType.info);
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelProcessRequestor#processDone(org.epics.ioc.util.RequestResult)
             */
            public void processDone(RequestResult requestResult) {
                lock.lock();
                try {
                    allDone = true;
                    waitDone.signal();
                } finally {
                    lock.unlock();
                }
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelStateListener#channelStateChange(org.epics.ioc.ca.Channel, boolean)
             */
            public void channelStateChange(Channel c, boolean isConnected) {
                // TODO Auto-generated method stub

            }
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelStateListener#disconnect(org.epics.ioc.ca.Channel)
             */
            public void disconnect(Channel c) {
                // TODO Auto-generated method stub

            }
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelFieldGroupListener#accessRightsChange(org.epics.ioc.ca.Channel, org.epics.ioc.ca.ChannelField)
             */
            public void accessRightsChange(Channel channel, ChannelField channelField) {
                // TODO Auto-generated method stub

            }
        }
        
        private class Get implements
        Runnable,
        ChannelCDGetRequestor,
        ChannelStateListener, ChannelFieldGroupListener
        {
            private Lock lock = new ReentrantLock();
            private Condition waitDone = lock.newCondition();
            private boolean allDone = false;
            private Channel channel;
            final private Requestor requestor;
            private boolean process;
            private ChannelCDGet channelCDGet;

            private Get(Channel channel,Requestor requestor,boolean process) {
                this.channel = channel;
                this.requestor = requestor;
                this.process = process;
            }

            private boolean connect(ChannelField channelField,Property[] properties) {
                ChannelFieldGroup getFieldGroup = channel.createFieldGroup(this);
                getFieldGroup.addChannelField(channelField);
                if(properties!=null && properties.length>0) {
                    for(Property property: properties) {
                        ChannelFindFieldResult result;
                        channel.findField(null);
                        String propertyName = property.getPropertyName();
                        result = channel.findField(propertyName);
                        if(result!=ChannelFindFieldResult.thisChannel) {
                            requestor.message(String.format(
                                    "property %s%n", propertyName),MessageType.error);
                            continue;
                        }
                        ChannelField propertyField = channel.getChannelField();
                        getFieldGroup.addChannelField(propertyField);
                    }
                }
                channelCDGet = channel.createChannelCDGet(getFieldGroup, this,true, process,true);
                if(channelCDGet==null) {
                    channelCDGet = channel.createChannelCDGet(getFieldGroup, this,true);
                }
                if(channelCDGet==null) return false;
                return true;
            }

            private void disconnect() {
                channel.destroy(channelCDGet);
            }

            private CD get() {                
                allDone = false;               
                iocExecutor.execute(this, scanPriority);
                lock.lock();
                try {
                    while(!allDone) {                       
                        waitDone.await();
                    }
                } catch (InterruptedException ie) {
                } finally {
                    lock.unlock();
                }

                return channelCDGet.getCD();
            }
            /* (non-Javadoc)
             * @see java.lang.Runnable#run()
             */
            public void run() {
                channelCDGet.get();
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
            public void message(final String message, final MessageType messageType) {
                requestor.message(message, MessageType.info);
            }           
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelCDGetRequestor#getDone(org.epics.ioc.util.RequestResult)
             */
            public void getDone(RequestResult requestResult) {
                lock.lock();
                try {
                    allDone = true;
                    waitDone.signal();
                } finally {
                    lock.unlock();
                }
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelStateListener#channelStateChange(org.epics.ioc.ca.Channel, boolean)
             */
            public void channelStateChange(Channel c, boolean isConnected) {
                // TODO Auto-generated method stub

            }
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelStateListener#disconnect(org.epics.ioc.ca.Channel)
             */
            public void disconnect(Channel c) {
                // TODO Auto-generated method stub

            }
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelFieldGroupListener#accessRightsChange(org.epics.ioc.ca.Channel, org.epics.ioc.ca.ChannelField)
             */
            public void accessRightsChange(Channel channel, ChannelField channelField) {
                // TODO Auto-generated method stub

            }
        }

        private static final boolean supportAlso = false;

        private class Put implements
        Runnable,
        ChannelCDPutRequestor,
        ChannelFieldGroupListener
        {
            private Lock lock = new ReentrantLock();
            private Condition waitDone = lock.newCondition();
            private Channel channel;
            final private Requestor requestor;
            private boolean process;
            private boolean allDone = false;
            private RequestResult requestResult;

            private ChannelCDPut channelCDPut;

            private Put(Channel channel,Requestor requestor,boolean process) {
                this.channel = channel;
                this.requestor = requestor;
                this.process = process;
            }

            private boolean connect(ChannelField channelField) {
                ChannelFieldGroup putFieldGroup = channel.createFieldGroup(this);
                putFieldGroup.addChannelField(channelField);
                channelCDPut = channel.createChannelCDPut(putFieldGroup, this, supportAlso,process,true);
                if(channelCDPut==null) return false;
                return true;
            }

            private void disconnect() {
                channel.destroy(channelCDPut);
            }

            private void put() {
                allDone = false;
                channelCDPut.get();
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
                if(requestResult!=RequestResult.success) {
                    requestor.message("get failed", MessageType.error);
                }
                allDone = false;
                CD cD = channelCDPut.getCD();
                cD.clearNumPuts();
                CDRecord cdRecord = cD.getCDRecord();
                GetCDValue getCDValue = new GetCDValue(shell);
                getCDValue.getValue(cdRecord);
                iocExecutor.execute(this, scanPriority);
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
                if(requestResult!=RequestResult.success) {
                    requestor.message("get failed", MessageType.error);
                }
            }
            /* (non-Javadoc)
             * @see java.lang.Runnable#run()
             */
            public void run() {
                channelCDPut.put();
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.util.Requestor#putRequestorName()
             */
            public String getRequestorName() {
                return requestor.getRequestorName();
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.util.Requestor#message(java.lang.String, org.epics.ioc.util.MessageType)
             */
            public void message(final String message, final MessageType messageType) {
                requestor.message(message, MessageType.info);
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelCDPutRequestor#getDone(org.epics.ioc.util.RequestResult)
             */
            public void getDone(RequestResult requestResult) {
                lock.lock();
                try {
                    this.requestResult = requestResult;
                    allDone = true;
                    waitDone.signal();
                } finally {
                    lock.unlock();
                }
            }

            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelCDPutRequestor#putDone(org.epics.ioc.util.RequestResult)
             */
            public void putDone(RequestResult requestResult) {
                lock.lock();
                try {
                    this.requestResult = requestResult;
                    allDone = true;
                    waitDone.signal();
                } finally {
                    lock.unlock();
                }
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelFieldGroupListener#accessRightsChange(org.epics.ioc.ca.Channel, org.epics.ioc.ca.ChannelField)
             */
            public void accessRightsChange(Channel channel, ChannelField channelField) {
                // TODO Auto-generated method stub

            }
        }
    }
}
