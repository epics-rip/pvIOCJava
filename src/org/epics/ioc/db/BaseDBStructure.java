/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.db;

import java.util.Iterator;

import org.epics.ioc.pv.*;

/**
 * @author mrk
 *
 */
public class BaseDBStructure extends BaseDBData implements DBStructure
{
    private PVStructure pvStructure;
    private DBData[] dbDatas;
    
    /**
     * Constructor.
     * @param parent the DBStructure of the parent.
     * @param structure the
     */
    public BaseDBStructure(DBData parent,DBRecord record, PVData pvData) {
        super(parent,record,pvData);
        this.pvStructure = (PVStructure)pvData;
        createFields();
    }
    
    /**
     * Constructor for record instance classes.
     * @param dbdRecordType The reflection interface for the record type.
     */
    public BaseDBStructure(DBRecord dbRecord,PVRecord pvRecord) {
        super(null,dbRecord,pvRecord);
        pvStructure = (PVStructure)pvRecord;
        createFields();
    }
    public void replacePVStructure() {
        this.pvStructure = (PVStructure)super.getPVData();
        createFields();
    }
    /**
     * Create the fields for the record.
     * This is only called by whatever called the record instance constructor.
     * @param record the record instance.
     */
    private void createFields() {
        PVData[] pvDatas = pvStructure.getFieldPVDatas();
        int length = pvDatas.length;
        dbDatas = new DBData[length];
        for(int i=0; i<length; i++) {
            PVData pvData = pvDatas[i];
            Type type = pvData.getField().getType();
            DBRecord dbRecord = super.getDBRecord();
            BaseDBData dbData = null;
            switch(type) {
            case pvEnum:
                dbData = new BaseDBEnum(this,dbRecord,pvData);
                break;
            case pvMenu:
                dbData = new BaseDBMenu(this,dbRecord,pvData);
                break;
            case pvLink:
                dbData = new BaseDBLink(this,dbRecord,pvData);
                break;
            case pvStructure:
                dbData = new BaseDBStructure(this,dbRecord,pvData);
                break;
            case pvArray:
                PVArray pvArray = (PVArray)pvData;
                if(((Array)pvArray.getField()).getElementType().isScalar()) {
                    dbData = new BaseDBData(this,dbRecord,pvArray);
                } else {
                    dbData = new BaseDBNonScalarArray(this,dbRecord,pvArray);
                }
                break;
            default:
                dbData = new BaseDBData(this,dbRecord,pvData);
                break;
            }
            dbDatas[i] = dbData;
        }
        
    }    
    /* (non-Javadoc)
     * @see org.epics.ioc.db.DBStructure#getFieldDBDatas()
     */
    public DBData[] getFieldDBDatas() {
        return dbDatas;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVStructure#endPut()
     */
    public void endPut() {
        Iterator<RecordListener> iter = super.getRecordListenerList().iterator();
        while(iter.hasNext()) {
            RecordListener listener = iter.next();
            listener.getDBListener().endPut(this);
        }   
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVStructure#beginPut()
     */
    public void beginPut() {
        Iterator<RecordListener> iter = super.getRecordListenerList().iterator();
        while(iter.hasNext()) {
            RecordListener listener = iter.next();
            listener.getDBListener().beginPut(this);
        }   
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.db.DBStructure#getPVStructure()
     */
    public PVStructure getPVStructure() {
        return (PVStructure)super.getPVData();
    }
}
