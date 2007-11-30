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
 * Get a field of a channel.
 * The user is presented with a window that has two controls: a select button and a text window.
 * The user can enter a field name in the text window
 * and press the enter key to determine the channelField.
 * If the user just enters a null name all fields of the channel (actually the record to which the channel
 * is connected) are selected.
 * The use can also click the select button.
 * This displays a tree showing the structure for the channel.
 * The user can select one node of the tree.
 * The name selected determines the channelField.
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

    /**
     * Constructor.
     * @param parent The parent shell.
     * @param requester The requestor.
     * @param channel The channel.
     */
    public GetChannelField(Shell parent,Requester requester,Channel channel) {
        super(parent,SWT.DIALOG_TRIM|SWT.NONE);
        this.requester = requester;
        this.channel = channel;
    }
    
    /**
     * Get a channelField.
     * @return The channelField or null if no fields were selected.
     */
    public ChannelField getChannelField() {
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
        channelField = channel.getChannelField(fieldName);
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
