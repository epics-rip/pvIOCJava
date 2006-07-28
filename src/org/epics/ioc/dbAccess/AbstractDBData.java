/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbAccess;

import org.epics.ioc.pvAccess.*;
import org.epics.ioc.dbDefinition.*;
import org.epics.ioc.dbProcess.*;

import java.util.*;

/**
 * Abstract class for implementing scalar DB fields.
 * Support for non-array DB data can derive from this class.
 * @author mrk
 *
 */
public abstract class AbstractDBData implements DBData{
    
    private DBDField dbdField;
    private DBStructure parent;
    private DBRecord record;
    private LinkedList<RecordListener> listenerList
        = new LinkedList<RecordListener>();
    private static String indentString = "    ";    
    private String supportName = null;
    private Support support = null;
    private DBStructure configDBStructure = null;
    
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
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() { return getString(0);}
    /* (non-Javadoc)
     * @see org.epics.ioc.pvAccess.PVData#toString(int)
     */
    public String toString(int indentLevel) {
        return getString(indentLevel);
    }
    private String getString(int indentLevel) {
        StringBuilder builder = new StringBuilder();
        if(supportName!=null) {
            builder.append(" supportName " + supportName);
        }
        if(configDBStructure!=null) {
            builder.append(configDBStructure.toString(indentLevel));
        }
        return builder.toString();
    }
    /**
     * specify the record that holds this data.
     * This is called by AbstractDBRecord.
     * @param record the record instance containing this field.
     */
    protected void setRecord(DBRecord record) {
        this.record = record;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.dbAccess.DBData#getDBDField()
     */
    public DBDField getDBDField() {
        return dbdField;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.dbAccess.DBData#getParent()
     */
    public DBStructure getParent() {
        return parent;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.dbAccess.DBData#getRecord()
     */
    public DBRecord getRecord() {
        return record;
    }

    /* (non-Javadoc)
     * @see org.epics.ioc.pvAccess.PVData#getField()
     */
    public Field getField() {
        return dbdField;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.dbAccess.DBData#addListener(org.epics.ioc.dbAccess.DBListener)
     */
    public void addListener(RecordListener listener) {
        if(listenerList.isEmpty()) record.addListenerSource(this);
        listenerList.add(listener);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.dbAccess.DBData#removeListener(org.epics.ioc.dbAccess.DBListener)
     */
    public void removeListener(RecordListener listener) {
        listenerList.remove(listener);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.dbAccess.DBData#postPut()
     */
    public final void postPut() {
        postPut(this);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.dbAccess.DBData#postPut()
     */
    public final void postPut(DBData dbData) {
        Iterator<RecordListener> iter = listenerList.iterator();
        while(iter.hasNext()) {
            RecordListener listener = iter.next();
            listener.newData(dbData);
        }
        if(parent==null) return;
        if(parent==this) {
            System.out.printf("postPut parent = this Why???\n");
        } else {
            parent.postPut(dbData);
        }
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pvAccess.PVData#getSupportName()
     */
    public String getSupportName() {
        return supportName;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pvAccess.PVData#setSupportName(java.lang.String)
     */
    public String setSupportName(String name) {
        DBD dbd = getRecord().getDBD();
        if(dbd==null) return "DBD was not set";
        DBDSupport dbdSupport = dbd.getSupport(name);
        if(dbdSupport==null) return "support " + name + " not defined";
        supportName = name;
        String configurationStructureName = dbdSupport.getConfigurationStructureName();
        if(configurationStructureName==null) return null;
        DBDStructure dbdStructure = dbd.getStructure(configurationStructureName);
        if(dbdStructure==null) {
            return "configurationStructure " + configurationStructureName
                + " for support " + name
                + " does not exist";
        }
        DBStructure parent;
        if(getDBDField().getDBType()==DBType.dbStructure) {
            parent = (DBStructure)this;
        } else {
            parent = getParent();
        }
        DBDAttributeValues dbdAttributeValues =
            new StructureDBDAttributeValues(configurationStructureName,"configurationStructure");
        DBDAttribute dbdAttribute = DBDAttributeFactory.create(
            dbd,dbdAttributeValues);
        DBDField dbdField = DBDCreateFactory.createField(
                dbdAttribute,null);
        configDBStructure  = (DBStructure)FieldDataFactory.createData(parent,dbdField);
        return null;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.dbAccess.DBData#getSupport()
     */
    public Support getSupport() {
        return support;
    }    
    /* (non-Javadoc)
     * @see org.epics.ioc.dbAccess.DBData#setSupport(org.epics.ioc.dbProcess.Support)
     */
    public void setSupport(Support support) {
        this.support = support;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pvAccess.PVData#getConfigurationStructure()
     */
    public DBStructure getConfigurationStructure() {
        return configDBStructure;
    }

    /**
     * used by toString to start a new line.
     * @param builder the stringBuilder to which output is added.
     * @param indentLevel indentation level.
     */
    protected static void newLine(StringBuilder builder, int indentLevel) {
        builder.append("\n");
        for (int i=0; i <indentLevel; i++) builder.append(indentString);
    }
    protected String baseString(int indentLevel) {
        if(configDBStructure!=null) {
            return(configDBStructure.toString(indentLevel));
        }
        return "";
    }
    /**
     * Called by AbstractDBRecord when DBRecord.removeListener or DBrecord.removeListeners are called.
     */
    protected void removeListeners(){
        listenerList.clear();
    }
    private static class StructureDBDAttributeValues
    implements DBDAttributeValues
    {
        private String structureName;
        private String fieldName;

        public StructureDBDAttributeValues(String structureName,
            String fieldName)
        {
            this.structureName = structureName;
            this.fieldName = fieldName;
        }

        public int getLength() {
            return 3;
        }

        public String getName(int index) {
            if(index==0) return "name";
            if(index==1) return "type";
            if(index==2) return "structureName";
            return null;
        }

        public String getValue(int index) {
            if(index==0) return fieldName;
            if(index==1) return "structure";
            if(index==2) return structureName;
            return null;
        }

        public String getValue(String name) {
            if(name.equals("name")) return fieldName;
            if(name.equals("type")) return "structure";
            if(name.equals("structureName")) return structureName;
            return null;
        }
    }
}
