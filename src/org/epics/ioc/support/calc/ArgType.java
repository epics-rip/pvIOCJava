/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.calc;

import org.epics.ioc.pv.Type;

/**
 * The name and type for an argument for a calculation.
 * @author mrk
 *
 */
public class ArgType {
    /**
     * The name of the argument.
     */
    public String name = null;
    /**
     * The type of the argument.
     */
    public Type type = null;
    /**
     * The element type for an array.
     */
    public Type elementType = null;
    /**
     * Constructor.
     * @param name The argument name.
     * @param type The argument type.
     * @param elementType The lement type for an array.
     */
    public ArgType(String name,Type type,Type elementType) {
        this.name = name;
        this.type = type;
        this.elementType = elementType;
    }
}
