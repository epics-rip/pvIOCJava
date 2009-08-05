/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.swtshell;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.epics.pvData.channelAccess.Channel;
import org.epics.pvData.channelAccess.ChannelProcess;
import org.epics.pvData.channelAccess.ChannelProcessRequester;
import org.epics.pvData.channelAccess.ChannelRequester;
import org.epics.pvData.misc.Executor;
import org.epics.pvData.misc.ExecutorNode;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.Requester;

/**
 * Shell for processing a channel.
 * @author mrk
 *
 */
public class ProcessFactory {

    /**
     * Create the process shell.
     * @param display The display.
     */
    public static void init(Display display) {
        ProcessImpl processImpl = new ProcessImpl(display);
        processImpl.start();
    }

    private static Executor executor = SwtshellFactory.getExecutor();
    
    private static class ProcessImpl implements DisposeListener,ChannelRequester,SelectionListener,Runnable  {

        private ProcessImpl(Display display) {
            this.display = display;
        }

        private boolean isDisposed = false;
        private static String windowName = "process";
        private ExecutorNode executorNode = executor.createNode(this);
        private Display display;
        private Shell shell = null;
        private Requester requester = null;
        private Channel channel = null;
        private Button connectButton = null;
        private Button disconnectButton = null;
        private Button processButton = null;
        private Text consoleText = null; 
        private Process process = null;

        private void start() {
            shell = new Shell(display);
            shell.setText(windowName);
            GridLayout gridLayout = new GridLayout();
            gridLayout.numColumns = 1;
            shell.setLayout(gridLayout);
            Composite composite = new Composite(shell,SWT.BORDER);
            RowLayout rowLayout = new RowLayout(SWT.HORIZONTAL);
            composite.setLayout(rowLayout);
            connectButton = new Button(composite,SWT.PUSH);
            connectButton.setText("connect");
            connectButton.addSelectionListener(this);               
            connectButton.setEnabled(true);
            disconnectButton = new Button(composite,SWT.PUSH);
            disconnectButton.setText("disconnect");
            disconnectButton.addSelectionListener(this);               
            disconnectButton.setEnabled(false);
            processButton = new Button(composite,SWT.PUSH);
            processButton.setText("process");
            processButton.addSelectionListener(this);               
            processButton.setEnabled(false);
            Composite consoleComposite = new Composite(shell,SWT.BORDER);
            gridLayout = new GridLayout();
            gridLayout.numColumns = 1;
            consoleComposite.setLayout(gridLayout);
            GridData gridData = new GridData(GridData.FILL_BOTH);
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
            if(isDisposed) {
                if(channel!=null) channel.destroy();
                return;
            }
            Object object = arg0.getSource();
            if(object==connectButton) {
                Channel channel = this.channel;
                if(channel!=null) {
                    channel.destroy();
                    this.channel = null;
                }
                ConnectChannel connectChannel = ConnectChannelFactory.create(shell, this);
                connectChannel.connect();
            } else if(object==disconnectButton) {
                process.disconnect();
                connectButton.setEnabled(true);
                disconnectButton.setEnabled(false);
                processButton.setEnabled(false);
                channel = null;
            } else if(object==processButton) {
                process.process();
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
        public void message(String message, MessageType messageType) {
            requester.message(message, messageType);
        }
        /* (non-Javadoc)
         * @see org.epics.pvData.channelAccess.ChannelRequester#channelCreated(org.epics.pvData.channelAccess.Channel)
         */
        @Override
        public void channelCreated(Channel channel) {
            this.channel = channel;
            message("channel created",MessageType.info);
            display.asyncExec(this);
        }
        /* (non-Javadoc)
         * @see org.epics.pvData.channelAccess.ChannelRequester#channelNotCreated()
         */
        @Override
        public void channelNotCreated() {
            message("channel not created",MessageType.error);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelRequester#channelStateChange(org.epics.ioc.ca.Channel, boolean)
         */
        @Override
        public void channelStateChange(Channel c, boolean isConnected) {
            display.asyncExec(this);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelRequester#disconnect(org.epics.ioc.ca.Channel)
         */
        @Override
        public void destroy(Channel c) {
            display.asyncExec(this);
        }
        
        /* (non-Javadoc)
         * @see java.lang.Runnable#run()
         */
        @Override
        public void run() {
            if(isDisposed) {
                if(channel!=null) channel.destroy();
                return;
            }
            if(channel==null) return;
            boolean isConnected = channel.isConnected();
            if(isConnected) {
                connectButton.setEnabled(false);
                disconnectButton.setEnabled(true);
                process = new Process(channel,this);
                processButton.setEnabled(true);
            } else {
                connectButton.setEnabled(true);
                disconnectButton.setEnabled(false);
                processButton.setEnabled(false);
            }
        }
        private enum RunCommand{process,disconnect};
        
        private static class Process implements Runnable,ChannelProcessRequester
        {   
            private Channel channel;
            private Requester requester;
            private ChannelProcess channelProcess;
            private ExecutorNode executorNode;
            
            private RunCommand runCommand = null;

            private Process(Channel channel,Requester requester) {
                this.channel = channel;
                this.requester = requester;
                executorNode = executor.createNode(this);
                channel.createChannelProcess(channel, this);
            }
            
            private void process() {
                runCommand = RunCommand.process;
                executor.execute(executorNode);
            }
            
            private void disconnect() {
                runCommand = RunCommand.disconnect;
                executor.execute(executorNode);
            }
            /* (non-Javadoc)
             * @see org.epics.pvData.channelAccess.ChannelProcessRequester#channelProcessConnect(org.epics.pvData.channelAccess.ChannelProcess)
             */
            @Override
            public void channelProcessConnect(ChannelProcess channelProcess) {
                this.channelProcess = channelProcess;
            }
            /* (non-Javadoc)
             * @see org.epics.pvData.channelAccess.ChannelProcessRequester#processDone(boolean)
             */
            @Override
            public void processDone(boolean success) {
                message("processDone success " + success,MessageType.info);
            }
            /* (non-Javadoc)
             * @see java.lang.Runnable#run()
             */
            @Override
            public void run() {

                switch(runCommand) {
                case process:
                    if(channelProcess!=null) {
                        channelProcess.process(false);
                    } else {
                        message("not channelProcessor ",MessageType.info);
                    }
                    break;
                case disconnect:
                    if(channelProcess!=null) {
                        channelProcess.destroy();
                    }
                    channel.destroy();
                    break;
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
