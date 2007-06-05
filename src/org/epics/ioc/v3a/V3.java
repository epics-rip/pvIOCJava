/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.v3a;

/**
 * @author mrk
 *
 */
public class V3 {
    public static native void iocsh(String cmdFile);
    
    static {
        System.loadLibrary("V3");
    }
}
