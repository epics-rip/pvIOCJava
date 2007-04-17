/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.recordSupport;
import org.epics.ioc.support.*;
import org.epics.ioc.db.*;

/**
 * Interface for incremental type records.
 * @author mrk
 *
 */
public interface IncrementalSupport extends Support{
    /**
     * Set the desiredName field.
     * @param dbField
     */
    void setDesiredField(DBField dbField);
}
