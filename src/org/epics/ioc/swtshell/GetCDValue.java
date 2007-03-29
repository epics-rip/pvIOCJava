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

public class GetCDValue extends Dialog implements SelectionListener {
    private static Convert convert = ConvertFactory.getConvert();
    private Shell parent;
    private Shell shell;
    private Button doneButton;
    private Button editButton;
    private Text text;
    private Tree tree;
    private CDField cdField = null;
    
    public GetCDValue(Shell parent) {
        super(parent,SWT.DIALOG_TRIM|SWT.NONE);
    }
    
    public void getValue(CDRecord cdRecord) {
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
    public void widgetDefaultSelected(SelectionEvent arg0) {
        widgetSelected(arg0);
    }
    
    private void textMessage(String message) {
        text.selectAll();
        text.clearSelection();
        text.setText(message);
    }
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
                convert.fromString(pvField, text.getText());
                cdField.incrementNumPuts();
            }
        }
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
    
    private static class GetCDMenu extends Dialog implements SelectionListener{
        CDMenu cdMenu;
        Shell shell;
        Button doneButton;
        String[] choices;
        Button[] choiceButtons;
        int numChoices;
        
        GetCDMenu(Shell parent,CDMenu cdMenu) {
            super(parent,SWT.DIALOG_TRIM|SWT.NONE);
            this.cdMenu = cdMenu;
        }
        
        void get() {
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
