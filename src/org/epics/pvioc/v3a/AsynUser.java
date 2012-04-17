/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */

package org.epics.pvioc.v3a;

/**
 * @author mrk
 *
 */
public class AsynUser {
    public String message = null;
    public double timeout = 0.0;
    public Object userPvt = null;
    public Object userData = null;
    public Object drvUser = null;
    public int reason = 0;
    public int auxStatus = 0;
    public int intValue = 0;
    public double doubleValue = 0.0;
    public String stringValue = null;
}
