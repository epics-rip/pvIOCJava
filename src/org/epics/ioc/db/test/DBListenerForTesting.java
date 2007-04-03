/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.db.test;

import org.epics.ioc.db.*;
import org.epics.ioc.pv.*;

/**
 * @author mrk
 *
 */
public class DBListenerForTesting implements DBListener{ 
    private RecordListener listener;
    private String pvName = null;
    private boolean verbose;
    private String actualFieldName = null;
    private boolean isProcessing = false;
    private String fullName = null;
    
    public DBListenerForTesting(IOCDB iocdb,String recordName,String pvName,
        boolean monitorProperties,boolean verbose)
    {
        this.pvName = pvName;
        this.verbose = verbose;
        DBRecord dbRecord = iocdb.findRecord(recordName);
        PVRecord pvRecord = dbRecord.getPVRecord();
        if(pvRecord==null) {
            System.out.printf("record %s not found%n",recordName);
            return;
        }
        PVAccess pvAccess = PVAccessFactory.createPVAccess(pvRecord);
        PVField pvField;
        if(pvName==null || pvName.length()==0) {
            pvField = pvAccess.getPVRecord();
        } else {
            if(pvAccess.findField(pvName)!=AccessSetResult.thisRecord){
                System.out.printf("name %s not in record %s%n",pvName,recordName);
                System.out.printf("%s\n",pvAccess.getPVRecord().toString());
                return;
            }
            pvField = pvAccess.getField();
        }
        actualFieldName = pvField.getField().getFieldName();
        fullName = pvField.getPVRecord().getRecordName() + pvField.getFullFieldName();
        listener = dbRecord.createRecordListener(this);
        DBField dbField = dbRecord.findDBField(pvField);
        dbField.addListener(listener);
        if(monitorProperties) {
            if(pvField.getField().getType()!=Type.pvStructure) {
                Property[] property = pvField.getField().getPropertys();
                DBField propertyField;
                for(Property prop : property) {
                    pvAccess.setPVField(pvField);
                    if(pvAccess.findField(prop.getPropertyName())!=AccessSetResult.thisRecord){
                        System.out.printf("name %s not in record %s%n",pvName,recordName);
                        System.out.printf("%s\n",pvAccess.getPVRecord().toString());
                    } else {
                        propertyField = (DBField)dbRecord.findDBField(pvAccess.getField());
                        propertyField.addListener(listener);
                    }
                }
            }
        }
    }
    
    public DBListenerForTesting(IOCDB iocdb,String recordName,String pvName)
    {
        this(iocdb,recordName,pvName,true,true);
    }
    
    private String putCommon(String message) {
        if(!verbose) {
            return fullName + " ";
        }
        return String.format("%s %s isProcessing %b pvName %s actualFieldName %s%n",
            message,
            fullName,
            isProcessing,
            pvName,
            actualFieldName);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.dbAccess.DBListener#beginProcess()
     */
    public void beginProcess() {
        isProcessing = true;
        putCommon("beginProcess");
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.dbAccess.DBListener#endProcess()
     */
    public void endProcess() {
        putCommon("endProcess");
        isProcessing = false;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.db.DBListener#beginPut(org.epics.ioc.db.DBStructure)
     */
    public void beginPut(DBStructure dbStructure) {
        PVStructure pvStructure = dbStructure.getPVStructure();
        if(!verbose) return;
        String name = pvStructure.getPVRecord().getRecordName() + pvStructure.getFullFieldName();
        System.out.println("beginPut " + name);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.db.DBListener#endPut(org.epics.ioc.db.DBStructure)
     */
    public void endPut(DBStructure dbStructure) {
        PVStructure pvStructure = dbStructure.getPVStructure();
        if(!verbose) return;
        String name = pvStructure.getPVRecord().getRecordName() + pvStructure.getFullFieldName();
        System.out.println("endPut " + name);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.db.DBListener#dataPut(org.epics.ioc.db.DBField)
     */
    public void dataPut(DBField dbField) {
        PVField pvField = dbField.getPVField();
        String common = putCommon("dataPut");
        if(!verbose) {
            System.out.println(common + dbField.toString(1));
            return;
        }
        String name = pvField.getPVRecord().getRecordName() + pvField.getFullFieldName();
        if(!name.equals(fullName)) {
            System.out.printf("%s%s NOT_EQUAL %s%n",common,name,fullName);
        }
        System.out.printf("%s    %s = %s%n",
            common,name,dbField.toString(2));
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.db.DBListener#enumChoicesPut(org.epics.ioc.db.DBEnum)
     */
    public void enumChoicesPut(DBEnum dbEnum) {
        PVEnum pvEnum = dbEnum.getPVEnum();
        String common = putCommon("enumChoicesPut");
        if(!verbose) {
            System.out.println(common + pvEnum.toString(1));
            return;
        }
        String name = pvEnum.getPVRecord().getRecordName() + pvEnum.getFullFieldName();
        if(!name.equals(fullName)) {
            System.out.printf("%s %s NOT_EQUAL %s%n",common,name,fullName);
        }
        System.out.printf("%s    %s = %s%n",
            common,name,pvEnum.toString(2));
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.db.DBListener#enumIndexPut(org.epics.ioc.db.DBEnum)
     */
    public void enumIndexPut(DBEnum dbEnum) {
        PVEnum pvEnum = dbEnum.getPVEnum();
        String common = putCommon("enumChoicesPut");
        if(!verbose) {
            System.out.println(common + pvEnum.toString(1));
            return;
        }
        String name = pvEnum.getPVRecord().getRecordName() + pvEnum.getFullFieldName();
        if(!name.equals(fullName)) {
            System.out.printf("%s %s NOT_EQUAL %s%n",common,name,fullName);
        }
        System.out.printf("%s    %s index = %d%n",
            common,name,pvEnum.getIndex());
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.db.DBListener#supportNamePut(org.epics.ioc.db.DBField)
     */
    public void supportNamePut(DBField dbField) {
        PVField pvField = dbField.getPVField();
        String common = putCommon("supportNamePut");
        String name = pvField.getPVRecord().getRecordName() + pvField.getFullFieldName();
        if(!name.equals(fullName)) {
            System.out.printf("%s %s NOT_EQUAL %s%n",common,name,fullName);
        }
        System.out.printf("%s    %s = %s%n",
            common,name,pvField.getSupportName());
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.db.DBListener#configurationStructurePut(org.epics.ioc.db.DBLink)
     */
    public void configurationStructurePut(DBLink dbLink) {
        PVLink pvLink = dbLink.getPVLink();
        System.out.printf("configStructPut pvName %s actualField %s%s%n",
            pvName,actualFieldName,pvLink.getConfigurationStructure().toString(2));
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.db.DBListener#dataPut(org.epics.ioc.db.DBStructure, org.epics.ioc.db.DBField)
     */
    public void dataPut(DBField requested, DBField dbField) {
        PVField pvRequested = requested.getPVField();
        PVField pvField = dbField.getPVField();
        String structureName = 
            pvRequested.getPVRecord().getRecordName()
            + pvRequested.getFullFieldName();
        String common = putCommon(structureName +" dataPut to field " + pvField.getFullFieldName());
        System.out.printf("%s    = %s%n",common,pvField.toString(2));
    }       
    /* (non-Javadoc)
     * @see org.epics.ioc.db.DBListener#enumChoicesPut(org.epics.ioc.db.DBStructure, org.epics.ioc.db.DBEnum)
     */
    public void enumChoicesPut(DBField requested,DBEnum dbEnum) {
        PVField pvRequested = requested.getPVField();
        PVEnum pvEnum = dbEnum.getPVEnum();
        String structureName = 
            pvRequested.getPVRecord().getRecordName()
            + pvRequested.getFullFieldName();
        String common = putCommon(structureName +" enumChoicesPut to field " + pvEnum.getFullFieldName());
        System.out.printf("%s    = %s%n",common,pvEnum.toString(2));
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.db.DBListener#enumIndexPut(org.epics.ioc.db.DBStructure, org.epics.ioc.db.DBEnum)
     */
    public void enumIndexPut(DBField requested,DBEnum dbEnum) {
        PVField pvRequested = requested.getPVField();
        PVEnum pvEnum = dbEnum.getPVEnum();
        String structureName = 
            pvRequested.getPVRecord().getRecordName()
            + pvRequested.getFullFieldName();
        String common = putCommon(structureName +" enumIndexPut to field " + pvEnum.getFullFieldName());
        System.out.printf("%s    index = %d%n",common,pvEnum.getIndex());
    }
    
    /* (non-Javadoc)
     * @see org.epics.ioc.db.DBListener#supportNamePut(org.epics.ioc.db.DBStructure, org.epics.ioc.db.DBField)
     */
    public void supportNamePut(DBField requested,DBField dbField) {
        PVField pvRequested = requested.getPVField();
        PVField pvField = dbField.getPVField();
        String structureName = 
            pvRequested.getPVRecord().getRecordName()
            + pvRequested.getFullFieldName();
        String common = putCommon(structureName +" supportNamePut to field " + pvField.getFullFieldName());
        System.out.printf("%s    = %s%n",common,pvField.getSupportName());
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.db.DBListener#configurationStructurePut(org.epics.ioc.db.DBStructure, org.epics.ioc.db.DBLink)
     */
    public void configurationStructurePut(DBField requested,DBLink dbLink) {
        PVLink pvLink = dbLink.getPVLink();
        System.out.printf("configStructPut pvName %s actualField %s%s%n",
            pvName,actualFieldName,pvLink.getConfigurationStructure().toString(2));
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.dbAccess.DBListener#unlisten(org.epics.ioc.dbAccess.RecordListener)
     */
    public void unlisten(RecordListener listener) {
        // Nothing to do.
    }
    
}