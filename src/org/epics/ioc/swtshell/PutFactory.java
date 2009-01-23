/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.swtshell;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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
import org.epics.ioc.ca.CD;
import org.epics.ioc.ca.CDFactory;
import org.epics.ioc.ca.CDPut;
import org.epics.ioc.ca.CDPutRequester;
import org.epics.ioc.ca.CDRecord;
import org.epics.ioc.ca.Channel;
import org.epics.ioc.ca.ChannelField;
import org.epics.ioc.ca.ChannelFieldGroup;
import org.epics.ioc.ca.ChannelFieldGroupListener;
import org.epics.ioc.ca.ChannelListener;
import org.epics.ioc.util.RequestResult;
import org.epics.pvData.misc.Executor;
import org.epics.pvData.misc.ExecutorFactory;
import org.epics.pvData.misc.ThreadPriority;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.Requester;

/*
 * A shell for channelGet.
 * @author mrk
 *
 */
public class PutFactory {

    /**
     * Create the shell.
     * @param display The display.
     */
    public static void init(Display display) {
        ChannelPutImpl channelPutImpl = new ChannelPutImpl(display);
        channelPutImpl.start();
    }

    private static class ChannelPutImpl implements DisposeListener,Requester,ChannelListener,SelectionListener  {

        private ChannelPutImpl(Display display) {
            this.display = display;
        }

        private static Executor executor
            = ExecutorFactory.create("swtshell:put",ThreadPriority.low);
        private static String windowName = "put";
        private Display display;
        private Shell shell = null;
        private Requester requester = null;
        private Channel channel = null;
        private ChannelConnect channelConnect = null;
        private ChannelField channelField = null;
        private Button putButton;
        private Button processButton;
        private Text consoleText = null;
        
        /* (non-Javadoc)
         * @see org.eclipse.swt.events.DisposeListener#widgetDisposed(org.eclipse.swt.events.DisposeEvent)
         */
        public void widgetDisposed(DisposeEvent e) {
            if(channel!=null) channel.destroy();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requester#getRequesterName()
         */
        public String getRequesterName() {
            return requester.getRequesterName();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requester#message(java.lang.String, org.epics.ioc.util.MessageType)
         */
        public void message(String message, MessageType messageType) {
            requester.message(message, messageType);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelListener#channelStateChange(org.epics.ioc.ca.Channel, boolean)
         */
        public void channelStateChange(Channel c, boolean isConnected) {
            if(isConnected) {
                channel = channelConnect.getChannel();
                String fieldName = channel.getFieldName();
                channelField = channel.createChannelField(fieldName);
                putButton.setEnabled(true);
                processButton.setEnabled(true);
                return;
            } else {
                channel = null;
                channelField = null;
                putButton.setEnabled(false);
                processButton.setEnabled(false);
                return;
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelListener#disconnect(org.epics.ioc.ca.Channel)
         */
        public void destroy(Channel c) {
            channelStateChange(c,false);
        }
        
        private void start() {
            shell = new Shell(display);
            shell.setText(windowName);
            GridLayout gridLayout = new GridLayout();
            gridLayout.numColumns = 1;
            shell.setLayout(gridLayout);
            channelConnect = ChannelConnectFactory.create(this,this);
            channelConnect.createWidgets(shell,true,false);
            Composite putComposite = new Composite(shell,SWT.BORDER);
            gridLayout = new GridLayout();
            gridLayout.numColumns = 2;
            putComposite.setLayout(gridLayout);
            putButton = new Button(putComposite,SWT.NONE);
            putButton.setText("put");
            putButton.addSelectionListener(this);
            processButton = new Button(putComposite,SWT.CHECK);
            processButton.setText("process");
            processButton.setSelection(false);
            putButton.setEnabled(false);
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
         * @see org.eclipse.swt.events.SelectionListener#widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent)
         */
        public void widgetDefaultSelected(SelectionEvent arg0) {
            widgetSelected(arg0);
        }
        /* (non-Javadoc)
         * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
         */
        public void widgetSelected(SelectionEvent arg0) {
            Object object = arg0.getSource(); 
            if(object==putButton) {
                boolean process = processButton.getSelection();
                PutIt putIt = new PutIt(channel,this,process,shell);
                putIt.connect(channelField);
            }
        }
        private class PutIt implements
        Runnable,
        CDPutRequester,
        ChannelFieldGroupListener
        {
            private Shell shell;
            private Lock lock = new ReentrantLock();
            private Condition waitDone = lock.newCondition();
            private Channel channel;
            final private Requester requester;
            private boolean process;
            private boolean gotData = false;
            private boolean getFailed = false;
            private boolean isActive = false;

            private CD cd;
            private CDPut cdPut;

            private PutIt(Channel channel,Requester requester,boolean process,Shell shell) {
                this.shell = shell;
                this.channel = channel;
                this.requester = requester;
                this.process = process;
            }

            private void connect(ChannelField channelField) {
                ChannelFieldGroup putFieldGroup = channel.createFieldGroup(this);
                putFieldGroup.addChannelField(channelField);
                cd = CDFactory.createCD(channel, putFieldGroup);
                cdPut = cd.createCDPut(this, process);
                if(cdPut==null) {
                    cd.destroy();
                    return;
                }
                gotData = false;
                getFailed = false;
                cdPut.get(cd);
                lock.lock();
                try {
                    while(!gotData) {                       
                        waitDone.await();
                    }
                } catch (InterruptedException ie) {
                    cd.destroy();
                    return;
                } finally {
                    lock.unlock();
                }
                if(getFailed) {
                    cd.destroy();
                    return;
                }
                CDRecord cdRecord = cd.getCDRecord();
                cdRecord.getCDStructure().clearNumPuts();
                CDGet cdGet = CDGetFactory.create(shell);
                cdGet.getValue(cdRecord);
                executor.execute(this);
            }
            /* (non-Javadoc)
             * @see java.lang.Runnable#run()
             */
            public void run() {
                isActive = true;
                cdPut.put(cd);
                lock.lock();
                try {
                    while(isActive) {                       
                        waitDone.await();
                    }
                } catch (InterruptedException ie) {
                    cd.destroy();
                } finally {
                    lock.unlock();
                }
                cd.destroy(cdPut);
                requester.message("putComplete", MessageType.info);
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.util.Requester#putRequesterName()
             */
            public String getRequesterName() {
                return requester.getRequesterName();
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.util.Requester#message(java.lang.String, org.epics.ioc.util.MessageType)
             */
            public void message(final String message, final MessageType messageType) {
                requester.message(message, MessageType.info);
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.CDPutRequester#getDone(org.epics.ioc.util.RequestResult)
             */
            public void getDone(RequestResult requestResult) {
                if(requestResult!=RequestResult.success) {
                    message("get failed", MessageType.error);
                    getFailed = true;
                }
                lock.lock();
                try {
                    gotData = true;
                    waitDone.signal();
                } finally {
                    lock.unlock();
                }
            }

            /* (non-Javadoc)
             * @see org.epics.ioc.ca.CDPutRequester#putDone(org.epics.ioc.util.RequestResult)
             */
            public void putDone(RequestResult requestResult) {
                if(requestResult!=RequestResult.success) {
                    message("get failed", MessageType.error);
                }
                lock.lock();
                try {
                    isActive = false;
                    waitDone.signal();
                } finally {
                    lock.unlock();
                }
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelFieldGroupListener#accessRightsChange(org.epics.ioc.ca.Channel, org.epics.ioc.ca.ChannelField)
             */
            public void accessRightsChange(Channel channel, ChannelField channelField) {
                // TODO Auto-generated method stub

            }
        }
    }
}
