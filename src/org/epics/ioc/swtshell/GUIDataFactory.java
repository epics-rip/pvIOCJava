/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.swtshell;

import java.util.BitSet;
import java.util.regex.Pattern;

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
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import org.epics.ioc.channelAccess.*;

import org.epics.pvData.factory.ConvertFactory;
import org.epics.pvData.channelAccess.*;
import org.epics.pvData.pv.*;
import org.epics.pvData.pvCopy.*;

/**
 * Factory which implements CDGet.
 * @author mrk
 *
 */
public class GUIDataFactory {
    /**
     * Create a CDGet and return the interface.
     * @param parent The parent shell.
     * @return The CDGet interface.
     */
    public static GUIData create(Shell parent) {
        return new GUIDataImpl(parent);
    }
    
    private static final Convert convert = ConvertFactory.getConvert();
    private static final Pattern separatorPattern = Pattern.compile("[,]");
    
    private static class GUIDataImpl extends Dialog implements GUIData, SelectionListener {
        
        private Shell parent;
        private Shell shell;
        private Button doneButton;
        private Button editButton;
        private Text text;
        private Tree tree;
        
        private BitSet bitSet;
        
        private PVField pvField = null;
        private Type type = null;
        
        
        /**
         * Constructor.
         * @param parent The parent shell.
         */
        public GUIDataImpl(Shell parent) {
            super(parent,SWT.DIALOG_TRIM|SWT.NONE);
            this.parent = parent;
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.swtshell.GUIData#get(org.epics.pvData.pv.PVStructure, java.util.BitSet)
         */
        @Override
        public void get(PVStructure pvStructure, BitSet bitSet) {
            this.bitSet = bitSet;
            bitSet.clear();
            PVField[] pvFields = pvStructure.getPVFields();
            if(pvFields.length==1) {
                PVField pvField = pvFields[0];
                Field field = pvField.getField();
                Type type = field.getType();
                if(type==Type.scalar) {
                    GetSimple getSimple = new GetSimple(parent,pvField);
                    boolean isModified = getSimple.get();
                    if(isModified) bitSet.set(pvField.getFieldOffset());
                    return;
                }
            }
            shell = new Shell(parent);
            shell.setText("getValue");
            GridLayout gridLayout = new GridLayout();
            gridLayout.numColumns = 1;
            shell.setLayout(gridLayout);
            Composite composite = new Composite(shell,SWT.BORDER);
            gridLayout = new GridLayout();
            gridLayout.numColumns = 1;
            composite.setLayout(gridLayout);
            GridData gridData = new GridData(GridData.FILL_BOTH);
            composite.setLayoutData(gridData);
            Composite modifyComposite = new Composite(composite,SWT.BORDER);
            gridLayout = new GridLayout();
            gridLayout.numColumns = 3;
            modifyComposite.setLayout(gridLayout);
            gridData = new GridData(GridData.FILL_HORIZONTAL);
            modifyComposite.setLayoutData(gridData);
            doneButton = new Button(modifyComposite,SWT.PUSH);
            doneButton.setText("Done");
            doneButton.addSelectionListener(this);
            editButton = new Button(modifyComposite,SWT.PUSH);
            editButton.setText("Edit");
            editButton.addSelectionListener(this);
            text = new Text(modifyComposite,SWT.BORDER);
            gridData = new GridData(GridData.FILL_HORIZONTAL);
            text.setLayoutData(gridData);
            text.addSelectionListener(this);
            tree = new Tree(composite,SWT.SINGLE|SWT.BORDER);
            gridData = new GridData(GridData.FILL_BOTH);
            tree.setLayoutData(gridData);
            TreeItem treeItem = new TreeItem(tree,SWT.NONE);
            treeItem.setText(pvStructure.getFullFieldName());
            createStructureTreeItem(treeItem,pvStructure);
            shell.open();
            Display display = shell.getDisplay();
            while(!shell.isDisposed()) {
                if(!display.readAndDispatch()) {
                    display.sleep();
                }
            }
            shell.dispose();
        }
        /* (non-Javadoc)
         * @see org.eclipse.swt.events.SelectionListener#widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent)
         */
        @Override
        public void widgetDefaultSelected(SelectionEvent arg0) {
            widgetSelected(arg0);
        }
        /* (non-Javadoc)
         * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
         */
        @Override
        public void widgetSelected(SelectionEvent arg0) {
            Object object = arg0.getSource();
            if(object==doneButton) {
                shell.close();
                return;
            }
            if(object==editButton) {
                TreeItem[] treeItems = tree.getSelection();
                int length = treeItems.length;
                if(length!=0) {
                    assert(length==1);
                    TreeItem treeItem = treeItems[0];
                    object = treeItem.getData();
                    if(object instanceof PVField) {
                        pvField = (PVField)object;
                        Field field = pvField.getField();
                        type = field.getType();
                        boolean ok = false;
                        if(type==Type.scalar) {
                            ok = true;
                            textMessage(pvField.toString());
                        } else if(type==Type.scalarArray) {
                            ok = true;
                            String values = pvField.toString();
                            int beginIndex = values.indexOf("{");
                            int endIndex = values.indexOf("}");
                            if(beginIndex>=0 && endIndex>=0) {
                                values = values.substring(beginIndex+1, endIndex-1);
                            }
                            textMessage(values);
                        }
                        if(!ok) {
                            textMessage("cant handle type");
                            pvField = null;
                            type = null;
                        }
                    } else {
                        pvField = null;
                        type = null;
                        text.setText("illegal field was selected");
                    }
                }
                return;
            }
            if(object==text) {
                if(pvField==null) {
                    textMessage("no field was selected");
                } else {
                    if(type==Type.scalar) {
                        try {
                            convert.fromString((PVScalar)pvField, text.getText());
                        }catch (NumberFormatException e) {
                            textMessage("exception " + e.getMessage());
                            return;
                        }
                    } else { // type is array; elementType.isScalar
                        PVArray pvArray = (PVArray)pvField;
                        String textValue = text.getText();
                        if(textValue==null || textValue.length()<=0) {
                            pvArray.setLength(0);
                            return;
                        }
                        if((textValue.charAt(0)=='[') && textValue.endsWith("]")) {
                            int offset = textValue.lastIndexOf(']');
                            textValue = textValue.substring(1, offset);
                        }
                        String[] values = separatorPattern.split(textValue);
                        int length = values.length;
                        if(length<=0) {
                            pvArray.setLength(0);
                            return;
                        }
                        
                        try {
                            convert.fromStringArray(pvArray, 0, values.length, values, 0);
                        }catch (NumberFormatException e) {
                            textMessage("exception " + e.getMessage());
                            return;
                        }
                    }
                    bitSet.set(pvField.getFieldOffset());
                }
                return;
            }
        }  
        
        
        private void textMessage(String message) {
            text.selectAll();
            text.clearSelection();
            text.setText(message);
        }
        
        private void createStructureTreeItem(TreeItem tree,PVStructure pvStructure) {
            PVField[] pvFields = pvStructure.getPVFields();
            for(PVField pvField : pvFields) {
                Field field = pvField.getField();
                TreeItem treeItem = new TreeItem(tree,SWT.NONE);
                treeItem.setText(field.getFieldName());
                Type type = field.getType();
                if(type==Type.structure) {
                    createStructureTreeItem(treeItem,(PVStructure)pvField);
                } else {
                    treeItem.setData(pvField);
                }
            }
        }
        
        private static class GetSimple extends Dialog implements SelectionListener{
            private PVField pvField;
            private Shell shell;
            private Text text;
            private boolean modified = false;
            
            private GetSimple(Shell parent,PVField pvField) {
                super(parent,SWT.DIALOG_TRIM|SWT.NONE);
                this.pvField = pvField;
            }
            
            private boolean get() {
                shell = new Shell(super.getParent());
                shell.setText("value");
                GridLayout gridLayout = new GridLayout();
                gridLayout.numColumns = 1;
                shell.setLayout(gridLayout);
                text = new Text(shell,SWT.BORDER);
                GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
                gridData.minimumWidth = 100;
                text.setLayoutData(gridData);
                text.addSelectionListener(this);
                shell.pack();
                shell.open();
                Display display = shell.getDisplay();
                while(!shell.isDisposed()) {
                    if(!display.readAndDispatch()) {
                        display.sleep();
                    }
                }
                shell.dispose();
                return  modified;
            }

            /* (non-Javadoc)
             * @see org.eclipse.swt.events.SelectionListener#widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent)
             */
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                widgetSelected(e);
            }
            /* (non-Javadoc)
             * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
             */
            @Override
            public void widgetSelected(SelectionEvent e) {
                Object object = e.getSource();
                if(object==text) {               
                    Type type = pvField.getField().getType();
                    try {
                        if(type==Type.scalar) {
                            convert.fromString((PVScalar)pvField, text.getText());
                            modified = true;
                        } else {
                            textMessage("CDField type is not scalar");
                        }
                    }catch (NumberFormatException ex) {
                        textMessage("exception " + ex.getMessage());
                        return;
                    }
                    shell.close();
                    return;
                }
            }
            
            private void textMessage(String message) {
                text.selectAll();
                text.clearSelection();
                text.setText(message);
            }
        }
    }
}
