/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.channelAccess;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.epics.pvData.channelAccess.ChannelAccess;
import org.epics.pvData.channelAccess.ChannelRequester;

/**
 * @author mrk
 *
 */
public class ChannelAccessFactory {
    private static final Map<String,ChannelProvider> channelProviderMap = new TreeMap<String,ChannelProvider>();
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
            for(int i=0; i<channelProviders.length; i++) {
                channelProviders[i].findChannel(channelName,channelRequester,timeOut);
            }
            
        }
    }
}
