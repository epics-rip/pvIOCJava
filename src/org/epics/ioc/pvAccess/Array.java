/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pvAccess;

/**
 * Base interface for array field reflection
 * @author mrk
 *
 */
public interface Array extends Field{
    /**
     * get the element type for the array
     * @return the array element <i>Type</i>.
     */
    Type getElementType();
}
