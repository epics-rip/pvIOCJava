/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.db;

import org.epics.ioc.pv.PVArray;

/**
 * Implementation of a DBArray.
 * It has package visibility.
 * @author mrk
 *
 */
class ImplDBArray extends ImplDBField implements DBArray {
    private PVArray pvArray;
    
    /**
     * Constructor.
     * @param parent The parent.
     * @param record The ImplDBRecord to which this field belongs.
     * @param pvArray The reflection interface.
     */
    ImplDBArray(ImplDBField parent,ImplDBRecord record, PVArray pvArray) {
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
