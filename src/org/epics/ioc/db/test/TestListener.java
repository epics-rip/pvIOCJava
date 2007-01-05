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
public class TestListener implements DBListener{ 
    private RecordListener listener;
    private String pvName = null;
    private boolean verbose;
    private String actualFieldName = null;
    private boolean isProcessing = false;
    private String fullName = null;
    
    public TestListener(IOCDB iocdb,String recordName,String pvName,
        boolean monitorProperties,boolean verbose)
    {
        this.pvName = pvName;
        this.verbose = verbose;
        DBAccess dbAccess = iocdb.createAccess(recordName);
        if(dbAccess==null) {
            System.out.printf("record %s not found%n",recordName);
            return;
        }
        DBData dbData;
        if(pvName==null || pvName.length()==0) {
            dbData = dbAccess.getDbRecord();
        } else {
            if(dbAccess.setField(pvName)!=AccessSetResult.thisRecord){
                System.out.printf("name %s not in record %s%n",pvName,recordName);
                System.out.printf("%s\n",dbAccess.getDbRecord().toString());
                return;
            }
            dbData = dbAccess.getField();
        }
        actualFieldName = dbData.getField().getFieldName();
        fullName = dbData.getPVRecord().getRecordName() + dbData.getFullFieldName();
        listener = dbData.getDBRecord().createRecordListener(this);
        dbData.addListener(listener);
        if(monitorProperties) {
            if(dbData.getField().getType()!=Type.pvStructure) {
                Property[] property = dbData.getField().getPropertys();
                for(Property prop : property) {
                    dbData = dbAccess.getPropertyField(prop);
                    dbData.addListener(listener);
                }
            }
        }
    }
    
    public TestListener(IOCDB iocdb,String recordName,String pvName)
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
     * @see org.epics.ioc.db.DBListener#beginPut(org.epics.ioc.pv.PVStructure)
     */
    public void beginPut(PVStructure pvStructure) {
        if(!verbose) return;
        DBData dbData = (DBData)pvStructure;
        String name = dbData.getPVRecord().getRecordName() + pvStructure.getFullFieldName();
        System.out.println("beginPut " + name);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.db.DBListener#endPut(org.epics.ioc.pv.PVStructure)
     */
    public void endPut(PVStructure pvStructure) {
        if(!verbose) return;
        DBData dbData = (DBData)pvStructure;
        String name = dbData.getPVRecord().getRecordName() + pvStructure.getFullFieldName();
        System.out.println("endPut " + name);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.db.DBListener#dataPut(org.epics.ioc.db.DBData)
     */
    public void dataPut(DBData dbData) {
        String common = putCommon("dataPut");
        if(!verbose) {
            System.out.println(common + dbData.toString(1));
            return;
        }
        String name = dbData.getPVRecord().getRecordName() + dbData.getFullFieldName();
        if(!name.equals(fullName)) {
            System.out.printf("%s%s NOT_EQUAL %s%n",common,name,fullName);
        }
        System.out.printf("%s    %s = %s%n",
            common,name,dbData.toString(2));
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.db.DBListener#enumChoicesPut(org.epics.ioc.pv.PVEnum)
     */
    public void enumChoicesPut(PVEnum pvEnum) {
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
     * @see org.epics.ioc.db.DBListener#enumIndexPut(org.epics.ioc.pv.PVEnum)
     */
    public void enumIndexPut(PVEnum pvEnum) {
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
     * @see org.epics.ioc.db.DBListener#supportNamePut(org.epics.ioc.db.DBData)
     */
    public void supportNamePut(DBData dbData) {
        String common = putCommon("supportNamePut");
        String name = dbData.getPVRecord().getRecordName() + dbData.getFullFieldName();
        if(!name.equals(fullName)) {
            System.out.printf("%s %s NOT_EQUAL %s%n",common,name,fullName);
        }
        System.out.printf("%s    %s = %s%n",
            common,name,dbData.getSupportName());
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.db.DBListener#configurationStructurePut(org.epics.ioc.pv.PVLink)
     */
    public void configurationStructurePut(PVLink pvLink) {
        String common = putCommon("configurationStructurePut");
        String name = pvLink.getPVRecord().getRecordName() + pvLink.getFullFieldName();
        if(!name.equals(fullName)) {
            System.out.printf("%s %s NOT_EQUAL %s%n",common,name,fullName);
        }
        System.out.printf("%s%n    %s = %s%n",
            common,name,pvLink.getConfigurationStructure().toString(2));
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.db.DBListener#structurePut(org.epics.ioc.pv.PVStructure, org.epics.ioc.db.DBData)
     */
    public void dataPut(PVStructure pvStructure, DBData dbData) {
        String structureName = 
            pvStructure.getPVRecord().getRecordName()
            + pvStructure.getFullFieldName();
        String common = putCommon(structureName +" dataPut to field " + dbData.getFullFieldName());
        System.out.printf("%s    = %s%n",common,dbData.toString(2));
    }       
    /* (non-Javadoc)
     * @see org.epics.ioc.db.DBListener#enumChoicesPut(org.epics.ioc.pv.PVStructure, org.epics.ioc.pv.PVEnum)
     */
    public void enumChoicesPut(PVStructure pvStructure,PVEnum pvEnum) {
        String structureName = 
            pvStructure.getPVRecord().getRecordName()
            + pvStructure.getFullFieldName();
        String common = putCommon(structureName +" enumChoicesPut to field " + pvEnum.getFullFieldName());
        System.out.printf("%s    = %s%n",common,pvEnum.toString(2));
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.db.DBListener#enumIndexPut(org.epics.ioc.pv.PVStructure, org.epics.ioc.pv.PVEnum)
     */
    public void enumIndexPut(PVStructure pvStructure,PVEnum pvEnum) {
        String structureName = 
            pvStructure.getPVRecord().getRecordName()
            + pvStructure.getFullFieldName();
        String common = putCommon(structureName +" enumIndexPut to field " + pvEnum.getFullFieldName());
        System.out.printf("%s    index = %d%n",common,pvEnum.getIndex());
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.db.DBListener#supportNamePut(org.epics.ioc.pv.PVStructure, org.epics.ioc.db.DBData)
     */
    public void supportNamePut(PVStructure pvStructure,DBData dbData) {
        String structureName = 
            pvStructure.getPVRecord().getRecordName()
            + pvStructure.getFullFieldName();
        String common = putCommon(structureName +" supportNamePut to field " + dbData.getFullFieldName());
        System.out.printf("%s    = %s%n",common,dbData.getSupportName());
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.db.DBListener#configurationStructurePut(org.epics.ioc.pv.PVStructure, org.epics.ioc.pv.PVLink)
     */
    public void configurationStructurePut(PVStructure pvStructure,PVLink pvLink) {
        String structureName = 
            pvStructure.getPVRecord().getRecordName()
            + pvStructure.getFullFieldName();
        String common = putCommon(structureName +" configurationStructurePut to field " + pvLink.getFullFieldName());
        System.out.printf("%s%n    = %s%n",common,pvLink.getConfigurationStructure().toString(2));
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.dbAccess.DBListener#unlisten(org.epics.ioc.dbAccess.RecordListener)
     */
    public void unlisten(RecordListener listener) {
        // Nothing to do.
    }
    
}
