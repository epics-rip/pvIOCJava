/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.calc;

import org.epics.ioc.db.DBField;
import org.epics.ioc.pv.PVField;
import org.epics.ioc.pv.Type;
import org.epics.ioc.support.Support;

/**
 * Calculation support.
 * @author mrk
 *
 */
public interface CalculatorSupport extends Support {
    ArgType[] getArgTypes();
    Type getValueType();
    void setArgPVFields(PVField[] pvArgs);
    void setValueDBField(DBField dbValue);
    void compute();
}
