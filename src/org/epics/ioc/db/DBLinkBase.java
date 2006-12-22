/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.db;

import java.util.Iterator;

import org.epics.ioc.dbd.*;
import org.epics.ioc.pv.*;
import org.epics.ioc.util.MessageType;

/**
 * Base class for PVLink.
 * @author mrk
 *
 */
public class DBLinkBase extends AbstractDBData implements PVLink {
    private static Convert convert = ConvertFactory.getConvert();
    private ConfigStructure configStructure = null;
    
    public DBLinkBase(DBData parent,Field field) {
        super(parent,field);
    }
    
    /* (non-Javadoc)
     * @see org.epics.ioc.db.AbstractDBData#setSupportName(java.lang.String)
     */
    public String setSupportName(String name) {
        DBD dbd = getRecord().getDBD();
        if(dbd==null) return "DBD was not set";
        DBDLinkSupport dbdLinkSupport = dbd.getLinkSupport(name);
        if(dbdLinkSupport==null) return "support " + name + " not defined";
        super.setSupportName(name);
        String configurationStructureName = dbdLinkSupport.getConfigurationStructureName();
        if(configurationStructureName==null) return null;
        DBDStructure dbdStructure = dbd.getStructure(configurationStructureName);
        if(dbdStructure==null) {
            return "configurationStructure " + configurationStructureName
                + " for support " + name
                + " does not exist";
        }
        configStructure = new ConfigStructure(this,dbdStructure);
        configStructure.createFields(dbdStructure);
        Iterator<RecordListener> iter = super.listenerList.iterator();
        while(iter.hasNext()) {
            RecordListener listener = iter.next();
            DBListener dbListener = listener.getDBListener();
            dbListener.configurationStructurePut(this);
        }
        return null;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVLink#getConfigurationStructure()
     */
    public PVStructure getConfigurationStructure() {
        return configStructure;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVLink#setConfigurationStructure(org.epics.ioc.pv.PVStructure)
     */
    public boolean setConfigurationStructure(PVStructure pvStructure) {
        if(configStructure==null) return false;
        if(!convert.isCopyStructureCompatible(
        (Structure)pvStructure.getField(),
        (Structure)configStructure.getField())) {
            return false;
        }
        convert.copyStructure(pvStructure, configStructure);
        Iterator<RecordListener> iter = super.listenerList.iterator();
        while(iter.hasNext()) {
            RecordListener listener = iter.next();
            DBListener dbListener = listener.getDBListener();
            dbListener.configurationStructurePut(this);
        }
        return true;
    }

    /* (non-Javadoc)
     * @see org.epics.ioc.db.AbstractDBData#toString()
     */
    public String toString() {
        return toString(0);
    }

    /* (non-Javadoc)
     * @see org.epics.ioc.db.AbstractDBData#toString(int)
     */
    public String toString(int indentLevel) {
        StringBuilder builder = new StringBuilder();
        builder.append(convert.getString(this, indentLevel));
        builder.append(super.toString(indentLevel));
        if(configStructure!=null) {
            builder.append(configStructure.toString(indentLevel));
        }
        return builder.toString();
    }
    
    private static class ConfigStructure extends AbstractDBData implements PVStructure{
        private DBData[] dbData = null;
        private String fullFieldName;
        
        private ConfigStructure(DBData parent, Structure structure) {
            super(parent,structure);
            dbData = new DBData[structure.getFields().length];
            fullFieldName = parent.getFullFieldName() + "." + "configStructure";
        }
        
        private void createFields(DBDStructure dbdStructure) {
            Field[] field = dbdStructure.getFields();
            for(int i=0; i < dbData.length; i++) {
                dbData[i] = FieldDataFactory.createData(this,field[i]);
            }
        }    
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.AbstractPVData#getFullFieldName()
         */
        public String getFullFieldName() {
            return fullFieldName;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pv.AbstractPVData#message(java.lang.String, org.epics.ioc.util.MessageType)
         */
        public void message(String message, MessageType messageType) {
            super.getParent().message(message, messageType);
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVStructure#beginPut()
         */
        public void beginPut() {
            // nothing to do
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVStructure#endPut()
         */
        public void endPut() {
            // nothing to do
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVStructure#getFieldPVDatas()
         */
        public PVData[] getFieldPVDatas() {
            return dbData;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVStructure#replaceField(java.lang.String, org.epics.ioc.pv.PVData)
         */
        public boolean replaceField(String fieldName, PVData pvData) {
            // nothing to do
            return false;
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

}
