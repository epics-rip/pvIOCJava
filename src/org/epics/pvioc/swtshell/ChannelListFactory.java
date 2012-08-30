/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS pvData is distributed subject to a Software License Agreement found
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
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.epics.pvaccess.client.Channel;
import org.epics.pvaccess.client.Channel.ConnectionState;
import org.epics.pvaccess.client.ChannelAccess;
import org.epics.pvaccess.client.ChannelAccessFactory;
import org.epics.pvaccess.client.ChannelProvider;
import org.epics.pvaccess.client.ChannelPutGet;
import org.epics.pvaccess.client.ChannelPutGetRequester;
import org.epics.pvaccess.client.ChannelRequester;
import org.epics.pvaccess.client.CreateRequestFactory;
import org.epics.pvdata.factory.FieldFactory;
import org.epics.pvdata.factory.PVDataFactory;
import org.epics.pvdata.misc.Executor;
import org.epics.pvdata.misc.ExecutorNode;
import org.epics.pvdata.misc.ThreadPriority;
import org.epics.pvdata.misc.Timer;
import org.epics.pvdata.misc.TimerFactory;
import org.epics.pvdata.pv.FieldCreate;
import org.epics.pvdata.pv.MessageType;
import org.epics.pvdata.pv.PVArray;
import org.epics.pvdata.pv.PVDataCreate;
import org.epics.pvdata.pv.PVString;
import org.epics.pvdata.pv.PVStringArray;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.Requester;
import org.epics.pvdata.pv.ScalarType;
import org.epics.pvdata.pv.Status;
import org.epics.pvdata.pv.StringArrayData;


/**
 * @author mrk
 *
 */
public class ChannelListFactory {
    
    public static void init(Display display) {
        ChannelListImpl channelListImpl = new ChannelListImpl(display);
        channelListImpl.start();
    }
    private static final PVDataCreate pvDataCreate = PVDataFactory.getPVDataCreate();
    private static final FieldCreate fieldCreate = FieldFactory.getFieldCreate();
    private static final ChannelAccess channelAccess = ChannelAccessFactory.getChannelAccess();
    private static final Executor executor = SwtshellFactory.getExecutor();
    private static final Timer timer = TimerFactory.create("channelListFactory", ThreadPriority.lowest);
    
    private static class ChannelListImpl implements DisposeListener,SelectionListener,Runnable,Timer.TimerCallback
    {
        private ChannelListImpl(Display display) {
            this.display = display;
        }
        
        private boolean isDisposed = false;
        private static String windowName = "channelList";
        private Display display;
        private Shell shell = null;
        private Requester requester = null;
        private Combo providerCombo = null;
        private Text iocnameText = null;
        private Text regularExpressionText = null;
        private Text consoleText = null; 
        private ExecutorNode executorNode = executor.createNode(this);
        private Timer.TimerNode timerNode = TimerFactory.createNode(this);
        private GetChannelField getChannelField = null;
        
        private void start() {
            shell = new Shell(display);
            shell.setText(windowName);
            GridLayout gridLayout = new GridLayout();
            gridLayout.numColumns = 1;
            shell.setLayout(gridLayout);
            Composite provider = new Composite(shell,SWT.BORDER);
            gridLayout = new GridLayout();
            gridLayout.numColumns = 2;
            provider.setLayout(gridLayout);
            new Label(provider,SWT.RIGHT).setText("provider");
            providerCombo = new Combo(provider,SWT.SINGLE|SWT.BORDER);
            String[] names = channelAccess.getProviderNames();
            for(String name :names) {
                providerCombo.add(name);
            }
            providerCombo.select(0);
            
            Composite composite = new Composite(shell,SWT.BORDER);
            gridLayout = new GridLayout();
            gridLayout.numColumns = 2;
            composite.setLayout(gridLayout);
            GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
            composite.setLayoutData(gridData);   
            new Label(composite,SWT.RIGHT).setText("iocname");
            iocnameText = new Text(composite,SWT.BORDER);
            gridData = new GridData(GridData.FILL_HORIZONTAL);
            gridData.minimumWidth = 100;
            iocnameText.setLayoutData(gridData);
            
            
            composite = new Composite(shell,SWT.BORDER);
            gridLayout = new GridLayout();
            gridLayout.numColumns = 2;
            composite.setLayout(gridLayout);
            gridData = new GridData(GridData.FILL_HORIZONTAL);
            composite.setLayoutData(gridData);   
            new Label(composite,SWT.RIGHT).setText("regularExpression");
            regularExpressionText = new Text(composite,SWT.BORDER);
            gridData = new GridData(GridData.FILL_HORIZONTAL);
            gridData.minimumWidth = 300;
            regularExpressionText.setLayoutData(gridData);
            regularExpressionText.addSelectionListener(this);
            regularExpressionText.setText(".*");
            
            composite = new Composite(shell,SWT.BORDER);
            gridLayout = new GridLayout();
            gridLayout.numColumns = 1;
            composite.setLayout(gridLayout);
            gridData = new GridData(GridData.FILL_BOTH);
            composite.setLayoutData(gridData);
            Button clearItem = new Button(composite,SWT.PUSH);
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
            consoleText = new Text(composite,SWT.BORDER|SWT.H_SCROLL|SWT.V_SCROLL|SWT.READ_ONLY);
            gridData = new GridData(GridData.FILL_BOTH);
            gridData.heightHint = 400;
            gridData.widthHint = 400;
            consoleText.setLayoutData(gridData);
            requester = SWTMessageFactory.create(windowName,display,consoleText);
            shell.addDisposeListener(this);
            shell.pack();
            shell.open();
            
        }
        /* (non-Javadoc)
         * @see org.eclipse.swt.events.DisposeListener#widgetDisposed(org.eclipse.swt.events.DisposeEvent)
         */
        @Override
        public void widgetDisposed(DisposeEvent e) {
            isDisposed = true;
            GetChannelField temp = getChannelField;
            if(temp!=null) {
                getChannelField = null;
                temp.destroy();
            }
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
            if(object==regularExpressionText) {
                String providerName = providerCombo.getText();
                String iocname = iocnameText.getText();
                String regularExpression = regularExpressionText.getText();
                if(getChannelField!=null) {
                    consoleText.append("already active");
                    consoleText.append(String.format("%n"));
                    return;
                }
                getChannelField = new GetChannelField(providerName,iocname,regularExpression);
                executor.execute(executorNode);
            }
        }
        
        /* (non-Javadoc)
         * @see java.lang.Runnable#run()
         */
        @Override
        public void run() {
            timer.scheduleAfterDelay(timerNode, 5.0);
            getChannelField.connect();
        }
        /* (non-Javadoc)
         * @see org.epics.pvdata.misc.Timer.TimerCallback#callback()
         */
        @Override
        public void callback() {
            if(getChannelField!=null) getChannelField.destroy();
            if(isDisposed) return;
            display.asyncExec( new Runnable() {
                public void run() {
                    consoleText.append("timeOut");
                    getChannelField = null;
                }

            });
        }
        /* (non-Javadoc)
         * @see org.epics.pvdata.misc.Timer.TimerCallback#timerStopped()
         */
        @Override
        public void timerStopped() {
            if(isDisposed) return;
            GetChannelField temp = getChannelField;
            if(temp!=null) temp.destroy();
            display.asyncExec( new Runnable() {
                public void run() {
                    consoleText.append("timerStopped");
                    getChannelField = null;
                }

            });
        }
        
        private void getDone() {
            if(isDisposed) return;
            display.asyncExec( new Runnable() {
                public void run() {
                    if(getChannelField!=null) {
                        consoleText.append(getChannelField.getResult());
                        getChannelField = null;
                    }
                }

            });
        }

        private class GetChannelField implements ChannelRequester,ChannelPutGetRequester {
            
            GetChannelField(String providerName,String iocname, String regularExpression) {
                super();
                this.providerName = providerName;
                this.iocname = iocname;
                this.regularExpression = regularExpression;
            }
            
            private StringArrayData stringArrayData = new StringArrayData(); 
            private String providerName;
            private String iocname;
            private String regularExpression;
            private Channel channel = null;
            private ChannelPutGet channelPutGet = null;
           
            private PVString pvDatabase = null;
            private PVString pvRegularExpression = null;
            
            private PVString pvStatus = null;
            private PVStringArray pvRecordNames = null;
            private String result = null;
            
 
            void connect() {
                String channelName = iocname + "recordListRPC";
                ChannelProvider channelProvider = channelAccess.getProvider(providerName);
                channel = channelProvider.createChannel(channelName, this, ChannelProvider.PRIORITY_DEFAULT);
            }
            
            void destroy() {
                channel.destroy();
            }
            
            String getResult() {
                return result;
            }
            
            private void createPutGet() { 
                PVStructure pvPutRequest = CreateRequestFactory.createRequest("record[process=true]putField(arguments)getField(result)", this);
                channelPutGet = channel.createChannelPutGet(this, pvPutRequest);
            }
            
            /* (non-Javadoc)
             * @see org.epics.pvaccess.client.ChannelRequester#channelCreated(Status,org.epics.pvaccess.client.Channel)
             */
            @Override
            public void channelCreated(Status status, Channel channel) {
                if (!status.isOK()) {
                	message(status.toString(), status.isSuccess() ? MessageType.warning : MessageType.error);
                	if (!status.isSuccess()) return;
                }
                this.channel = channel;
            }
            /* (non-Javadoc)
             * @see org.epics.pvaccess.client.ChannelRequester#channelStateChange(org.epics.pvaccess.client.Channel, org.epics.pvaccess.client.Channel.ConnectionState)
             */
            @Override
            public void channelStateChange(Channel channel, ConnectionState state) {
                if(state == ConnectionState.CONNECTED) {
                    this.channel = channel;
                    createPutGet();
                }
            }

            /* (non-Javadoc)
             * @see org.epics.pvaccess.client.ChannelPutGetRequester#channelPutGetConnect(Status,org.epics.pvaccess.client.ChannelPutGet, org.epics.pvdata.pv.PVStructure, org.epics.pvdata.pv.PVStructure)
             */
            @Override
            public void channelPutGetConnect(Status status,ChannelPutGet channelPutGet,
                    PVStructure pvPutStructure, PVStructure pvGetStructure)
            {
                if (!status.isOK()) {
                	message(status.toString(), status.isSuccess() ? MessageType.warning : MessageType.error);
                	if (!status.isSuccess()) return;
                }
                this.channelPutGet = channelPutGet;
                if(pvPutStructure!=null && pvGetStructure!=null) {
                    pvDatabase = pvPutStructure.getStringField("arguments.database");
                    pvRegularExpression = pvPutStructure.getStringField("arguments.regularExpression");
                    pvStatus = pvGetStructure.getStringField("result.status");
                    PVArray pvArray = pvGetStructure.getScalarArrayField("result.names", ScalarType.pvString);
                    if(pvArray!=null) pvRecordNames = (PVStringArray)pvArray;
                    if(pvDatabase!=null && pvRegularExpression!=null && pvStatus!=null && pvArray!=null) {
                        pvDatabase.put("master");
                        pvRegularExpression.put(regularExpression);
                        this.channelPutGet.putGet(true);
                        return;
                    }
                }
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("createPutGet failed");
                if(pvPutStructure!=null && pvGetStructure!=null) {
                    if(pvDatabase==null) {
                        requester.message("field database does not exist", MessageType.error);
                    }
                    if(pvRegularExpression==null) {
                        requester.message("field regularExpression does not exist", MessageType.error);
                    }
                    if(pvStatus==null) {
                        requester.message("field status does not exist", MessageType.error);
                    }
                    if(pvRecordNames==null) {
                        requester.message("field names does not exist", MessageType.error);
                    }
                }
                channel.destroy();
                requester.message(stringBuilder.toString(), MessageType.error);
            }

            /* (non-Javadoc)
             * @see org.epics.pvaccess.client.ChannelPutGetRequester#getGetDone(Status)
             */
            @Override
            public void getGetDone(Status success) {}

            /* (non-Javadoc)
             * @see org.epics.pvaccess.client.ChannelPutGetRequester#getPutDone(Status)
             */
            @Override
            public void getPutDone(Status success) {}

            /* (non-Javadoc)
             * @see org.epics.pvaccess.client.ChannelPutGetRequester#putGetDone(Status)
             */
            @Override
            public void putGetDone(Status status) {
                if (!status.isOK()) {
                	message(status.toString(), status.isSuccess() ? MessageType.warning : MessageType.error);
                	if (!status.isSuccess()) return;
                }
                int length = pvRecordNames.getLength();
                if(length<1) {
                    requester.message(pvStatus.get(),MessageType.error);
                    return;
                }
                StringBuilder stringBuilder = new StringBuilder();
                pvRecordNames.get(0, length, stringArrayData);
                String[] names = stringArrayData.data;
                for(int i=0; i<length; i++) {
                    stringBuilder.append(names[i]);
                    stringBuilder.append(String.format("%n"));
                }
                result = stringBuilder.toString();
                timerNode.cancel();
                getDone();
                channel.destroy();
            }
            /* (non-Javadoc)
             * @see org.epics.pvdata.pv.Requester#getRequesterName()
             */
            @Override
            public String getRequesterName() {
                return requester.getRequesterName();
            }

            /* (non-Javadoc)
             * @see org.epics.pvdata.pv.Requester#message(java.lang.String, org.epics.pvdata.pv.MessageType)
             */
            @Override
            public void message(String message, MessageType messageType) {
                requester.message(message, MessageType.info);
            }
        }
    }
}
