/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.db;

import java.util.*;
import java.util.concurrent.locks.*;

import org.epics.ioc.pv.*;
import org.epics.ioc.dbd.*;
import org.epics.ioc.process.*;

/**
 * Abstract base class for a record instance.
 * @author mrk
 *
 */
public class DBRecordFactory {
    public static DBRecord create(PVRecord pvRecord) {
        return new DBRecordImpl(pvRecord);
    }

    private static class DBRecordImpl implements DBRecord {
        PVDataCreate pvDataCreate = PVDataFactory.getPVDataCreate();
        private static int numberRecords = 0;
        private int id = numberRecords++;
        private PVRecord pvRecord;
        private DBStructure dbStructure;
        private IOCDB iocdb = null;
        private ReentrantLock lock = new ReentrantLock();
        private RecordProcess recordProcess = null;
        private LinkedList<RecordListenerPvt> recordListenerList
        = new LinkedList<RecordListenerPvt>();
        private LinkedList<DBData> listenerSourceList
        = new LinkedList<DBData>();
        private DBD dbd = null;

        /**
         * @param recordName
         *            The name of the record.
         * @param dbdRecordType
         *            The introspection interface for the record. Constructor.
         */
        public DBRecordImpl(PVRecord pvRecord){
            this.pvRecord = pvRecord;
            this.dbStructure = new BaseDBStructure(this,pvRecord);
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.db.DBRecord#findDBData(org.epics.ioc.pv.PVData)
         */
        public DBData findDBData(PVData pvData) {
            if(pvData==pvRecord) {
                return dbStructure;
            }
            int nlevels = 0;
            PVData parent = pvData.getParent();
            while(parent!=null) {
                nlevels++;
                parent = parent.getParent();
            }
            PVData[] pvDataFind = new PVData[nlevels];
            pvDataFind[nlevels-1] = pvData;
            parent = pvData.getParent();
            int index = nlevels-2;
            while(parent!=pvRecord) {
                pvDataFind[index--] = parent;
                parent = parent.getParent();
            }
            DBData[] dbDatas = dbStructure.getFieldDBDatas();
            PVData[] pvDatas = pvRecord.getFieldPVDatas();
            return findDBData(pvData,pvDataFind,0,dbDatas,pvDatas);
        }
        
        private DBData findDBData(
            PVData pvData,PVData[] pvDataFind,int index,DBData[] dbDatas,PVData[] pvDatas)
        {
            PVData pvNow = pvDataFind[index];
            for(int j=0; j<dbDatas.length; j++) {
                if(pvDatas[j]==pvNow) {
                    if(pvNow==pvData) return dbDatas[j];
                    Type typeNow = pvNow.getField().getType();
                    if(typeNow==Type.pvArray) {
                        return findArrayData(pvData,pvDataFind,index+1,dbDatas[j],(PVArray)pvDatas[j]);
                    }
                    if(pvNow.getField().getType()!=Type.pvStructure) return dbDatas[j];
                    DBStructure dbStructure = (DBStructure)dbDatas[j];
                    dbDatas = dbStructure.getFieldDBDatas();
                    PVStructure pvStructure = (PVStructure)pvDatas[j];
                    pvDatas = pvStructure.getFieldPVDatas();
                    index++;
                    return findDBData(pvData,pvDataFind,index,dbDatas,pvDatas);
                }
            }
            throw new IllegalStateException("Logic error");
        }
        
        private DBData findArrayData(
                PVData pvData,PVData[]pvDataFind,int index,DBData dbData,PVArray pvArray)
        {
            Array array = (Array)pvArray.getField();
            Type arrayElementType = array.getElementType();
            if(arrayElementType.isScalar()) {
                if(dbData.getPVData()==pvData) return dbData;
                throw new IllegalStateException("Logic error");
            }
            int size = pvArray.getLength();
            DBNonScalarArray dbNonScalarArray = (DBNonScalarArray)dbData;
            DBData[] dbDatas = dbNonScalarArray.getElementDBDatas();
            switch(arrayElementType) {
            case pvEnum:
                PVEnumArray pvEnumArray = (PVEnumArray)pvArray;
                EnumArrayData enumArrayData = new EnumArrayData();
                pvEnumArray.get(0, size, enumArrayData);
                PVEnum[] pvEnums = enumArrayData.data;
                for(int i=0; i<size; i++) {
                    PVEnum pvEnum = pvEnums[i];
                    if(pvEnum==null) continue;
                    if(pvEnum==pvData) {
                        if(pvEnum==dbDatas[i].getPVData()) {
                            return dbDatas[i];
                        }
                        throw new IllegalStateException("Logic error");
                    }
                }
                throw new IllegalStateException("Logic error");
            case pvStructure:
                PVStructureArray pvStructureArray = (PVStructureArray)pvArray;
                StructureArrayData structureArrayData = new StructureArrayData();
                pvStructureArray.get(0, size, structureArrayData);
                PVStructure[] pvStructures = structureArrayData.data;
                for(int i=0; i<size; i++) {
                    PVStructure pvStructure = pvStructures[i];
                    if(pvStructure==null) continue;
                    if(pvStructure==pvDataFind[index]) {
                        DBStructure dbStructure = (DBStructure)dbDatas[i];
                        if(pvStructure==pvData) {
                            if(dbStructure.getPVData()==pvStructure) return dbStructure;
                            throw new IllegalStateException("Logic error");
                        }
                        return findDBData(pvData,pvDataFind,index+1,
                            dbStructure.getFieldDBDatas(),pvStructure.getFieldPVDatas());
                    }
                }
                throw new IllegalStateException("Logic error");
            case pvArray:
                PVArrayArray pvArrayArray = (PVArrayArray)pvArray;
                ArrayArrayData arrayArrayData = new ArrayArrayData();
                pvArrayArray.get(0, size, arrayArrayData);
                PVArray[] pvArrays = arrayArrayData.data;
                for(int i=0; i<size; i++) {
                    PVArray elementArray = pvArrays[i];
                    if(elementArray==null) continue;
                    if(elementArray==pvDataFind[index]) {
                        Type elementType = ((Array)elementArray.getField()).getElementType();
                        if(elementType.isScalar()) {
                            if(elementArray==pvData) return dbDatas[i];
                            throw new IllegalStateException("Logic error");
                        }
                        return findArrayData(pvData,pvDataFind,index+1,dbDatas[i],elementArray);
                    }
                }
                throw new IllegalStateException("Logic error");
            case pvMenu:
                PVMenuArray pvMenuArray = (PVMenuArray)pvArray;
                MenuArrayData menuArrayData = new MenuArrayData();
                pvMenuArray.get(0, size, menuArrayData);
                PVMenu[] pvMenus = menuArrayData.data;
                for(int i=0; i<size; i++) {
                    PVMenu pvMenu = pvMenus[i];
                    if(pvMenu==null) continue;
                    if(pvMenu==pvData) {
                        if(pvMenu==dbDatas[i].getPVData()) return dbDatas[i];
                        throw new IllegalStateException("Logic error");
                    }
                }
                throw new IllegalStateException("Logic error");
            case pvLink:
                PVLinkArray pvLinkArray = (PVLinkArray)pvArray;
                LinkArrayData linkArrayData = new LinkArrayData();
                pvLinkArray.get(0, size, linkArrayData);
                PVLink[] pvLinks = linkArrayData.data;
                for(int i=0; i<size; i++) {
                    PVLink pvLink = pvLinks[i];
                    if(pvLink==null) continue;
                    if(pvLink==pvData) {
                        if(pvLink==dbDatas[i].getPVData()) return dbDatas[i];
                        throw new IllegalStateException("Logic error");
                    }
                }
                throw new IllegalStateException("Logic error");
            }
            throw new IllegalStateException("Logic error");
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.epics.ioc.db.DBRecord#getDBStructure()
         */
        public DBStructure getDBStructure() {
            return dbStructure;
        }
        /*
         * (non-Javadoc)
         * 
         * @see org.epics.ioc.db.DBRecord#getPVRecord()
         */
        public PVRecord getPVRecord() {
            return pvRecord;
        }
        /*
         * (non-Javadoc)
         * 
         * @see org.epics.ioc.db.DBRecord#lock()
         */
        public void lock() {
            lock.lock();
        }
        /*
         * (non-Javadoc)
         * 
         * @see org.epics.ioc.db.DBRecord#unlock()
         */
        public void unlock() {
            lock.unlock();
        }
        /*
         * (non-Javadoc)
         * 
         * @see org.epics.ioc.db.DBRecord#lockOtherRecord(org.epics.ioc.dbAccess.DBRecord)
         */
        public void lockOtherRecord(DBRecord otherRecord) {
            int otherId = otherRecord.getRecordID();
            if(id<=otherId) {
                otherRecord.lock();
                return;
            }
            int count = lock.getHoldCount();
            for(int i=0; i<count; i++) lock.unlock();
            otherRecord.lock();
            for(int i=0; i<count; i++) lock.lock();
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.epics.ioc.db.DBRecord#getRecordProcess()
         */
        public RecordProcess getRecordProcess() {
            return recordProcess;
        }
        /*
         * (non-Javadoc)
         * 
         * @see org.epics.ioc.db.DBRecord#setRecordProcess(org.epics.ioc.process.RecordProcess)
         */
        public boolean setRecordProcess(RecordProcess process) {
            if(recordProcess!=null) return false;
            recordProcess = process;
            return true;
        }
        /*
         * (non-Javadoc)
         * 
         * @see org.epics.ioc.db.DBRecord#getRecordID()
         */
        public int getRecordID() {
            return id;
        }
        /*
         * (non-Javadoc)
         * 
         * @see org.epics.ioc.db.DBRecord#beginProcess()
         */
        public void beginProcess() {
            Iterator<RecordListenerPvt> iter = recordListenerList.iterator();
            while(iter.hasNext()) {
                RecordListenerPvt listener = iter.next();
                listener.getDBListener().beginProcess();
            }
        }
        /*
         * (non-Javadoc)
         * 
         * @see org.epics.ioc.db.DBRecord#endProcess()
         */
        public void endProcess() {
            Iterator<RecordListenerPvt> iter = recordListenerList.iterator();
            while(iter.hasNext()) {
                RecordListenerPvt listener = iter.next();
                listener.getDBListener().endProcess();
            }
        }
        /*
         * (non-Javadoc)
         * 
         * @see org.epics.ioc.db.DBRecord#createRecordListener(org.epics.ioc.db.DBListener)
         */
        public RecordListener createRecordListener(DBListener listener) {
            RecordListenerPvt recordListenerPvt = new RecordListenerPvt(listener);
            recordListenerList.add(recordListenerPvt);
            return recordListenerPvt;
        }   
        /*
         * (non-Javadoc)pvData
         * 
         * @see org.epics.ioc.db.DBRecord#removeRecordListener(org.epics.ioc.db.RecordListener)
         */
        public void removeRecordListener(RecordListener listener) {
            Iterator<DBData> iter = listenerSourceList.iterator();
            while(iter.hasNext()) {
                DBData dbData = iter.next();
                dbData.removeListener(listener);
            }
            recordListenerList.remove(listener);
        }
        /*
         * (non-Javadoc)
         * 
         * @see org.epics.ioc.db.BaseDBData#removeListeners()
         */
        public void removeRecordListeners() {
            while(true) {
                RecordListenerPvt listener = recordListenerList.remove();
                if(listener==null) break;
                listener.getDBListener().unlisten(listener);
                Iterator<DBData> iter = listenerSourceList.iterator();
                while(iter.hasNext()) {
                    DBData dbData = iter.next();
                    dbData.removeListener(listener);
                }
            }
            listenerSourceList.clear();
        }
        /*
         * (non-Javadoc)
         * 
         * @see org.epics.ioc.db.DBRecord#addListenerSource(org.epics.ioc.db.BaseDBData)
         */
        public void addListenerSource(DBData dbData) {
            listenerSourceList.add(dbData);
        }
        /*
         * (non-Javadoc)
         * 
         * @see org.epics.ioc.db.DBRecord#getDBD()
         */
        public DBD getDBD() {
            return dbd;
        }
        /*
         * (non-Javadoc)
         * 
         * @see org.epics.ioc.db.DBRecord#setDBD(org.epics.ioc.dbDefinition.DBD)
         */
        public void setDBD(DBD dbd) {
            this.dbd = dbd;
        }
        /*
         * (non-Javadoc)
         * 
         * @see org.epics.ioc.db.DBRecord#getIOCDB()
         */
        public IOCDB getIOCDB() {
            return iocdb;
        }
        /*
         * (non-Javadoc)
         * 
         * @see org.epics.ioc.db.DBRecord#setIOCDB(org.epics.ioc.db.IOCDB)
         */
        public void setIOCDB(IOCDB iocdb) {
            this.iocdb = iocdb;
        }
        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Object#toString()
         */
        public String toString() { return pvRecord.toString(0);}

        /*
         * (non-Javadoc)
         * 
         * @see org.epics.ioc.pvAccess.PVData#toString(int)
         */
        public String toString(int indentLevel) {
            return pvRecord.toString(indentLevel);
        }

        private static class RecordListenerPvt implements RecordListener {
            private DBListener dbListener;

            RecordListenerPvt(DBListener listener) {
                dbListener = listener;
            }

            /*
             * (non-Javadoc)
             * 
             * @see org.epics.ioc.db.RecordListener#getDBListener()
             */
            public DBListener getDBListener() {
                return dbListener;
            }
        }
    }
}
