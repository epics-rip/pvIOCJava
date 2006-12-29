/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.db;

import java.util.Iterator;

import org.epics.ioc.dbd.*;
import org.epics.ioc.pv.*;
import org.epics.ioc.process.*;
import org.epics.ioc.util.*;

/**
 * Base class for PVLink.
 * @author mrk
 *
 */
public class DBLinkBase extends AbstractDBData implements PVLink, Requestor {
    private static Convert convert = ConvertFactory.getConvert();
    private ConfigStructure configStructure = null;
    
    public DBLinkBase(DBData parent,Field field) {
        super(parent,field);
    }
    
    /* (non-Javadoc)
     * @see org.epics.ioc.db.AbstractDBData#setSupportName(java.lang.String)
     */
    public String setSupportName(String name) {
        String result = super.setSupportName(name);
        if(result!=null) return result;
        DBRecord dbRecord = super.getDBRecord();
        DBD dbd = dbRecord.getDBD();
        DBDLinkSupport dbdLinkSupport = dbd.getLinkSupport(name);
        if(dbdLinkSupport==null) return "DBDLinkSupport not found";
        String configurationStructureName = dbdLinkSupport.getConfigurationStructureName();
        if(configurationStructureName==null) {
            SupportState supportState = super.getDBRecord().getSupport().getSupportState();
            Support support = super.getSupport();
            switch(supportState) {
            case readyForInitialize:
                break;
            case readyForStart:
                support.initialize();
                break;
            case ready:
                support.initialize();
                if(support.getSupportState()!=SupportState.readyForStart) break;
                support.start();
                break;
            default:
            }
            return null;
        }
        DBDStructure dbdStructure = dbd.getStructure(configurationStructureName);
        if(dbdStructure==null) {
            return "configurationStructure " + configurationStructureName
                + " for support " + name
                + " does not exist";
        }
        configStructure = new ConfigStructure(this,dbdStructure);
        configStructure.createFields(dbdStructure);
        if(super.getSupport()==null) {
            // Wait until SupportCreation has been run
            return super.setSupportName(name);
        }
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
        Support support = super.getSupport();
        if(support==null) {
            //Wait until SupportCreation has been run
            return true;
        }
        SupportState supportState = support.getSupportState();
        if(supportState!=SupportState.readyForInitialize) {
            support.uninitialize();
        }
        supportState = super.getDBRecord().getSupport().getSupportState();
        switch(supportState) {
        case readyForInitialize:
            break;
        case readyForStart:
            support.initialize();
            break;
        case ready:
            support.initialize();
            if(support.getSupportState()!=SupportState.readyForStart) break;
            support.start();
            break;
        default:
        }
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
        
        private ConfigStructure(DBData parent, Structure structure) {
            super(parent,structure);
            dbData = new DBData[structure.getFields().length];
        }
        
        private void createFields(DBDStructure dbdStructure) {
            Field[] field = dbdStructure.getFields();
            for(int i=0; i < dbData.length; i++) {
                dbData[i] = FieldDataFactory.createData(this,field[i]);
            }
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
        public boolean replaceStructureField(String fieldName, String structureName) {
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
