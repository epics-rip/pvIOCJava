/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.channelAccess;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.epics.pvData.channelAccess.ChannelAccess;
import org.epics.pvData.channelAccess.ChannelRequester;
import org.epics.pvData.misc.ThreadPriority;
import org.epics.pvData.misc.Timer;
import org.epics.pvData.misc.TimerFactory;

/**
 * @author mrk
 *
 */
public class ChannelAccessFactory {
    private static final Map<String,ChannelProvider> channelProviderMap = new TreeMap<String,ChannelProvider>();
    private static final Timer timer = TimerFactory.create("channelAccessFindChannel", ThreadPriority.lowest);
    private static ChannelAccess channelAccess = new ChannelAccessImpl();
    static {
        ChannelProviderFactory.register();
    }
    
    /**
     * Get the ChannelAccess interface.
     * @return The interface.
     */
    public static ChannelAccess getChannelAccess() {
        return channelAccess;
    }
    
    public static void registerChannelProvider(ChannelProvider channelProvider) {
        synchronized(channelProviderMap) {
            channelProviderMap.put(channelProvider.getProviderName(), channelProvider);
        }
    }
    
    
    private static class ChannelAccessImpl implements ChannelAccess{
        ChannelProvider[] channelProviders = null;
        Set<String> keySet = null;

        /* (non-Javadoc)
         * @see org.epics.pvData.channelAccess.ChannelAccess#createChannel(java.lang.String, org.epics.pvData.channelAccess.ChannelRequester)
         */
        @Override
        public void createChannel(String channelName,
                ChannelRequester channelRequester, double timeOut)
        {
            synchronized(channelProviderMap) {
                if(channelProviders==null || channelProviders.length!=channelProviderMap.size()) {
                    channelProviders = new ChannelProvider[channelProviderMap.size()];
                    keySet = channelProviderMap.keySet();
                    int index = 0;
                    for(String key : keySet) {
                        channelProviders[index++] = channelProviderMap.get(key);
                    }
                }
            }
            ChannelRequesterSurrogate channelRequesterSurrogate = new ChannelRequesterSurrogate(channelProviders);
            channelRequesterSurrogate.find(channelName, channelRequester,timeOut);
        }

        /* (non-Javadoc)
         * @see org.epics.pvData.channelAccess.ChannelAccess#findChannel(java.lang.String)
         */
        @Override
        public boolean findChannel(String channelName) {
            throw new IllegalStateException("not implemented");
        }
        

    }
    
    private enum FindState{noReply,wasFound,wasNotFound};
    
    private static class ChannelRequesterSurrogate implements ChannelFindRequester,Timer.TimerCallback {
        
        ChannelRequesterSurrogate(ChannelProvider[] channelProviders) {
            this.channelProviders = channelProviders;
            channelFinds = new ChannelFind[channelProviders.length];
            findStates = new FindState[channelProviders.length];
            timerNode = TimerFactory.createNode(this);
        }
        
        void find(String channelName,ChannelRequester channelRequester,double timeOut) {
            this.channelRequester = channelRequester;
            timer.scheduleAfterDelay(timerNode, timeOut);
            synchronized(this) {
                for(int i=0; i<channelProviders.length; i++) {
                    ChannelProvider channelProvider  = channelProviders[i];
                    findStates[i] = FindState.noReply;
                    channelFinds[i] = channelProvider.channelFind(channelName, this,channelRequester);
                    atomicInteger.addAndGet(0);
                }
            }
        }
        
        private ChannelProvider[] channelProviders = null;
        
        private ChannelRequester channelRequester;
        private ChannelFind[] channelFinds = null;
        private FindState[] findStates = null;
        private Timer.TimerNode timerNode = null;
        private AtomicBoolean gotFirst = new AtomicBoolean(false);
        // channelFindResult can get called before channelProvider.channelFind returns;
        private AtomicInteger atomicInteger = new AtomicInteger(0);
        
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelFindRequester#channelFindResult(org.epics.ioc.channelAccess.ChannelFind, boolean)
         */
        @Override
        public void channelFindResult(ChannelFind channelFind, boolean wasFound) {
            synchronized(this) {
                int index = atomicInteger.get();
                if(index<channelProviders.length) {
                    channelFinds[index] = channelFind;
                } else {
                    for(int i=0; i<channelProviders.length; i++) {
                        if(channelProviders[i]==channelFind.getChannelProvider()) {
                            index = i;
                            break;
                        }
                    }
                }
                if(wasFound) {
                    findStates[index] = FindState.wasFound;
                    if(gotFirst.compareAndSet(false, true)) {
                        timerNode.cancel();
                        callback();
                    }
                } else {
                    findStates[index] = FindState.wasNotFound;
                }
            }
        }
        /* (non-Javadoc)
         * @see org.epics.pvData.misc.Timer.TimerCallback#callback()
         */
        @Override
        public void callback() {
            synchronized(this) {
                if(gotFirst.compareAndSet(false, true)) {
                    channelRequester.channelNotCreated();
                }
                for(int i=0; i<channelProviders.length; i++) {
                    ChannelFind channelFind = channelFinds[i];
                    if(channelFind!=null &&findStates[i]==FindState.noReply) channelFind.cancelChannelFind();
                }
            }
        }
        /* (non-Javadoc)
         * @see org.epics.pvData.misc.Timer.TimerCallback#timerStopped()
         */
        @Override
        public void timerStopped() {
            callback();
        }
    }
}
