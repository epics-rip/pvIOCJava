/**
 * Copyright - See the COPYRIGHT that is included with this disctibution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbProcess;

/**
 * interface that must be implemented by record or structure support.
 * @author mrk
 *
 */
public interface RecordSupport extends Support {
    /**
     * perform record or structure processing.
     * @param recordProcess the RecordProcess that called process.
     * @return the result of link processing.
     */
    ProcessReturn process(RecordProcess recordProcess);
    /**
     * called by link support to signify completion.
     * If the link support returns active than the listener must expect additional calls.
     * @param result the reason for calling. A value of active is permissible.
     * In this case link support will again call linkSupportDone.
     */
    void linkSupportDone(LinkReturn result);
}
