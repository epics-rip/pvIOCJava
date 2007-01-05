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

/**
 * Base class for PVLink.
 * @author mrk
 *
 */
public class DBLinkBase extends AbstractDBData implements DBLink {
    private static Convert convert = ConvertFactory.getConvert();
    private ConfigStructure configStructure = null;
    
    public DBLinkBase(DBData parent,Field field) {
        super(parent,field);
    }   
    /* (non-Javadoc)
     * @see org.epics.ioc.db.DBLink#newSupport(org.epics.ioc.dbd.DBDLinkSupport)
     */
    public String newSupport(DBDLinkSupport linkSupport,DBD dbd) {
        String configurationStructureName = linkSupport.getConfigurationStructureName();
        DBDStructure dbdStructure = dbd.getStructure(configurationStructureName);
        if(dbdStructure==null) {
            return "configurationStructure " + configurationStructureName
                + " for support " + super.getSupportName()
                + " does not exist";
        }
        FieldCreate fieldCreate = FieldFactory.getFieldCreate();
        Field[] fields = dbdStructure.getFields();
        Structure structure = fieldCreate.createStructure(
            "configuration",
            dbdStructure.getStructureName(),
            fields);
        configStructure = new ConfigStructure(this,structure);
        configStructure.createFields(fields);
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
        SupportState supportState = super.getDBRecord().getSupport().getSupportState();
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
        AbstractDBData dbData = this;
        while(dbData!=null) {
            Iterator<RecordListener> iter = dbData.listenerList.iterator();
            while(iter.hasNext()) {
                RecordListener listener = iter.next();
                DBListener dbListener = listener.getDBListener();
                Type type = dbData.getField().getType();
                if(type==Type.pvEnum || type ==Type.pvMenu) {
                    dbListener.configurationStructurePut(this);
                } else if(type==Type.pvStructure) {
                    dbListener.configurationStructurePut(dbData.thisStructure, this);
                } else {
                    throw new IllegalStateException("Logic error");
                }
            }
            if(dbData.parent==dbData) {
                System.err.printf("setChoices parent = this Why???%n");
            } else {
                dbData = (AbstractDBData)dbData.parent;
            }
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
        
        private void createFields(Field[] field) {
            for(int i=0; i < dbData.length; i++) {
                dbData[i] = DBDataFactory.getDBDataCreate().createData(this,field[i]);
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
