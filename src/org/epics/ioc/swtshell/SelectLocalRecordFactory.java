/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.swtshell;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;
import org.epics.ioc.db.DBRecord;
import org.epics.ioc.db.IOCDB;
import org.epics.ioc.db.IOCDBFactory;
import org.epics.ioc.util.MessageType;
import org.epics.ioc.util.Requester;

/**
 * Factory which implements SelectLocalRecord.
 * @author mrk
 *
 */
public class SelectLocalRecordFactory {
    /**
     * Create the shell.
     * @param parent The parent shell.
     * @param requester The requester.
     * @return The SelectLocalRecord interface.
     */
    public static SelectLocalRecord create(Shell parent,Requester requester) {
        return new SelectRecordFactory(parent,requester);
    }
    
    private static class SelectRecordFactory extends Dialog
    implements SelectLocalRecord, SelectionListener
    {
        private Requester requester;
        private IOCDB iocdb = IOCDBFactory.getMaster();        
        private Shell shell;
        private List list;
        private int ntimes = 0;
        private String recordName = null;

        /**
         * Constructor
         * @param parent The parent shell.
         * @param requester The requestor.
         */
        private SelectRecordFactory(Shell parent,Requester requester){
            super(parent,SWT.DIALOG_TRIM|SWT.NONE);
            this.requester = requester;
        }

        /**
         * Select and return the name of the selected record.
         * @return The name or null if no record was selected.
         */
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
            DBRecord[] dbRecords = iocdb.getDBRecords();
            if(dbRecords.length==0) {
                requester.message(String.format(
                        "iocdb %s has no records",
                        iocdb.getName()),
                        MessageType.error);
                return null;
            }
            for(DBRecord dbRecord : dbRecords) {
                list.add(dbRecord.getPVRecord().getRecordName());
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
            ntimes = 0;
            return recordName;
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
                recordName = names[0];
                // An automatic selection is made. Skip it
                // Don't know why this happens.
                ntimes++;
                if(ntimes<2) return;
                shell.close();
            }  
        }
    }

}
