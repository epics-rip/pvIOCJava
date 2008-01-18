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
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.epics.ioc.ca.*;
import org.epics.ioc.ca.ChannelAccess;
import org.epics.ioc.ca.ChannelAccessFactory;
import org.epics.ioc.ca.ChannelListener;
import org.epics.ioc.db.DBRecord;
import org.epics.ioc.db.IOCDB;
import org.epics.ioc.db.IOCDBFactory;
import org.epics.ioc.pv.PVRecord;
import org.epics.ioc.util.MessageType;
import org.epics.ioc.util.Requester;

/**
 * @author mrk
 *
 */
public class ChannelConnectFactory {
    
    public static ChannelConnect create(ChannelListener channelListener,Requester requester) {
        return new ChannelConnectImpl(channelListener,requester);
    }
    
    private static final ChannelAccess channelAccess = ChannelAccessFactory.getChannelAccess();
    
    private static class ChannelConnectImpl
    implements ChannelConnect,Requester,
        DisposeListener,ChannelListener,SelectionListener,
        Runnable
    {
        private IOCDB iocdb = IOCDBFactory.getMaster();
        private ChannelListener channelListener;
        private Requester requester;
        private Display display = null;
        private Shell shell = null;
        private Channel channel = null;
        private enum ConnectState {connected,disconnected}
        private ConnectState connectState = ConnectState.disconnected;
        private String[] connectStateText = {"connect    ","disconnect"};
        
        private Button connectButton = null;
        private Button selectLocalRecordButton = null;
        private Button selectLocalFieldButton = null;
        private Combo providerCombo = null;
        private int localIndex = 0;
        private Text pvNameText = null;
        private String providerName = "local";
        private String pvName = null;
        
        private Button timeStampButton = null;
        private Button alarmButton = null;
        private Button displayButton = null;
        private Button controlButton = null;
        
        
        private enum ListenerState {channelStateChange, disconnect}
        private ListenerState listernerState = ListenerState.channelStateChange;
        private boolean isConnected = false;
        
        
        private ChannelConnectImpl(ChannelListener channelListener,Requester requester) {
            this.channelListener = channelListener;
            this.requester = requester;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.swtshell.ChannelConnect#createWidgets(org.eclipse.swt.widgets.Composite)
         */
        public void createWidgets(Composite parent) {
            display = parent.getShell().getDisplay();
            shell = parent.getShell();
            Composite shellComposite = new Composite(parent,SWT.BORDER);
            GridLayout gridLayout = new GridLayout();
            gridLayout.numColumns = 1;
            shellComposite.setLayout(gridLayout);
            GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
            shellComposite.setLayoutData(gridData); 
            
            Composite connectGetLocal = new Composite(shellComposite,SWT.BORDER);
            gridLayout = new GridLayout();
            gridLayout.numColumns = 3;
            connectGetLocal.setLayout(gridLayout);
            connectButton = new Button(connectGetLocal,SWT.PUSH);
            connectButton.setText(connectStateText[0]);
            connectButton.addSelectionListener(this);
            selectLocalRecordButton = new Button(connectGetLocal,SWT.PUSH);
            selectLocalRecordButton.setText("selectLocalRecord");
            selectLocalRecordButton.addSelectionListener(this);
            selectLocalFieldButton = new Button(connectGetLocal,SWT.PUSH);
            selectLocalFieldButton.setText("selectLocalField");
            selectLocalFieldButton.addSelectionListener(this);
            selectLocalFieldButton.setEnabled(false);
            
            Composite pv = new Composite(shellComposite,SWT.BORDER);
            gridLayout = new GridLayout();
            gridLayout.numColumns = 2;
            pv.setLayout(gridLayout);
            gridData = new GridData(GridData.FILL_HORIZONTAL);
            pv.setLayoutData(gridData);
            Composite provider = new Composite(pv,SWT.BORDER);
            gridLayout = new GridLayout();
            gridLayout.numColumns = 2;
            provider.setLayout(gridLayout);
            gridData = new GridData(GridData.FILL_HORIZONTAL);
            provider.setLayoutData(gridData);
            new Label(provider,SWT.RIGHT).setText("provider");
            providerCombo = new Combo(provider,SWT.DROP_DOWN);
            ChannelProvider[] providers = channelAccess.getChannelProviders();
            for(int i=0; i<providers.length; i++) {
                ChannelProvider channelProvider = providers[i];
                String name = channelProvider.getProviderName();
                providerCombo.add(name);
                if(name.equals("local")) {
                    localIndex = i;
                }
            }
            providerCombo.select(localIndex);
            providerCombo.addSelectionListener(this);
            Composite pvname = new Composite(pv,SWT.NONE);
            gridLayout = new GridLayout();
            gridLayout.numColumns = 2;
            pvname.setLayout(gridLayout);
            gridData = new GridData(GridData.FILL_HORIZONTAL);
            pvname.setLayoutData(gridData);   
            new Label(pvname,SWT.RIGHT).setText("pvname");
            pvNameText = new Text(pvname,SWT.BORDER);
            gridData = new GridData(GridData.FILL_HORIZONTAL);
            gridData.minimumWidth = 500;
            pvNameText.setLayoutData(gridData);
            pvNameText.addSelectionListener(this);
            
            Composite propertys = new Composite(shellComposite,SWT.NONE);
            gridLayout = new GridLayout();
            gridLayout.numColumns = 5;
            propertys.setLayout(gridLayout);
            gridData = new GridData(GridData.FILL_HORIZONTAL);
            propertys.setLayoutData(gridData);   
            new Label(propertys,SWT.RIGHT).setText("property");
            timeStampButton = new Button(propertys,SWT.TOGGLE);
            timeStampButton.setText("timeStamp");
            timeStampButton.setSelection(false);
            alarmButton = new Button(propertys,SWT.TOGGLE);
            alarmButton.setText("alarm");
            alarmButton.setSelection(false);
            displayButton = new Button(propertys,SWT.TOGGLE);
            displayButton.setText("display");
            displayButton.setSelection(false);
            controlButton = new Button(propertys,SWT.TOGGLE);
            controlButton.setText("control");
            controlButton.setSelection(false);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.swtshell.ChannelConnect#getChannel()
         */
        public Channel getChannel() {
            return channel;
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
         * @see org.eclipse.swt.events.DisposeListener#widgetDisposed(org.eclipse.swt.events.DisposeEvent)
         */
        public void widgetDisposed(DisposeEvent e) {
            if(channel!=null) destroy(channel);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelListener#channelStateChange(org.epics.ioc.ca.Channel, boolean)
         */
        public void channelStateChange(Channel c, boolean isConnected) {
            listernerState = ListenerState.channelStateChange;
            this.isConnected = isConnected;
            display.syncExec(this);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelListener#disconnect(org.epics.ioc.ca.Channel)
         */
        public void destroy(Channel c) {
            listernerState = ListenerState.disconnect;
            display.syncExec(this);
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
            if(object==connectButton) {
                switch(connectState) {
                case disconnected:
                    if(providerName==null) {
                        message("provider is null",MessageType.info);
                        return;
                    }
                    if(pvName==null) {
                        message("record is null",MessageType.info);
                        return;
                    }
                    String[] propertyNames = getPropertyNames();
                    channel = channelAccess.createChannel(pvName,propertyNames, providerName, this);
                    if(channel==null) return;
                    selectLocalRecordButton.setEnabled(false);
                    providerCombo.setEnabled(false);
                    pvNameText.setEnabled(false);
                    channel.connect();
                    connectState = ConnectState.connected;
                    connectButton.setText(connectStateText[1]);
                    message("getChannel " + channel.getChannelName(),MessageType.info);
                    return;
                case connected:
                    connectState = ConnectState.disconnected;
                    connectButton.setText(connectStateText[0]);
                    message("destroyChannel " + channel.getChannelName(),MessageType.info);
                    channel.disconnect();
                    selectLocalRecordButton.setEnabled(true);
                    providerCombo.setEnabled(true);
                    pvNameText.setEnabled(true);
                    channel = null;
                    return;
                }
            } else if(object==selectLocalRecordButton) {
                SelectLocalRecord selectLocalRecord = 
                    SelectLocalRecordFactory.create(shell, requester);
                pvName = selectLocalRecord.getRecordName();
                if(pvName==null) return;
                channel = channelAccess.createChannel(pvName,null,"local", channelListener);
                if(channel==null) {
                    message("getChannel failed",MessageType.error);
                    return;
                }
                providerCombo.select(localIndex);
                providerName = providerCombo.getText();
                pvNameText.setText(pvName);
                pvName = pvNameText.getText();
                selectLocalFieldButton.setEnabled(true);
            } else if(object==selectLocalFieldButton) {
                DBRecord dbRecord = iocdb.findRecord(pvName);
                if(dbRecord==null) {
                    message("findRecord failed",MessageType.error);
                    return;
                }
                PVRecord pvRecord = dbRecord.getPVRecord();
                SelectField selectField = SelectFieldFactory.create(shell, requester);
                String fieldName = selectField.selectFieldName(pvRecord);
                pvName = pvName + "." + fieldName;
                pvNameText.setText(pvName);
                selectLocalFieldButton.setEnabled(false);
            } else if(object==providerCombo) {
                providerName = providerCombo.getText();
            } else if(object==pvNameText) {
                pvName = pvNameText.getText();
                selectLocalFieldButton.setEnabled(false);
            }
            
        }
        /* (non-Javadoc)
         * @see java.lang.Runnable#run()
         */
        public void run() {
            switch(listernerState) {
            case channelStateChange:
                channelListener.channelStateChange(channel, isConnected);
                break;
            case disconnect:
                channelListener.destroy(channel);
                channel.disconnect();
                channel = null;
                break;
            }
        }
        
        private String[] getPropertyNames() {
            int num = 0;
            boolean time = timeStampButton.getSelection();
            boolean alarm = alarmButton.getSelection();
            boolean display = displayButton.getSelection();
            boolean control = controlButton.getSelection();
            if(time) num++;
            if(alarm) num++;
            if(display) num++;
            if(control) num++;
            String[] propertys = new String[num];
            num = 0;
            if(time) propertys[num++] = "timeStamp";
            if(alarm) propertys[num++] = "alarm";
            if(display) propertys[num++] = "display";
            if(control) propertys[num++] = "control";
            return propertys;
        }  
    }
}
