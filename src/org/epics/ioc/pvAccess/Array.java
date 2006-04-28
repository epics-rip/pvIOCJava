/**
 *  
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
