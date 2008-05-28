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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.epics.ioc.ca.CD;
import org.epics.ioc.ca.CDMonitor;
import org.epics.ioc.ca.CDMonitorFactory;
import org.epics.ioc.ca.CDMonitorRequester;
import org.epics.ioc.ca.Channel;
import org.epics.ioc.ca.ChannelField;
import org.epics.ioc.ca.ChannelFieldGroup;
import org.epics.ioc.ca.ChannelFieldGroupListener;
import org.epics.ioc.ca.ChannelListener;
import org.epics.ioc.pv.Field;
import org.epics.ioc.pv.Type;
import org.epics.ioc.util.IOCExecutor;
import org.epics.ioc.util.IOCExecutorFactory;
import org.epics.ioc.util.MessageType;
import org.epics.ioc.util.Requester;
import org.epics.ioc.util.ScanPriority;
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
    implements DisposeListener,Requester,ChannelListener,
    ChannelFieldGroupListener,SelectionListener,CDMonitorRequester  {

        private MonitorImpl(Display display) {
            this.display = display;
        }

        private static IOCExecutor iocExecutor
            = IOCExecutorFactory.create("swtshellMonitor", ScanPriority.low);
        private static String windowName = "monitor";
        private Display display;
        private Shell shell = null;
        private Requester requester = null;
        private Channel channel = null;
        private ChannelField channelField = null;
        private ChannelConnect channelConnect = null;
        private static enum MonitorType{ put, change, absolute, percentage }
        private int queueSize = 10;
        private MonitorType monitorType = MonitorType.put;
        private double deadband = 0.0;       
        private Button propertyButton;          
        private Button putButton;
        private Button changeButton;
        private Button absoluteButton;
        private Button percentageButton;
        private Button startStopButton;
        private Text deadbandText;
        private Text consoleText = null;

        private String[] propertyNames = null;
        private CDMonitor cdMonitor = null;
        private boolean isMonitoring = false;

        private Lock lock = new ReentrantLock();
        private Condition waitDone = lock.newCondition();
        private boolean allDone = false;
        
        /* (non-Javadoc)
         * @see org.eclipse.swt.events.DisposeListener#widgetDisposed(org.eclipse.swt.events.DisposeEvent)
         */
        public void widgetDisposed(DisposeEvent e) {
            if(channel!=null) channel.destroy();
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
         * @see org.epics.ioc.ca.ChannelListener#channelStateChange(org.epics.ioc.ca.Channel, boolean)
         */
        public void channelStateChange(Channel c, boolean isConnected) {
            if(isConnected) {
                channel = channelConnect.getChannel();
                String fieldName = channel.getFieldName();
                channelField = channel.createChannelField(fieldName);
                cdMonitor = null;
                startStopButton.setText("startMonitor");
                enableOptions();
                return;
            } else { 
                isMonitoring = false;
                if(cdMonitor!=null) cdMonitor.stop();
                cdMonitor = null;
                startStopButton.setText("startMonitor");
                disableOptions();                   
                return;
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelListener#disconnect(org.epics.ioc.ca.Channel)
         */
        public void destroy(Channel c) {
            channelStateChange(c,false);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelFieldGroupListener#accessRightsChange(org.epics.ioc.ca.Channel, org.epics.ioc.ca.ChannelField)
         */
        public void accessRightsChange(Channel channel, ChannelField channelField) {
            // Nothing to do
        }
        
        private void start() {
            shell = new Shell(display);
            shell.setText(windowName);
            GridLayout gridLayout = new GridLayout();
            gridLayout.numColumns = 1;
            shell.setLayout(gridLayout);
            channelConnect = ChannelConnectFactory.create(this,this);
            channelConnect.createWidgets(shell,true,true);
            Composite rowComposite = new Composite(shell,SWT.BORDER);
            gridLayout = new GridLayout();
            gridLayout.numColumns = 2;
            rowComposite.setLayout(gridLayout);
            propertyButton = new Button(rowComposite,SWT.PUSH);
            propertyButton.setText("property");
            propertyButton.addSelectionListener(this);
            Composite deadbandComposite = new Composite(rowComposite,SWT.BORDER);
            gridLayout = new GridLayout();
            gridLayout.numColumns = 2;
            deadbandComposite.setLayout(gridLayout);
            new Label(deadbandComposite,SWT.NONE).setText("deadband");
            deadbandText = new Text(deadbandComposite,SWT.BORDER);
            GridData gridData = new GridData(); 
            gridData.widthHint = 75;
            deadbandText.setLayoutData(gridData);
            deadbandText.addSelectionListener(this);
            rowComposite = new Composite(shell,SWT.BORDER);
            gridLayout = new GridLayout();
            gridLayout.numColumns = 1;
            rowComposite.setLayout(gridLayout);
            Composite monitorTypeComposite = new Composite(rowComposite,SWT.BORDER);
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
            disableOptions();
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
            shell.open();
            shell.addDisposeListener(this);
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

            if(object==propertyButton) {
                PropertyGet propertyGet = PropertyGetFactory.create(shell);
                propertyNames = propertyGet.getPropertyNames(channelField);
            }
            if(object==putButton) {
                if(!putButton.getSelection()) return;
                monitorType = MonitorType.put;
                return;
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
            if(object==deadbandText) {
                String value = deadbandText.getText();
                try {
                    deadband = Double.parseDouble(value);
                } catch (NumberFormatException e) {
                    message("Illegal value", MessageType.error);
                }
            }
            if(object==startStopButton) {
                if(isMonitoring) {
                    isMonitoring = false;
                    cdMonitor.stop();
                    cdMonitor = null;
                    startStopButton.setText("startMonitor");
                    enableOptions();
                    return;
                }
                if(cdMonitor==null) createMonitor();
                if(cdMonitor==null) {
                    message("no cdMonitor", MessageType.error);
                    return;
                }
                isMonitoring = true;
                startStopButton.setText("stopMonitor");
                disableOptions();
                startStopButton.setEnabled(true);
                cdMonitor.start(queueSize,iocExecutor);
                return;
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelMonitorRequester#dataOverrun(int)
         */
        public void dataOverrun(int number) {
            message(String.format("dataOverrun number = %d", number), MessageType.info);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelMonitorRequester#monitorData(org.epics.ioc.ca.CD)
         */
        public void monitorCD(final CD cD) {
            allDone = false;
            display.asyncExec( new Runnable() {
                public void run() {
                    CDPrint cdPrint = CDPrintFactory.create(cD.getCDRecord(),consoleText);
                    cdPrint.print();
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
        
        private void disableOptions() {
            propertyButton.setEnabled(false);
            putButton.setEnabled(false);
            changeButton.setEnabled(false);
            absoluteButton.setEnabled(false);
            percentageButton.setEnabled(false);
            deadbandText.setEnabled(false);
            startStopButton.setEnabled(false);
        }
        
        private void enableOptions() {
            Field field = channelField.getField();
            Type type = field.getType();
            if(!type.isNumeric()) {
                monitorType = MonitorType.put;
                putButton.setSelection(true);
            }
            propertyButton.setEnabled(true);
            putButton.setEnabled(true);
            changeButton.setEnabled(true);
            absoluteButton.setEnabled(true);
            percentageButton.setEnabled(true);
            deadbandText.setEnabled(true);
            startStopButton.setEnabled(true);
        }

        private void createMonitor() {
            ChannelFieldGroup channelFieldGroup = channel.createFieldGroup(this);
            channelFieldGroup.addChannelField(channelField);
            if(propertyNames!=null && propertyNames.length>0) {
                for(String propertyName: propertyNames) {
                    ChannelField propChannelField = channelField.findProperty(propertyName);
                    if(propChannelField==null) {
                        requester.message(String.format(
                                "property %s not found%n", propertyName),MessageType.error);
                        continue;
                    }
                    channelFieldGroup.addChannelField(propChannelField);
                }
            }
            
            cdMonitor = CDMonitorFactory.create(channel, this);
            List<ChannelField> channelFieldList = channelFieldGroup.getList();
            ChannelField channelField = channelFieldList.get(0);
            switch(monitorType) {
            case put:
                cdMonitor.lookForPut(channelField, true);
                break;
            case change:
                cdMonitor.lookForChange(channelField, true);
                break;
            case absolute:
                cdMonitor.lookForAbsoluteChange(channelField, deadband);
                break;
            case percentage:
                cdMonitor.lookForPercentageChange(channelField, deadband);
                break;
            }
            for(int i = 1; i<channelFieldList.size(); i++) {
                channelField = channelFieldList.get(i);
                String fieldName = channelField.getField().getFieldName();
                boolean causeMonitor = true;
                if(fieldName.equals("timeStamp")) causeMonitor = false;
                cdMonitor.lookForPut(channelField, causeMonitor);
            }
        }
    }
}