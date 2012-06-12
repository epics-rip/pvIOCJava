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
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.epics.pvaccess.client.Channel;
import org.epics.pvaccess.client.GetFieldRequester;
import org.epics.pvdata.factory.ConvertFactory;
import org.epics.pvdata.factory.PVDataFactory;
import org.epics.pvdata.misc.BitSet;
import org.epics.pvdata.misc.Executor;
import org.epics.pvdata.misc.ExecutorNode;
import org.epics.pvdata.pv.Convert;
import org.epics.pvdata.pv.Field;
import org.epics.pvdata.pv.MessageType;
import org.epics.pvdata.pv.PVDataCreate;
import org.epics.pvdata.pv.PVField;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.Status;
import org.epics.pvdata.pv.Structure;
import org.epics.pvdata.pv.Type;

/**
 * Shell for creating a PVStructure for a pvRequest.
 * @author mrk
 *
 */
public class CreateFieldRequestFactory {
    /**
     * Create a create request.
     * @param parent The parent shell.
     * @param channel The channel.
     * @param createFieldRequestRequester The requester.
     * @return The CreateRequest interface.
     */
    public static CreateFieldRequest create(Shell parent,Channel channel,CreateFieldRequestRequester createFieldRequestRequester) {
        return new CreateFieldRequestImpl(parent,channel,createFieldRequestRequester);
    }
    
    private static final PVDataCreate pvDataCreate = PVDataFactory.getPVDataCreate();
    private static final Convert convert = ConvertFactory.getConvert();
    private static Executor executor = SwtshellFactory.getExecutor();
    
    private static class CreateFieldRequestImpl extends Dialog
    implements CreateFieldRequest,GetFieldRequester,SelectionListener,Runnable  {
        
        private CreateFieldRequestImpl(Shell parent,Channel channel,CreateFieldRequestRequester createFieldRequestRequester) {
            super(parent,SWT.DIALOG_TRIM|SWT.NONE);
            this.createFieldRequestRequester = createFieldRequestRequester;
            this.parent = parent;
            this.channel = channel;
            display = parent.getDisplay();
        }
        
        private Shell parent = null;
        private Channel channel = null;
        private CreateFieldRequestRequester createFieldRequestRequester;
        private Display display = null;
        
        private ExecutorNode executorNode = null;
        private Shell shell = null;
        private Button doneButton;
        private Button introspectButton;
        private Text requestText = null;
        private Structure channelStructure = null;
        
        private enum RunState {
            introspectDone,introspectRequest
        }
        private RunState runState = null;
        /* (non-Javadoc)
         * @see org.epics.pvioc.swtshell.CreateRequest#create()
         */
        @Override
        public void create() {
            executorNode = executor.createNode(this);
            shell = new Shell(parent);
            shell.setText("createRequest");
            GridLayout gridLayout = new GridLayout();
            gridLayout.numColumns = 4;
            shell.setLayout(gridLayout);
            doneButton = new Button(shell,SWT.PUSH);
            doneButton.setText("done");
            doneButton.addSelectionListener(this);               
            doneButton.setEnabled(true);
            introspectButton = new Button(shell,SWT.PUSH);
            introspectButton.setText("introspect");
            introspectButton.addSelectionListener(this);               
            introspectButton.setEnabled(true);
            requestText = new Text(shell,SWT.BORDER);
            GridData gridData = new GridData(); 
            gridData.widthHint = 400;
            requestText.setLayoutData(gridData);
            requestText.setText(createFieldRequestRequester.getDefault());
            requestText.addSelectionListener(this);
            shell.pack();
            shell.open();
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
            if(object==introspectButton) {
                runState = RunState.introspectRequest;
                executor.execute(executorNode);
            } else if(object==doneButton) {
                try{
                    createFieldRequestRequester.request(requestText.getText());
                    shell.close();
                } catch (IllegalArgumentException e) {
                    message("illegal request value",MessageType.error);
                }
            }
        }
        /* (non-Javadoc)
         * @see org.epics.pvaccess.client.GetFieldRequester#getDone(Status,org.epics.pvdata.pv.Field)
         */
        @Override
        public void getDone(Status status,Field field) {
            if (!status.isOK()) {
            	message(status.toString(), status.isSuccess() ? MessageType.warning : MessageType.error);
            	if (!status.isSuccess()) return;
            }
            if(field.getType()!=Type.structure) {
                message("CreateRequest: channel introspection did not return a Structure",MessageType.error);
            } else {
                this.channelStructure = (Structure)field;
                runState = RunState.introspectDone;
                display.asyncExec(this);
            }
        }
        /* (non-Javadoc)
         * @see org.epics.pvdata.pv.Requester#getRequesterName()
         */
        @Override
        public String getRequesterName() {
            return createFieldRequestRequester.getRequesterName();
        }
        /* (non-Javadoc)
         * @see org.epics.pvdata.pv.Requester#message(java.lang.String, org.epics.pvdata.pv.MessageType)
         */
        @Override
        public void message(String message, MessageType messageType) {
            createFieldRequestRequester.message(message, messageType);
        }
        /* (non-Javadoc)
         * @see java.lang.Runnable#run()
         */
        @Override
        public void run() {
            switch(runState) {
            case introspectDone: { // must run as swt thread
                SelectSubSet selectSubSet = new SelectSubSet(shell);
                PVStructure pvStructure = pvDataCreate.createPVStructure(channelStructure);
                BitSet bitSet = selectSubSet.getSubSet(pvStructure,channel.getChannelName());
                String request = "";
                request = createRequest(request,bitSet,pvStructure.getPVFields());
                requestText.selectAll();
                requestText.clearSelection();
                requestText.setText(request);
                return;
            }
            case introspectRequest: { // must run as executor thread
                channel.getField(this, null);
                return;
            }
            }
        }
        
        private String createRequest(String request,BitSet bitSet,PVField[]pvFields) {
        	for(int i=0; i<pvFields.length; i++) {
        		PVField pvField = pvFields[i];
        		int offset = pvField.getFieldOffset();
        		int next = pvField.getNextFieldOffset();
        		int nextSet = bitSet.nextSetBit(offset);
        		if(nextSet<0) return request;
        		if(nextSet>=next) continue;
        		if(nextSet==offset) {
        			if(request.length()>1 && (request.charAt(request.length()-1) != '{')) request += ",";
        			StringBuilder builder = new StringBuilder();
        			convert.getFullFieldName(builder,pvField);
        			request += builder.toString();
        		} else if(pvField.getField().getType()==Type.structure) {
        			if(request.length()>1 && (request.charAt(request.length()-1) != '{')) request += ",";
        			request += pvField.getFieldName();
        			request += "{";
        			PVStructure pvStruct = (PVStructure)pvField;
        			request = createRequest(request,bitSet,pvStruct.getPVFields());
        			request += "}";
        		}
        	}
        	return request;
        }
        
        private static class SelectSubSet extends Dialog implements SelectionListener {
            
            public SelectSubSet(Shell parent) {
                super(parent,SWT.DIALOG_TRIM|SWT.NONE);
                this.parent = parent;
            }
            
            private Shell parent;
            private Shell shell;
            private Button doneButton;
            private Tree tree;
            private PVStructure pvStructure;
            private BitSet bitSet;
            
            private BitSet getSubSet(PVStructure pvStructure,String topName) {
                this.pvStructure = pvStructure;
                bitSet = new BitSet(pvStructure.getNumberFields());
                shell = new Shell(parent);
                shell.setText("getSubSet");
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
                gridLayout.numColumns = 1;
                modifyComposite.setLayout(gridLayout);
                gridData = new GridData(GridData.FILL_HORIZONTAL);
                modifyComposite.setLayoutData(gridData);
                doneButton = new Button(modifyComposite,SWT.PUSH);
                doneButton.setText("Done");
                doneButton.addSelectionListener(this);
                tree = new Tree(composite,SWT.CHECK|SWT.BORDER);
                gridData = new GridData(GridData.FILL_BOTH);
                tree.setLayoutData(gridData);
                TreeItem treeItem = new TreeItem(tree,SWT.NONE);
                treeItem.setText(topName);
                initTreeItem(treeItem,pvStructure);
                shell.open();
                Display display = shell.getDisplay();
                while(!shell.isDisposed()) {
                    if(!display.readAndDispatch()) {
                        display.sleep();
                    }
                }
                shell.dispose();
                return bitSet;
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
                    bitSet.clear();
                    setBitSet(bitSet,tree.getItems()[0],pvStructure);
                    shell.close();
                    return;
                }
            }
            
            private void initTreeItem(TreeItem tree,PVField pv) {
                tree.setData(pv);
                if(!(pv.getField().getType()==Type.structure)) return;
                PVStructure pvStructure = (PVStructure)pv;
                PVField[] pvFields = pvStructure.getPVFields();
                for(PVField pvField : pvFields) {
                    TreeItem treeItem = new TreeItem(tree,SWT.NONE);
                    treeItem.setText(pvField.getFieldName());
                    initTreeItem(treeItem,pvField);
                }
            }
            
            private void setBitSet(BitSet bitSet,TreeItem tree,PVField pv) {
                if(tree.getChecked()) {
                    bitSet.set(pv.getFieldOffset());
                    return;
                }
                if(!(pv.getField().getType()==Type.structure)) return;
                PVStructure pvStructure = (PVStructure)pv;
                PVField[] pvFields = pvStructure.getPVFields();
                TreeItem[] treeItems = tree.getItems();
                for(int i=0; i<pvFields.length; i++) {
                    PVField pvField = pvFields[i];
                    TreeItem treeItem = treeItems[i];
                    setBitSet(bitSet,treeItem,pvField);
                }
            }
        }
    }
}
