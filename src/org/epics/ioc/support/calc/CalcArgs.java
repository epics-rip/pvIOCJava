/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.calc;

import org.epics.pvData.pv.*;
import org.epics.ioc.support.Support;

/**
 * Interface implemented by CalcArgArrayFactory
 * @author mrk
 *
 */
public interface CalcArgs extends Support{
    /**
     * Get the calcArg value field that has the name argName.
     * @param argName The name of the calcArg.
     * @return The interface for the value field or null if no calcArg has name argName.
     */
    PVField getPVField(String argName);
}
