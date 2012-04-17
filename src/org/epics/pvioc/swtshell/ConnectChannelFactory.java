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
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.epics.pvaccess.client.Channel;
import org.epics.pvaccess.client.ChannelAccess;
import org.epics.pvaccess.client.ChannelAccessFactory;
import org.epics.pvaccess.client.ChannelProvider;
import org.epics.pvaccess.client.ChannelRequester;
import org.epics.pvdata.misc.Executor;
import org.epics.pvdata.misc.ExecutorNode;
import org.epics.pvdata.misc.ThreadPriority;
import org.epics.pvdata.misc.Timer;
import org.epics.pvdata.misc.Timer.TimerCallback;
import org.epics.pvdata.misc.Timer.TimerNode;
import org.epics.pvdata.misc.TimerFactory;


/**
 * @author mrk
 *
 */
public class ConnectChannelFactory {
    
    /**
     * Create a connect to channel.
     * When connect is called a window appears that allows the user to create a channel that is connected to a record.
     * @param parent The parent shell.
     * @param connectChannelRequester The connectChannelRequester.
     * @param channelRequester The channel requester.
     * @return The ConnectChannel interface.
     */
    public static ConnectChannel create(Shell parent,ConnectChannelRequester connectChannelRequester,ChannelRequester channelRequester) {
        return new ConnectChannelImpl(parent,connectChannelRequester,channelRequester);
    }
    
    public static boolean pvDataCompatible(Channel channel) {
        String providerName = channel.getProvider().getProviderName();
        if(providerName.equals("local")) return true;
        if(providerName.indexOf('4')>=0) return true;
        return false;
    }
    
    private static final ChannelAccess channelAccess = ChannelAccessFactory.getChannelAccess();
    private static Executor executor = SwtshellFactory.getExecutor();
    private static Timer timer = TimerFactory.create("connectChannel", ThreadPriority.lower);
    
    private static class ConnectChannelImpl extends Dialog
    implements ConnectChannel,SelectionListener,Runnable,TimerCallback
    {
        private ConnectChannelImpl(Shell parent,ConnectChannelRequester connectChannelRequester,ChannelRequester channelRequester) {
            super(parent,SWT.DIALOG_TRIM|SWT.NONE);
            this.parent = parent;
            this.connectChannelRequester = connectChannelRequester;
            this.channelRequester = channelRequester;
        }
        private ExecutorNode executorNode = executor.createNode(this);
        private TimerNode timerNode = TimerFactory.createNode(this);
        private Shell parent = null;
        private ConnectChannelRequester connectChannelRequester = null;
        private ChannelRequester channelRequester;
        
        private Shell shell = null;
        private Button selectLocalRecordButton = null;
        private Combo providerCombo = null;
        private String providerName = null;
        private Text timeoutText = null;
        private Text pvNameText = null;
        private String pvName = null;
        private double delay = 2.0;
        /* (non-Javadoc)
         * @see org.epics.pvioc.swtshell.ConnectChannel#connect()
         */
        @Override
        public void connect() {
            shell = new Shell(parent);  
            shell.setText("connectChannel");
            GridLayout gridLayout = new GridLayout();
            gridLayout.numColumns = 4;
            shell.setLayout(gridLayout);
            selectLocalRecordButton = new Button(shell,SWT.PUSH);
            selectLocalRecordButton.setText("selectLocalRecord");
            selectLocalRecordButton.addSelectionListener(this);
            
            Composite provider = new Composite(shell,SWT.BORDER);
            gridLayout = new GridLayout();
            gridLayout.numColumns = 2;
            provider.setLayout(gridLayout);
            new Label(provider,SWT.RIGHT).setText("provider");
            providerCombo = new Combo(provider,SWT.SINGLE|SWT.BORDER);
            String[] names = channelAccess.getProviderNames();
            providerName = names[0];
            for(String name :names) {
                providerCombo.add(name);
            }
            providerCombo.select(0);
            
            Composite timeout = new Composite(shell,SWT.BORDER);
            gridLayout = new GridLayout();
            gridLayout.numColumns = 2;
            timeout.setLayout(gridLayout);
            GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
            timeout.setLayoutData(gridData);   
            new Label(timeout,SWT.RIGHT).setText("timeout");
            timeoutText = new Text(timeout,SWT.BORDER);
            gridData = new GridData(GridData.FILL_HORIZONTAL);
            gridData.minimumWidth = 100;
            timeoutText.setLayoutData(gridData);
            timeoutText.setText("2.0");
            
            Composite pvname = new Composite(shell,SWT.BORDER);
            gridLayout = new GridLayout();
            gridLayout.numColumns = 2;
            pvname.setLayout(gridLayout);
            gridData = new GridData(GridData.FILL_HORIZONTAL);
            pvname.setLayoutData(gridData);   
            new Label(pvname,SWT.RIGHT).setText("pvname");
            pvNameText = new Text(pvname,SWT.BORDER);
            gridData = new GridData(GridData.FILL_HORIZONTAL);
            gridData.minimumWidth = 300;
            pvNameText.setLayoutData(gridData);
            pvNameText.addSelectionListener(this);
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
         * @see org.epics.pvioc.swtshell.ConnectChannel#cancelTimeout()
         */
        @Override
        public void cancelTimeout() {
            timerNode.cancel();
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
            if(object==selectLocalRecordButton) {
                SelectLocalRecord selectLocalRecord = 
                    SelectLocalRecordFactory.create(shell, channelRequester);
                pvName = selectLocalRecord.getRecordName();
                if(pvName==null) return;
                pvNameText.setText(pvName);
                pvName = pvNameText.getText();
                providerName = "local";
                executor.execute(executorNode);
                shell.close();
                return;
            } else if(object==pvNameText) {
                pvName = pvNameText.getText();
                providerName = providerCombo.getText();
                delay = Double.parseDouble(timeoutText.getText());
                executor.execute(executorNode);
                shell.close();
            }
        }
        /* (non-Javadoc)
         * @see java.lang.Runnable#run()
         */
        @Override
        public void run() {
            ChannelProvider channelProvider = channelAccess.getProvider(providerName);
            timer.scheduleAfterDelay(timerNode, delay);
            channelProvider.createChannel(pvName, channelRequester,ChannelProvider.PRIORITY_DEFAULT);
        }
        /* (non-Javadoc)
         * @see org.epics.pvdata.misc.Timer.TimerCallback#callback()
         */
        @Override
        public void callback() {
            connectChannelRequester.timeout();
        }
        /* (non-Javadoc)
         * @see org.epics.pvdata.misc.Timer.TimerCallback#timerStopped()
         */
        @Override
        public void timerStopped() {
            connectChannelRequester.timeout();
        }
    }
}
