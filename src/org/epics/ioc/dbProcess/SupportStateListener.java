/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbProcess;

/**
 * A callback for change of support state.
 * @author mrk
 *
 */
public interface SupportStateListener {
    /**
     * The SupportState has changed.
     * @param The support that is calling newState.
     * @param state The new state.
     */
    void newState(Support support,SupportState state);
}
