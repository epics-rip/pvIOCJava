package org.epics.ioc.dbAccess;

import org.epics.ioc.pvAccess.*;
import org.epics.ioc.dbDefinition.*;


/**
 * Abstract class for implementing support for Array data.
 * Most implementation of array fields can derive from this class.
 * @author mrk
 *
 */
public abstract class AbstractDBArray implements DBArray{

    /* (non-Javadoc)
     * @see org.epics.ioc.dbAccess.DBArray#getDBDArrayField()
     */
    public DBDArrayField getDBDArrayField() {
        return dbdArrayField;
    }

    /* (non-Javadoc)
     * @see org.epics.ioc.pvAccess.PVArray#isCapacityMutable()
     */
    public boolean isCapacityMutable() {
        return dbdArrayField.isMutable();
    }

    /* (non-Javadoc)
     * @see org.epics.ioc.dbAccess.DBData#getDBDField()
     */
    public DBDField getDBDField() {
        return dbdArrayField;
    }

    /**
     * get the element Type
     * @return the Type
     */
    public Type getElementType() {
        return dbdArrayField.getElementType();
    }

    /**
     * get the element DBType
     * @return the DBType
     */
    public DBType getElementDBType() {
        dbdArrayField.getElementDBType();
        return null;
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
     * @see org.epics.ioc.pvAccess.PVData#getField()
     */
    public Field getField() {
        return dbdArrayField;
    }

    /**
     * get the DBType
     * @return the DBType
     */
    public DBType getDBType() {
        return dbdArrayField.getDBType();
    }
    
    /**
     * constructer that derived classes must call
     * @param dbdArrayField the DBDArrayField which describes the fields
     */
    protected AbstractDBArray(DBDArrayField dbdArrayField) {
        this.dbdArrayField = dbdArrayField;
    }
    
    /**
     * 
     */
    protected DBDArrayField dbdArrayField;
    
}
