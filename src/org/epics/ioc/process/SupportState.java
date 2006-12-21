/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.process;


/**
 * State of support.
 * @author mrk
 *
 */
public enum SupportState {
    /**
     * Ready for initialize.
     * During initialization support can do initialization related to the record instance which is supported
     * but can not access other records or other support. 
     */
    readyForInitialize,
    /**
     * Ready for start.
     * The initialize methods sets this state when initialization is successful.
     */
    readyForStart,
    /**
     * The support is ready for processing.
     */
    ready,
    /**
     * The support has been asked to be deleted and will no longer perform any actions.
     */
    zombie;
}
