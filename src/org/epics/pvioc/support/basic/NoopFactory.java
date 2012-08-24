/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS pvData is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.support.basic;

import org.epics.pvioc.database.PVRecordField;
import org.epics.pvioc.database.PVRecordStructure;
import org.epics.pvioc.support.AbstractSupport;
import org.epics.pvioc.support.Support;

/**
 * This is a support that does nothing except act like it connects, etc.
 * @author mrk
 *
 */
public class NoopFactory {
    /**
     * Create noop support for a DBRecordStructure.
     * @param dbStructure The structure to support.
     * @return The Support interface.
     */
    public static Support create(PVRecordStructure dbStructure) {
        return new Noop(dbStructure);
    }    
    /**
     * Create noop Support for a DBField.
     * @param dbField The field to support.
     * @return The Support interface.
     */
    public static Support create(PVRecordField dbField) {
        return new Noop(dbField);
    }    
    
    private static class Noop extends AbstractSupport {
        private static final String supportName = "org.epics.pvioc.noop";
        
        private Noop(PVRecordField dbField) {
            super(supportName,dbField);
        }
        // The AbstractSupport methods provide semantics
    }
}
