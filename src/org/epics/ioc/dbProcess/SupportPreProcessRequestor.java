/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbProcess;

/**
 * Interface implemented by code that calls Support.preProcess.
 * @author mrk
 *
 */
public interface SupportPreProcessRequestor extends SupportProcessRequestor{
    /**
     * The record is active and ready for processNow.
     * This is called if a preProcess request has been issued.
     * @return The return value for record processing.
     */
    RequestResult ready();
}
