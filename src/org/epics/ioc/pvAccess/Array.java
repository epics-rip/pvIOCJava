/**
 
 */
package org.epics.ioc.pvAccess;

/**
 * Base interface for introspection of an array field
 * @author mrk
 *
 */
public interface Array extends Field{
    /**
     * get the element type for the array
     * @return <i>PVType</i> of the array elements.
     */
    Type getElementType();
}
