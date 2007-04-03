/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;

import org.epics.ioc.pv.*;
import org.epics.ioc.util.*;

/**
 * A factory for creating CD and CDQueue.
 * @author mrk
 *
 */
public class CDFactory {
     /**
      * Create a CD for the specified channel and ChannelFieldGroup.
     * @param channel The channel.
     * @param channelFieldGroup The field group defining what should be in the channelData.
     * @param supportAlso Should support be read/written?
     * @return The CD interface.
     */
    public static CD createCD(
         Channel channel,ChannelFieldGroup channelFieldGroup,boolean supportAlso)
     {
        return new BaseCD(channel,channelFieldGroup,
            FieldFactory.getFieldCreate(),PVDataFactory.getPVDataCreate(),supportAlso);
     }
    
     /**
      * Create a queue of CD.
     * @param queueSize The queueSize. This is can not be changed after creation.
     * @param channel The channel.
     * @param channelFieldGroup The field group defining what should be in each channelDataField.
     * @param supportAlso Should support be read/written?
     * @return The CDQueue interface.
     */
    public static CDQueue createCDQueue(
             int queueSize,
             Channel channel,ChannelFieldGroup channelFieldGroup,boolean supportAlso)
     {
          if(queueSize<3) {
              channel.message("queueSize changed to 3", MessageType.warning);
              queueSize = 3;
          }
          CD[] queue = new CD[queueSize];
          for(int i = 0; i<queueSize; i++) {
              queue[i] = createCD(channel,channelFieldGroup,supportAlso);
          }
          return new BaseCDQueue(queue);
     }
}