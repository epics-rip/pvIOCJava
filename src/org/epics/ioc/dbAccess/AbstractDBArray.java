package org.epics.ioc.dbAccess;

import org.epics.ioc.pvAccess.*;
import org.epics.ioc.dbDefinition.*;


/**
 * Abstract class for implementing support for Array data.
 * Most implementation of array fields can derive from this class.
 * @author mrk
 *
 */
public abstract class AbstractDBArray extends AbstractDBData implements DBArray{


    /* (non-Javadoc)
     * @see org.epics.ioc.pvAccess.PVArray#isCapacityMutable()
     */
    public boolean isCapacityMutable() {
        return dbdField.isMutable();
    }


    /**
     * get the element Type
     * @return the Type
     */
    public Type getElementType() {
        return dbdField.getDBDAttribute().getElementType();
    }

    /**
     * get the element DBType
     * @return the DBType
     */
    public DBType getElementDBType() {
        return dbdField.getDBDAttribute().getElementDBType();
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
     * constructer that derived classes must call
     * @param dbdField the DBDField which describes the fields
     */
    protected AbstractDBArray(DBDField dbdField) {
        super(dbdField);
    }
    
}
