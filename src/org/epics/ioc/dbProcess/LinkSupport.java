/**
 * Copyright - See the COPYRIGHT that is included with this disctibution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbProcess;

import org.epics.ioc.pvAccess.*;

/**
 * Interface that must be implemented by support for a channel access link field.
 * @author mrk
 *
 */
public interface LinkSupport extends Support {
    /**
     * Set the field for which the link support should get/put data.
     * This is called by processDB.createSupport(DBData dbData) when it is called for a link field. 
     * @param field The field.
     */
    void setField(PVData field);
}
