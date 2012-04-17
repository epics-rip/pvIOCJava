/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.swtshell;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.epics.pvdata.factory.ConvertFactory;
import org.epics.pvdata.pv.Convert;
import org.epics.pvdata.pv.Field;
import org.epics.pvdata.pv.MessageType;
import org.epics.pvdata.pv.PVField;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.Requester;
import org.epics.pvdata.pv.Type;
import org.epics.pvioc.database.PVRecord;

/**
 * Factory which implements SelectField.
 * @author mrk
 *
 */
public class SelectFieldFactory {
    /**
     * Create the shell.
     * @param parent The parent shell.
     * @param requester The requester.
     * @return The SelectField interface.
     */
    public static SelectField create(Shell parent,Requester requester) {
        return new SelectFieldImpl(parent,requester);
    }
    private static final Convert convert = ConvertFactory.getConvert();
    private static class SelectFieldImpl extends Dialog implements SelectField, SelectionListener {
        private Requester requester;
        private Shell shell;
        private Button doneButton;
        private Tree tree;
        private String fieldName = null;

        private SelectFieldImpl(Shell parent,Requester requester){
            super(parent,SWT.DIALOG_TRIM|SWT.NONE);
            this.requester = requester;
        }

        /* (non-Javadoc)
         * @see org.epics.pvioc.swtshell.SelectField#selectFieldName(org.epics.pvioc.pv.PVRecord)
         */
        public String selectFieldName(PVRecord pvRecord) {
            shell = new Shell(getParent(),getStyle());
            shell.setText("getFieldName");
            GridLayout gridLayout = new GridLayout();
            gridLayout.numColumns = 1;
            shell.setLayout(gridLayout);
            Composite composite = new Composite(shell,SWT.BORDER);
            gridLayout = new GridLayout();
            gridLayout.numColumns = 1;
            composite.setLayout(gridLayout);
            GridData compositeGridData = new GridData(GridData.FILL_BOTH);
            composite.setLayoutData(compositeGridData);
            doneButton = new Button(composite,SWT.PUSH);
            doneButton.setText("Done");
            doneButton.addSelectionListener(this);
            tree = new Tree(composite,SWT.SINGLE|SWT.BORDER);
            GridData treeGridData = new GridData(GridData.FILL_BOTH);
            tree.setLayoutData(treeGridData);
            TreeItem treeItem = new TreeItem(tree,SWT.NONE);
            treeItem.setText(pvRecord.getRecordName());
            treeItem.setData(pvRecord);
            createStructureTreeItem(treeItem,pvRecord.getPVRecordStructure().getPVStructure());
            shell.open();
            Display display = shell.getDisplay();
            while(!shell.isDisposed()) {
                if(!display.readAndDispatch()) {
                    display.sleep();
                }
            }
            shell.dispose();
            return fieldName;
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
        public void widgetSelected(SelectionEvent e) {
            if(e.getSource()==doneButton) {
                TreeItem[] treeItems = tree.getSelection();
                int length = treeItems.length;
                if(length!=0) {
                    assert(length==1);
                    TreeItem treeItem = treeItems[0];
                    Object object = treeItem.getData();
                    if(object instanceof PVField) {
                        PVField pvField = (PVField) object;
                        StringBuilder builder = new StringBuilder();
                        convert.getFullFieldName(builder,pvField);
                        fieldName = builder.toString();
                    } else if(object==null) {
                        requester.message("property is illegal selection",MessageType.error);
                    }
                }
                shell.close();
            }
        }

        private void createStructureTreeItem(TreeItem tree,PVStructure pvStructure) {
            PVField[] pvFields = pvStructure.getPVFields();
            for(PVField pvField : pvFields) {
                Field field = pvField.getField();
                TreeItem treeItem = new TreeItem(tree,SWT.NONE);
                treeItem.setText(pvField.getFieldName());
                treeItem.setData(pvField);
                Type type = field.getType();
                if(type==Type.structure) {
                    createStructureTreeItem(treeItem,(PVStructure)pvField);
                }
            }
        }
    }
}
