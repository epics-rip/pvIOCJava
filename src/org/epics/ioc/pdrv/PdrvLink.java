/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pdrv;

/**
 * This is a class that holds the data from a pdrvLink structure.
 * @author mrk
 *
 */
public class PdrvLink {
    /**
     * The portName.
     */
    public String portName;
    /**
     * The device address.
     */
    public int addr;
    /**
     * The mask.
     */
    public int mask;
    /**
     * The timeout value.
     */
    public double timeout;
    /**
     * The driver parameters.
     */
    public String drvParams;
}
