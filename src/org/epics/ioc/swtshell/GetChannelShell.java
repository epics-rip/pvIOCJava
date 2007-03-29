/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.swtshell;

import org.eclipse.swt.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.widgets.*;

import org.epics.ioc.pv.*;
import org.epics.ioc.util.*;
import org.epics.ioc.ca.*;
import org.epics.ioc.db.*;
/**
 * @author mrk
 *
 */
public class GetChannelShell implements DisposeListener{
    static private IOCDB iocdb = IOCDBFactory.getMaster();
    private Composite parent;
    private Requestor requestor;
    private String recordName = null;
    private String fieldName = null;
    private Channel channel = null;
    private ChannelField channelField = null;
    
    public GetChannelShell(Composite parentWidget,Requestor requestor) {
        this.parent = parentWidget;
        this.requestor = requestor;
        parent.addDisposeListener(this);
        new RecordShell();
        new FieldShell();
    }
     
    /* (non-Javadoc)
     * @see org.eclipse.swt.events.DisposeListener#widgetDisposed(org.eclipse.swt.events.DisposeEvent)
     */
    public void widgetDisposed(DisposeEvent e) {
        if(channel!=null) channel.destroy();
    }

    public Channel getChannel() {
        return channel;
    }
    
    public ChannelField getChannelField() {
        return channelField;
    }
    
    public Property[] getPropertys() {
        return channelField.getField().getPropertys();
    }
    
    private abstract class ShellBase implements SelectionListener,ChannelStateListener {
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
            // TODO Auto-generated method stub
            
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
    }
    
    private class RecordShell extends ShellBase {
        private Composite composite;
        private Button selectButton;
        private Text text;
        private SelectRecord selectRecord;
        
        private RecordShell() {
            composite = new Composite(parent,SWT.BORDER);
            GridLayout gridLayout = new GridLayout();
            gridLayout.numColumns = 4;
            composite.setLayout(gridLayout);
            GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
            composite.setLayoutData(gridData);
            Label label = new Label(composite,SWT.NONE);
            label.setText("record");
            selectButton = new Button(composite,SWT.NONE);
            selectButton.setText("select");
            selectRecord = new SelectRecord(parent.getShell(),requestor);
            selectButton.addSelectionListener(this);
            text = new Text(composite,SWT.BORDER);
            gridData = new GridData(GridData.FILL_HORIZONTAL);
            text.setLayoutData(gridData);
            text.addSelectionListener(this);
        }

        /* (non-Javadoc)
         * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
         */
        public void widgetSelected(SelectionEvent arg0) {
            if(channel!=null) {
                channel.destroy();
            }
            Object object = arg0.getSource();
            if(object==selectButton) {
                text.selectAll();
                text.clearSelection();
                recordName = selectRecord.getRecordName();
                if(recordName!=null) {
                    text.setText(recordName);
                }
            } else if(object==text) {
                recordName = text.getText();
            }
            if(recordName==null) {
                requestor.message("recordName is null", MessageType.error);
            }
            fieldName = null;
            channel = ChannelFactory.createChannel(recordName, this, false);
            if(channel==null) {
                requestor.message(String.format(
                    "pvname %s not found%n",recordName),MessageType.error);
            }
        }
    }
    
    
    private class FieldShell extends ShellBase {
        private Composite fieldWidget;
        private Button selectButton;
        private Text text;
        private SelectField selectField;
        
        private FieldShell() {
            fieldWidget = new Composite(parent,SWT.BORDER);
            GridLayout gridLayout = new GridLayout();
            gridLayout.numColumns = 4;
            fieldWidget.setLayout(gridLayout);
            GridData fieldWidgetGridData = new GridData(GridData.FILL_HORIZONTAL);
            fieldWidget.setLayoutData(fieldWidgetGridData);
            Label label = new Label(fieldWidget,SWT.NONE);
            label.setText(" field");
            selectButton = new Button(fieldWidget,SWT.NONE);
            selectButton.setText("select");
            selectField = new SelectField(parent.getShell(),requestor);
            selectButton.addSelectionListener(this);
            text = new Text(fieldWidget,SWT.BORDER);
            GridData textGridData = new GridData(GridData.FILL_HORIZONTAL);
            text.setLayoutData(textGridData);
            text.addSelectionListener(this);
        }
        /* (non-Javadoc)
         * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
         */
        public void widgetSelected(SelectionEvent arg0) {
            Object object = arg0.getSource();
            if(channel==null) {
                requestor.message(String.format(
                        "not connected%n"),MessageType.error);
                return;
            }
            if(object==selectButton) {
                DBRecord dbRecord = iocdb.findRecord(recordName);
                if(dbRecord==null) return;
                fieldName = selectField.getFieldName(dbRecord.getPVRecord());
                if(fieldName==null) {
                    requestor.message("fieldName is null", MessageType.error);
                    return;
                }
                text.selectAll();
                text.clearSelection();
                text.setText(fieldName);

            } else if(object==text) {
                fieldName = text.getText();
            }
            channelField = null;
            outer:
            while(true) {
                channel.findField(null);
                ChannelFindFieldResult result = channel.findField(fieldName);
                switch(result) {
                case otherChannel:
                    recordName = text.getText();
                    channel = ChannelFactory.createChannel(recordName, this, false);
                    if(channel==null) {
                        requestor.message(String.format(
                                "%s not found%n",recordName),MessageType.error);
                        return;
                    }
                    continue outer;
                case thisChannel:
                    channelField =channel.getChannelField();
                    return;
                case notFound:
                    requestor.message(String.format(
                            "field %s not found%n",fieldName),MessageType.error);
                    return;
                case failure:
                    requestor.message(String.format(
                    "Logic Error: findField failed.%n"),MessageType.error);
                    return;
                }
            }
        }   
    }
}
