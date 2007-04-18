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
    
    private Requester requester;
    private Channel channel;
    private Shell shell;
    private Button selectButton;
    private Text text;
    private SelectFieldName selectFieldName;
    private String fieldName = null;

    public GetChannelField(Shell parent,Requester requester,Channel channel) {
        super(parent,SWT.DIALOG_TRIM|SWT.NONE);
        this.requester = requester;
        this.channel = channel;
    }
    
    public ChannelField getChannelField(Channel channel) {
        shell = new Shell(getParent(),getStyle());
        shell.setText("getChannelField");      
        GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 2;
        shell.setLayout(gridLayout);
        GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
        shell.setLayoutData(gridData);
        selectButton = new Button(shell,SWT.NONE);
        selectButton.setText("select");
        selectFieldName = new SelectFieldName(shell,requester);
        selectButton.addSelectionListener(this);
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
        channelField = channel.findField(fieldName);
        if(channelField==null) requester.message(
            String.format("field %s not found%n",fieldName),MessageType.error);
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
                if(fieldName!=null) {
                    shell.close();
                }
            }
            return;
        } else if(object==text) {
            fieldName = text.getText();
            shell.close();
        }
    }
}
