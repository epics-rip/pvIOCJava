/**
 * Copyright - See the COPYRIGHT that is included with this disctibution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support;

/**
 * @author mrk
 *
 */
public interface CALinkListener {
    String connect();
    void disconnect();
}
