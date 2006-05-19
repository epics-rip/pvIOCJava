/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */

package org.epics.ioc.pvAccess;

/**
 * Interface for enum field reflection.
 * An enum is a field that has an array of
 * choices and an index specifying the current choice.
 * 
 * @author mrk
 * 
 */

public interface Enum extends Field {
    /**
     * Can the choices be modified?
     * @return (true,false) if choices (can, can't) be modified
    */
    boolean isChoicesMutable();
}
