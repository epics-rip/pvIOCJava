/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS pvData is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.support.basic;

import org.epics.pvioc.database.PVRecordStructure;
import org.epics.pvioc.support.Support;


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
     * @param pvRecordStructure The structure.
     * @return An interface to the support.
     */
    public static Support create(PVRecordStructure pvRecordStructure) {
        return new GenericBase(supportName,pvRecordStructure);
    }
    
    private static final String supportName = "org.epics.pvioc.generic";
}
