/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbAccess;

import org.epics.ioc.dbDefinition.*;
import org.epics.ioc.pvAccess.*;


/**
 * Abstract class for implementing support for Array data.
 * Implementations of array fields should derive from this class.
 * @author mrk
 *
 */
public abstract class AbstractDBArray extends AbstractDBData implements DBArray{
    /**
     * constructer that derived classes must call.
     * @param parent the parent interface.
     * @param dbdArrayField the reflection interface for the DBArray data.
     */
    protected AbstractDBArray(DBData parent,DBDArrayField dbdArrayField) {
        super(parent,dbdArrayField);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pvAccess.PVArray#isCapacityMutable()
     */
    public boolean isCapacityMutable() {
        PVArray array = (PVArray)this;
        return array.isCapacityMutable();
    }

    /* (non-Javadoc)
     * @see org.epics.ioc.pvAccess.PVArray#getCapacity()
     */
    abstract public int getCapacity();

    /* (non-Javadoc)
     * @see org.epics.ioc.pvAccess.PVArray#getLength()
     */
    abstract public int getLength();

    /* (non-Javadoc)
     * @see org.epics.ioc.pvAccess.PVArray#setCapacity(int)
     */
    abstract public void setCapacity(int len);

    /* (non-Javadoc)
     * @see org.epics.ioc.pvAccess.PVArray#setLength(int)
     */
    abstract public void setLength(int len);
    /* (non-Javadoc)
     * @see org.epics.ioc.dbAccess.DBArray#getElementDBType()
     */
    public DBType getElementDBType() {
        return getDBDField().getAttribute().getElementDBType();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pvAccess.PVArray#getElementType()
     */
    public Type getElementType() {
        return getDBDField().getAttribute().getElementType();
    }
}
