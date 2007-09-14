/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.swtshell;

import org.eclipse.swt.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

import org.epics.ioc.db.*;
import org.epics.ioc.pv.*;
import org.epics.ioc.util.*;
/**
 * Given a record name this selects the name of a field within the record.
 * @author mrk
 *
 */        
public class SelectFieldName extends Dialog implements SelectionListener {
    static private IOCDB iocdb = IOCDBFactory.getMaster();
    private Requester requester;
    private Shell shell;
    private Button doneButton;
    private Tree tree;
    private String fieldName = null;

    /**
     * Constructor
     * @param parent The parent shell.
     * @param requester The requestor.
     */
    public SelectFieldName(Shell parent,Requester requester){
        super(parent,SWT.DIALOG_TRIM|SWT.NONE);
        this.requester = requester;
    }

    /**
     * Return the name of the selected field.
     * @return The name or null of no name was selected.
     */
    public String getFieldName() {
        return fieldName;
    }

    /**
     * Select the field name.
     * @param recordName The record name.
     * @return (false,true) if a name (was not, was) selected.
     */
    public boolean selectFieldName(String recordName) {
        DBRecord dbRecord = iocdb.findRecord(recordName);
        if(dbRecord==null) {
            requester.message("recordName " + recordName + " not found",
                MessageType.error);
            return false;
        }
        PVRecord pvRecord = dbRecord.getPVRecord();
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
        createStructureTreeItem(treeItem,pvRecord);
        shell.open();
        Display display = shell.getDisplay();
        while(!shell.isDisposed()) {
            if(!display.readAndDispatch()) {
                display.sleep();
            }
        }
        shell.dispose();
        if(fieldName==null) return false;
        return true;
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
                    fieldName = pvField.getFullFieldName();
                } else if(object==null) {
                    requester.message("property is illegal selection",MessageType.error);
                }
            }
            shell.close();
        }
    }

    private void createStructureTreeItem(TreeItem tree,PVStructure pvStructure) {
        PVField[] pvFields = pvStructure.getFieldPVFields();
        for(PVField pvField : pvFields) {
            Field field = pvField.getField();
            TreeItem treeItem = new TreeItem(tree,SWT.NONE);
            treeItem.setText(field.getFieldName());
            treeItem.setData(pvField);
            Type type = field.getType();
            if(type==Type.pvStructure) {
                createStructureTreeItem(treeItem,(PVStructure)pvField);
            } else if(type==Type.pvArray) {
                createArrayTreeItem(treeItem,(PVArray)pvField);
            }
        }
    }

    private void createArrayTreeItem(TreeItem tree, PVArray pvArray) {
        Array array = (Array)pvArray.getField();
        Type elementType = array.getElementType();
        if(elementType.isScalar()) return;
        int length = pvArray.getLength();
        if(elementType==Type.pvArray) {
            PVArrayArray pvArrayArray = (PVArrayArray)pvArray;
            ArrayArrayData arrayArrayData = new ArrayArrayData();
            pvArrayArray.get(0, length, arrayArrayData);
            PVArray[] pvArrays = arrayArrayData.data;
            for(PVArray elementPVArray : pvArrays) {
                if(elementPVArray==null) continue;
                TreeItem treeItem = new TreeItem(tree,SWT.NONE);
                Field field = elementPVArray.getField();
                treeItem.setText(field.getFieldName());
                treeItem.setData(elementPVArray);
                createArrayTreeItem(treeItem,elementPVArray);
            }
        } else if(elementType==Type.pvStructure) {
            PVStructureArray pvStructureArray = (PVStructureArray)pvArray;
            StructureArrayData structureArrayData = new StructureArrayData();
            pvStructureArray.get(0, length, structureArrayData);
            PVStructure[] pvStructures = structureArrayData.data;
            for(PVStructure pvStructure : pvStructures) {
                if(pvStructure==null) continue;
                TreeItem treeItem = new TreeItem(tree,SWT.NONE);
                Field field = pvStructure.getField();
                treeItem.setText(field.getFieldName());
                treeItem.setData(pvStructure);
                createStructureTreeItem(treeItem,pvStructure);
            }
        }
    }
}