/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;

import org.epics.ioc.pv.*;

/**
 * A factory for creating ChannelData and ChannelDataQueue.
 * @author mrk
 *
 */
public class ChannelDataFactory {
     /**
      * Create a ChannelData for the specified channel and ChannelFieldGroup.
     * @param channel The channel.
     * @param channelFieldGroup The field group defining what should be in the channelData.
     * @return The ChannelData interface.
     */
    public static ChannelData createChannelData(
         Channel channel,ChannelFieldGroup channelFieldGroup)
     {
        return new BaseChannelData(channel,channelFieldGroup,
            FieldFactory.getFieldCreate(),PVDataFactory.getPVDataCreate());
     }
    
     /**
      * Create a queue of ChannelData.
     * @param queueSize The queueSize. This is can not be changed after creation.
     * @param channel The channel.
     * @param channelFieldGroup The field group defining what should be in each channelDataField.
     * @return The ChannelDataQueue interface.
     */
    public static ChannelDataQueue createQueue(
             int queueSize,
             Channel channel,ChannelFieldGroup channelFieldGroup)
     {
          ChannelData[] queue = new ChannelData[queueSize];
          for(int i = 0; i<queueSize; i++) {
              queue[i] = createChannelData(channel,channelFieldGroup);
          }
          return new BaseChannelDataQueue(queue);
     }
}
