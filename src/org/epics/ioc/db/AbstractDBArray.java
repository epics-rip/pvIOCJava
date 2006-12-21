/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.db;

import org.epics.ioc.pv.*;


/**
 * Abstract class for implementing support for Array data.
 * Implementations of array fields should derive from this class.
 * @author mrk
 *
 */
public abstract class AbstractDBArray extends AbstractDBData implements PVArray{
    /**
     * constructer that derived classes must call.
     * @param parent the parent interface.
     * @param dbdArrayField the reflection interface for the DBArray data.
     */
    protected AbstractDBArray(DBData parent,Array array) {
        super(parent,array);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVArray#isCapacityMutable()
     */
    public boolean isCapacityMutable() {
        PVArray array = (PVArray)this;
        return array.isCapacityMutable();
    }

    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVArray#getCapacity()
     */
    abstract public int getCapacity();

    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVArray#getLength()
     */
    abstract public int getLength();

    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVArray#setCapacity(int)
     */
    abstract public void setCapacity(int len);

    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVArray#setLength(int)
     */
    abstract public void setLength(int len);
}
