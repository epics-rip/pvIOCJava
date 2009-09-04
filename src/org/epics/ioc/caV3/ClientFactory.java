
/**
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.caV3;

import gov.aps.jca.CAException;
import gov.aps.jca.Context;
import gov.aps.jca.JCALibrary;
import gov.aps.jca.event.ContextExceptionEvent;
import gov.aps.jca.event.ContextExceptionListener;
import gov.aps.jca.event.ContextMessageEvent;
import gov.aps.jca.event.ContextMessageListener;
import gov.aps.jca.event.ContextVirtualCircuitExceptionEvent;

import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Pattern;

import org.epics.ca.channelAccess.client.Channel;
import org.epics.ca.channelAccess.client.ChannelFind;
import org.epics.ca.channelAccess.client.ChannelFindRequester;
import org.epics.ca.channelAccess.client.ChannelProvider;
import org.epics.ca.channelAccess.client.ChannelRequester;
import org.epics.ca.channelAccess.client.Query;
import org.epics.ca.channelAccess.client.QueryRequester;
import org.epics.ca.channelAccess.server.impl.ChannelAccessFactory;
import org.epics.ioc.install.AfterStart;
import org.epics.ioc.install.AfterStartFactory;
import org.epics.ioc.install.AfterStartNode;
import org.epics.ioc.install.AfterStartRequester;
import org.epics.ioc.install.NewAfterStartRequester;
import org.epics.pvData.misc.RunnableReady;
import org.epics.pvData.misc.ThreadCreate;
import org.epics.pvData.misc.ThreadCreateFactory;
import org.epics.pvData.misc.ThreadPriority;
import org.epics.pvData.misc.ThreadReady;
import org.epics.pvData.pv.PVField;
import org.epics.pvData.pv.ScalarType;



/**
 * Factory and implementation of Channel Access V3 client. This provides communication
 * between a javaIOC and a V3 EPICS IOC.
 * @author mrk
 *
 */
public class ClientFactory  {
    static ChannelProviderImpl channelProvider = new ChannelProviderImpl();
    private static JCALibrary jca = null;
    private static Context context = null;
    private static ThreadCreate threadCreate = ThreadCreateFactory.getThreadCreate();
    /**
     * JavaIOC. This registers the V3 ChannelProvider.
     */
    public static void start() {
        AfterStartDelay afterStartDelay = new AfterStartDelay();
        afterStartDelay.start();
        channelProvider.register();
    }
    
    // afterStartDelay ensures that no run method gets called until after 2 seconds after
    // the last record has started. This allows time to connect to servers.
    private static class AfterStartDelay extends TimerTask  implements NewAfterStartRequester,AfterStartRequester {
        private static final Timer timer = new Timer("caClientDelay");
        private AfterStartNode afterStartNode = null;
        private AfterStart afterStart = null;
      
        private AfterStartDelay() {}
        
        private void start() {
            afterStartNode = AfterStartFactory.allocNode(this);
            AfterStartFactory.newAfterStartRegister(this);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.install.NewAfterStartRequester#callback(org.epics.ioc.install.AfterStart)
         */
        public void callback(AfterStart afterStart) {
            this.afterStart = afterStart;
            afterStart.requestCallback(afterStartNode, false, ThreadPriority.middle);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.install.AfterStartRequester#callback(org.epics.ioc.install.AfterStartNode)
         */
        public void callback(AfterStartNode node) {
            timer.schedule(this, 2000);
        }
        /* (non-Javadoc)
         * @see java.util.TimerTask#run()
         */
        @Override
        public void run() {
            afterStart.done(afterStartNode);
            afterStart = null;
        }
        
    }
    private static class ChannelProviderImpl
    implements ChannelProvider,ContextExceptionListener, ContextMessageListener
    {
        static private final String providerName = "caV3";
        private boolean isRegistered = false; 
        private CAThread caThread = null;
        
        synchronized private void register() {
            if(isRegistered) return;
            isRegistered = true;
            try {
                jca = JCALibrary.getInstance();
                context = jca.createContext(JCALibrary.CHANNEL_ACCESS_JAVA);
                context.addContextExceptionListener(this);
                context.addContextMessageListener(this);
                caThread = new CAThread("cav3",3);
            } catch (CAException e) {
                System.err.println(e.getMessage());
                return;
            }     
            ChannelAccessFactory.registerChannelProvider(this);
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelProvider#destroy()
         */
        @Override
        public void destroy() {
            caThread.stop();
            try {
                context.destroy();
            } catch (CAException e) {
                System.err.println(e.getMessage());
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ca.channelAccess.client.ChannelProvider#channelFind(java.lang.String, org.epics.ca.channelAccess.client.ChannelFindRequester)
         */
        @Override
        public ChannelFind channelFind(String channelName,ChannelFindRequester channelFindRequester) {
            LocateFind locateFind = new LocateFind(channelName);
            locateFind.find(channelFindRequester);
            return locateFind;
        }
        /* (non-Javadoc)
		 * @see org.epics.ca.channelAccess.client.ChannelProvider#channelFind(org.epics.pvData.pv.PVField, org.epics.ca.channelAccess.client.QueryRequester)
		 */
		@Override
		public Query channelFind(PVField query, QueryRequester queryRequester) {
			return null;
		}
        /* (non-Javadoc)
         * @see org.epics.ca.channelAccess.client.ChannelProvider#createChannel(java.lang.String, org.epics.ca.channelAccess.client.ChannelRequester, short)
         */
        @Override
        public Channel createChannel(String channelName,
                ChannelRequester channelRequester, short priority)
        {
            LocateFind locateFind = new LocateFind(channelName);
            return locateFind.create(channelRequester);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelProvider#getProviderName()
         */
        public String getProviderName() {
            return providerName;
        }
        /* (non-Javadoc)
         * @see gov.aps.jca.event.ContextExceptionListener#contextException(gov.aps.jca.event.ContextExceptionEvent)
         */
        public void contextException(ContextExceptionEvent arg0) {
            String message = arg0.getMessage();
            System.err.println(message);
            System.err.flush();
        }
        /* (non-Javadoc)
         * @see gov.aps.jca.event.ContextExceptionListener#contextVirtualCircuitException(gov.aps.jca.event.ContextVirtualCircuitExceptionEvent)
         */
        public void contextVirtualCircuitException(ContextVirtualCircuitExceptionEvent arg0) {
            String message = "status " + arg0.getStatus().toString();
            System.err.println(message);
            System.err.flush();
        }
        /* (non-Javadoc)
         * @see gov.aps.jca.event.ContextMessageListener#contextMessage(gov.aps.jca.event.ContextMessageEvent)
         */
        public void contextMessage(ContextMessageEvent arg0) {
            String message = arg0.getMessage();
            System.out.println(message);
            System.out.flush();
        }
    }
    
    private static class LocateFind implements ChannelFind,ChannelFindRequester{
        static private final Pattern periodPattern = Pattern.compile("[.]");
        static private final Pattern leftBracePattern = Pattern.compile("[{]");
        static private final Pattern rightBracePattern = Pattern.compile("[}]");
        static private final Pattern commaPattern = Pattern.compile("[,]");
        private ChannelFindRequester channelFindRequester = null;
        private BaseV3Channel v3Channel = null;
        private String channelName = null;
        private String recordName = null;
        private String fieldName = null;
        private String[] propertys = new String[0];
        private ScalarType enumRequestType = null;
        
        
        LocateFind(String channelName) {
            this.channelName = channelName;
        }
        
        void find(ChannelFindRequester channelFindRequester) {
            this.channelFindRequester = channelFindRequester;
            common();
            v3Channel = new BaseV3Channel(
                    this,null,context,channelName,recordName,fieldName,enumRequestType,propertys);
            v3Channel.connectCaV3();
        }
        
        Channel create(ChannelRequester channelRequester) {
            common();
            v3Channel = new BaseV3Channel(
                    null,channelRequester,context,channelName,recordName,fieldName,enumRequestType,propertys);
            v3Channel.connectCaV3();
            return v3Channel;
        }
        
        private void common() {
            String[] names = periodPattern.split(channelName,2);
            recordName = names[0];
            if(names.length==2) {
                names = leftBracePattern.split(names[1], 2);
                fieldName = names[0];
                if(fieldName.length()==0) fieldName = null;
                if(names.length==2) {
                    names = rightBracePattern.split(names[1], 2);
                    propertys = commaPattern.split(names[0]);
                }
            }
            String remoteFieldName = null;
            
            if(fieldName!=null) {
                if(fieldName.equals("value")) {
                    remoteFieldName = "VAL";
                } else if(fieldName.equals("value.index")) {
                    enumRequestType = ScalarType.pvInt;
                    fieldName = "value";
                    remoteFieldName = "VAL";
                } else if(fieldName.equals("value.choice")) {
                    enumRequestType = ScalarType.pvString;
                    fieldName = "value";
                    remoteFieldName = "VAL";
                } else if(fieldName.equals("VAL")) {
                    remoteFieldName = "VAL";
                    fieldName = "value";
                } else {
                    remoteFieldName = fieldName;
                }
            } else {
                fieldName = "value";
                remoteFieldName = "VAL";
            }
            channelName =  recordName + "." + remoteFieldName;
           
        }
        /* (non-Javadoc)
         * @see org.epics.ca.channelAccess.client.ChannelFind#cancelChannelFind()
         */
        @Override
        public void cancelChannelFind() {
            v3Channel.destroy();
        }
        /* (non-Javadoc)
         * @see org.epics.ca.channelAccess.client.ChannelFind#getChannelProvider()
         */
        @Override
        public ChannelProvider getChannelProvider() {
            return channelProvider;
        }

        /* (non-Javadoc)
         * @see org.epics.ca.channelAccess.client.ChannelFindRequester#channelFindResult(org.epics.ca.channelAccess.client.ChannelFind, boolean)
         */
        @Override
        public void channelFindResult(ChannelFind channelFind, boolean wasFound) {
            channelFindRequester.channelFindResult(channelFind, wasFound);
            v3Channel.destroy();
        }
    }
    
    private static class CAThread implements RunnableReady {
        private Thread thread = null;
        private CAThread(String threadName,int threadPriority)
        {
            thread = threadCreate.create(threadName, threadPriority, this);
        }         
        /* (non-Javadoc)
         * @see org.epics.ioc.util.RunnableReady#run(org.epics.ioc.util.ThreadReady)
         */
        public void run(ThreadReady threadReady) {        
System.out.println("CaV3Client");
context.printInfo();
System.out.println();
            threadReady.ready();
            try {
                while(true) {
                    try {
                        context.poll();
                    } catch (CAException e) {
                        System.out.println(e.getMessage());
                        break;
                    }
                    Thread.sleep(5);
                }
            } catch(InterruptedException e) {

            }
        }
        
        private void stop() {
            thread.interrupt();
        }
    }
}
