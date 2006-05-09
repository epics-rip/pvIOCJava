package org.epics.ioc.dbAccess;

import org.epics.ioc.dbDefinition.*;


/**
 * Abstract class for implementing support for Array data.
 * Implementations of array fields should derive from this class.
 * @author mrk
 *
 */
public abstract class AbstractDBArray extends AbstractDBData implements DBArray{


    /* (non-Javadoc)
     * @see org.epics.ioc.pvAccess.PVArray#isCapacityMutable()
     */
    public boolean isCapacityMutable() {
        return super.getDBDField().isMutable();
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

    
    /**
     * constructer that derived classes must call.
     * @param parent the parent interface.
     * @param dbdArrayField the reflection interface for the DBArray data.
     */
    protected AbstractDBArray(DBStructure parent,DBDArrayField dbdArrayField) {
        super(parent,dbdArrayField);
    }
    
}
