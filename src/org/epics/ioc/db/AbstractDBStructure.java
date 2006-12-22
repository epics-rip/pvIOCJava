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
public abstract class AbstractDBStructure extends AbstractDBData
    implements PVStructure
{
    
    private DBDRecordType dbdRecordType  = null;
    private DBData[] dbData;
    
    /**
     * constructor that derived classes must call.
     * @param parent the DBStructure of the parent.
     * @param dbdStructureField the reflection interface for the DBStructure data.
     */
    protected AbstractDBStructure(DBData parent, Structure structure) {
        super(parent,structure);
        String structureName = structure.getStructureName();
        if(structureName==null) {
            dbData = new DBData[0];
            return;
        }
        DBD dbd = DBDFactory.getMasterDBD();
        DBDStructure dbdStructure = dbd.getStructure(structureName);
        Field[] fields = dbdStructure.getFields();
        dbData = new DBData[fields.length];
        for(int i=0; i < dbData.length; i++) {
            dbData[i] = FieldDataFactory.createData(this,fields[i]);
        }
    }
    
    /**
     * constructor for record instance classes.
     * @param dbdRecordType the reflection interface for the record type.
     */
    protected AbstractDBStructure(DBDRecordType dbdRecordType) {
        super(null,dbdRecordType);
        this.dbdRecordType = dbdRecordType;
        int numberFields = dbdRecordType.getFields().length;
        dbData = new DBData[numberFields];
    }
    /**
     * create the fields for the record.
     * This is only called by whatever called the record instance constructor.
     * @param record the record instance.
     */
    public void createFields(DBRecord record) {
        Field[] field = dbdRecordType.getFields();
        for(int i=0; i < dbData.length; i++) {
            dbData[i] = FieldDataFactory.createData(record,field[i]);
        }
    }    
    
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVStructure#replaceField(java.lang.String, org.epics.ioc.pv.PVData)
     */
    public boolean replaceField(String fieldName, PVData pvData) {
        Structure oldStructure = (Structure)super.getField();
        int index = oldStructure.getFieldIndex(fieldName);
        Field oldField = oldStructure.getFields()[index];
        Field newField = pvData.getField();
        if(!oldField.getFieldName().equals(newField.getFieldName())) return false;
        Structure newStructure = oldStructure.copy();
        if(!newStructure.replaceField(newField.getFieldName(),newField)) return false;
        super.replaceField(newField);
        dbData[index] = (DBData)pvData;
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
