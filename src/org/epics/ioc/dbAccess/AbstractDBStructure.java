/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbAccess;

import org.epics.ioc.dbDefinition.*;
import org.epics.ioc.pvAccess.*;

/**
 * @author mrk
 *
 */
public abstract class AbstractDBStructure extends AbstractDBData
    implements DBStructure
{
    
    private DBDStructure dbdStructure;
    private PVData[] pvData;
    private DBData[] dbData;
    
    /**
     * constructor that derived classes must call.
     * @param parent the DBStructure of the parent.
     * @param dbdStructureField the reflection interface for the DBStructure data.
     */
    protected AbstractDBStructure(DBData parent, DBDStructureField dbdStructureField) {
        super(parent,dbdStructureField);
        dbdStructure = dbdStructureField.getDBDStructure();
        if(dbdStructure==null) {
            pvData = new PVData[0];
            dbData = new DBData[0];
            return;
        }
        DBDField[] dbdFields = dbdStructure.getDBDFields();
        dbData = new DBData[dbdFields.length];
        pvData = new PVData[dbData.length];
        for(int i=0; i < dbData.length; i++) {
            dbData[i] = FieldDataFactory.createData(this,dbdFields[i]);
            pvData[i] = dbData[i];
        }
    }
    
    /**
     * constructor for record instance classes.
     * @param dbdRecordType the reflection interface for the record type.
     */
    protected AbstractDBStructure(DBDRecordType dbdRecordType) {
        super(null,dbdRecordType);
        int numberFields = dbdRecordType.getDBDFields().length;
        dbdStructure = dbdRecordType;
        dbData = new DBData[numberFields];
        pvData = new PVData[numberFields];
    }
    /**
     * create the fields for the record.
     * This is only called by whatever called the record instance constructor.
     * @param record the record instance.
     */
    protected void createFields(DBRecord record) {
        DBDRecordType dbdRecordType = (DBDRecordType)dbdStructure;
        DBDField[] dbdField = dbdRecordType.getDBDFields();
        for(int i=0; i < dbData.length; i++) {
            dbData[i] = FieldDataFactory.createData(record,dbdField[i]);
            pvData[i] = dbData[i];
        }
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.dbAccess.DBStructure#createFields(org.epics.ioc.dbDefinition.DBDStructure)
     */
    public boolean createFields(DBDStructure dbdStructure) {
        if(this.dbdStructure!=null) {
            return false;
        }
        this.dbdStructure = dbdStructure;
        DBDField[] dbdFields = dbdStructure.getDBDFields();
        dbData = new DBData[dbdFields.length];
        pvData = new PVData[dbData.length];
        for(int i=0; i < dbData.length; i++) {
            dbData[i] = FieldDataFactory.createData(this,dbdFields[i]);
            pvData[i] = dbData[i];
        }
        return true;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.dbAccess.DBStructure#getDBDStructure()
     */
    public DBDStructure getDBDStructure() {
        return dbdStructure;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pvAccess.PVStructure#getFieldPVDatas()
     */
    public PVData[] getFieldPVDatas() {
        return pvData;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.dbAccess.DBStructure#getFieldDBDataIndex(java.lang.String)
     */
    public int getFieldDBDataIndex(String fieldName) {
        return dbdStructure.getDBDFieldIndex(fieldName);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.dbAccess.DBStructure#getFieldDBDatas()
     */
    public DBData[] getFieldDBDatas() {
        return dbData;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() { return toString(0);}

    /* (non-Javadoc)
     * @see org.epics.ioc.pvAccess.PVData#toString(int)
     */
    public String toString(int indentLevel) {
        return toString("structure",indentLevel);
    }

    public String toString(String prefix,int indentLevel) {
        return getString(prefix,indentLevel);
    }
    private String getString(String prefix,int indentLevel) {
        StringBuilder builder = new StringBuilder();
        newLine(builder,indentLevel);
        Structure structure = (Structure)dbdStructure;
        builder.append(prefix + " " + structure.getStructureName());
        builder.append(super.toString(indentLevel));
        newLine(builder,indentLevel);
        builder.append("{");
        for(int i=0, n= dbData.length; i < n; i++) {
            newLine(builder,indentLevel + 1);
            Field field = pvData[i].getField();
            builder.append(field.getName() + " = ");
            builder.append(dbData[i].toString(indentLevel + 2));            
        }
        newLine(builder,indentLevel);
        builder.append("}");
        return builder.toString();
    }
}
