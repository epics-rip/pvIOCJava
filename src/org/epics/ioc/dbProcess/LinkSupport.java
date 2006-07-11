/**
 * Copyright - See the COPYRIGHT that is included with this disctibution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbProcess;

import org.epics.ioc.dbAccess.*;
import org.epics.ioc.pvAccess.*;

/**
 * interface that must be implemented by link support.
 * @author mrk
 *
 */
public interface LinkSupport extends Support {
    /**
     * set the field for which the link support should get/put data.
     * @param field the field.
     * @return true if the support can access the field
     * and false if the support does not know how to access the field.
     */
    boolean setField(PVData field);
    /**
     * perform record processing.
     * @param recordProcess the RecordProcess that called RecordSupport.process.
     * @param recordSupport the RecordSupport that call LinkSupport.process.
     * @return the result of processing.
     */
    LinkReturn process(RecordProcess recordProcess,RecordSupport recordSupport);
}
