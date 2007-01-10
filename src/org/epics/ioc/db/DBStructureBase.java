/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.db;

import java.util.Iterator;

import org.epics.ioc.dbd.*;
import org.epics.ioc.pv.*;

/**
 * @author mrk
 *
 */
public class DBStructureBase extends AbstractDBData implements PVStructure
{
    private static DBDataCreate dbDataCreate = DBDataFactory.getDBDataCreate();
    private static FieldCreate fieldCreate = FieldFactory.getFieldCreate();
    private DBDRecordType dbdRecordType  = null;
    private DBData[] dbData;
    
    /**
     * Constructor.
     * @param parent the DBStructure of the parent.
     * @param structure the reflection interface for the DBStructure data.
     */
    public DBStructureBase(DBData parent, Structure structure) {
        super(parent,structure);
        String structureName = structure.getStructureName();
        if(structureName==null) {
            dbData = new DBData[0];
            return;
        }
        Field[] fields = structure.getFields();
        dbData = new DBData[fields.length];
        for(int i=0; i < dbData.length; i++) {
            dbData[i] = dbDataCreate.createData(this,fields[i]);
        }
    }
    
    /**
     * Constructor for record instance classes.
     * @param dbdRecordType The reflection interface for the record type.
     */
    protected DBStructureBase(DBDRecordType dbdRecordType) {
        super(null,dbdRecordType);
        this.dbdRecordType = dbdRecordType;
        int numberFields = dbdRecordType.getFields().length;
        dbData = new DBData[numberFields];
    }
    /**
     * Create the fields for the record.
     * This is only called by whatever called the record instance constructor.
     * @param record the record instance.
     */
    protected void createFields(DBRecord record) {
        Field[] field = dbdRecordType.getFields();
        for(int i=0; i < dbData.length; i++) {
            dbData[i] = dbDataCreate.createData(record,field[i]);
        }
    }    
    
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVStructure#replaceField(java.lang.String, org.epics.ioc.pv.PVData)
     */
    public boolean replaceStructureField(String fieldName, String structureName) {
        Structure oldStructure = (Structure)super.getField();
        int index = oldStructure.getFieldIndex(fieldName);
        Field oldField = oldStructure.getFields()[index];
        DBD dbd = DBDFactory.getMasterDBD();
        DBDStructure dbdStructure = dbd.getStructure(structureName);
        if(dbdStructure==null) return false;
        Structure fieldStructure = fieldCreate.createStructure(
              fieldName,
              dbdStructure.getStructureName(),
              dbdStructure.getFields(),
              oldField.getPropertys(),
              oldField.getFieldAttribute());
        DBData newData = dbDataCreate.createData(this, fieldStructure);
        dbData[index] = newData;
        String supportName = dbdStructure.getSupportName();
        if(supportName!=null) newData.setSupportName(supportName);
        // Must create and replace the Structure for this structure.
        Field[] oldFields = oldStructure.getFields();
        int length = oldFields.length;
        Field[] newFields = new Field[length];
        for(int i=0; i<length; i++) newFields[i] = oldFields[i];
        newFields[index] = fieldStructure;
        Structure newStructure = fieldCreate.createStructure(
            oldStructure.getFieldName(),
            oldStructure.getStructureName(),
            newFields,
            oldStructure.getPropertys(),
            oldStructure.getFieldAttribute());
        supportName = oldStructure.getSupportName();
        super.replaceField(newStructure);
        super.setSupportName(supportName);
        return true;
    }

    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVStructure#getFieldPVDatas()
     */
    public PVData[] getFieldPVDatas() {
        return dbData;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVStructure#endPut()
     */
    public void endPut() {
        AbstractDBData dbData = this;
        while(dbData!=null) {
            Iterator<RecordListener> iter = dbData.listenerList.iterator();
            while(iter.hasNext()) {
                RecordListener listener = iter.next();
                listener.getDBListener().endPut((PVStructure)dbData);
            }
            DBData parent = (DBData)dbData.getParent();
            if(parent==dbData) {
                System.err.printf("endPut parent = this Why???%n");
            } else {
                dbData = (AbstractDBData)parent;
            }
        }
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVStructure#beginPut()
     */
    public void beginPut() {
        AbstractDBData dbData = this;
        while(dbData!=null) {
            Iterator<RecordListener> iter = dbData.listenerList.iterator();
            while(iter.hasNext()) {
                RecordListener listener = iter.next();
                listener.getDBListener().beginPut((PVStructure)dbData);
            }
            DBData parent = (DBData)dbData.getParent();
            if(parent==dbData) {
                System.err.printf("endPut parent = this Why???%n");
            } else {
                dbData = (AbstractDBData)parent;
            }
        }
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() { return toString(0);}
    /* (non-Javadoc)
     * @see org.epics.ioc.db.AbstractDBData#toString(int)
     */
    public String toString(int indentLevel) {
        return toString("structure",indentLevel);
    }    
    /**
     * Convert to string with a prefix.
     * @param prefix The prefix.
     * @param indentLevel The indentation level.
     * @return The string showing the structure.
     */
    protected String toString(String prefix,int indentLevel) {
        return getString(prefix,indentLevel);
    }
    private String getString(String prefix,int indentLevel) {
        StringBuilder builder = new StringBuilder();
        newLine(builder,indentLevel);
        Structure structure = (Structure)super.getField();
        builder.append(prefix + " " + structure.getStructureName());
        builder.append(super.toString(indentLevel));
        newLine(builder,indentLevel);
        builder.append("{");
        for(int i=0, n= dbData.length; i < n; i++) {
            newLine(builder,indentLevel + 1);
            Field field = dbData[i].getField();
            builder.append(field.getFieldName() + " = ");
            builder.append(dbData[i].toString(indentLevel + 2));            
        }
        newLine(builder,indentLevel);
        builder.append("}");
        return builder.toString();
    }
}
