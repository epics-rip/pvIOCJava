/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.db.test;

import org.epics.ioc.db.DBField;
import org.epics.ioc.db.DBListener;
import org.epics.ioc.db.DBRecord;
import org.epics.ioc.db.DBStructure;
import org.epics.ioc.db.IOCDB;
import org.epics.ioc.db.RecordListener;
import org.epics.ioc.pv.PVField;
import org.epics.ioc.pv.PVRecord;
import org.epics.ioc.pv.PVStructure;

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
        PVField pvField;
        if(pvName==null || pvName.length()==0) {
            pvField = pvRecord;
        } else {
            pvField = pvRecord.findProperty(pvName);
            if(pvField==null){
                System.out.printf("name %s not in record %s%n",pvName,recordName);
                System.out.printf("%s\n",pvRecord.toString());
                return;
            }
        }
        actualFieldName = pvField.getField().getFieldName();
        fullName = pvField.getFullName();
        listener = dbRecord.createRecordListener(this);
        DBField dbField = dbRecord.findDBField(pvField);
        dbField.addListener(listener);
        if(monitorProperties) {
            String[] propertyNames = pvField.getPropertyNames();
            if(propertyNames!=null) {
                for(String propertyName : propertyNames) {
                    PVField pvf = pvField.findProperty(propertyName);
                    DBField dbf = dbRecord.findDBField(pvf);
                    dbf.addListener(listener);
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
        String name = pvField.getPVRecord().getRecordName() + "." + pvField.getFullFieldName();
        if(!name.equals(fullName)) {
            System.out.printf("%s%s NOT_EQUAL %s%n",common,name,fullName);
        }
        System.out.printf("%s    %s = %s%n",
            common,name,dbField.toString(2));
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.db.DBListener#dataPut(org.epics.ioc.db.DBStructure, org.epics.ioc.db.DBField)
     */
    public void dataPut(DBField requested, DBField dbField) {
        PVField pvRequested = requested.getPVField();
        PVField pvField = dbField.getPVField();
        String structureName = pvRequested.getFullName();
        String common = putCommon(structureName +" dataPut to field " + pvField.getFullFieldName());
        System.out.printf("%s    = %s%n",common,pvField.toString(2));
    }       
    /* (non-Javadoc)
     * @see org.epics.ioc.dbAccess.DBListener#unlisten(org.epics.ioc.dbAccess.RecordListener)
     */
    public void unlisten(RecordListener listener) {
        // Nothing to do.
    }
    
}
