/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support;

import org.epics.ioc.db.*;

/**
 * @author mrk
 *
 */
public interface DeviceLimit {
    void get(DBField low, DBField high);
}
