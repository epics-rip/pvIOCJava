/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support;

import org.epics.ioc.db.*;
import org.epics.ioc.process.*;
import org.epics.ioc.util.RequestResult;

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
    public static Support create(DBStructure dbStructure) {
        return new Noop(dbStructure);
    }    
    /**
     * Create noop Support for a DBField.
     * @param dbField The field to support.
     * @return The Support interface.
     */
    public static Support create(DBField dbField) {
        return new Noop(dbField);
    }    
    /**
     * Create noop LinkSupport for a DBLink.
     * @param dbLink The field to support;
     * @return The LinkSupport interfaace.
     */
    public static LinkSupport create(DBLink dbLink) {
        return new Noop(dbLink);
    }
    
    private static class Noop extends AbstractLinkSupport {
        private static String supportName = "noop";
        
        private Noop(DBField dbField) {
            super(supportName,dbField);
        }
        // The AbstractPdrvLinkSupport methods provide semantics
    }
}
