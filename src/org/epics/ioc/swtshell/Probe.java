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
    
    private static class ProbeImpl  implements Requestor, Runnable{
        private Display display;
        private Shell shell;
        private PVShell pvShell;
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
            Composite shellComposite = new Composite(shell,SWT.BORDER);
            gridLayout = new GridLayout();
            gridLayout.numColumns = 1;
            shellComposite.setLayout(gridLayout);
            GridData gridData = new GridData(GridData.FILL_BOTH);
            shellComposite.setLayoutData(gridData);   
            pvShell = new PVShell(shellComposite,this);
            Composite actionComposite = new Composite(shellComposite,SWT.NONE);
            gridLayout = new GridLayout();
            gridLayout.numColumns = 3;
            actionComposite.setLayout(gridLayout);
            gridData = new GridData(GridData.FILL_HORIZONTAL);
            actionComposite.setLayoutData(gridData);
            new ProcessShell(actionComposite,this);
            new GetShell(actionComposite,this);
            new PutShell(actionComposite,this);
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
        
        private class ProcessShell implements SelectionListener {
            private Requestor requestor;
            private Composite processWidget;
            private Button processButton;
            private Button showProcessorButton;
            private Button releaseProcessorButton;
            
            private ProcessShell(Composite parentWidget,Requestor requestor) {
                this.requestor = requestor;
                processWidget = new Composite(parentWidget,SWT.BORDER);
                GridLayout gridLayout = new GridLayout();
                gridLayout.numColumns = 3;
                processWidget.setLayout(gridLayout);
                processButton = new Button(processWidget,SWT.PUSH);
                processButton.setText("process");
                processButton.addSelectionListener(this);
                showProcessorButton = new Button(processWidget,SWT.PUSH);
                showProcessorButton.setText("showProcessor");
                showProcessorButton.addSelectionListener(this);
                releaseProcessorButton = new Button(processWidget,SWT.PUSH);
                releaseProcessorButton.setText("releaseProcessor");
                releaseProcessorButton.addSelectionListener(this);
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
                Channel channel = pvShell.getChannel();
                if(channel==null) {
                    requestor.message(String.format("no record selected%n"),MessageType.error);
                    return;
                }
                if(object==processButton) {
                    Process process = new Process(channel,requestor);
                    process.process();
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
                if(object==showProcessorButton) {
                    String name = recordProcess.getRecordProcessRequestorName();
                    requestor.message("recordProcessor " + name, MessageType.info);
                    return;
                }
                if(object==releaseProcessorButton) {
                    MessageBox mb = new MessageBox(
                        processWidget.getShell(),SWT.ICON_WARNING|SWT.YES|SWT.NO);
                    mb.setMessage("VERY DANGEROUS. DO YOU WANT TO PROCEED?");
                    int rc = mb.open();
                    if(rc==SWT.YES) {
                        recordProcess.releaseRecordProcessRequestor();
                    }
                    return;
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
                
                private void process() {
                    channelProcess = channel.createChannelProcess(this);
                    if(channelProcess==null) return;
                    allDone = false;
                    iocExecutor.execute(this, scanPriority);
                }
                /* (non-Javadoc)
                 * @see java.lang.Runnable#run()
                 */
                public void run() {
                    boolean result = channelProcess.process();
                    if(result) {
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
                    } else {
                        result = channelProcess.processSelf();
                        if(result) {
                            requestor.message("processSelf", MessageType.info);
                        } else {
                            requestor.message("record could not be processed", MessageType.info);
                        }
                    }
                    channel.destroy(channelProcess);
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
        }
        
        private class GetShell implements SelectionListener {
            private Requestor requestor;
            private Button getButton;
            private Button processButton;
            private Button propertyButton;
            
            private GetShell(Composite parentWidget,Requestor requestor) {
                this.requestor = requestor;
                Composite getWidget = new Composite(parentWidget,SWT.BORDER);
                GridLayout gridLayout = new GridLayout();
                gridLayout.numColumns = 3;
                getWidget.setLayout(gridLayout);
                getButton = new Button(getWidget,SWT.NONE);
                getButton.setText("get");
                getButton.addSelectionListener(this);
                processButton = new Button(getWidget,SWT.CHECK);
                processButton.setText("process");
                processButton.setSelection(false);
                propertyButton = new Button(getWidget,SWT.CHECK);
                propertyButton.setText("properties");
                propertyButton.setSelection(true);
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
                Channel channel = pvShell.getChannel();
                if(channel==null) {
                    requestor.message(String.format("no record selected%n"),MessageType.error);
                    return;
                }
                ChannelField channelField = pvShell.getChannelField();
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
                if(!propertysOK) {
                    propertyButton.setSelection(false);
                    propertyButton.setEnabled(false);
                } else {
                    propertyButton.setEnabled(true);
                }
                boolean getProperties = propertyButton.getSelection();
                if(getProperties) properties = pvShell.getPropertys();
                Get get = new Get(channel,requestor,process);
                ChannelData channelData = get.get(channelField, properties);
                if(channelData==null) return;
                CDRecordPrint cdRecordPrint = new CDRecordPrint(channelData.getCDRecord(),consoleText);
                cdRecordPrint.print();
            }
        }
                
        private class PutShell implements SelectionListener {
            private Requestor requestor;
            private Button putButton;
            private Button processButton;
            
            private PutShell(Composite parentWidput,Requestor requestor) {
                this.requestor = requestor;
                Composite putComposite = new Composite(parentWidput,SWT.BORDER);
                GridLayout gridLayout = new GridLayout();
                gridLayout.numColumns = 3;
                putComposite.setLayout(gridLayout);
                putButton = new Button(putComposite,SWT.NONE);
                putButton.setText("put");
                putButton.addSelectionListener(this);
                processButton = new Button(putComposite,SWT.CHECK);
                processButton.setText("process");
                processButton.setSelection(false);
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
                Channel channel = pvShell.getChannel();
                if(channel==null) {
                    requestor.message(String.format("no record selected%n"),MessageType.error);
                    return;
                }
                ChannelField channelField = pvShell.getChannelField();
                if(channelField==null) {
                    requestor.message(String.format("no field selected%n"),MessageType.error);
                    return;
                }
                boolean process = processButton.getSelection();
                Put put = new Put(channel,requestor,process);
                put.put(channelField);
            }


        }
        
        private class Get implements
        Runnable,
        ChannelGetRequestor,
        ChannelStateListener, ChannelFieldGroupListener
        {
            private Lock lock = new ReentrantLock();
            private Condition waitDone = lock.newCondition();
            private boolean allDone = false;
            private Channel channel;
            final private Requestor requestor;
            private boolean process;
            private ChannelGet channelGet;
            private ChannelFieldGroup getFieldGroup;
            private ChannelData channelData;
            
            private Get(Channel channel,Requestor requestor,boolean process) {
                this.channel = channel;
                this.requestor = requestor;
                this.process = process;
            }
            
            private ChannelData get(ChannelField channelField,Property[] properties) {
                getFieldGroup = channel.createFieldGroup(this);
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
                channelGet = channel.createChannelGet(getFieldGroup, this, process);
                if(channelGet==null) {
                    channelGet = channel.createChannelGet(getFieldGroup, this,false);
                }
                if(channelGet==null) return null;
                allDone = false;
                channelData = ChannelDataFactory.createChannelData(channel,getFieldGroup,true);
                if(channelData==null) {
                    requestor.message("ChannelDataFactory.createData failed",MessageType.error);
                } else {
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
                }
                return channelData;
            }
            /* (non-Javadoc)
             * @see java.lang.Runnable#run()
             */
            public void run() {
                channelGet.get();
                channel.destroy(channelGet);
            }

            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelGetRequestor#nextDelayedGetData(org.epics.ioc.pvAccess.PVData)
             */
            public boolean nextDelayedGetField(PVField pvField) {
                return false;
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
             * @see org.epics.ioc.ca.ChannelGetRequestor#nextGetData(org.epics.ioc.ca.Channel, org.epics.ioc.ca.ChannelField, org.epics.ioc.pvAccess.PVData)
             */
            public boolean nextGetField(ChannelField channelField, PVField pvField) {
                channelData.dataPut(pvField);
                return false;
            }
            
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelGetRequestor#getDone(org.epics.ioc.util.RequestResult)
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
        
        private static boolean supportAlso = false;
        
        private class Put implements
        Runnable,
        ChannelDataPutRequestor,
        ChannelFieldGroupListener
        {
            
            private Lock lock = new ReentrantLock();
            private Condition waitDone = lock.newCondition();
            private Channel channel;
            final private Requestor requestor;
            private boolean process;
            private boolean allDone = false;
            
            private ChannelDataPut channelDataPut;
            
            private Put(Channel channel,Requestor requestor,boolean process) {
                this.channel = channel;
                this.requestor = requestor;
                this.process = process;
            }
            
            private void put(ChannelField channelField) {
                ChannelFieldGroup putFieldGroup = channel.createFieldGroup(this);
                putFieldGroup.addChannelField(channelField);
                channelDataPut = channel.createChannelDataPut(putFieldGroup, this, process, supportAlso);
                allDone = false;
                boolean result = channelDataPut.get();
                if(!result) {
                    requestor.message("channelDataPut.get failed", MessageType.error);
                    channel.destroy(channelDataPut);
                    return;
                }
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
                allDone = false;
                ChannelData channelData = channelDataPut.getChannelData();
                channelData.clearNumPuts();
                CDRecord cdRecord = channelData.getCDRecord();
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
                channel.destroy(channelDataPut);
            }
            /* (non-Javadoc)
             * @see java.lang.Runnable#run()
             */
            public void run() {
                channelDataPut.put();
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
             * @see org.epics.ioc.ca.ChannelDataPutRequestor#getDone()
             */
            public void getDone() {
                lock.lock();
                try {
                    allDone = true;
                    waitDone.signal();
                } finally {
                    lock.unlock();
                }
            }

            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelDataPutRequestor#putDone(org.epics.ioc.util.RequestResult)
             */
            public void putDone(RequestResult requestResult) {
                lock.lock();
                try {
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
