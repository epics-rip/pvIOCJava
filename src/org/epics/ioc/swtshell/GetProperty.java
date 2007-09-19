/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.swtshell;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.epics.ioc.pv.Field;
import org.epics.ioc.ca.*;

/**
 * @author mrk
 *
 */
public class GetProperty extends Dialog implements SelectionListener {
    private Button doneButton;
    private String[] propertyNames = null;
    private Button[] propertyButtons;
    String[] associatedNames = null;
    private Shell shell;
    
    public GetProperty(Shell parent) {
        super(parent,SWT.PRIMARY_MODAL|SWT.DIALOG_TRIM);
    }
    public String[] open(ChannelField channelField) {
        propertyNames = channelField.getPropertyNames();
        if(propertyNames==null) return null;
        int length = propertyNames.length;
        if(length==0) return null;
        shell = new Shell(getParent(),getStyle());
        shell.setText("getProperty");
        GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 1;
        shell.setLayout(gridLayout);
        doneButton = new Button(shell,SWT.PUSH);
        doneButton.setText("Done");
        doneButton.addSelectionListener(this);
        propertyButtons = new Button[length];
        for(int i=0; i<length; i++) {
            Button button = new Button(shell,SWT.CHECK);
            button.setText(propertyNames[i]);
            propertyButtons[i] = button;
        }
        shell.pack();
        shell.open();
        Display display = getParent().getDisplay();
        while(!shell.isDisposed()) {
            if(!display.readAndDispatch()) {
                display.sleep();
            }
        }
        return associatedNames;
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
        Object object = arg0.getSource();
        if(object==doneButton) {
            int numSelected = 0;
            for(Button button : propertyButtons) {
                if(button.getSelection()) numSelected++;
            }
            
            if(numSelected>0) {
                associatedNames = new String[numSelected];
                int next = 0;
                for(int i=0; i<propertyNames.length; i++) {
                    Button button = propertyButtons[i];
                    if(button.getSelection()) {
                        associatedNames[next] = propertyNames[i];
                        next++;
                    }
                }
            }
            shell.close();
            return;
        }
    }
}