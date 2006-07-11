/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbProcess;


/**
 * @author mrk
 *
 */
public interface ChannelLocal extends ChannelIOC {
    void setLinkSupport(LinkSupport linkSupport);
    void setRecordProcess(RecordProcess recordProcess);
    void setRecordSupport(RecordSupport recordSupport);
}
