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

import org.epics.ioc.pv.*;
/**
 * @author mrk
 *
 */
public class SelectField {
    private Shell parent;
    
    public SelectField(Shell parent) {
        this.parent = parent;
    }
    
    public String getFieldName(PVRecord pvRecord) {
        FieldTree fieldTree = new FieldTree(parent);
        return fieldTree.getFieldName(pvRecord);
    }
        
    private class FieldTree implements SelectionListener {
        private Shell parent;
        private Shell shell;
        private Button doneButton;
        private Tree tree;
        private String fieldName = null;
        
        private FieldTree(Shell parent){
            this.parent = parent;
        }
        
        private String getFieldName(PVRecord pvRecord) {
            shell = new Shell(parent);
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
            tree = new Tree(composite,SWT.SINGLE|SWT.BORDER|SWT.CHECK);
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
                        fieldName = pvField.getFullFieldName();
                    } else if(object instanceof Property) {
                        TreeItem itemParent = treeItem.getParentItem();
                        PVField pvParent = (PVField)itemParent.getParentItem().getData();
                        Property[] propertys = pvParent.getField().getPropertys();
                        TreeItem[] childItems = itemParent.getItems();
                        assert(propertys.length==childItems.length);
                        for(int i=0; i<propertys.length; i++) {
                            if(childItems[i]==treeItem) {
                                fieldName = propertys[i].getPropertyName();
                                String parentName = pvParent.getFullFieldName();
                                if(parentName!=null && parentName.length()>0) {
                                    fieldName = pvParent.getFullFieldName() + "." + fieldName;
                                }
                                break;
                            }
                        }
                    }
                }
                shell.close();
            }
        }
        
        private void createPropertyTreeItem(TreeItem tree,Property[] propertys) {
            TreeItem treeItem = new TreeItem(tree,SWT.NONE);
            treeItem.setText("property");
            treeItem.setGrayed(true);
            for(Property property : propertys) {
                TreeItem propertyItem = new TreeItem(treeItem,SWT.NONE);
                propertyItem.setText(property.getPropertyName());
                propertyItem.setData(property);
            }
        }
        
        private void createStructureTreeItem(TreeItem tree,PVStructure pvStructure) {
            Property[] propertys = pvStructure.getField().getPropertys();
            if(propertys!=null&&propertys.length>0) {
                createPropertyTreeItem(tree,propertys);
            }
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
                } else {
                    propertys = field.getPropertys();
                    if(propertys!=null&&propertys.length>0) {
                        createPropertyTreeItem(treeItem,propertys);
                    }
                }
            }
        }
        
        private void createArrayTreeItem(TreeItem tree, PVArray pvArray) {
            Array array = (Array)pvArray.getField();
            Property[] propertys = array.getPropertys();
            if(propertys!=null&&propertys.length>0) {
                createPropertyTreeItem(tree,propertys);
            }
            Type elementType = array.getElementType();
            if(elementType.isScalar()) return;
            int length = pvArray.getLength();
            if(elementType==Type.pvEnum) {
                PVEnumArray pvEnumArray = (PVEnumArray)pvArray;
                EnumArrayData enumArrayData = new EnumArrayData();
                pvEnumArray.get(0, length, enumArrayData);
                PVEnum[] pvEnums = enumArrayData.data;
                for(PVEnum pvEnum : pvEnums) {
                    if(pvEnum==null) continue;
                    TreeItem treeItem = new TreeItem(tree,SWT.NONE);
                    Field field = pvEnum.getField();
                    treeItem.setText(field.getFieldName());
                    treeItem.setData(pvEnum);
                    propertys = field.getPropertys();
                    if(propertys!=null&&propertys.length>0) {
                        createPropertyTreeItem(treeItem,propertys);
                    }
                }
            } else if(elementType==Type.pvMenu) {
                PVMenuArray pvMenuArray = (PVMenuArray)pvArray;
                MenuArrayData menuArrayData = new MenuArrayData();
                pvMenuArray.get(0, length, menuArrayData);
                PVMenu[] pvMenus = menuArrayData.data;
                for(PVMenu pvMenu : pvMenus) {
                    if(pvMenu==null) continue;
                    TreeItem treeItem = new TreeItem(tree,SWT.NONE);
                    Field field = pvMenu.getField();
                    treeItem.setText(field.getFieldName());
                    treeItem.setData(pvMenu);
                    propertys = field.getPropertys();
                    if(propertys!=null&&propertys.length>0) {
                        createPropertyTreeItem(treeItem,propertys);
                    }
                }
            } else if(elementType==Type.pvArray) {
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
}
