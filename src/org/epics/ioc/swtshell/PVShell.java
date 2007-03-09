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
public class PVShell {
    static private IOCDB iocdb = IOCDBFactory.getMaster();
    private Composite parentWidget;
    private Requestor requestor;
    private String recordName = null;
    private String fieldName = null;
    private Channel channel = null;
    private ChannelField channelField = null;
    
    public PVShell(Composite parentWidget,Requestor requestor) {
        this.parentWidget = parentWidget;
        this.requestor = requestor;
        new RecordShell();
        new FieldShell();
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
        private Composite recordWidget;
        private Button selectButton;
        private Button propertyButton;
        private Text text;
        private SelectRecord selectRecord;
        
        private RecordShell() {
            recordWidget = new Composite(parentWidget,SWT.BORDER);
            GridLayout gridLayout = new GridLayout();
            gridLayout.numColumns = 4;
            recordWidget.setLayout(gridLayout);
            GridData recordWidgetGridData = new GridData(GridData.FILL_HORIZONTAL);
            recordWidget.setLayoutData(recordWidgetGridData);
            Label label = new Label(recordWidget,SWT.NONE);
            label.setText("record");
            selectButton = new Button(recordWidget,SWT.NONE);
            selectButton.setText("select");
            selectRecord = new SelectRecord(parentWidget.getShell());
            selectButton.addSelectionListener(this);
            propertyButton = new Button(recordWidget,SWT.NONE);
            propertyButton.setText("properties");
            propertyButton.addSelectionListener(this);
            text = new Text(recordWidget,SWT.BORDER);
            GridData textGridData = new GridData(GridData.FILL_HORIZONTAL);
            text.setLayoutData(textGridData);
            text.addSelectionListener(this);
        }

        /* (non-Javadoc)
         * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
         */
        public void widgetSelected(SelectionEvent arg0) {
            if(arg0.getSource()==propertyButton) {
                if(channel==null) {
                    requestor.message(String.format(
                            "no record selected%n"),MessageType.error);
                    return;
                }
                channel.findField(null);
                ChannelField channelField = channel.getChannelField();
                Field field = channelField.getField();
                requestor.message(String.format(
                        "recordType %s",field.getFieldName()),MessageType.info);
                Property[] properties = field.getPropertys();
                if(properties.length==0) {
                    requestor.message(String.format(
                            "no properties"),MessageType.info);
                } else {
                    for(Property property: properties) {
                        requestor.message(String.format(
                        "%s",property.toString()),MessageType.info);
                    }
                }
                return;
            }
            if(arg0.getSource()==selectButton || arg0.getSource()==text) {
                recordName = null;
                if(arg0.getSource()==selectButton) {
                    recordName = selectRecord.getRecordName();
                    text.selectAll();
                    text.clearSelection();
                    text.setText(recordName);
                } else if(arg0.getSource()==text) {
                    recordName = text.getText();
                }
                if(recordName==null) {
                    requestor.message("fieldName is null", MessageType.error);
                }
                fieldName = null;
                channel = ChannelFactory.createChannel(recordName, this, false);
                if(channel==null) {
                    requestor.message(String.format(
                        "pvname %s not found%n",recordName),MessageType.error);
                }
                return;
            }
        }
    }
    
    
    private class FieldShell extends ShellBase {
        private Composite fieldWidget;
        private Button selectButton;
        private Button propertyButton;
        private Text text;
        private SelectField selectField;
        
        private FieldShell() {
            fieldWidget = new Composite(parentWidget,SWT.BORDER);
            GridLayout gridLayout = new GridLayout();
            gridLayout.numColumns = 4;
            fieldWidget.setLayout(gridLayout);
            GridData fieldWidgetGridData = new GridData(GridData.FILL_HORIZONTAL);
            fieldWidget.setLayoutData(fieldWidgetGridData);
            Label label = new Label(fieldWidget,SWT.NONE);
            label.setText(" field");
            selectButton = new Button(fieldWidget,SWT.NONE);
            selectButton.setText("select");
            selectField = new SelectField(parentWidget.getShell());
            selectButton.addSelectionListener(this);
            propertyButton = new Button(fieldWidget,SWT.NONE);
            propertyButton.setText("properties");
            propertyButton.addSelectionListener(this);
            text = new Text(fieldWidget,SWT.BORDER);
            GridData textGridData = new GridData(GridData.FILL_HORIZONTAL);
            text.setLayoutData(textGridData);
            text.addSelectionListener(this);
        }
        /* (non-Javadoc)
         * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
         */
        public void widgetSelected(SelectionEvent arg0) {
            if(arg0.getSource()==propertyButton) {
                if(channel==null || channelField==null) {
                    requestor.message(String.format(
                            "no field selected"),MessageType.error);
                    return;
                }
                channel.findField(null);
                Field field = channelField.getField();
                requestor.message(String.format(
                        "field %s",field.getFieldName()),MessageType.info);
                Property[] properties = field.getPropertys();
                if(properties.length==0) {
                    requestor.message(String.format(
                            "no properties"),MessageType.info);
                } else {
                    for(Property property: properties) {
                        requestor.message(String.format(
                        "%s",property.toString()),MessageType.info);
                    }
                }
                return;
            }
            if(arg0.getSource()==selectButton || arg0.getSource()==text){
                if(channel==null) {
                    requestor.message(String.format(
                            "not connected%n"),MessageType.error);
                    return;
                }
                if(arg0.getSource()==selectButton) {
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
            
                } else if(arg0.getSource()==text) {
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
}
