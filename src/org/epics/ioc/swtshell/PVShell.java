/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.swtshell;

import java.util.Iterator;
import java.util.Map;

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
    private RecordShell recordShell;
    private String recordName = null;
    private FieldShell fieldShell;
    private String fieldName = null;
    private Channel channel = null;
    private ChannelField channelField = null;
    
    public PVShell(Composite parentWidget,Requestor requestor) {
        this.parentWidget = parentWidget;
        this.requestor = requestor;
        recordShell = new RecordShell();
        fieldShell = new FieldShell();
    }
    
    public void start() {
        recordShell.start();
        fieldShell.start();
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
        
        private RecordShell() {
            recordWidget = new Composite(parentWidget,SWT.BORDER);
            GridLayout gridLayout = new GridLayout();
            gridLayout.numColumns = 4;
            recordWidget.setLayout(gridLayout);
            Label label = new Label(recordWidget,SWT.NONE);
            label.setText("record");
            selectButton = new Button(recordWidget,SWT.NONE);
            selectButton.setText("select");
            selectButton.addSelectionListener(this);
            propertyButton = new Button(recordWidget,SWT.NONE);
            propertyButton.setText("properties");
            propertyButton.addSelectionListener(this);
            text = new Text(recordWidget,SWT.BORDER);
            Swtshell.makeBlanks(text, 1, 100);
            text.addSelectionListener(this);
        }
        
        private void start() {
            text.selectAll();
            text.clearSelection();
            text.setText("");
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
            } else {
                recordName = null;
                if(arg0.getSource()==selectButton) {
                    GetRecordName get = new GetRecordName(parentWidget.getShell());
                    recordName = get.getRecordName();
                    text.selectAll();
                    text.clearSelection();
                    text.setText(recordName);
                } else if(arg0.getSource()==text) {
                    recordName = text.getText();
                } else {
                    requestor.message("??? selected", MessageType.error);
                }
                if(recordName==null) {
                    requestor.message("fieldName is null", MessageType.error);
                }
                fieldName = null;
                channel = ChannelFactory.createChannel(recordName, this, false);
                if(channel==null) {
                    requestor.message(String.format(
                        "pvname %s not found%n",recordName),MessageType.error);
                    return;
                }
            }
        }
        
        private class GetRecordName extends Dialog implements SelectionListener {
            private Map<String,DBRecord> recordMap = iocdb.getRecordMap();
            private List list;
            private String recordName = null;
            private Shell shell;
            private int ntimes = 0;
            
            public GetRecordName(Shell parent) {
                super(parent,SWT.DIALOG_TRIM|SWT.APPLICATION_MODAL);
            }
            
            public String getRecordName() {
                shell = new Shell(getParent(),getStyle());
                shell.setText("getRecord");
                GridLayout gridLayout = new GridLayout();
                gridLayout.numColumns = 1;
                shell.setLayout(gridLayout);
                Composite composite = new Composite(shell,SWT.BORDER);
                gridLayout = new GridLayout();
                gridLayout.numColumns = 1;
                composite.setLayout(gridLayout);
                list = new List(composite,SWT.SINGLE|SWT.V_SCROLL);
                Swtshell.makeBlanks(list, 5, 80);
                shell.pack();
                Iterator<String> iter = recordMap.keySet().iterator();
                list.removeAll();
                for(int i=0; i< recordMap.size(); i++) {
                    list.add(iter.next());
                }
                list.addSelectionListener(this);
                shell.open();
                Display display = getParent().getDisplay();
                while(!shell.isDisposed()) {
                    if(!display.readAndDispatch()) {
                        display.sleep();
                    }
                }
                return recordName;
            }
            public void widgetDefaultSelected(SelectionEvent arg0) {
                widgetSelected(arg0);
            }
            public void widgetSelected(SelectionEvent arg0) {
                String[] names = list.getSelection();
                recordName = names[0];
                if(recordName==null) recordName = "";
                // An automatic selection is made. Skip it
                // Don't know why this happens.
                ntimes++;
                if(ntimes<2) return;
                shell.close();
            }
            
        }
    }
    
    
    private class FieldShell extends ShellBase {
        private Composite fieldWidget;
        private Button selectButton;
        private Button propertyButton;
        private Text text;
        
        private FieldShell() {
            fieldWidget = new Composite(parentWidget,SWT.BORDER);
            GridLayout gridLayout = new GridLayout();
            gridLayout.numColumns = 4;
            fieldWidget.setLayout(gridLayout);
            Label label = new Label(fieldWidget,SWT.NONE);
            label.setText(" field");
            selectButton = new Button(fieldWidget,SWT.NONE);
            selectButton.setText("select");
            selectButton.addSelectionListener(this);
            propertyButton = new Button(fieldWidget,SWT.NONE);
            propertyButton.setText("properties");
            propertyButton.addSelectionListener(this);
            text = new Text(fieldWidget,SWT.BORDER);
            Swtshell.makeBlanks(text, 1, 100);
            text.addSelectionListener(this);
        }
        
        private void start() {
            text.selectAll();
            text.clearSelection();
            text.setText("");
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
            } else {
                if(channel==null) {
                    requestor.message(String.format(
                            "not connected%n"),MessageType.error);
                    return;
                }
                if(arg0.getSource()==selectButton) {
                    GetFieldName get = new GetFieldName(parentWidget.getShell());
                    fieldName = get.getFieldName();
                    if(fieldName==null) {
                        requestor.message("fieldName is null", MessageType.error);
                        return;
                    }
                    text.selectAll();
                    text.clearSelection();
                    text.setText(fieldName);
            
                } else if(arg0.getSource()==text) {
                    fieldName = text.getText();
                } else {
                    requestor.message("??? selected", MessageType.info);
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
                                "pvname %s not found%n",recordName),MessageType.error);
                            return;
                        }
                        break outer;
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
        
        private class GetFieldName extends Dialog implements SelectionListener {
            private List list;
            private String fieldName = null;
            private Shell shell;
            private int ntimes = 0;
            
            public GetFieldName(Shell parent) {
                super(parent,SWT.DIALOG_TRIM|SWT.APPLICATION_MODAL);
            }
            
            public String getFieldName() {
                shell = new Shell(getParent(),getStyle());
                shell.setText("getFieldName");
                GridLayout gridLayout = new GridLayout();
                gridLayout.numColumns = 1;
                shell.setLayout(gridLayout);
                Composite composite = new Composite(shell,SWT.BORDER);
                gridLayout = new GridLayout();
                gridLayout.numColumns = 1;
                composite.setLayout(gridLayout);
                list = new List(composite,SWT.SINGLE|SWT.V_SCROLL);
                Swtshell.makeBlanks(list, 5, 80);
                shell.pack();
                DBRecord dbRecord = iocdb.findRecord(recordName);
                if(dbRecord==null) {
                    requestor.message("record " + recordName + " is not local",MessageType.error);
                    return null;
                }
                PVData[] fields = dbRecord.getFieldPVDatas();
                list.removeAll();
                for(PVData pvData: fields) list.add(pvData.getField().getFieldName());
                list.addSelectionListener(this);
                shell.open();
                Display display = getParent().getDisplay();
                while(!shell.isDisposed()) {
                    if(!display.readAndDispatch()) {
                        display.sleep();
                    }
                }
                return fieldName;
            }
            public void widgetDefaultSelected(SelectionEvent arg0) {
                widgetSelected(arg0);
            }
            public void widgetSelected(SelectionEvent arg0) {
                String[] names = list.getSelection();
                fieldName = names[0];
                if(fieldName==null) fieldName = "";
                // An automatic selection is made. Skip it
                // Don't know why this happens.
                ntimes++;
                if(ntimes<2) return;
                shell.close();
            }
            
        }

    }
    
}
