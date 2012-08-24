/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS pvData is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.install;

/**
 * @author mrk
 *
 */
public interface AfterStartRequester {
    void callback(AfterStartNode node);
}
