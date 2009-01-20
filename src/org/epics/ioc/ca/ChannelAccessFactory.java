/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.epics.pvData.pv.MessageType;

/**
 * @author mrk
 *
 */
public class ChannelAccessFactory {
    private static ChannelAccess channelAccess = new ChannelAccessImpl();
    static {
        ChannelProviderLocalFactory.register();
    }
    
    /**
     * Get the ChannelAccess interface.
     * @return The interface.
     */
    public static ChannelAccess getChannelAccess() {
        return channelAccess;
    }
    
    private static class ChannelAccessImpl implements ChannelAccess{
        private Map<String,ChannelProvider> channelProviders = new TreeMap<String,ChannelProvider>();
        private static final String[] noPropertys = new String[0];
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelAccess#createChannel(java.lang.String, java.lang.String[], java.lang.String, org.epics.ioc.ca.ChannelListener)
         */
        public Channel createChannel(String pvName,String[] propertys,String providerName, ChannelListener listener) {
            ChannelProvider channelProvider = getChannelProvider(providerName);
            if(channelProvider==null) {
                listener.message(
                    "providerName " + providerName + " not a registered provider",
                    MessageType.error);
                return null;
            }
            if(pvName==null) {
                listener.message("pvName is null",MessageType.error);
                return null;
            }
            if(propertys==null) propertys = noPropertys;
            return channelProvider.createChannel(pvName,propertys, listener);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelAccess#registerChannelProvider(org.epics.ioc.ca.ChannelProvider)
         */
        public synchronized void registerChannelProvider(ChannelProvider channelProvider) {
            channelProviders.put(channelProvider.getProviderName(), channelProvider);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelAccess#getChannelProvider(java.lang.String)
         */
        public synchronized ChannelProvider getChannelProvider(String providerName) {
            return channelProviders.get(providerName);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelAccess#getChannelProviders()
         */
        public synchronized ChannelProvider[] getChannelProviders() {
            ChannelProvider[] providers = new ChannelProvider[channelProviders.size()];
            Set<String> keys = channelProviders.keySet();
            int index = 0;
            for(String key: keys) {
                providers[index++] = channelProviders.get(key);
            }
            return providers;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelAccess#isChannelProvider(java.lang.String, java.lang.String)
         */
        public synchronized boolean isChannelProvider(String channelName,String providerName) {
            ChannelProvider channelProvider = channelProviders.get(providerName);
            if(channelProvider==null) return false;
            return channelProvider.isProvider(channelName);
            
        }
    }
}
