/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.swtshell;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.epics.pvaccess.client.Channel;
import org.epics.pvaccess.client.ChannelArray;
import org.epics.pvaccess.client.ChannelArrayRequester;
import org.epics.pvaccess.client.ChannelRequester;
import org.epics.pvaccess.client.Channel.ConnectionState;
import org.epics.pvdata.factory.ConvertFactory;
import org.epics.pvdata.factory.PVDataFactory;
import org.epics.pvdata.factory.FieldFactory;
import org.epics.pvdata.pv.*;

/**
 * Shell for processing a channel.
 * @author mrk
 *
 */
public class ArrayFactory {

    /**
     * Create the process shell.
     * @param display The display.
     */
    public static void init(Display display) {
        ArrayImpl processImpl = new ArrayImpl();
        processImpl.start(display);
    }

    private static final Convert convert = ConvertFactory.getConvert();
    private static final PVDataCreate pvDataCreate = PVDataFactory.getPVDataCreate();
    private static final FieldCreate fieldCreate = FieldFactory.getFieldCreate();
    
    private static class ArrayImpl implements DisposeListener,SelectionListener
    {
        // following are global to embedded classes
        private enum State{
            readyForConnect,connecting,
            readyForCreateArray,creatingArray,
            ready,active
        };
        private StateMachine stateMachine = new StateMachine();
        private ChannelClient channelClient = new ChannelClient();
        private Requester requester = null;
        private boolean isDisposed = false;

        private static String windowName = "array";
        private Shell shell = null;
        private Button connectButton = null;
        private Button createArrayButton = null;
        private Text subFieldText = null;
        
        private Button getButton = null;
        private Text getOffsetText = null;
        private Text countText = null;
        
        private Button putButton = null;
        private Text putOffsetText = null;
        private Text valueText = null;
        
        private Button setLengthButton = null;
        private Text lengthText = null;
        private Text capacityText = null;
        
        private Text consoleText = null;
        private String subField = "value";

        private void start(Display display) {
            shell = new Shell(display);
            shell.setText(windowName);
            GridLayout gridLayout = new GridLayout();
            gridLayout.numColumns = 1;
            shell.setLayout(gridLayout);
            Composite composite = new Composite(shell,SWT.BORDER);
            gridLayout = new GridLayout();
            gridLayout.numColumns = 3;
            composite.setLayout(gridLayout);
            connectButton = new Button(composite,SWT.PUSH);
            connectButton.setText("disconnect");
            connectButton.addSelectionListener(this);               
            
            Composite subFieldComposite = new Composite(composite,SWT.BORDER);
            gridLayout = new GridLayout();
            gridLayout.numColumns = 3;
            subFieldComposite.setLayout(gridLayout);
            createArrayButton = new Button(subFieldComposite,SWT.PUSH);
            createArrayButton.setText("destroyArray");
            createArrayButton.addSelectionListener(this);

            new Label(subFieldComposite,SWT.NONE).setText("subField");
            subFieldText = new Text(subFieldComposite,SWT.BORDER);
            GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
            gridData.minimumWidth = 500;
            subFieldText.setLayoutData(gridData);
            subFieldText.setText(subField);
            subFieldText.addSelectionListener(this);
            
            Composite getComposite = new Composite(shell,SWT.BORDER);
            gridLayout = new GridLayout();
            gridLayout.numColumns = 3;
            getComposite.setLayout(gridLayout);
            getButton = new Button(getComposite,SWT.PUSH);
            getButton.setText("get");
            getButton.addSelectionListener(this);
            Composite offsetComposite = new Composite(getComposite,SWT.BORDER);
            gridLayout = new GridLayout();
            gridLayout.numColumns = 2;
            offsetComposite.setLayout(gridLayout);
            new Label(offsetComposite,SWT.NONE).setText("offset");
            getOffsetText = new Text(offsetComposite,SWT.BORDER);
            gridData = new GridData(); 
            gridData.widthHint = 100;
            getOffsetText.setLayoutData(gridData);
            getOffsetText.setText("0");
            Composite countComposite = new Composite(getComposite,SWT.BORDER);
            gridLayout = new GridLayout();
            gridLayout.numColumns = 3;
            countComposite.setLayout(gridLayout);
            new Label(countComposite,SWT.NONE).setText("count");
            countText = new Text(countComposite,SWT.BORDER);
            gridData = new GridData(); 
            gridData.widthHint = 100;
            countText.setLayoutData(gridData);
            countText.setText("-1");
            
            Composite putComposite = new Composite(shell,SWT.BORDER);
            gridLayout = new GridLayout();
            gridLayout.numColumns = 3;
            putComposite.setLayout(gridLayout);
            gridData = new GridData(GridData.FILL_HORIZONTAL);
            putComposite.setLayoutData(gridData);
            putButton = new Button(putComposite,SWT.PUSH);
            putButton.setText("put");
            putButton.addSelectionListener(this);
            offsetComposite = new Composite(putComposite,SWT.BORDER);
            gridLayout = new GridLayout();
            gridLayout.numColumns = 2;
            offsetComposite.setLayout(gridLayout);
            new Label(offsetComposite,SWT.NONE).setText("offset");
            putOffsetText = new Text(offsetComposite,SWT.BORDER);
            gridData = new GridData(); 
            gridData.widthHint = 100;
            putOffsetText.setLayoutData(gridData);
            putOffsetText.setText("0");
            Composite valueComposite = new Composite(putComposite,SWT.BORDER);
            gridLayout = new GridLayout();
            gridLayout.numColumns = 3;
            valueComposite.setLayout(gridLayout);
            gridData = new GridData(GridData.FILL_HORIZONTAL);
            valueComposite.setLayoutData(gridData);
            new Label(valueComposite,SWT.NONE).setText("value");
            valueText = new Text(valueComposite,SWT.BORDER);
            gridData = new GridData(GridData.FILL_HORIZONTAL);
            valueText.setLayoutData(gridData);
            valueText.setText("[]");
            
            Composite setLengthComposite = new Composite(shell,SWT.BORDER);
            gridLayout = new GridLayout();
            gridLayout.numColumns = 3;
            setLengthComposite.setLayout(gridLayout);
            setLengthButton = new Button(setLengthComposite,SWT.PUSH);
            setLengthButton.setText("setLength");
            setLengthButton.addSelectionListener(this);
            Composite lengthComposite = new Composite(setLengthComposite,SWT.BORDER);
            gridLayout = new GridLayout();
            gridLayout.numColumns = 2;
            lengthComposite.setLayout(gridLayout);
            new Label(lengthComposite,SWT.NONE).setText("length");
            lengthText = new Text(lengthComposite,SWT.BORDER);
            gridData = new GridData(); 
            gridData.widthHint = 100;
            lengthText.setLayoutData(gridData);
            lengthText.setText("0");
            Composite capacityComposite = new Composite(setLengthComposite,SWT.BORDER);
            gridLayout = new GridLayout();
            gridLayout.numColumns = 3;
            capacityComposite.setLayout(gridLayout);
            new Label(capacityComposite,SWT.NONE).setText("capacity");
            capacityText = new Text(capacityComposite,SWT.BORDER);
            gridData = new GridData(); 
            gridData.widthHint = 100;
            capacityText.setLayoutData(gridData);
            capacityText.setText("-1");
            
            Composite consoleComposite = new Composite(shell,SWT.BORDER);
            gridLayout = new GridLayout();
            gridLayout.numColumns = 1;
            consoleComposite.setLayout(gridLayout);
            gridData = new GridData(GridData.FILL_BOTH);
            consoleComposite.setLayoutData(gridData);
            Button clearItem = new Button(consoleComposite,SWT.PUSH);
            clearItem.setText("&Clear");
            clearItem.addSelectionListener(new SelectionListener() {
                public void widgetDefaultSelected(SelectionEvent arg0) {
                    widgetSelected(arg0);
                }
                public void widgetSelected(SelectionEvent arg0) {
                    consoleText.selectAll();
                    consoleText.clearSelection();
                    consoleText.setText("");
                }
            });
            consoleText = new Text(consoleComposite,SWT.BORDER|SWT.H_SCROLL|SWT.V_SCROLL|SWT.READ_ONLY);
            gridData = new GridData(GridData.FILL_BOTH);
            gridData.heightHint = 100;
            gridData.widthHint = 200;
            consoleText.setLayoutData(gridData);
            requester = SWTMessageFactory.create(windowName,display,consoleText);
            shell.pack();
            stateMachine.setState(State.readyForConnect);
            shell.open();
            shell.addDisposeListener(this);
        }
        /* (non-Javadoc)
         * @see org.eclipse.swt.events.DisposeListener#widgetDisposed(org.eclipse.swt.events.DisposeEvent)
         */
        @Override
        public void widgetDisposed(DisposeEvent e) {
            isDisposed = true;
            channelClient.disconnect();
            
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
            if(isDisposed) return;
            Object object = arg0.getSource();
            if(object==connectButton) {
                State state = stateMachine.getState();
                if(state==State.readyForConnect) {
                    stateMachine.setState(State.connecting);
                    channelClient.connect(shell);
                } else {
                    channelClient.disconnect();
                    stateMachine.setState(State.readyForConnect);
                }
            } else if(object==createArrayButton) {
                State state = stateMachine.getState();
                if(state==State.readyForCreateArray) {
                    stateMachine.setState(State.creatingArray);
                    channelClient.createArray();
                } else {
                    channelClient.destroyArray();
                    stateMachine.setState(State.readyForCreateArray);
                }
            } else if(object==subFieldText) {
                subField = subFieldText.getText();
            } else if(object==getButton) {
                stateMachine.setState(State.active);
                int offset = Integer.parseInt(getOffsetText.getText());
                int count = Integer.parseInt(countText.getText());
                channelClient.get(offset, count);
            } else if(object==putButton) {
                stateMachine.setState(State.active);
                int offset = Integer.parseInt(putOffsetText.getText());
                String value = valueText.getText();
                channelClient.put(offset, value);
            } else if(object==setLengthButton) {
            	stateMachine.setState(State.active);
                int length = Integer.parseInt(lengthText.getText());
                int capacity = Integer.parseInt(capacityText.getText());
                channelClient.setLength(length,capacity);
            }
        }
        
        private class StateMachine {
            private State state = null;
            
            void setState(State newState) {
                if(isDisposed) return;
                state = newState;
                switch(state) {
                case readyForConnect:
                    connectButton.setText("connect");
                    createArrayButton.setText("createArray");
                    subFieldText.setEnabled(true);
                    getButton.setEnabled(false);
                    putButton.setEnabled(false);
                    setLengthButton.setEnabled(false);
                    return;
                case connecting:
                    connectButton.setText("disconnect");
                    createArrayButton.setText("createArray");
                    subFieldText.setEnabled(true);
                    getButton.setEnabled(false);
                    putButton.setEnabled(false);
                    setLengthButton.setEnabled(false);
                    return;
                case readyForCreateArray:
                    connectButton.setText("disconnect");
                    createArrayButton.setText("createArray");
                    subFieldText.setEnabled(true);
                    getButton.setEnabled(false);
                    putButton.setEnabled(false);
                    setLengthButton.setEnabled(false);
                    return;
                case creatingArray:
                    connectButton.setText("disconnect");
                    createArrayButton.setText("destroyArray");
                    subFieldText.setEnabled(false);
                    getButton.setEnabled(false);
                    putButton.setEnabled(false);
                    setLengthButton.setEnabled(false);
                    return;
                case ready:
                    connectButton.setText("disconnect");
                    createArrayButton.setText("destroyArray");
                    subFieldText.setEnabled(false);
                    getButton.setEnabled(true);
                    putButton.setEnabled(true);
                    setLengthButton.setEnabled(true);
                    return;
                case active:
                    connectButton.setText("disconnect");
                    createArrayButton.setText("destroyArray");
                    subFieldText.setEnabled(false);
                    getButton.setEnabled(false);
                    putButton.setEnabled(false);
                    setLengthButton.setEnabled(false);
                    return;
                }
                
            }
            State getState() {return state;}
        }
        
        private enum RunCommand {
            channelConnected,timeout,destroy,channelArrayConnect,getDone,putDone,setLengthDone
        }
        
        
        private class ChannelClient implements ChannelRequester,ConnectChannelRequester,Runnable,ChannelArrayRequester
        {
            private Channel channel = null;
            private ConnectChannel connectChannel = null;
            private ChannelArray channelArray = null;
            private PVArray pvArray = null;
            private RunCommand runCommand;
            
            void connect(Shell shell) {
                if(connectChannel!=null) {
                    message("connect in propress",MessageType.error);
                }
                connectChannel = ConnectChannelFactory.create(shell, this,this);
                connectChannel.connect();
            }
            
            
            void createArray() {
                Field[] fields = new Field[1];
                String[] fieldNames = new String[1];
                fields[0] = fieldCreate.createScalar(ScalarType.pvString);
                fieldNames[0] = "field";
                Structure structure = fieldCreate.createStructure(fieldNames, fields);
            	PVStructure pvRequest = pvDataCreate.createPVStructure(null,structure);
            	PVString pvFieldName = pvRequest.getStringField("field");
            	pvFieldName.put(subField);
                channelArray = channel.createChannelArray(this, pvRequest);
            }
            
            void destroyArray() {
                ChannelArray channelArray = this.channelArray;
                if(channelArray!=null) {
                    this.channelArray = null;
                    channelArray.destroy();
                }
            }
            void disconnect() {
                Channel channel = this.channel;
                if(channel!=null) {
                    this.channel = null;
                    channel.destroy();
                }
            }
  
            void get(int offset,int count) {
                channelArray.getArray(false, offset, count);
            }
            
            void put(int offset,String value) {
                try {
                    int len = convert.fromString((PVScalarArray)pvArray,value);
                    pvArray.setLength(len);
                } catch (Exception e) {
                    message("exception " + e.getMessage(),MessageType.error);
                    return;
                }
                try {
                    channelArray.putArray(false, offset, pvArray.getLength());
                } catch (IllegalArgumentException e) {
                    message("IllegalArgumentException " + e.getMessage(),MessageType.error);
                }
            }
            
            void setLength(int length,int capacity) {
            	channelArray.setLength(false, length, capacity);
            }
            /* (non-Javadoc)
             * @see org.epics.pvaccess.client.ChannelRequester#channelStateChange(org.epics.pvaccess.client.Channel, org.epics.pvaccess.client.Channel.ConnectionState)
             */
            @Override
            public void channelStateChange(Channel c, ConnectionState state) {

            	if(state == ConnectionState.DESTROYED) {
                    this.channel = null;
                    runCommand = RunCommand.destroy;
                    shell.getDisplay().asyncExec(this);
            	}
                
            	if(state != ConnectionState.CONNECTED) {
                    message("channel " + state,MessageType.error);
                    return;
                }
                
            	channel = c;
                ConnectChannel connectChannel = this.connectChannel;
                if(connectChannel!=null) {
                    connectChannel.cancelTimeout();
                    this.connectChannel = null;
                }
                runCommand = RunCommand.channelConnected;
                shell.getDisplay().asyncExec(this);
            }
            /* (non-Javadoc)
             * @see org.epics.pvaccess.client.ChannelRequester#channelCreated(org.epics.pvdata.pv.Status, org.epics.pvaccess.client.Channel)
             */
            @Override
            public void channelCreated(Status status,Channel c) {
                if (!status.isOK()) {
                    message(status.toString(),MessageType.error);
                    return;
                }
                channel = c;;
            }
            /* (non-Javadoc)
             * @see org.epics.pvioc.swtshell.ConnectChannelRequester#timeout()
             */
            @Override
            public void timeout() {
                Channel channel = this.channel;
                if(channel!=null) {
                    this.channel = null;
                    channel.destroy();
                }
                message("channel connect timeout",MessageType.info);
                runCommand = RunCommand.destroy;
                shell.getDisplay().asyncExec(this);
            }
            /* (non-Javadoc)
             * @see org.epics.pvaccess.client.ChannelArrayRequester#channelArrayConnect(Status,org.epics.pvaccess.client.ChannelArray, org.epics.pvdata.pv.PVArray)
             */
            @Override
            public void channelArrayConnect(Status status,ChannelArray channelArray,PVArray pvArray) {
                if (!status.isOK()) {
                	message(status.toString(), status.isSuccess() ? MessageType.warning : MessageType.error);
                	if (!status.isSuccess()) return;
                }
                if(pvArray.getField().getType()==Type.structureArray) {
                	message("The elementType is structure. Use structureArray to access.",MessageType.error);
                	return;
                }
                this.pvArray = pvArray;
                runCommand = RunCommand.channelArrayConnect;
                shell.getDisplay().asyncExec(this);
            }
            /* (non-Javadoc)
             * @see org.epics.pvaccess.client.ChannelArrayRequester#getArrayDone(Status)
             */
            @Override
            public void getArrayDone(Status status) {
                if (!status.isOK()) {
                	message(status.toString(), status.isSuccess() ? MessageType.warning : MessageType.error);
                	if (!status.isSuccess()) return;
                }
                runCommand = RunCommand.getDone;
                shell.getDisplay().asyncExec(this);
                
            }
            /* (non-Javadoc)
             * @see org.epics.pvaccess.client.ChannelArrayRequester#putArrayDone(Status)
             */
            @Override
            public void putArrayDone(Status status) {
                if (!status.isOK()) {
                	message(status.toString(), status.isSuccess() ? MessageType.warning : MessageType.error);
                	if (!status.isSuccess()) return;
                }
                runCommand = RunCommand.putDone;
                shell.getDisplay().asyncExec(this);
            }
            /* (non-Javadoc)
             * @see org.epics.pvaccess.client.ChannelArrayRequester#setLengthDone(org.epics.pvdata.pv.Status)
             */
            @Override
			public void setLengthDone(Status status) {
            	if (!status.isOK()) {
                	message(status.toString(), status.isSuccess() ? MessageType.warning : MessageType.error);
                	if (!status.isSuccess()) return;
                }
                runCommand = RunCommand.setLengthDone;
                shell.getDisplay().asyncExec(this);
			}


			/* (non-Javadoc)
             * @see java.lang.Runnable#run()
             */
            @Override
            public void run() {
                switch(runCommand) {
                case channelConnected:
                    stateMachine.setState(State.readyForCreateArray);
                    return;
                case timeout:
                    stateMachine.setState(State.readyForConnect);
                    return;
                case destroy:
                    stateMachine.setState(State.readyForConnect);
                    return;
                case channelArrayConnect:
                    stateMachine.setState(State.ready);
                    return;
                case getDone:
                    String value = pvArray.toString();
                    int indStart = value.lastIndexOf('[');
                    int indEnd = value.lastIndexOf(']');
                    if(indEnd>indStart) value = value.substring(indStart+1,indEnd);
                    valueText.setText(value);
                    stateMachine.setState(State.ready);
                    return;
                case putDone:
                    message("putDone",MessageType.info);
                    stateMachine.setState(State.ready);
                    return;
                case setLengthDone:
                	message("setLengthDone",MessageType.info);
                    stateMachine.setState(State.ready);
                    return;
                }
                
            }
            /* (non-Javadoc)
             * @see org.epics.pvioc.util.Requester#getRequesterName()
             */
            @Override
            public String getRequesterName() {
                return requester.getRequesterName();
            }
            /* (non-Javadoc)
             * @see org.epics.pvioc.util.Requester#message(java.lang.String, org.epics.pvioc.util.MessageType)
             */
            @Override
            public void message(final String message, final MessageType messageType) {
                requester.message(message, MessageType.info);
            }
        }
    }
}
