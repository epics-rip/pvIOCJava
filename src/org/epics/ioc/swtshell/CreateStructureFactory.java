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
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.epics.pvData.factory.FieldFactory;
import org.epics.pvData.factory.PVDataFactory;
import org.epics.pvData.pv.Field;
import org.epics.pvData.pv.FieldCreate;
import org.epics.pvData.pv.PVDataCreate;
import org.epics.pvData.pv.ScalarType;
import org.epics.pvData.pv.Structure;

/**
 * Factory which implements CDGet.
 * @author mrk
 *
 */
public class CreateStructureFactory {
    /**
     * Create a CDGet and return the interface.
     * @param parent The parent shell.
     * @return The CDGet interface.
     */
    public static CreateStructure create(Shell parent) {
        return new CreateStructureImpl(parent);
    }
    private static class CreateStructureImpl extends Dialog implements CreateStructure, SelectionListener {
    	private static final FieldCreate fieldCreate = FieldFactory.getFieldCreate();
    	private static final PVDataCreate pvDataCreate = PVDataFactory.getPVDataCreate();
        private Shell parent = null;
        private Shell shell = null;
        private Button doneButton = null;
        private Button addButton = null;
        private Text fieldNameText = null;
        private List typeList = null;
        private List scalarTypeList = null;
        private Text consoleText = null;
        
        private Structure structure = null;
        
        /**
         * Constructor.
         * @param parent The parent shell.
         */
        public CreateStructureImpl(Shell parent) {
            super(parent,SWT.DIALOG_TRIM|SWT.NONE);
            this.parent = parent;
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.swtshell.CreatePVStructure#get(org.epics.pvData.pv.PVStructure, org.epics.pvData.misc.BitSet)
         */
        @Override
        public Structure create(String fieldName) {
            shell = new Shell(parent);
            shell.setText("createStructure");
            GridLayout gridLayout = new GridLayout();
            gridLayout.numColumns = 1;
            shell.setLayout(gridLayout);
            
            Composite composite = new Composite(shell,SWT.BORDER);
            gridLayout = new GridLayout();
            gridLayout.numColumns = 3;
            composite.setLayout(gridLayout);
                        
            doneButton = new Button(composite,SWT.PUSH);
            doneButton.setText("Done");
            doneButton.addSelectionListener(this);
            addButton = new Button(composite,SWT.PUSH);
            addButton.setText("Add");
            addButton.addSelectionListener(this);
            
            composite = new Composite(shell,SWT.BORDER);
            gridLayout = new GridLayout();
            gridLayout.numColumns = 2;
            composite.setLayout(gridLayout);
            GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
            composite.setLayoutData(gridData);   
            new Label(composite,SWT.RIGHT).setText("fieldName");
            fieldNameText = new Text(composite,SWT.BORDER);
            gridData = new GridData(GridData.FILL_HORIZONTAL);
            gridData.minimumWidth = 100;
            fieldNameText.setLayoutData(gridData);
            
            typeList = new List(shell,SWT.BORDER | SWT.H_SCROLL);
            typeList.add("scalar");
            typeList.add("scalarArray");
            typeList.add("structure");
            typeList.add("structureArray");
            
            scalarTypeList = new List(shell,SWT.BORDER | SWT.H_SCROLL);
            scalarTypeList.add("boolean");
            scalarTypeList.add("byte");
            scalarTypeList.add("short");
            scalarTypeList.add("int");
            scalarTypeList.add("long");
            scalarTypeList.add("float");
            scalarTypeList.add("double");
            scalarTypeList.add("string");
            
            
            consoleText = new Text(shell,SWT.BORDER|SWT.H_SCROLL|SWT.V_SCROLL|SWT.READ_ONLY);
            gridData = new GridData(GridData.FILL_BOTH);
            gridData.heightHint = 400;
            gridData.widthHint = 400;
            consoleText.setLayoutData(gridData);
            structure = fieldCreate.createStructure(fieldName, new Field[0]);
            consoleText.setText(structure.toString());
            shell.open();
            Display display = shell.getDisplay();
            while(!shell.isDisposed()) {
                if(!display.readAndDispatch()) {
                    display.sleep();
                }
            }
            shell.dispose();
            return structure;
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
            if(object==addButton) {
            	String fieldName = fieldNameText.getText();
            	if(fieldName.length()<=0) {
            		consoleText.selectAll();
                    consoleText.clearSelection();
                    consoleText.setText("fieldName not set");
                    return;
            	}
            	Field field = null;
            	int type = typeList.getFocusIndex();
            	int scalarType = scalarTypeList.getFocusIndex();
            	if(scalarType<0) {
            		consoleText.selectAll();
                    consoleText.clearSelection();
                    consoleText.setText("scalarType not specified");
            	}
            	switch(type) {
            	case 0:
            		field = fieldCreate.createScalar(fieldName, ScalarType.values()[scalarType]);
            	    break;
            	case 1:
            		field = fieldCreate.createScalarArray(fieldName,ScalarType.values()[scalarType]);
            		break;
            	case 2: {
            		CreateStructure createStructure = CreateStructureFactory.create(shell);
            		Structure struct = createStructure.create(fieldName);
            		field = fieldCreate.createStructure(fieldName, struct.getFields());
            		break;
            	}
                case 3: {
                	CreateStructure createStructure = CreateStructureFactory.create(shell);
            		Structure struct = createStructure.create("");
            		field = fieldCreate.createStructureArray(fieldName, struct);
                    break;
                }
                default :
                	consoleText.selectAll();
                    consoleText.clearSelection();
                    consoleText.setText("illegal type");
                    return;
            	}
            	Field[] oldFields = structure.getFields();
            	Field[] newFields = new Field[oldFields.length + 1];
            	for(int i=0; i< oldFields.length; i++) newFields[i] = oldFields[i];
            	newFields[oldFields.length] = field;
            	structure = fieldCreate.createStructure(structure.getFieldName(), newFields);
            	consoleText.selectAll();
                consoleText.clearSelection();
                consoleText.setText(structure.toString());
                return;
            }
        }  
        
    }
}
