/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.util;

/**
 * Adds method isReady to Runnable
 * @author mrk
 *
 */
public interface ReadyRunnable extends Runnable {
    /**
     * Is the run method ready.
     * @return (false,true) if it (is not,is) ready.
     */
    boolean isReady();
}
