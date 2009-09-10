/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.swtshell;

import java.util.concurrent.atomic.AtomicReference;

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
import org.epics.ca.channelAccess.client.Channel;
import org.epics.ca.channelAccess.client.ChannelArray;
import org.epics.ca.channelAccess.client.ChannelArrayRequester;
import org.epics.ca.channelAccess.client.ChannelRequester;
import org.epics.pvData.factory.ConvertFactory;
import org.epics.pvData.misc.Executor;
import org.epics.pvData.misc.ExecutorNode;
import org.epics.pvData.pv.Convert;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVArray;
import org.epics.pvData.pv.Requester;
import org.epics.pvData.pv.Status;

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
        ArrayImpl processImpl = new ArrayImpl(display);
        processImpl.start();
    }

    private static final Convert convert = ConvertFactory.getConvert();
    private static Executor executor = SwtshellFactory.getExecutor();
    
    private static class ArrayImpl implements DisposeListener,SelectionListener,ChannelRequester,ConnectChannelRequester,Runnable  {

        private ArrayImpl(Display display) {
            this.display = display;
        }

        private boolean isDisposed = false;
        private static String windowName = "array";
        private ExecutorNode executorNode = executor.createNode(this);
        private Display display;
        private Shell shell = null;
        private Requester requester = null;
        private Button connectButton = null;
        private Button disconnectButton = null;
        private Button createArrayButton = null;
        private Text subFieldText = null;
        
        private Button getButton = null;
        private Text getOffsetText = null;
        private Text countText = null;
        
        private Button putButton = null;
        private Text putOffsetText = null;
        private Text valueText = null;
        
        private Text consoleText = null;
        private Button connectArrayButton = null;
        private String subField = "value";
        
        private Array array = new Array();
        private boolean getFinished = false;
        private String getValue = null;
        private AtomicReference<Channel> channel = new AtomicReference<Channel>(null);
        private AtomicReference<ConnectChannel> connectChannel = new AtomicReference<ConnectChannel>(null);

        private void start() {
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
            connectButton.setText("connect");
            connectButton.addSelectionListener(this);               
            connectButton.setEnabled(true);
            disconnectButton = new Button(composite,SWT.PUSH);
            disconnectButton.setText("disconnect");
            disconnectButton.addSelectionListener(this);               
            disconnectButton.setEnabled(false);
            Composite subFieldComposite = new Composite(composite,SWT.BORDER);
            gridLayout = new GridLayout();
            gridLayout.numColumns = 3;
            subFieldComposite.setLayout(gridLayout);
            createArrayButton = new Button(subFieldComposite,SWT.PUSH);
            createArrayButton.setText("createArray");
            createArrayButton.addSelectionListener(this);
            createArrayButton.setEnabled(false);
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
            getButton.setEnabled(false);
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
            gridLayout.numColumns = 2;
            putComposite.setLayout(gridLayout);
            gridData = new GridData(GridData.FILL_HORIZONTAL);
            putComposite.setLayoutData(gridData);
            putButton = new Button(putComposite,SWT.PUSH);
            putButton.setText("put");
            putButton.addSelectionListener(this);
            putButton.setEnabled(false);
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
            shell.open();
            shell.addDisposeListener(this);
        }
        /* (non-Javadoc)
         * @see org.eclipse.swt.events.DisposeListener#widgetDisposed(org.eclipse.swt.events.DisposeEvent)
         */
        @Override
        public void widgetDisposed(DisposeEvent e) {
            isDisposed = true;
            executor.execute(executorNode);
            
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
                Channel channel = this.channel.get();
                if(channel!=null) {
                    message("must disconnect first",MessageType.error);
                    return;
                }
                connectChannel.set(ConnectChannelFactory.create(shell,this, this));
                connectChannel.get().connect();
            } else if(object==disconnectButton) {
                array.disconnect();
                connectButton.setEnabled(true);
                disconnectButton.setEnabled(false);
                getButton.setEnabled(false);
                putButton.setEnabled(false);
            } else if(object==createArrayButton) {
                array.create();
            } else if(object==subFieldText) {
                subField = subFieldText.getText();
            } else if(object==getButton) {
                int offset = Integer.parseInt(getOffsetText.getText());
                int count = Integer.parseInt(countText.getText());
                array.get(offset, count);
            } else if(object==putButton) {
                int offset = Integer.parseInt(putOffsetText.getText());
                array.put(offset, valueText.getText());
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requester#getRequesterName()
         */
        @Override
        public String getRequesterName() {
            return requester.getRequesterName();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requester#message(java.lang.String, org.epics.ioc.util.MessageType)
         */
        @Override
        public void message(String message, MessageType messageType) {
            requester.message(message, messageType);
        }
        /* (non-Javadoc)
         * @see org.epics.ca.channelAccess.client.ChannelRequester#channelStateChange(org.epics.ca.channelAccess.client.Channel, boolean)
         */
        @Override
        public void channelStateChange(Channel c, boolean isConnected) {
            if(!isConnected) {
                message("channel disconnected",MessageType.error);
                return;
            }
            channel.set(c);
            ConnectChannel connectChannel = this.connectChannel.getAndSet(null);
            if(connectChannel!=null) connectChannel.cancelTimeout();
            display.asyncExec(this);
        }
        /* (non-Javadoc)
         * @see org.epics.ca.channelAccess.client.ChannelRequester#channelCreated(Status,org.epics.ca.channelAccess.client.Channel)
         */
        @Override
        public void channelCreated(Status status,Channel c) {
            if (!status.isOK()) {
            	message(status.toString(), status.isSuccess() ? MessageType.warning : MessageType.error);
            	if (!status.isSuccess()) return;
            }
            channel.set(c);
            c.connect();
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelRequester#disconnect(org.epics.ioc.ca.Channel)
         */
        @Override
        public void destroy(Channel c) {
            display.asyncExec(this);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.swtshell.ConnectChannelRequester#timeout()
         */
        @Override
        public void timeout() {
            Channel channel = this.channel.getAndSet(null);
            if(channel!=null) channel.destroy();
            message("channel connect timeout",MessageType.info);
        }
        /* (non-Javadoc)
         * @see java.lang.Runnable#run()
         */
        @Override
        public void run() {
            Channel channel = null;
            if(isDisposed) {
                channel = this.channel.getAndSet(null);
                if(channel!=null) channel.destroy();
                return;
            }
            channel = this.channel.get();
            if(channel==null) return;
            if(getFinished) {
                valueText.setText(getValue);
                getFinished = false;
                getValue = null;
            }
            boolean isConnected = channel.isConnected();
            if(isConnected) {
                connectButton.setEnabled(false);
                disconnectButton.setEnabled(true);
                getButton.setEnabled(true);
                putButton.setEnabled(true);
                createArrayButton.setEnabled(true);
            } else {
                connectButton.setEnabled(true);
                disconnectButton.setEnabled(false);
                getButton.setEnabled(false);
                putButton.setEnabled(false);
                createArrayButton.setEnabled(false);
            }
        }
        
        private void getDone(String value) {
            getFinished = true;
            getValue = value;
            display.asyncExec(this);
        }
        
        private enum ArrayRunRequest{create,disconnect,get,put};
        
        private class Array implements Runnable,ChannelArrayRequester
        {
            private AtomicReference<ChannelArray> channelArray = new AtomicReference<ChannelArray>(null);
            private PVArray pvArray = null;
            private ExecutorNode executorNode = executor.createNode(this);
            private int offset;
            private int count;
            private String value;
            
            private ArrayRunRequest runRequest = null;
            
            void create() {
                runRequest = ArrayRunRequest.create;
                executor.execute(executorNode);
            }
            
            void disconnect() {
                pvArray = null;
                runRequest = ArrayRunRequest.disconnect;
                executor.execute(executorNode);
            }
  
            void get(int offset,int count) {
                this.offset = offset;
                this.count = count;
                runRequest = ArrayRunRequest.get;
                executor.execute(executorNode);
            }
            
            void put(int offset,String value) {
                this.offset = offset;
                this.value = value;
                runRequest = ArrayRunRequest.put;
                executor.execute(executorNode);
            }
            
            /* (non-Javadoc)
             * @see org.epics.ca.channelAccess.client.ChannelArrayRequester#channelArrayConnect(Status,org.epics.ca.channelAccess.client.ChannelArray, org.epics.pvData.pv.PVArray)
             */
            @Override
            public void channelArrayConnect(Status status,ChannelArray channelArray,PVArray pvArray) {
                if (!status.isOK()) {
                	message(status.toString(), status.isSuccess() ? MessageType.warning : MessageType.error);
                	if (!status.isSuccess()) return;
                }
                message("getArrayConnect succeeded",MessageType.info);
                this.channelArray.compareAndSet(null,channelArray);
                this.pvArray = pvArray;
            }
            /* (non-Javadoc)
             * @see org.epics.ca.channelAccess.client.ChannelArrayRequester#getArrayDone(Status)
             */
            @Override
            public void getArrayDone(Status status) {
                if (!status.isOK()) {
                	message(status.toString(), status.isSuccess() ? MessageType.warning : MessageType.error);
                	if (!status.isSuccess()) return;
                }
                message("getArrayDone succeeded",MessageType.info);
                getDone(pvArray.toString());
            }

            /* (non-Javadoc)
             * @see org.epics.ca.channelAccess.client.ChannelArrayRequester#putArrayDone(Status)
             */
            @Override
            public void putArrayDone(Status status) {
                if (!status.isOK()) {
                	message(status.toString(), status.isSuccess() ? MessageType.warning : MessageType.error);
                	if (!status.isSuccess()) return;
                }
                message("putArrayDone succeeded",MessageType.info);
            }
            
            /* (non-Javadoc)
             * @see java.lang.Runnable#run()
             */
            @Override
            public void run() {
                Channel c = channel.get();
                if(c==null) return;
                switch(runRequest) {
                case create:
                    channelArray.compareAndSet(null,c.createChannelArray(this, subField,null));
                    return;
                case disconnect:
                    c.destroy();
                    channel.set(null);
                    channelArray.set(null);
                    return;
                case get:
                    if(pvArray==null) {
                        message("get failed ",MessageType.error);
                        break;
                    }
                    channelArray.get().getArray(false, offset, count);
                    return;
                case put:
                    if(pvArray==null) {
                        message("put failed ",MessageType.error);
                        break;
                    }
                    ChannelArray channelArray = this.channelArray.get();
                    try {
                        int len = convert.fromString(pvArray,value);
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
                    return;
                }
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.util.Requester#getRequesterName()
             */
            @Override
            public String getRequesterName() {
                return requester.getRequesterName();
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.util.Requester#message(java.lang.String, org.epics.ioc.util.MessageType)
             */
            @Override
            public void message(final String message, final MessageType messageType) {
                requester.message(message, MessageType.info);
            }
        }
    }
}
