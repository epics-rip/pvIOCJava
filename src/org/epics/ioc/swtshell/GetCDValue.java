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
import org.eclipse.swt.widgets.*;
import org.epics.ioc.pv.*;
import org.epics.ioc.pv.Menu;
import org.epics.ioc.ca.*;

/**
 * Get values and put the result in a CDRecord (Channel Data Record)
 * @author mrk
 *
 */
public class GetCDValue extends Dialog implements SelectionListener {
    private static Convert convert = ConvertFactory.getConvert();
    private Shell parent;
    private Shell shell;
    private Button doneButton;
    private Button editButton;
    private Text text;
    private Tree tree;
    private CDField cdField = null;
    
    /**
     * Constructor.
     * @param parent The parent shell.
     */
    public GetCDValue(Shell parent) {
        super(parent,SWT.DIALOG_TRIM|SWT.NONE);
        this.parent = parent;
    }
    /**
     * Get values from the operator and put them in CDRecord.
     * @param cdRecord The CDRecord that holds the data.
     */
    public void getValue(CDRecord cdRecord) {
        cdRecord.getCDStructure().clearNumPuts();
        CDField[] cdFields = cdRecord.getCDStructure().getFieldCDFields();
        if(cdFields.length==1) {
            CDField cdField = cdFields[0];
            Field field = cdField.getPVField().getField();
            Type type = field.getType();
            if(type.isScalar()) {
                GetCDSimple getCDSimple = new GetCDSimple(parent,cdField);
                getCDSimple.get();
                return;
            }
        }
        shell = new Shell(parent);
        shell.setText("getFieldName");
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
        treeItem.setText(cdRecord.getPVRecord().getRecordName());
        createStructureTreeItem(treeItem,cdRecord.getCDStructure());
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
    public void widgetDefaultSelected(SelectionEvent arg0) {
        widgetSelected(arg0);
    }
    /* (non-Javadoc)
     * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
     */
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
                if(object instanceof CDField) {
                    cdField = (CDField)object;
                    PVField pvField = cdField.getPVField();
                    Type type = pvField.getField().getType();
                    if(type.isScalar()) {
                        textMessage(cdField.getPVField().toString());
                    } else if(type==Type.pvMenu) {
                        new GetCDMenu(shell,(CDMenu)cdField).get();
                        cdField = null;
                    } else {
                        textMessage("cant handle type");
                        cdField = null;
                    }
                } else {
                    cdField = null;
                    text.setText("illegal field was selected");
                }
            }
            return;
        }
        if(object==text) {
            if(cdField==null) {
                textMessage("no field was selected");
            } else {
                PVField pvField = cdField.getPVField();
                try {
                convert.fromString(pvField, text.getText());
                }catch (NumberFormatException e) {
                    textMessage("exception " + e.getMessage());
                    return;
                }
                cdField.incrementNumPuts();
            }
            return;
        }
    }  
    
    private void textMessage(String message) {
        text.selectAll();
        text.clearSelection();
        text.setText(message);
    }
    
    private void createStructureTreeItem(TreeItem tree,CDStructure cdStructure) {
        CDField[] cdFields = cdStructure.getFieldCDFields();
        for(CDField cdField : cdFields) {
            Field field = cdField.getPVField().getField();
            TreeItem treeItem = new TreeItem(tree,SWT.NONE);
            treeItem.setText(field.getFieldName());
            Type type = field.getType();
            if(type==Type.pvStructure) {
                createStructureTreeItem(treeItem,(CDStructure)cdField);
            } else if(type==Type.pvArray) {
                createArrayTreeItem(treeItem,cdField);
            } else {
                treeItem.setData(cdField);
            }
        }
    }
    
    private void createArrayTreeItem(TreeItem tree, CDField cdField) {
        PVArray pvArray = (PVArray)cdField.getPVField();
        Array array = (Array)pvArray.getField();
        Type elementType = array.getElementType();
        if(elementType.isScalar()) {
            tree.setData(cdField);
            return;
        }
        CDNonScalarArray cdArray = (CDNonScalarArray)cdField;
        CDField[] cdFields = cdArray.getElementCDFields();
        for(CDField cdf : cdFields) {
            if(cdf==null) continue;
            TreeItem treeItem = new TreeItem(tree,SWT.NONE);
            Field field = cdf.getPVField().getField();
            treeItem.setText(field.getFieldName());
            if(elementType==Type.pvArray) {
                createArrayTreeItem(treeItem,cdf);
            } else if(elementType==Type.pvStructure) {
                createStructureTreeItem(treeItem,(CDStructure)cdf);
            } else {
                treeItem.setData(cdField);
            }
        }
    }
    
    private static class GetCDSimple extends Dialog implements SelectionListener{
        private CDField cdField;
        private Shell shell;
        private Text text;
        
        private GetCDSimple(Shell parent,CDField cdField) {
            super(parent,SWT.DIALOG_TRIM|SWT.NONE);
            this.cdField = cdField;
        }
        
        private void get() {
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
            Object object = e.getSource();
            if(object==text) {               
                PVField pvField = cdField.getPVField();
                try {
                convert.fromString(pvField, text.getText());
                }catch (NumberFormatException ex) {
                    textMessage("exception " + ex.getMessage());
                    return;
                }
                cdField.incrementNumPuts();
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
    
    private static class GetCDMenu extends Dialog implements SelectionListener{
        CDMenu cdMenu;
        Shell shell;
        Button doneButton;
        String[] choices;
        Button[] choiceButtons;
        int numChoices;
        
        private GetCDMenu(Shell parent,CDMenu cdMenu) {
            super(parent,SWT.DIALOG_TRIM|SWT.NONE);
            this.cdMenu = cdMenu;
        }
        
        private void get() {
            shell = new Shell(super.getParent());
            shell.setText("getFieldName");
            GridLayout gridLayout = new GridLayout();
            gridLayout.numColumns = 1;
            shell.setLayout(gridLayout);
            doneButton = new Button(shell,SWT.PUSH);
            doneButton.setText("Done");
            doneButton.addSelectionListener(this);
            Composite composite = new Composite(shell,SWT.BORDER);
            gridLayout = new GridLayout();
            gridLayout.numColumns = 1;
            composite.setLayout(gridLayout);
            PVMenu pvMenu = cdMenu.getPVMenu();
            int index = pvMenu.getIndex();
            Menu menu = (Menu)cdMenu.getPVField().getField();
            choices = menu.getMenuChoices();
            numChoices = choices.length;
            choiceButtons = new Button[numChoices];
            for(int i=0; i<numChoices; i++) {
                Button button = new Button(composite,SWT.RADIO);
                choiceButtons[i] = button;
                button.setText(choices[i]);
                if(index==i) button.setSelection(true);
            }
            shell.pack();
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
        public void widgetDefaultSelected(SelectionEvent e) {
            // TODO Auto-generated method stub           
        }
        /* (non-Javadoc)
         * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
         */
        public void widgetSelected(SelectionEvent e) {
            Object object = e.getSource();
            if(object==doneButton) {
                for(int i=0; i<numChoices; i++) {
                    if(choiceButtons[i].getSelection()) {
                        cdMenu.enumIndexPut(i);
                        break;
                    }
                }
                shell.close();
                return;
            }
        }
    }
}
