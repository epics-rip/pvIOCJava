/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.basic;

import org.epics.ioc.support.Support;
import org.epics.pvData.pv.PVStructure;


/**
 * Support for a structure.
 * If any subfield is a structure with a boolean field named wait then the support for the next field will not
 * be called until the all supports called so far are complete.
 * @author mrk
 *
 */
public class GenericFactory {
    /**
     * Create support for a structure that can have fields with support.
     * The support for each field that has support is called.
     * @param pvStructure The structure.
     * @return An interface to the support.
     */
    public static Support create(PVStructure pvStructure) {
        return new GenericBase(supportName,pvStructure);
    }
    
    private static String supportName = "generic";
}
