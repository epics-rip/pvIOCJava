/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.swtshell;

import org.epics.ioc.ca.CDRecord;

/**
 * Get values from a user and put them in a CD (ChannelData)
 * @author mrk
 *
 */
public interface CDGet {
    /**
     * get values from a user and put them in cdRecord.
     * @param cdRecord
     */
    void getValue(CDRecord cdRecord);
}
