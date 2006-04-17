package org.epics.ioc.dbAccess;

import org.epics.ioc.pvAccess.*;
import org.epics.ioc.dbDefinition.*;

/**
 * Abstract class for implementing Scalar DB fields.
 * Support for non-array DB data can derive from this class.
 * @author mrk
 *
 */
public abstract class AbstractDBData implements DBData{

    /* (non-Javadoc)
     * @see org.epics.ioc.dbAccess.DBData#getDBDField()
     */
    public DBDField getDBDField() {
        return dbdField;
    }

    /**
     * get the DBType
     * @return the DBType
     */
    public DBType getDBType() {
        return dbdField.getDBType();
    }

    /* (non-Javadoc)
     * @see org.epics.ioc.pvAccess.PVData#getField()
     */
    public Field getField() {
        return dbdField;
    }
    
    /**
     * constructor which must be called by classes that derive from this class
     * @param dbdField the DBDField that describes the field
     */
    protected AbstractDBData(DBDField dbdField) {
        this.dbdField = dbdField;
    }
    
    protected DBDField dbdField;

}
