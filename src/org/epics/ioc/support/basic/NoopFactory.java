/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.basic;

import org.epics.ioc.support.AbstractSupport;
import org.epics.ioc.support.Support;
import org.epics.pvData.pv.PVField;
import org.epics.pvData.pv.PVStructure;

/**
 * This is a support that does nothing except act like it connects, etc.
 * @author mrk
 *
 */
public class NoopFactory {
    /**
     * Create noop support for a DBStructure.
     * @param dbStructure The structure to support.
     * @return The Support interface.
     */
    public static Support create(PVStructure dbStructure) {
        return new Noop(dbStructure);
    }    
    /**
     * Create noop Support for a DBField.
     * @param dbField The field to support.
     * @return The Support interface.
     */
    public static Support create(PVField dbField) {
        return new Noop(dbField);
    }    
    
    private static class Noop extends AbstractSupport {
        private static String supportName = "noop";
        
        private Noop(PVField dbField) {
            super(supportName,dbField);
        }
        // The AbstractSupport methods provide semantics
    }
}
