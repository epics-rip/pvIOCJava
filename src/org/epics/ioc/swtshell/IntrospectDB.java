/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.swtshell;

import java.util.*;

import org.eclipse.swt.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.widgets.List;

import org.epics.ioc.db.*;

/**
 * @author mrk
 *
 */
public class IntrospectDB {
    static private IOCDB iocdb = IOCDBFactory.getMaster();
    
    public static void init(Display display) {
        Introspect introspect = new Introspect(display);
        introspect.start();
    }
    
    private static class Introspect {
        private Display display;
        private Shell shell;
        private Text text;
        
        private Introspect(Display display) {
            this.display = display;
        }
        
        public void start() {
            shell = new Shell(display);
            shell.setText("introspectDB");
            GridLayout gridLayout = new GridLayout();
            gridLayout.numColumns = 1;
            shell.setLayout(gridLayout);
            Composite composite = new Composite(shell,SWT.BORDER);
            gridLayout = new GridLayout();
            gridLayout.numColumns = 1;
            composite.setLayout(gridLayout);
            List list = new List(composite,SWT.MULTI|SWT.V_SCROLL);
            Swtshell.makeBlanks(list, 5, 80);
            Button clearItem = new Button(composite,SWT.PUSH);
            clearItem.setText("&Clear");
            clearItem.addSelectionListener(new SelectionListener() {
                public void widgetDefaultSelected(SelectionEvent arg0) {
                    widgetSelected(arg0);
                }
                public void widgetSelected(SelectionEvent arg0) {
                    text.selectAll();
                    text.clearSelection();
                    text.setText("");
                }
            });
            text = new Text(composite,SWT.BORDER|SWT.WRAP|SWT.V_SCROLL|SWT.READ_ONLY);
            //text.setSize(sizeX,sizeY); DOES NOT WORK
            Swtshell.makeBlanks(text,20,100);
            shell.pack();
            new RecordShow(list);
            list.deselectAll();
            text.selectAll();
            text.clearSelection();
            text.setText("");
            shell.open();
        }
        
        private class RecordShow implements SelectionListener {
            private Map<String,DBRecord> recordMap = iocdb.getRecordMap();
            private List list;
            
            private RecordShow(List list) {
                this.list = list;
                Iterator<String> iter = recordMap.keySet().iterator();
                list.removeAll();
                for(int i=0; i< recordMap.size(); i++) {
                    list.add(iter.next());
                }
                list.addSelectionListener(this);
            }
            public void widgetDefaultSelected(SelectionEvent arg0) {
                widgetSelected(arg0);
            }
            public void widgetSelected(SelectionEvent arg0) {
                String[] names = list.getSelection();
                for(String name : names) {
                    DBRecord dbRecord = recordMap.get(name);
                    text.append(dbRecord.toString());
                }
            }
            
        }
        
    }
}
