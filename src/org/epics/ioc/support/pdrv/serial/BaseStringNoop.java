/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.pdrv.serial;

import org.epics.ioc.database.PVRecordStructure;
import org.epics.ioc.support.pdrv.AbstractPortDriverSupport;

/**
 * Implement StringNoop.
 * AbstractPortDriverSupport does everything.
 * @author mrk
 *
 */
public class BaseStringNoop extends AbstractPortDriverSupport
{
    /**
     * Constructor.
     * @param pvRecordStructure The structure being supported.
     * @param supportName The name of the support.
     */
    public BaseStringNoop(PVRecordStructure pvRecordStructure,String supportName) {
        super(supportName,pvRecordStructure);
    }
}
