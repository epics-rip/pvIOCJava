/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.db;

import org.epics.ioc.pv.*;
import org.epics.ioc.dbd.*;
/**
 * Interface for a IOC database link.
 * @author mrk
 *
 */
public interface DBLink extends DBData,PVLink {
    /**
     * Called by AbstractDBData when support is changed.
     * @param linkSupport The new link support.
     * @param dbd The DBD that defines the link support.
     * @return (null,message) if the request (is,is not) successful.
     */
    String newSupport(DBDLinkSupport linkSupport,DBD dbd);
}
