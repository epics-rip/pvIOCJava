/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support;

/**
 * Calculation support.
 * @author mrk
 *
 */
public interface CalculatorSupport extends Support {
    /**
     * Specify the support for the calcArgArray which contains the definitions
     * of the arguments for the calculation.
     * @param calcArgArraySupport The calcArgArraySupport.
     */
    void setCalcArgArraySupport(CalcArgArraySupport calcArgArraySupport);
}
