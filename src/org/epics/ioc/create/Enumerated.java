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
 * @author mrk
 *
 */
public interface Enumerated extends Create{
    PVInt getIndexField();
    PVString getChoiceField();
    PVStringArray getChoicesField();
}
