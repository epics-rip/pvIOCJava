/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.swtshell;

import java.util.Iterator;
import java.util.Map;

import org.eclipse.swt.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

import org.epics.ioc.db.*;
/**
 * @author mrk
 *
 */
public class SelectRecord {
    private Shell parent;
    private IOCDB iocdb = IOCDBFactory.getMaster();
    private Map<String,DBRecord> recordMap;
    
    public SelectRecord(Shell parent) {
        this.parent = parent;
    }
    
    public String getRecordName() {
        RecordShell recordShell = new RecordShell(parent);
        return recordShell.getRecordName();
    }
        
    private class RecordShell extends Dialog implements SelectionListener {
        private Shell shell;
        private List list;
        private String recordName = null;
        private int ntimes = 0;
        
        private RecordShell(Shell parent){
            super(parent,SWT.DIALOG_TRIM|SWT.NONE);
        }
        
        private String getRecordName() {
            shell = new Shell(getParent(),getStyle());
            shell.setText("getRecord");
            GridLayout gridLayout = new GridLayout();
            gridLayout.numColumns = 1;
            shell.setLayout(gridLayout);
            Composite composite = new Composite(shell,SWT.BORDER);
            gridLayout = new GridLayout();
            gridLayout.numColumns = 1;
            composite.setLayout(gridLayout);
            list = new List(composite,SWT.SINGLE|SWT.V_SCROLL|SWT.H_SCROLL);
            GridData listGridData = new GridData(GridData.FILL_BOTH);
            list.setLayoutData(listGridData);
            recordMap = iocdb.getRecordMap();
            Iterator<String> iter = recordMap.keySet().iterator();
            for(int i=0; i< recordMap.size(); i++) {
                list.add(iter.next());
            }
            list.addSelectionListener(this);
            Display display = shell.getDisplay();
            shell.open();
            while(!shell.isDisposed()) {
                if(!display.readAndDispatch()) {
                    display.sleep();
                }
            }
            shell.dispose();
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
