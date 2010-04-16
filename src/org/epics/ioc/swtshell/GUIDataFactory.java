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
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.epics.pvData.factory.ConvertFactory;
import org.epics.pvData.factory.PVDataFactory;
import org.epics.pvData.misc.BitSet;
import org.epics.pvData.pv.Array;
import org.epics.pvData.pv.Convert;
import org.epics.pvData.pv.Field;
import org.epics.pvData.pv.PVArray;
import org.epics.pvData.pv.PVDataCreate;
import org.epics.pvData.pv.PVField;
import org.epics.pvData.pv.PVScalar;
import org.epics.pvData.pv.PVStructure;
import org.epics.pvData.pv.PVStructureArray;
import org.epics.pvData.pv.PVStructureScalar;
import org.epics.pvData.pv.Scalar;
import org.epics.pvData.pv.ScalarType;
import org.epics.pvData.pv.StructureArrayData;
import org.epics.pvData.pv.Type;

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
    
    private static class GUIDataImpl extends Dialog implements GUIData, SelectionListener {
    	private static final PVDataCreate pvDataCreate = PVDataFactory.getPVDataCreate();
        private Shell parent;
        private Shell shell;
        private Button doneButton;
        private Button editButton;
        private Text text;
        private Tree tree;
        
        private BitSet bitSet;
        
        private PVField pvField = null;
        private Type type = null;
        private StructureArrayData structureArrayData = new StructureArrayData();
        
        
        /**
         * Constructor.
         * @param parent The parent shell.
         */
        public GUIDataImpl(Shell parent) {
            super(parent,SWT.DIALOG_TRIM|SWT.NONE);
            this.parent = parent;
        }
        
        private static void textMessage(Text text,String message) {
            text.selectAll();
            text.clearSelection();
            text.setText(message);
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.swtshell.GUIData#get(org.epics.pvData.pv.PVStructure, org.epics.pvData.misc.BitSet)
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
            		Scalar scalar = (Scalar)field;
            		if(scalar.getScalarType()!=ScalarType.pvStructure) {
            			GetSimple getSimple = new GetSimple(parent,pvField);
            			boolean isModified = getSimple.get();
            			if(isModified) bitSet.set(pvField.getFieldOffset());
            			return;
            		} else {
            			PVStructureScalar pvStructureScalar = (PVStructureScalar)pvField;
                		PVStructure pvStruct = pvStructureScalar.getPVStructure();
                		BitSet newBitSet = new BitSet(pvStruct.getNumberFields());
                		newBitSet.clear();
                		GUIData guiData = GUIDataFactory.create(parent);
                		guiData.get(pvStruct, newBitSet);
                		bitSet.set(pvField.getFieldOffset());
                		return;
            		}
            	}
            	if(type==Type.scalarArray) {
            		Array array = (Array)field;
            		if(array.getElementType()!=ScalarType.pvStructure) {
            			GetSimple getSimple = new GetSimple(parent,pvField);
            			boolean isModified = getSimple.get();
            			if(isModified) bitSet.set(pvField.getFieldOffset());
            			return;
            		} else {
            			PVStructureArray pvStructureArray = (PVStructureArray)pvField;
                		PVStructure[] pvStructures = new PVStructure[1];
                		while(true) {
                			GetStructureArrayIndex getStructureArray = new GetStructureArrayIndex(parent,pvStructureArray);
                			int index = getStructureArray.getIndex();
                			if(index<0) break;
                			int num = pvStructureArray.get(index, 1, structureArrayData);
                			pvStructures[0] = null;
                			if(num==1) {
                				pvStructures[0] = structureArrayData.data[structureArrayData.offset];
                			}
                			if(pvStructures[0]==null) {
                				pvStructures[0] = pvDataCreate.createPVStructure(null, pvStructureArray.getStructureArray().getStructure());
                			}
                			PVStructure pvStruct = pvStructures[0];
                			BitSet newBitSet = new BitSet(pvStruct.getNumberFields());
                    		newBitSet.clear();
                    		GUIData guiData = GUIDataFactory.create(parent);
                    		guiData.get(pvStruct, newBitSet);
                    		pvStructureArray.put(index, 1, pvStructures, 0);
                    		bitSet.set(pvStructureArray.getFieldOffset());
                		}
                		return;
            		}
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
                        	Scalar scalar = (Scalar)field;
                        	if(scalar.getScalarType()!=ScalarType.pvStructure) {
                        		ok = true;
                        		textMessage(text,pvField.toString());
                        	} else {
                        		PVStructureScalar pvStructureScalar = (PVStructureScalar)pvField;
                        		PVStructure pvStruct = pvStructureScalar.getPVStructure();
                        		BitSet newBitSet = new BitSet(pvStruct.getNumberFields());
                        		newBitSet.clear();
                        		GUIData guiData = GUIDataFactory.create(shell);
                        		guiData.get(pvStruct, newBitSet);
                        		bitSet.set(pvField.getFieldOffset());
                        		ok=true;
                        	}
                        } else if(type==Type.scalarArray) {
                        	Array array = (Array)field;
                        	if(array.getElementType()!=ScalarType.pvStructure) {
                        		ok = true;
                        		String values = pvField.toString();
                        		textMessage(text,values);
                        	} else {
                        		PVStructureArray pvStructureArray = (PVStructureArray)pvField;
                        		PVStructure[] pvStructures = new PVStructure[1];
                        		while(true) {
                        			GetStructureArrayIndex getStructureArray = new GetStructureArrayIndex(shell,pvStructureArray);
                        			int index = getStructureArray.getIndex();
                        			if(index<0) break;
                        			int num = pvStructureArray.get(index, 1, structureArrayData);
                        			pvStructures[0] = null;
                        			if(num==1) {
                        				pvStructures[0] = structureArrayData.data[structureArrayData.offset];
                        			}
                        			if(pvStructures[0]==null) {
                        				pvStructures[0] = pvDataCreate.createPVStructure(null, pvStructureArray.getStructureArray().getStructure());
                        			}
                        			PVStructure pvStruct = pvStructures[0];
                        			BitSet newBitSet = new BitSet(pvStruct.getNumberFields());
                            		newBitSet.clear();
                            		GUIData guiData = GUIDataFactory.create(parent);
                            		guiData.get(pvStruct, newBitSet);
                            		pvStructureArray.put(index, 1, pvStructures, 0);
                            		bitSet.set(pvStructureArray.getFieldOffset());
                        		}
                        	}
                        }
                        if(!ok) {
                            textMessage(text,"cant handle type");
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
                    textMessage(text,"no field was selected");
                } else {
                    if(type==Type.scalar) {
                        try {
                            convert.fromString((PVScalar)pvField, text.getText());
                        }catch (NumberFormatException e) {
                            textMessage(text,"exception " + e.getMessage());
                            return;
                        }
                    } else { // type is array; elementType.isScalar
                        PVArray pvArray = (PVArray)pvField; 
                        try {
                            convert.fromString(pvArray,text.getText());
                        }catch (NumberFormatException e) {
                            textMessage(text,"exception " + e.getMessage());
                            return;
                        }
                    }
                    bitSet.set(pvField.getFieldOffset());
                }
                return;
            }
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
        
        private static class GetStructureArrayIndex extends Dialog implements SelectionListener{
        	private PVStructureArray pvStructureArray;
        	private Shell shell;
            private Text text;
            private int index=0;
        	
        	private GetStructureArrayIndex(Shell parent,PVStructureArray pvStructureArray) {
        		super(parent,SWT.DIALOG_TRIM|SWT.NONE);
        		this.pvStructureArray = pvStructureArray;
        	}

        	private int getIndex() {
        		shell = new Shell(super.getParent());
                shell.setText("arrayIndex");
                GridLayout gridLayout = new GridLayout();
                gridLayout.numColumns = 1;
                shell.setLayout(gridLayout);
                text = new Text(shell,SWT.BORDER);
                GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
                gridData.minimumWidth = 100;
                text.setLayoutData(gridData);
                text.addSelectionListener(this);
                textMessage(text,String.valueOf(index));
                shell.pack();
                shell.open();
                Display display = shell.getDisplay();
                while(!shell.isDisposed()) {
                    if(!display.readAndDispatch()) {
                        display.sleep();
                    }
                }
                shell.dispose();
                return index;
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
                    try {
                    	index = Integer.parseInt(text.getText());
                    }catch (NumberFormatException ex) {
                        textMessage(text,"exception " + ex.getMessage());
                        return;
                    }
                    shell.close();
                    return;
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
                textMessage(text,pvField.toString());
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
                        } else if(type==Type.scalarArray) {
                            convert.fromString((PVArray)pvField, text.getText());
                            modified = true;
                        } else {
                            textMessage(text,"Field type is a structure");
                        }
                    }catch (NumberFormatException ex) {
                        textMessage(text,"exception " + ex.getMessage());
                        return;
                    }
                    shell.close();
                    return;
                }
            }
            
        }
    }
}
