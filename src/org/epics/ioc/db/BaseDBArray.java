/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.db;

import org.epics.ioc.pv.*;

/**
 * @author mrk
 *
 */
public class BaseDBArray extends BaseDBField implements DBArray {
    private PVArray pvArray;
    
    /**
     * Constructor.
     * @param parent The parent.
     * @param record The DBRecord to which this field belongs.
     * @param pvArray The reflection interface.
     */
    public BaseDBArray(DBField parent,DBRecord record, PVArray pvArray) {
        super(parent,record,pvArray);
        this.pvArray = pvArray;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.db.DBArray#getPVArray()
     */
    public PVArray getPVArray() {
        return pvArray;
    }
}
