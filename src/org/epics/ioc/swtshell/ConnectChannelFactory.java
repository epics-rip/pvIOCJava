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
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.epics.ca.channelAccess.client.ChannelAccess;
import org.epics.ca.channelAccess.client.ChannelProvider;
import org.epics.ca.channelAccess.client.ChannelRequester;
import org.epics.ca.channelAccess.server.impl.ChannelAccessFactory;
import org.epics.pvData.misc.Executor;
import org.epics.pvData.misc.ExecutorNode;


/**
 * @author mrk
 *
 */
public class ConnectChannelFactory {
    
    /**
     * Create a connect to channel.
     * When connect is called a window appears that allows the user to create a channel that is connected to a record.
     * @param parent The parent shell.
     * @param channelRequester The channel requester.
     * @return The ConnectChannel interface.
     */
    public static ConnectChannel create(Shell parent,ChannelRequester channelRequester) {
        return new ConnectChannelImpl(parent,channelRequester);
    }
    
    private static final ChannelAccess channelAccess = ChannelAccessFactory.getChannelAccess();
    private static Executor executor = SwtshellFactory.getExecutor();
    
    private static class ConnectChannelImpl extends Dialog
    implements ConnectChannel,SelectionListener,Runnable
    {
        private ConnectChannelImpl(Shell parent,ChannelRequester channelRequester) {
            super(parent,SWT.DIALOG_TRIM|SWT.NONE);
            this.channelRequester = channelRequester;
            this.parent = parent;
        }
        private ChannelRequester channelRequester;
        private Shell parent = null;
        private Shell shell = null;
        private ExecutorNode executorNode = executor.createNode(this);
        private Button selectLocalRecordButton = null;
        private Combo providerCombo = null;
        private Text pvNameText = null;
        private String providerName = null;
        private String pvName = null;
        /* (non-Javadoc)
         * @see org.epics.ioc.swtshell.ConnectChannel#connect()
         */
        @Override
        public void connect() {
            shell = new Shell(parent);  
            shell.setText("connectChannel");
            GridLayout gridLayout = new GridLayout();
            gridLayout.numColumns = 3;
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
            Composite pvname = new Composite(shell,SWT.BORDER);
            gridLayout = new GridLayout();
            gridLayout.numColumns = 2;
            pvname.setLayout(gridLayout);
            GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
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
            channelProvider.createChannel(pvName, channelRequester,ChannelProvider.PRIORITY_DEFAULT);
        }
    }
}
