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

import org.epics.ioc.util.*;
import org.epics.ioc.ca.*;
/**
 * @author mrk
 *
 */    
public class GetChannelField extends Dialog implements SelectionListener {
    
    private Requestor requestor;
    private Channel channel;
    private Shell shell;
    private Button selectButton;
    private Button doneButton;
    private Text text;
    private SelectFieldName selectFieldName;
    private String fieldName = null;

    public GetChannelField(Shell parent,Requestor requestor,Channel channel) {
        super(parent,SWT.DIALOG_TRIM|SWT.NONE);
        this.requestor = requestor;
        this.channel = channel;
    }
    
    public ChannelField getChannelField(Channel channel) {
        shell = new Shell(getParent(),getStyle());
        shell.setText("getChannelField");      
        GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 3;
        shell.setLayout(gridLayout);
        GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
        shell.setLayoutData(gridData);
        selectButton = new Button(shell,SWT.NONE);
        selectButton.setText("select");
        selectFieldName = new SelectFieldName(shell,requestor);
        selectButton.addSelectionListener(this);
        doneButton = new Button(shell,SWT.NONE);
        doneButton.setText("done");
        doneButton.addSelectionListener(this);
        text = new Text(shell,SWT.BORDER);
        gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.minimumWidth = 500;
        text.setLayoutData(gridData);
        text.addSelectionListener(this);
        Display display = shell.getDisplay();
        shell.pack();
        shell.open();
        while(!shell.isDisposed()) {
            if(!display.readAndDispatch()) {
                display.sleep();
            }
        }
        shell.dispose();
        ChannelField channelField = null;
        channel.findField(null);
        ChannelFindFieldResult result = channel.findField(fieldName);
        switch(result) {
        case otherChannel:
            requestor.message(String.format(
                    "field %s i located via channelName %s and fieldName %s",
                    fieldName,channel.getOtherChannel(),channel.getOtherField()),
                    MessageType.info);
            return null;
        case thisChannel:
            channelField =channel.getChannelField();
            break;
        case notFound:
            requestor.message(String.format(
                    "field %s not found%n",fieldName),MessageType.error);
            return null;
        case failure:
            requestor.message(String.format(
                    "Logic Error: findField failed.%n"),MessageType.error);
            return null;
        }
        return channelField;
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
    public void widgetSelected(SelectionEvent arg0) {
        Object object = arg0.getSource();
        if(object==selectButton) {
            text.selectAll();
            text.clearSelection();
            boolean result = selectFieldName.selectFieldName(channel.getChannelName());
            if(result) {
                fieldName = selectFieldName.getFieldName();
                text.setText(fieldName);
            }
            return;
        } else if(object==text) {
            fieldName = text.getText();
        } else if(object==doneButton) {
            shell.close();
        }
    }
}
