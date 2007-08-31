/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.create;

import org.epics.ioc.pv.PVInt;
import org.epics.ioc.pv.PVString;
import org.epics.ioc.pv.PVStringArray;

/**
 * Interface for an enumerated structure.
 * @author mrk
 *
 */
public interface Enumerated extends Create{
    /**
     * Get the index field of an enumerated structure.
     * @return The interface.
     */
    PVInt getIndexField();
    /**
     * Get the choice field of an enumerated structure.
     * @return The interface.
     */
    PVString getChoiceField();
    /**
     * * Get the choices field of an enumerated structure.
     * @return The interface.
     */
    PVStringArray getChoicesField();
}
