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
     * @see org.epics.ioc.dbAccess.DBData#getRecord()
     */
    public DBRecord getRecord() {
        return record;
    }

    /* (non-Javadoc)
     * @see org.epics.ioc.dbAccess.DBData#getDBDField()
     */
    public DBDField getDBDField() {
        return dbdField;
    }

    /* (non-Javadoc)
     * @see org.epics.ioc.pvAccess.PVData#getField()
     */
    public Field getField() {
        return dbdField;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.dbAccess.DBData#getParent()
     */
    public DBStructure getParent() {
        return parent;
    }
    
    /**
     * used by toString to start a new line.
     * @param builder the stringBuilder to which output is added.
     * @param indentLevel indentation level.
     */
    public static void newLine(StringBuilder builder, int indentLevel) {
        builder.append("\n");
        for (int i=0; i <indentLevel; i++) builder.append(indentString);
    }
    private static String indentString = "    ";
    
    /**
     * specify the record that holds this data.
     * This is called by AbstractDBRecord.
     * @param record the record instance containing this field.
     */
    protected void setRecord(DBRecord record) {
        this.record = record;
    }
    
    /**
     * constructor which must be called by classes that derive from this class.
     * @param parent the parent structure.
     * @param dbdField the reflection interface for the DBData data.
     */
    protected AbstractDBData(DBStructure parent, DBDField dbdField) {
        this.dbdField = dbdField;
        this.parent = parent;
        if(parent!=null) {
            record = parent.getRecord();
        } else {
            record = null;
        }
    }
    
    private DBDField dbdField;
    private DBStructure parent;
    private DBRecord record;

}
