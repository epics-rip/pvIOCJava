package org.epics.ioc.swtshell;

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
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.epics.ioc.ca.Channel;
import org.epics.ioc.ca.ChannelStateListener;
import org.epics.ioc.util.MessageNode;
import org.epics.ioc.util.MessageQueue;
import org.epics.ioc.util.MessageQueueFactory;
import org.epics.ioc.util.MessageType;
import org.epics.ioc.util.Requester;

public abstract class AbstractChannelShell
implements Requester, Runnable,
SelectionListener,ChannelStateListener,DisposeListener {

    protected AbstractChannelShell(String name) {
        windowName = name;
    }
    abstract void startClient(Composite parentWidget);
    
    protected Shell shell = null;
    protected Display display;
    protected Text consoleText = null;
    protected Channel channel = null;
    protected enum ConnectState {connected,disconnected}
    protected ConnectState connectState = ConnectState.disconnected;
    protected String[] connectStateText = {"connect    ","disconnect"};
    protected Button connectButton = null;

    
    private String windowName;    
    private MessageQueue messageQueue = MessageQueueFactory.create(3);

    protected void start(Display display) {
        this.display = display;
        shell = new Shell(display);
        shell.setText(windowName);
        GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 1;
        shell.setLayout(gridLayout);
        Composite shellComposite = new Composite(shell,SWT.NONE);
        gridLayout = new GridLayout();
        gridLayout.numColumns = 1;
        shellComposite.setLayout(gridLayout);
        GridData gridData = new GridData(GridData.FILL_BOTH);
        shellComposite.setLayoutData(gridData);   
        Composite clientComposite = new Composite(shellComposite,SWT.NONE);
        gridLayout = new GridLayout();
        gridLayout.numColumns = 1;
        clientComposite.setLayout(gridLayout);
        gridData = new GridData(GridData.FILL_HORIZONTAL);
        clientComposite.setLayoutData(gridData);
        startClient(clientComposite);
        Composite consoleComposite = new Composite(shellComposite,SWT.BORDER);
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
        shell.pack();
        shell.open();
    }
    
    protected void connectButtonCreate(Composite parent) {
        connectButton = new Button(parent,SWT.PUSH);
        connectButton.setText(connectStateText[0]);
        connectButton.addSelectionListener(this);
    }
    protected void connectButtonSelected() {
        switch(connectState) {
        case disconnected:
            GetChannel getChannel = new GetChannel(shell,this,this);
            channel = getChannel.getChannel();
            if(channel==null) {
                message("getChannel failed",MessageType.error);
                return;
            }
            connectState = ConnectState.connected;
            connectButton.setText(connectStateText[1]);
            message("getChannel " + channel.getChannelName(),MessageType.info);
            return;
        case connected:
            connectState = ConnectState.disconnected;
            connectButton.setText(connectStateText[0]);
            message("destroyChannel " + channel.getChannelName(),MessageType.info);
            channel.destroy();
            channel = null;
            return;
        }
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.util.Requester#getRequesterName()
     */
    public String getRequesterName() {
        return "swtshell " + windowName;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.util.Requester#message(java.lang.String, org.epics.ioc.util.MessageType)
     */
    public void message(final String message, MessageType messageType) {
        boolean syncExec = false;
        messageQueue.lock();
        try {
            if(messageQueue.isEmpty()) syncExec = true;
            if(messageQueue.isFull()) {
                messageQueue.replaceLast(message, messageType);
            } else {
                messageQueue.put(message, messageType);
            }
        } finally {
            messageQueue.unlock();
        }
        if(syncExec) {
            display.syncExec(this);
        }
    }
    /* (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    public void run() {
        while(true) {
            String message = null;
            int numOverrun = 0;
            messageQueue.lock();
            try {
                MessageNode messageNode = messageQueue.get();
                numOverrun = messageQueue.getClearOverrun();
                if(messageNode==null && numOverrun==0) break;
                message = messageNode.message;
            } finally {
                messageQueue.unlock();
            }
            if(numOverrun>0) {
                consoleText.append(String.format("%n%d missed messages&n", numOverrun));
            }
            if(message!=null) {
                consoleText.append(String.format("%s%n",message));
            }
        }
    }


    /* (non-Javadoc)
     * @see org.eclipse.swt.events.SelectionListener#widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent)
     */
    public void widgetDefaultSelected(SelectionEvent arg0) {
        widgetSelected(arg0);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.ChannelStateListener#channelStateChange(org.epics.ioc.ca.Channel, boolean)
     */
    public void channelStateChange(Channel c, boolean isConnected) {
        // TODO Auto-generated method stub

    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.ChannelStateListener#disconnect(org.epics.ioc.ca.Channel)
     */
    public void disconnect(Channel c) {
        // TODO 
    }

    /* (non-Javadoc)
     * @see org.eclipse.swt.events.DisposeListener#widgetDisposed(org.eclipse.swt.events.DisposeEvent)
     */
    public void widgetDisposed(DisposeEvent e) {
        if(channel!=null) channel.destroy();
        channel = null;
    }
}
