/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pv;

/**
 * @author mrk
 *
 */
public class LinkArrayData {
    /**
     * The PVLink[].
     * PVLinkArray.get sets this value.
     * PVLinkArray.put requires that the caller set the value. 
     */
    public PVLink[] data;
    /**
     * The offset.
     * PVLinkArray.get sets this value.
     * PVLinkArray.put requires that the caller set the value. 
     */
    public int offset;
}
