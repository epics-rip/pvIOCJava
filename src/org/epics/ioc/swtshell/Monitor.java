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
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.epics.ioc.ca.CD;
import org.epics.ioc.ca.CDMonitor;
import org.epics.ioc.ca.CDMonitorFactory;
import org.epics.ioc.ca.CDMonitorRequester;
import org.epics.ioc.ca.Channel;
import org.epics.ioc.ca.ChannelField;
import org.epics.ioc.ca.ChannelFieldGroup;
import org.epics.ioc.ca.ChannelFieldGroupListener;
import org.epics.ioc.pv.Field;
import org.epics.ioc.pv.Type;
import org.epics.ioc.util.MessageType;
import org.epics.ioc.util.Requester;
import org.epics.ioc.util.ScanPriority;
/**
 * ChannelMonitorRequester a channelField.
 * The controls in the first row are:
 * <ul>
 *     <li>connect<br />
 *     Clicking this brings up a small window that allows the user to connect to a channel.
 *     The window has two controls:
 *     <ul>
 *        <li>select<br />
 *        Clicking this brings up a list of the names of all the records in the local JavaIOC.
 *        Selecting a name determines the channel.
 *        </li>
 *        <li>deadbandText widget< />
 *        A channel name followed by the enter key selects a channel.
 *        </li>
 *        </ul>
 *     </ul>
 *     Assuming a channel has been selected. Another small window is presented that allows the
 *     user to select a field of the channel. It also has two controls:
 *     <ul>
 *        <li>select<br />
 *        Clicking this brings up a tree showing all the fields in the channel.
 *        The user can select a field, which determines the channelField.
 *        </li>
 *        <li>deadbandText widget< />
 *        A name followed by the enter key selects a field. Entering a null field name selects all
 *        the fields in the channel.
 *        </li>
 *        </ul>
 *     </ul>
 *     </li>
 *     <li>property<br />
 *     This allows the user to select properties to be displayed.
 *     </li>
 *     <li>radio button selection<br />
 *     This allows the user to select three modes for monitoring: onChange, onAbsolute, and onPercentage.
 *     obAbsolute and onPercentage are only valid if the channelField has a scalar numeric type.
 *     </li>
 *     <li>deadband<br />
 *     This is the deadband for onAbsolute and onPercertage.
 *     </li>
 *  </ul>
 *  When connected the "connect" button changes to a "disconnect" button. Clicking it disconnects.
 *  The second row has just one control "startMonitor". Clicking it starts monitoring.
 *  When monitoring starts the "startMonitor" button changes to a "stopMonitor" button.
 * @author mrk
 *
 */
public class Monitor {

    /**
     * Called by SwtShell after the default constructor has been called.
     * @param display The display.
     */
    public static void init(Display display) {
        MonitorImpl monitorImpl = new MonitorImpl(display);
        monitorImpl.init(display);
    }

    private static class MonitorImpl extends AbstractChannelShell
    implements ChannelFieldGroupListener, CDMonitorRequester  {

        private MonitorImpl(Display display) {
            super("monitor");
        }

        /**
         * Called by SwtShell after the default constructor has been called.
         * @param display The display.
         */
        public void init(Display display) {
            super.start(display);
        }

        private static enum MonitorType{ put, change, absolute, percentage }
        private int queueSize = 3;
        private MonitorType monitorType = MonitorType.put;
        private double deadband = 0.0;       
        private Button propertyButton;          
        private Button putButton;
        private Button changeButton;
        private Button absoluteButton;
        private Button percentageButton;
        private Button startStopButton;
        private Text deadbandText;

        private ChannelField valueField;
        private ChannelFieldGroup channelFieldGroup;
        private String[] propertyNames = null;
        private CDMonitor cdMonitor = null;
        private boolean isMonitoring = false;

        private Lock lock = new ReentrantLock();
        private Condition waitDone = lock.newCondition();
        private boolean allDone = false;

        public void startClient(Composite parent) {
            Composite rowComposite = new Composite(parent,SWT.BORDER);
            GridLayout gridLayout = new GridLayout();
            gridLayout.numColumns = 3;
            rowComposite.setLayout(gridLayout);
            super.connectButtonCreate(rowComposite);
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
            rowComposite = new Composite(parent,SWT.BORDER);
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
            Composite startStopComposite = new Composite(parent,SWT.BORDER);
            gridLayout = new GridLayout();
            gridLayout.numColumns = 1;
            startStopComposite.setLayout(gridLayout);
            startStopButton = new Button(startStopComposite,SWT.PUSH);
            startStopButton.setText("startMonitor");
            startStopButton.addSelectionListener(this);
            startStopButton.setEnabled(false);
            disableOptions();
        }
        
        /* (non-Javadoc)
         * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
         */
        public void widgetSelected(SelectionEvent arg0) {
            Object object = arg0.getSource();
            if(object==connectButton) {
                super.connectButtonSelected();
                switch(connectState) {
                case connected:
                    GetChannelField getChannelField = new GetChannelField(shell,this,channel);
                    valueField = getChannelField.getChannelField();
                    if(valueField==null) {
                        message(String.format("no field selected%n"),MessageType.error);
                        return;
                    }
                    cdMonitor = null;
                    channelFieldGroup = channel.createFieldGroup(this);
                    channelFieldGroup.addChannelField(valueField);
                    enableOptions();
                    propertyNames = null;
                    startStopButton.setEnabled(true);
                    startStopButton.setText("startMonitor");
                    return;
                case disconnected:   
                    isMonitoring = false;
                    cdMonitor = null;
                    valueField = null;
                    channelFieldGroup = null;
                    startStopButton.setText("startMonitor");
                    disableOptions();                   
                    startStopButton.setEnabled(false);
                    return;
                }
            }
            if(object==propertyButton) {
                GetProperty getProperty = new GetProperty(shell);
                propertyNames = getProperty.open(valueField);
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
                    propertyNames = null;
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
                cdMonitor.start(
                        queueSize,
                        getRequesterName(),
                        ScanPriority.getJavaPriority(ScanPriority.low));
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
         * @see org.epics.ioc.ca.ChannelFieldGroupListener#accessRightsChange(org.epics.ioc.ca.Channel, org.epics.ioc.ca.ChannelField)
         */
        public void accessRightsChange(Channel channel, ChannelField channelField) {
            // Nothing to do
        }
        
        private void disableOptions() {
            propertyButton.setEnabled(false);
            putButton.setEnabled(false);
            changeButton.setEnabled(false);
            absoluteButton.setEnabled(false);
            percentageButton.setEnabled(false);
            deadbandText.setEnabled(false);
        }
        
        private void enableOptions() {
            if(valueField==null) return;
            Field field = valueField.getField();
            Type type = field.getType();
            if(!type.isNumeric()) {
                monitorType = MonitorType.put;
                putButton.setSelection(true);
                return;
            }
            propertyButton.setEnabled(true);
            putButton.setEnabled(true);
            changeButton.setEnabled(true);
            absoluteButton.setEnabled(true);
            percentageButton.setEnabled(true);
            deadbandText.setEnabled(true);
        }

        private void createMonitor() {
            if(propertyNames!=null) {
                if(!valueField.getField().getFieldName().equals("value")) {
                    channelFieldGroup.removeChannelField(valueField);
                }
                for(String fieldName: propertyNames) {
                    ChannelField propChannelField = channel.createChannelField(fieldName);
                    if(propChannelField!=null) {
                        channelFieldGroup.addChannelField(propChannelField);
                    } else {
                        message("fieldName " + fieldName + " not found",MessageType.warning);
                    }
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