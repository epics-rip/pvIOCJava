/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.swtshell;
import java.util.*;
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
    public static void init(Display display) {
        Introspect introspect = new Introspect(display);
        introspect.start();
    }
    
    private static class Introspect  implements Requestor{
        private Display display;
        private Shell shell;
        private PVShell pvShell;
        private Text showText;
        
        private Introspect(Display display) {
            this.display = display;
        }
        
        public void start() {
            shell = new Shell(display);
            shell.setText("probe");
            GridLayout gridLayout = new GridLayout();
            gridLayout.numColumns = 1;
            shell.setLayout(gridLayout);
            Composite composite = new Composite(shell,SWT.BORDER);
            gridLayout = new GridLayout();
            gridLayout.numColumns = 1;
            composite.setLayout(gridLayout);
            pvShell = new PVShell(composite,this);
            new ProcessShell(composite,this);
            new GetShell(composite,this);
            Composite showComposite = new Composite(composite,SWT.BORDER);
            gridLayout = new GridLayout();
            gridLayout.numColumns = 1;
            showComposite.setLayout(gridLayout);
            Button clearItem = new Button(showComposite,SWT.PUSH);
            clearItem.setText("&Clear");
            clearItem.addSelectionListener(new SelectionListener() {
                public void widgetDefaultSelected(SelectionEvent arg0) {
                    widgetSelected(arg0);
                }
                public void widgetSelected(SelectionEvent arg0) {
                    showText.selectAll();
                    showText.clearSelection();
                    showText.setText("");
                }
            });
            showText = new Text(showComposite,SWT.BORDER|SWT.WRAP|SWT.V_SCROLL|SWT.READ_ONLY);
            //showText.setSize(sizeX,sizeY); DOES NOT WORK
            Swtshell.makeBlanks(showText,20,100);
            shell.pack();
            pvShell.start();
            showText.selectAll();
            showText.clearSelection();
            showText.setText("");
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
            display.syncExec( new Runnable() {
                public void run() {
                    showText.append(String.format("%s%n",message));
                }
                
            });
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
                processButton = new Button(processWidget,SWT.NONE);
                processButton.setText("process");
                processButton.addSelectionListener(this);
                showProcessorButton = new Button(processWidget,SWT.NONE);
                showProcessorButton.setText("showProcessor");
                showProcessorButton.addSelectionListener(this);
                releaseProcessorButton = new Button(processWidget,SWT.NONE);
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
                Channel channel = pvShell.getChannel();
                if(channel==null) {
                    requestor.message(String.format("no record selected%n"),MessageType.error);
                    return;
                }
                if(arg0.getSource()==processButton) {
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
                if(arg0.getSource()==showProcessorButton) {
                    String name = recordProcess.getRecordProcessRequestorName();
                    requestor.message("recordProcessor " + name, MessageType.info);
                    return;
                }
                if(arg0.getSource()==releaseProcessorButton) {
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
                private ChannelExecutor channelExecutor;
                
                private Process(Channel channel,Requestor requestor) {
                    this.channel = channel;
                    this.requestor = requestor;
                }
                
                private void process() {
                    channelProcess = channel.createChannelProcess(this);
                    channelExecutor = new ChannelExecutor(requestor,channel);
                    allDone = false;
                    channelExecutor.request(this);
                }
                /* (non-Javadoc)
                 * @see java.lang.Runnable#run()
                 */
                public void run() {
                    channelProcess.process();
                    lock.lock();
                    try {
                        if(!allDone) {                       
                            waitDone.await();
                        }
                    } catch (InterruptedException ie) {
                        return;
                    } finally {
                        lock.unlock();
                    }
                    display.syncExec( new Runnable() {
                        public void run() {
                            showText.append(String.format("process complete%n"));
                        }      
                    });
                    channel.destroy(channelProcess);
                    channelExecutor = null;
                    channelProcess = null;
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
                    display.syncExec( new Runnable() {
                        public void run() {
                            requestor.message(message, messageType);
                        }
                        
                    });
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
                boolean getProperties = propertyButton.getSelection();
                Property[] properties = null;
                if(getProperties) properties = pvShell.getPropertys();
                Get get = new Get(channel,requestor,process);
                get.get(channelField, properties);
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
                private ChannelExecutor channelExecutor;
                private ChannelFieldGroup getFieldGroup;
                private ChannelData channelData;
                
                private Get(Channel channel,Requestor requestor,boolean process) {
                    this.channel = channel;
                    this.requestor = requestor;
                    this.process = process;
                }
                
                private void get(ChannelField channelField,Property[] properties) {
                    channelGet = channel.createChannelGet(this, process);
                    channelExecutor = new ChannelExecutor(requestor,channel);
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
                    allDone = false;
                    channelData = ChannelDataFactory.createData(channel,getFieldGroup);
                    if(channelData==null) {
                        requestor.message("ChannelDataFactory.createData failed",MessageType.error);
                        return;
                    }
                    channelExecutor.request(this);
                }
                /* (non-Javadoc)
                 * @see java.lang.Runnable#run()
                 */
                public void run() {
                    channelGet.get(getFieldGroup);
                    lock.lock();
                    try {
                        if(!allDone) {                       
                            waitDone.await();
                        }
                    } catch (InterruptedException ie) {
                        return;
                    } finally {
                        lock.unlock();
                    }
                    final ChannelData copy = channelData;
                    final String channelName = channel.getChannelName();
                    display.syncExec( new Runnable() {
                        public void run() {
                            showText.append(String.format("%s%n",channelName));
                            java.util.List<ChannelDataPV> channelDataPVList = copy.getChannelDataPVList();
                            Iterator<ChannelDataPV> iter = channelDataPVList.iterator();
                            while(iter.hasNext()) {
                                ChannelDataPV channelDataPV = iter.next();
                                PVData pvData = channelDataPV.getPVData();
                                showText.append(String.format("    %s %s%n",
                                    pvData.getFullFieldName(),Swtshell.pvDataToString(pvData)));
                            }
                        }      
                    });
                    channel.destroy(channelGet);
                    channelData = null;
                    getFieldGroup = null;
                    channelExecutor = null;
                    channelGet = null;
                }

                /* (non-Javadoc)
                 * @see org.epics.ioc.ca.ChannelGetRequestor#nextDelayedGetData(org.epics.ioc.pvAccess.PVData)
                 */
                public boolean nextDelayedGetData(PVData data) {
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
                    display.syncExec( new Runnable() {
                        public void run() {
                            requestor.message(message, messageType);
                        }
                        
                    });
                }
                /* (non-Javadoc)
                 * @see org.epics.ioc.ca.ChannelGetRequestor#nextGetData(org.epics.ioc.ca.Channel, org.epics.ioc.ca.ChannelField, org.epics.ioc.pvAccess.PVData)
                 */
                public boolean nextGetData(ChannelField field, PVData data) {
                    channelData.dataPut(data);
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
        }
    }
}
