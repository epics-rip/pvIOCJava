/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support;

import java.util.Set;
import java.util.TreeMap;

import org.epics.pvData.factory.PVDatabaseFactory;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVDatabase;
import org.epics.pvData.pv.PVField;
import org.epics.pvData.pv.PVRecord;

/**
 * @author mrk
 *
 */
public class SupportDatabaseFactory {
    public static synchronized SupportDatabase create(PVDatabase pvDatabase) {
        if(pvDatabase==PVDatabaseFactory.getMaster()) {
            return masterSupportDatabase;
        }
        SupportDatabaseImpl supportDatabase = supportDatabaseMap.get(pvDatabase.getName());
        if(supportDatabase==null) {
            supportDatabase = new SupportDatabaseImpl(pvDatabase);
            supportDatabaseMap.put(pvDatabase.getName(), supportDatabase);
        }
        return supportDatabase;
    }
    
    public static synchronized SupportDatabase get(PVDatabase pvDatabase) {
        if(pvDatabase==PVDatabaseFactory.getMaster()) {
            return masterSupportDatabase;
        }
        return supportDatabaseMap.get(pvDatabase.getName());
    }

    private static SupportDatabaseImpl masterSupportDatabase = new SupportDatabaseImpl(PVDatabaseFactory.getMaster());
    private static TreeMap<String,SupportDatabaseImpl> supportDatabaseMap = new TreeMap<String,SupportDatabaseImpl>();
    
    private static class SupportDatabaseImpl implements SupportDatabase {
        private PVDatabase pvDatabase;
        private TreeMap<String,RecordSupport> recordSupportMap = new TreeMap<String,RecordSupport>();

        private SupportDatabaseImpl(PVDatabase pvDatabase) {
            this.pvDatabase = pvDatabase;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.support.SupportDatabase#getPVDatabase()
         */
        @Override
        public PVDatabase getPVDatabase() {
            return pvDatabase;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.support.SupportDatabase#getRecordSupport(org.epics.pvData.pv.PVRecord)
         */
        @Override
        public synchronized RecordSupport getRecordSupport(PVRecord pvRecord) {
            RecordSupport recordSupport = recordSupportMap.get(pvRecord.getRecordName());
            if(recordSupport==null) {
                recordSupport = new RecordSupportImpl(pvRecord);
                recordSupportMap.put(pvRecord.getRecordName(), recordSupport);
            }
            return recordSupport;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.support.SupportDatabase#mergeIntoMaster()
         */
        public synchronized void mergeIntoMaster() {
            if(pvDatabase==PVDatabaseFactory.getMaster()) {
                pvDatabase.message("mergeIntoMaster called for master", MessageType.error);
                return;
            }
            Set<String> keys;
            keys = recordSupportMap.keySet();
            for(String key: keys) {
                RecordSupport recordSupport = recordSupportMap.get(key);
                masterSupportDatabase.merge(key, recordSupport);
            }
            recordSupportMap.clear();
            supportDatabaseMap.remove(pvDatabase.getName());
        }
        
        private synchronized void merge(String recordSupportName, RecordSupport recordSupport) {
            this.recordSupportMap.put(recordSupportName, recordSupport);
        }
    }
    
    private static class RecordSupportImpl implements RecordSupport {
        private PVRecord pvRecord;
        private Support supportForRecord = null;
        private TreeMap<String,Support> supportMap = new TreeMap<String,Support>();
        private RecordProcess recordProcess = null;
        
        private RecordSupportImpl(PVRecord pvRecord) {
            this.pvRecord = pvRecord;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.support.RecordSupport#getRecordProcess()
         */
        @Override
        public synchronized RecordProcess getRecordProcess() {
            return recordProcess;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.support.RecordSupport#getSupport(org.epics.pvData.pv.PVField)
         */
        @Override
        public synchronized Support getSupport(PVField pvField) {
            if(pvField==null || pvField==pvRecord) return supportForRecord;
            return supportMap.get(pvField.getFullFieldName());
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.support.RecordSupport#setRecordProcess(org.epics.ioc.support.RecordProcess)
         */
        @Override
        public synchronized void setRecordProcess(RecordProcess recordProcess) {
            this.recordProcess = recordProcess;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.support.RecordSupport#setSupport(org.epics.pvData.pv.PVField, org.epics.ioc.support.Support)
         */
        @Override
        public synchronized void setSupport(PVField pvField, Support support) {
            if(pvField==null || pvField==pvRecord) {
                supportForRecord = support;
                return;
            }
            supportMap.put(pvField.getFullFieldName(), support);
        }

    }
}
