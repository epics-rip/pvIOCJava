/**
 * 
 */
package org.epics.ioc.pvAccess;

/**
 * @author mrk
 *
 */
public interface Array extends Field{
    PVType getElementType();
}
