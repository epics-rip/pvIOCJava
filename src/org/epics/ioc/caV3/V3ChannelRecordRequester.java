/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.caV3;

import org.epics.ioc.util.RequestResult;

/**
 * Implemented by BaseV3Channel; Called by BaseV3ChannelRecord.
 * @author mrk
 *
 */
public interface V3ChannelRecordRequester {
     /**
      * V3ChannelRecord has finished createPVRecord.
     * @param requestResult The result.
     */
    void createPVRecordDone(RequestResult requestResult);
}
