/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.db;

import org.epics.ioc.pv.*;

/**
 * Abstract base class for any DBArray field.
 * Any code that implements a PVArray field for an IOC database should extend this class.
 * @author mrk
 *
 */
public abstract class AbstractDBArray extends AbstractDBData implements PVArray{
    protected int length = 0;
    protected int capacity;
    protected boolean capacityMutable = true;
    /**
     * Constructer that derived classes must call.
     * @param parent The parent interface.
     * @param array The reflection interface for the DBArray data.
     * @param capacity The default capacity.
     * @param capacityMutable Is the capacity mutable.
     */
    public AbstractDBArray(DBData parent,Array array,int capacity,boolean capacityMutable) {
        super(parent,array);
        this.capacity = capacity;
        this.capacityMutable = capacityMutable;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVArray#isCapacityMutable()
     */
    public boolean isCapacityMutable() {
        return capacityMutable;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVArray#getCapacity()
     */
    public int getCapacity() {
        return capacity;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVArray#getLength()
     */
    public int getLength() {
        return length;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVArray#setCapacity(int)
     */
    abstract public void setCapacity(int capacity);
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVArray#setLength(int)
     */
    public void setLength(int len) {
        if(!super.getField().isMutable())
            throw new IllegalStateException("PVData.isMutable is false");
        if(len>capacity) setCapacity(len);
        length = len;
    }
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return toString(0);
    }
}
