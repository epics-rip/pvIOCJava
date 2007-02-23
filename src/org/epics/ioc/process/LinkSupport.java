/**
 * Copyright - See the COPYRIGHT that is included with this disctibution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.process;

import org.epics.ioc.db.DBData;
import org.epics.ioc.pv.*;

/**
 * Interface that must be implemented by support for a channel access link field.
 * @author mrk
 *
 */
public interface LinkSupport extends Support {
    /**
     * Set the field for which the link support should get/put data.
     * This is called by processDB.createSupport(DBData dbData) when it is called for a link field. 
     * @param dbData The field.
     */
    void setField(DBData dbData);
}
