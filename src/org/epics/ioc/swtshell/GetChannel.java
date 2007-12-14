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
 * Get a channel.
 * The user is presented with a window that has two controls: a select button and a text window.
 * The user can enter a channel name in the text window and press the enter key to determine the channel.
 * The use can also click the select button.
 * This displays a list of the names of the records in the local IOC.
 * The name selected determines the channel.
 * @author mrk
 *
 */    
public class GetChannel extends Dialog implements SelectionListener {
    private Requester requester;
    private ChannelStateListener channelStateListener;
    private Shell shell;
    private Button selectButton;
    private Text text;
    private SelectRecord selectRecord;
    private String recordName = null;

    /**The constructor.
     * @param parent The parent shell.
     * @param requester The requestor.
     * @param channelStateListener The channel state listener.
     */
    public GetChannel(Shell parent,Requester requester,ChannelStateListener channelStateListener) {
        super(parent,SWT.DIALOG_TRIM|SWT.NONE);
        this.requester = requester;
        this.channelStateListener = channelStateListener;
    }
    
    /**
     * Get the channel.
     * @return The channel or null if not found.
     */
    public Channel getChannel() {
        shell = new Shell(getParent(),getStyle());
        shell.setText("getChannel");      
        GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 2;
        shell.setLayout(gridLayout);
        GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
        shell.setLayoutData(gridData);
        selectButton = new Button(shell,SWT.NONE);
        selectButton.setText("select");
        selectRecord = new SelectRecord(shell,requester);
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
        Channel channel = ChannelFactory.createChannel(recordName,"local", channelStateListener);
        if(channel==null) {
            requester.message(String.format(
                    "pvname %s not found%n",recordName),MessageType.error);
        }
        return channel;
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
            recordName = selectRecord.getRecordName();
            if(recordName!=null) {
                shell.close();
            }
            return;
        } else if(object==text) {
            recordName = text.getText();
            if(recordName!=null) {
                shell.close();
            }
            return;
        }
    }
}
