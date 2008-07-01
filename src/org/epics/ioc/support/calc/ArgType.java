/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.calc;

import org.epics.ioc.pv.Type;

/**
 * @author mrk
 *
 */
public class ArgType {
    public String name = null;
    public Type type = null;
    public Type elementType = null;
    public ArgType(String name,Type type,Type elementType) {
        this.name = name;
        this.type = type;
        this.elementType = elementType;
    }
}
