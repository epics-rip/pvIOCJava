/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.swtshell;

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
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.epics.pvdata.pv.MessageType;
import org.epics.pvioc.pdrv.ConnectExceptionListener;
import org.epics.pvioc.pdrv.ConnectExceptionType;
import org.epics.pvioc.pdrv.Device;
import org.epics.pvioc.pdrv.Factory;
import org.epics.pvioc.pdrv.Port;
import org.epics.pvioc.pdrv.Status;
import org.epics.pvioc.pdrv.Trace;
import org.epics.pvioc.pdrv.TraceOptionChangeListener;
import org.epics.pvioc.pdrv.User;

/**
 * A shell for introspecting a JavaIOC Database.
 * The menubar at the top of the display provides access to the DBD (Database Definition Database).
 * The recordName row provides access to record instances. It provides two controls: a select
 * button and a text entry widget.
 * A record name can be entered in the text window followed by the enter key.
 * If the select button is pressed, a list of all record instances is displayed.
 * The user can select a name.
 * @author mrk
 *
 */
public class PortDriverFactory { 
    /**
     * A shell for introspecting the local IOC database.
     * @param display The display.
     */
    public static void init(Display display) {
        PortDriver portDriver = new PortDriver(display);
        portDriver.start();
    }
    
    
    private static class PortDriver implements DisposeListener,SelectionListener,ConnectExceptionListener,TraceOptionChangeListener{
        private Display display;
        private Shell shell;
        private Button selectPortButton;
        private Button selectDeviceButton;
        private Text selectText = null;
        private Button traceMaskPortButton;
        private Button traceFLOWPortButton;
        private Button traceDRIVERPortButton;
        private Button traceINTERPOSEPortButton;
        private Button traceSUPPORTPortButton;
        private Button traceERRORPortButton;
        private Button traceIOMaskPortButton;
        private Button traceIO_HEXPortButton;
        private Button traceIO_ESCAPEPortButton;
        private Button traceIO_ASCIIPortButton;
        private Button reportPortButton;
        private Button connectPortButton;
        private Button enablePortButton;
        private Button autoConnectPortButton;
        private Button traceMaskDeviceButton;
        private Button traceFLOWDeviceButton;
        private Button traceDRIVERDeviceButton;
        private Button traceSUPPORTDeviceButton;
        private Button traceERRORDeviceButton;
        private Button traceIOMaskDeviceButton;
        private Button traceIO_HEXDeviceButton;
        private Button traceIO_ESCAPEDeviceButton;
        private Button traceIO_ASCIIDeviceButton;
        private Button reportDeviceButton;
        private Button connectDeviceButton;
        private Button enableDeviceButton;
        private Button autoConnectDeviceButton;
        private Button clearButton;
        private Text consoleText;
        
        private Port port = null;
        private Device device = null;
        private User user = Factory.createUser(null);
        
        
        private PortDriver(Display display) {
            this.display = display;
        }
        
        private void start() {
            shell = new Shell(display);
            shell.setText("portDriver");
            GridLayout gridLayout = new GridLayout();
            gridLayout.numColumns = 1;
            shell.setLayout(gridLayout);
            
            Composite composite = new Composite(shell,SWT.BORDER);
            GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
            composite.setLayoutData(gridData);
            GridLayout layout = new GridLayout();
            layout.numColumns = 3;
            composite.setLayout(layout);
            selectPortButton = new Button(composite,SWT.PUSH);
            selectPortButton.setText("selectPort");
            selectPortButton.addSelectionListener(this);
            selectDeviceButton = new Button(composite,SWT.PUSH);
            selectDeviceButton.setText("selectDevice");
            selectDeviceButton.addSelectionListener(this);
            selectText = new Text(composite,SWT.BORDER);
            gridData = new GridData(GridData.FILL_HORIZONTAL);
            gridData.minimumWidth = 250;
            selectText.setLayoutData(gridData);
            selectText.setEnabled(false);
            
            composite = new Composite(shell,SWT.BORDER);
            gridData = new GridData(GridData.FILL_HORIZONTAL);
            composite.setLayoutData(gridData);
            layout = new GridLayout();
            layout.numColumns = 3;
            composite.setLayout(layout);
            new Label(composite,SWT.NONE).setText("Port  ");
            traceMaskPortButton = new Button(composite,SWT.PUSH);
            traceMaskPortButton.setText("setTraceMask");
            traceMaskPortButton.addSelectionListener(this);
            Group group = new Group(composite,SWT.NO_RADIO_GROUP);
            group.setLayout(new RowLayout(SWT.HORIZONTAL));
            traceFLOWPortButton = new Button(group,SWT.RADIO);
            traceFLOWPortButton.setText("FLOW");
            traceDRIVERPortButton = new Button(group,SWT.RADIO);
            traceDRIVERPortButton.setText("DRIVER");
            traceINTERPOSEPortButton = new Button(group,SWT.RADIO);
            traceINTERPOSEPortButton.setText("INTERPOSE");
            traceSUPPORTPortButton = new Button(group,SWT.RADIO);
            traceSUPPORTPortButton.setText("SUPPORT");
            traceERRORPortButton = new Button(group,SWT.RADIO);
            traceERRORPortButton.setText("ERROR");
            composite = new Composite(shell,SWT.BORDER);
            gridData = new GridData(GridData.FILL_HORIZONTAL);
            composite.setLayoutData(gridData);
            layout = new GridLayout();
            layout.numColumns = 3;
            composite.setLayout(layout);
            new Label(composite,SWT.NONE).setText("Port  ");
            traceIOMaskPortButton = new Button(composite,SWT.PUSH);
            traceIOMaskPortButton.setText("setTraceIOMask");
            traceIOMaskPortButton.addSelectionListener(this);
            group = new Group(composite,SWT.NO_RADIO_GROUP);
            group.setLayout(new RowLayout(SWT.HORIZONTAL));
            traceIO_HEXPortButton = new Button(group,SWT.RADIO);
            traceIO_HEXPortButton.setText("IO_HEX");
            traceIO_ESCAPEPortButton = new Button(group,SWT.RADIO);
            traceIO_ESCAPEPortButton.setText("IO_ESCAPE");
            traceIO_ASCIIPortButton = new Button(group,SWT.RADIO);
            traceIO_ASCIIPortButton.setText("IO_ASCII");
            composite = new Composite(shell,SWT.BORDER);
            gridData = new GridData(GridData.FILL_HORIZONTAL);
            composite.setLayoutData(gridData);
            layout = new GridLayout();
            layout.numColumns = 5;
            composite.setLayout(layout);
            new Label(composite,SWT.NONE).setText("Port  ");
            reportPortButton = new Button(composite,SWT.PUSH);
            reportPortButton.setText("report");
            reportPortButton.addSelectionListener(this);
            autoConnectPortButton = new Button(composite,SWT.PUSH);
            autoConnectPortButton.setText("noAutoConnect");
            autoConnectPortButton.addSelectionListener(this);
            connectPortButton = new Button(composite,SWT.PUSH);
            connectPortButton.setText("disconnect");
            connectPortButton.addSelectionListener(this);
            enablePortButton = new Button(composite,SWT.PUSH);
            enablePortButton.setText("disable");
            enablePortButton.addSelectionListener(this);
            composite = new Composite(shell,SWT.BORDER);
            gridData = new GridData(GridData.FILL_HORIZONTAL);
            composite.setLayoutData(gridData);
            layout = new GridLayout();
            layout.numColumns = 3;
            composite.setLayout(layout);
            new Label(composite,SWT.NONE).setText("Device");
            traceMaskDeviceButton = new Button(composite,SWT.PUSH);
            traceMaskDeviceButton.setText("setTraceMask");
            traceMaskDeviceButton.addSelectionListener(this);
            group = new Group(composite,SWT.NO_RADIO_GROUP);
            group.setLayout(new RowLayout(SWT.HORIZONTAL));
            traceFLOWDeviceButton = new Button(group,SWT.RADIO);
            traceFLOWDeviceButton.setText("FLOW");
            traceDRIVERDeviceButton = new Button(group,SWT.RADIO);
            traceDRIVERDeviceButton.setText("DRIVER");
            traceSUPPORTDeviceButton = new Button(group,SWT.RADIO);
            traceSUPPORTDeviceButton.setText("SUPPORT");
            traceERRORDeviceButton = new Button(group,SWT.RADIO);
            traceERRORDeviceButton.setText("ERROR");
            composite = new Composite(shell,SWT.BORDER);
            gridData = new GridData(GridData.FILL_HORIZONTAL);
            composite.setLayoutData(gridData);
            layout = new GridLayout();
            layout.numColumns = 3;
            composite.setLayout(layout);
            new Label(composite,SWT.NONE).setText("Device");
            traceIOMaskDeviceButton = new Button(composite,SWT.PUSH);
            traceIOMaskDeviceButton.setText("setTraceIOMask");
            traceIOMaskDeviceButton.addSelectionListener(this);
            group = new Group(composite,SWT.NO_RADIO_GROUP);
            group.setLayout(new RowLayout(SWT.HORIZONTAL));
            traceIO_HEXDeviceButton = new Button(group,SWT.RADIO);
            traceIO_HEXDeviceButton.setText("IO_HEX");
            traceIO_ESCAPEDeviceButton = new Button(group,SWT.RADIO);
            traceIO_ESCAPEDeviceButton.setText("IO_ESCAPE");
            traceIO_ASCIIDeviceButton = new Button(group,SWT.RADIO);
            traceIO_ASCIIDeviceButton.setText("IO_ASCII");
            composite = new Composite(shell,SWT.BORDER);
            gridData = new GridData(GridData.FILL_HORIZONTAL);
            composite.setLayoutData(gridData);
            layout = new GridLayout();
            layout.numColumns = 5;
            composite.setLayout(layout);
            new Label(composite,SWT.NONE).setText("Device");
            reportDeviceButton = new Button(composite,SWT.PUSH);
            reportDeviceButton.setText("report");
            reportDeviceButton.addSelectionListener(this);
            autoConnectDeviceButton = new Button(composite,SWT.PUSH);
            autoConnectDeviceButton.setText("noAutoConnect");
            autoConnectDeviceButton.addSelectionListener(this);
            connectDeviceButton = new Button(composite,SWT.PUSH);
            connectDeviceButton.setText("disconnect");
            connectDeviceButton.addSelectionListener(this);
            enableDeviceButton = new Button(composite,SWT.PUSH);
            enableDeviceButton.setText("disable");
            enableDeviceButton.addSelectionListener(this);
            composite = new Composite(shell,SWT.BORDER);
            layout = new GridLayout();
            layout.numColumns = 1;
            composite.setLayout(layout);
            gridData = new GridData(GridData.FILL_BOTH);
            composite.setLayoutData(gridData);
            clearButton = new Button(composite,SWT.PUSH);
            clearButton.setText("&Clear");
            clearButton.addSelectionListener(this);
            consoleText = new Text(composite,SWT.BORDER|SWT.H_SCROLL|SWT.V_SCROLL|SWT.READ_ONLY);
            gridData = new GridData(GridData.FILL_BOTH);
            gridData.heightHint = 100;
            consoleText.setLayoutData(gridData);
            setWidgets();
            shell.pack();
            shell.open();
            shell.addDisposeListener(this);
        }    
        
        /* (non-Javadoc)
         * @see org.eclipse.swt.events.DisposeListener#widgetDisposed(org.eclipse.swt.events.DisposeEvent)
         */
        public void widgetDisposed(DisposeEvent e) {
            user.disconnectPort();
        }

        /* (non-Javadoc)
         * @see org.eclipse.swt.events.SelectionListener#widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent)
         */
        public void widgetDefaultSelected(SelectionEvent e) {
            widgetSelected(e);
        }
        /* (non-Javadoc)
         * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
         */
        public void widgetSelected(SelectionEvent e) {
            Object object = e.getSource();
            Status status = Status.success;
            if(object==selectPortButton) {
                String[] portNames = Factory.getPortNames();
                if(portNames.length<1) {
                    message("no ports are registered",MessageType.error);
                } else {
                    SelectName selectName = new SelectName(selectPortButton.getShell(),portNames);
                    String portName = selectName.getName("portName");
                    if(portName!=null) {
                        user.disconnectPort();
                        port = user.connectPort(portName);
                        status = port.exceptionListenerAdd(user,this);
                        if(status!=Status.success) {
                            message(user.getMessage(),MessageType.warning);
                        }
                        status = port.getTrace().optionChangeListenerAdd(user,this);
                        if(status!=Status.success) {
                            message(user.getMessage(),MessageType.warning);
                        }
                        device = null;
                    }
                }
                setWidgets();
                return;
            }
            if(object==selectDeviceButton) {
                Device[] devices = port.getDevices();
                int length = devices.length;
                if(length<1) {
                    message("port has no devices",MessageType.error);
                } else {
                    String[] deviceNames = new String[length];
                    for(int i=0; i<length; i++) deviceNames[i] = devices[i].getDeviceName();
                    SelectName selectName = new SelectName(selectDeviceButton.getShell(),deviceNames);
                    String deviceName = selectName.getName("deviceName");
                    if(deviceName!=null) {
                        user.disconnectDevice();
                        device = user.connectDevice(deviceName);
                        status = device.exceptionListenerAdd(user,this);
                        if(status!=Status.success) {
                            message(user.getMessage(),MessageType.warning);
                        }
                        status = device.getTrace().optionChangeListenerAdd(user,this);
                        if(status!=Status.success) {
                            message(user.getMessage(),MessageType.warning);
                        }
                    }
                }
                setWidgets();
                return;
            }
            if(object==traceMaskPortButton) {
                int mask = getMask(true);
                port.getTrace().setMask(mask);
                setWidgets();
                return;
            }
            if(object==traceIOMaskPortButton) {
                int mask = getIOMask(true);
                port.getTrace().setIOMask(mask);
                setWidgets();
                return;
            }
            if(object==traceMaskDeviceButton) {
                int mask = getMask(false);
                device.getTrace().setMask(mask);
                setWidgets();
                return;
            }
            if(object==traceIOMaskDeviceButton) {
                int mask = getIOMask(false);
                device.getTrace().setIOMask(mask);
                setWidgets();
                return;
            }
            if(object==reportPortButton) {
                String report = port.report(true, 3);
                message(report,MessageType.info);
                return;
            }
            if(object==connectPortButton) {
                status = Status.success;
                if(port.isConnected()) {
                    status = user.lockPort();
                    if(status==Status.success) {
                        try {
                            status = port.disconnect(user);
                        } finally {
                            user.unlockPort();
                        }
                    }
                } else {
                    status = user.lockPortForConnect();
                    if(status==Status.success) {
                        try {
                            status = port.connect(user);
                        } finally {
                            user.unlockPort();
                        }
                    }
                }
                if(status!=Status.success) {
                    message(user.getMessage(),MessageType.error);
                }
                setWidgets();
                return;
            }
            if(object==enablePortButton) {
                port.enable(!port.isEnabled());
                setWidgets();
                return;
            }
            if(object==autoConnectPortButton) {
                port.autoConnect(!port.isAutoConnect());
                setWidgets();
                return;
            }
            if(object==reportDeviceButton) {
                String report = device.report(3);
                message(String.format("%n") + device.getFullName() + String.format("%n"),MessageType.info);
                message(report,MessageType.info);
                return;
            }
            if(object==connectDeviceButton) {
                status = Status.success;
                if(device.isConnected()) {
                    status = user.lockPort();
                    if(status==Status.success) {
                        try {
                            status = device.disconnect(user);
                        } finally {
                            user.unlockPort();
                        }
                    }
                } else {
                    status = user.lockDeviceForConnect();
                    if(status==Status.success) {
                        try {
                            status = device.connect(user);
                        } finally {
                            user.unlockPort();
                        }
                    }
                }
                if(status!=Status.success) {
                    message(user.getMessage(),MessageType.error);
                }
                setWidgets();
                return;
            }
            if(object==enableDeviceButton) {
                device.enable(!device.isEnabled());
                setWidgets();
                return;
            }
            if(object==autoConnectDeviceButton) {
                device.autoConnect(!device.isAutoConnect());
                setWidgets();
                return;
            }
            if(object==clearButton) {
                consoleText.selectAll();
                consoleText.clearSelection();
                consoleText.setText("");
                return;
            }
            
        }
        
        /* (non-Javadoc)
         * @see org.epics.pvioc.pdrv.ConnectExceptionListener#exception(org.epics.pvioc.pdrv.ConnectException)
         */
        public void exception(ConnectExceptionType connectException) {
            display.asyncExec( new Runnable() {
                public void run() {
                    setWidgets();
                }

            });
        }

        /* (non-Javadoc)
         * @see org.epics.pvioc.pdrv.TraceOptionChangeListener#optionChange()
         */
        public void optionChange() {
            display.asyncExec( new Runnable() {
                public void run() {
                    setWidgets();
                }

            });
        }

        private void message(String message, MessageType messageType) {
            if(messageType!=MessageType.info) {
                consoleText.append(messageType.name() + " ");
            }
            consoleText.append(message);
        }
        
        private void setWidgets() {
            String name = "";
            if(port!=null) name += port.getPortName();
            if(device!=null) name += "[" + device.getDeviceName() + "]";
            selectText.clearSelection();
            selectText.setText(name);
            if(port!=null) {
                String value = (port.isConnected() ? "disconnect" : "connect");
                connectPortButton.setText(value);
                value = (port.isEnabled() ? "disable" : "enable");
                enablePortButton.setText(value);
                value = (port.isAutoConnect()) ? "noAutoConnect" : "autoConnect";
                autoConnectPortButton.setText(value);
            }
            int mask = (port!=null) ? port.getTrace().getMask() : 0;
            int iomask = (port!=null) ? port.getTrace().getIOMask() : 0;
            setTraceValues(mask,iomask,true);
            boolean value = (port!=null) ? true : false;
            selectDeviceButton.setEnabled(value);
            traceMaskPortButton.setEnabled(value);
            traceFLOWPortButton.setEnabled(value);
            traceDRIVERPortButton.setEnabled(value);
            traceINTERPOSEPortButton.setEnabled(value);
            traceSUPPORTPortButton.setEnabled(value);
            traceERRORPortButton.setEnabled(value);
            traceIOMaskPortButton.setEnabled(value);
            traceIO_HEXPortButton.setEnabled(value);
            traceIO_ESCAPEPortButton.setEnabled(value);
            traceIO_ASCIIPortButton.setEnabled(value);
            connectPortButton.setEnabled(value);
            enablePortButton.setEnabled(value);
            autoConnectPortButton.setEnabled(value);
            if(device!=null) {
                String val = (device.isConnected() ? "disconnect" : "connect");
                connectDeviceButton.setText(val);
                val = (device.isEnabled() ? "disable" : "enable");
                enableDeviceButton.setText(val);
                val = (device.isAutoConnect()) ? "noAutoConnect" : "autoConnect";
                autoConnectDeviceButton.setText(val);
            }
            mask = (device!=null) ? device.getTrace().getMask() : 0;
            iomask = (device!=null) ? device.getTrace().getIOMask() : 0;
            setTraceValues(mask,iomask,false);
            value = (device==null) ? false : true;
            traceMaskDeviceButton.setEnabled(value);
            traceFLOWDeviceButton.setEnabled(value);
            traceDRIVERDeviceButton.setEnabled(value);
            traceSUPPORTDeviceButton.setEnabled(value);
            traceERRORDeviceButton.setEnabled(value);
            traceIOMaskDeviceButton.setEnabled(value);
            traceIO_HEXDeviceButton.setEnabled(value);
            traceIO_ESCAPEDeviceButton.setEnabled(value);
            traceIO_ASCIIDeviceButton.setEnabled(value);
            connectDeviceButton.setEnabled(value);
            enableDeviceButton.setEnabled(value);
            autoConnectDeviceButton.setEnabled(value);
        }
        
        private void setTraceValues(int mask, int iomask,boolean isPort) {
            boolean value = ((mask&Trace.ERROR)==0) ? false : true;
            if(isPort) traceERRORPortButton.setSelection(value);
            else traceERRORDeviceButton.setSelection(value);
            value = ((mask&Trace.SUPPORT)==0) ? false : true;
            if(isPort) traceSUPPORTPortButton.setSelection(value);
            else traceSUPPORTDeviceButton.setSelection(value);
            value = ((mask&Trace.DRIVER)==0) ? false : true;
            if(isPort) traceDRIVERPortButton.setSelection(value);
            else traceDRIVERDeviceButton.setSelection(value);
            value = ((mask&Trace.FLOW)==0) ? false : true;
            if(isPort) traceFLOWPortButton.setSelection(value);
            else traceFLOWDeviceButton.setSelection(value);
            value = ((iomask&Trace.IO_HEX)==0) ? false : true;
            if(isPort) traceIO_HEXPortButton.setSelection(value);
            else traceIO_HEXDeviceButton.setSelection(value);
            value = ((iomask&Trace.IO_ESCAPE)==0) ? false : true;
            if(isPort) traceIO_ESCAPEPortButton.setSelection(value);
            else traceIO_ESCAPEDeviceButton.setSelection(value);
            value = ((iomask&Trace.IO_ASCII)==0) ? false : true;
            if(isPort) traceIO_ASCIIPortButton.setSelection(value);
            else traceIO_ASCIIDeviceButton.setSelection(value);
        }
        
        private int getMask(boolean isPort) {
            int mask = 0;
            if(isPort) mask |= (traceERRORPortButton.getSelection() ? Trace.ERROR : 0);
            else mask |= (traceERRORDeviceButton.getSelection() ? Trace.ERROR : 0);
            if(isPort) mask |= (traceSUPPORTPortButton.getSelection() ? Trace.SUPPORT : 0);
            else mask |= (traceSUPPORTDeviceButton.getSelection() ? Trace.SUPPORT : 0);
            if(isPort) mask |= (traceDRIVERPortButton.getSelection() ? Trace.DRIVER : 0);
            else mask |= (traceDRIVERDeviceButton.getSelection() ? Trace.DRIVER : 0);
            if(isPort) mask |= (traceFLOWPortButton.getSelection() ? Trace.FLOW : 0);
            else mask |= (traceFLOWDeviceButton.getSelection() ? Trace.FLOW : 0);
            return mask;
        }
        
        private int getIOMask(boolean isPort) {
            int iomask = 0;
            if(isPort) iomask |= (traceIO_ESCAPEPortButton.getSelection() ? Trace.IO_ESCAPE : 0);
            else iomask |= (traceIO_ESCAPEDeviceButton.getSelection() ? Trace.IO_ESCAPE : 0);
            if(isPort) iomask |= (traceIO_HEXPortButton.getSelection() ? Trace.IO_HEX : 0);
            else iomask |= (traceIO_HEXDeviceButton.getSelection() ? Trace.IO_HEX : 0);
            if(isPort) iomask |= (traceIO_ASCIIPortButton.getSelection() ? Trace.IO_ASCII : 0);
            else iomask |= (traceIO_ASCIIDeviceButton.getSelection() ? Trace.IO_ASCII : 0);
            return iomask;
        }
        
        private static class SelectName extends Dialog implements SelectionListener {
            private Shell shell;
            private List list;
            private String[] names = null;
            private String name = null;
            private int ntimes = 0;
             private SelectName(Shell parent,String[]portNames) {
                 super(parent,SWT.DIALOG_TRIM|SWT.NONE);
                 this.names = portNames;
             }
            private String getName(String displayName) {
                shell = new Shell(getParent(),getStyle());
                shell.setText(displayName);
                GridLayout gridLayout = new GridLayout();
                gridLayout.numColumns = 1;
                shell.setLayout(gridLayout);
                Composite composite = new Composite(shell,SWT.BORDER);
                gridLayout = new GridLayout();
                gridLayout.numColumns = 1;
                composite.setLayout(gridLayout);
                list = new List(composite,SWT.SINGLE|SWT.V_SCROLL);
                for(String portName : names) {
                    list.add(portName);
                }
                list.addSelectionListener(this);
                GridData listGridData = new GridData();
                listGridData.heightHint = 600;
                listGridData.widthHint = 200;
                list.setLayoutData(listGridData);
                Display display = shell.getDisplay();
                shell.pack();
                shell.open();
                while(!shell.isDisposed()) {
                    if(!display.readAndDispatch()) {
                        display.sleep();
                    }
                }
                shell.dispose();
                return name;
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
                if(arg0.getSource()==list) {
                    String[] names = list.getSelection();
                    name = names[0];
                    // An automatic selection is made. Skip it
                    // Don't know why this happens.
                    ntimes++;
                    if(ntimes<2) return;
                    shell.close();
                }  
            }   
        }
    }
}
