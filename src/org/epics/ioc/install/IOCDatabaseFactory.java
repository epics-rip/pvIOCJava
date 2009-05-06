/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.install;

import java.util.Set;
import java.util.TreeMap;

import org.epics.ioc.support.RecordProcess;
import org.epics.ioc.support.Support;
import org.epics.pvData.factory.PVDatabaseFactory;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVDatabase;
import org.epics.pvData.pv.PVField;
import org.epics.pvData.pv.PVRecord;

/**
 * @author mrk
 *
 */
public class IOCDatabaseFactory {
    public static synchronized IOCDatabase create(PVDatabase pvDatabase) {
        if(pvDatabase==PVDatabaseFactory.getMaster()) {
            return masterIOCDatabase;
        }
        if(beingInstalledIOCDatabase!=null) {
            throw new IllegalStateException("database already being installed");
        }
        if(pvDatabase.getName().equals("beingInstalled")) {
            IOCDatabaseImpl supportDatabase = new IOCDatabaseImpl(pvDatabase);
            beingInstalledIOCDatabase = supportDatabase;
            return supportDatabase;
        }
       throw new IllegalStateException("database must be master or beingInstalled");
    }
    
    public static synchronized IOCDatabase get(PVDatabase pvDatabase) {
        if(pvDatabase==PVDatabaseFactory.getMaster()) {
            return masterIOCDatabase;
        }
        if(beingInstalledIOCDatabase!=null) return beingInstalledIOCDatabase;
        throw new IllegalStateException("database must be master or beingInstalled");
    }

    private static IOCDatabaseImpl masterIOCDatabase = new IOCDatabaseImpl(PVDatabaseFactory.getMaster());
    private static IOCDatabaseImpl beingInstalledIOCDatabase = null;
    
    private static class IOCDatabaseImpl implements IOCDatabase {
        private PVDatabase pvDatabase;
        private TreeMap<String,LocateSupport> recordSupportMap = new TreeMap<String,LocateSupport>();

        private IOCDatabaseImpl(PVDatabase pvDatabase) {
            this.pvDatabase = pvDatabase;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.support.SupportDatabase#getPVDatabase()
         */
        public PVDatabase getPVDatabase() {
            return pvDatabase;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.support.SupportDatabase#getRecordSupport(org.epics.pvData.pv.PVRecord)
         */
        public synchronized LocateSupport getLocateSupport(PVRecord pvRecord) {
            LocateSupport recordSupport = recordSupportMap.get(pvRecord.getRecordName());
            if(recordSupport==null) {
                recordSupport = new LocateSupportImpl(pvRecord);
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
                LocateSupport recordSupport = recordSupportMap.get(key);
                masterIOCDatabase.merge(key, recordSupport);
            }
            recordSupportMap.clear();
            IOCDatabaseFactory.beingInstalledIOCDatabase = null;
        }
        
        private synchronized void merge(String recordSupportName, LocateSupport recordSupport) {
            this.recordSupportMap.put(recordSupportName, recordSupport);
        }
    }
    
    private static class LocateSupportImpl implements LocateSupport {
        private PVRecord pvRecord;
        private Support supportForRecord = null;
        private TreeMap<String,Support> supportMap = new TreeMap<String,Support>();
        private RecordProcess recordProcess = null;
        
        private LocateSupportImpl(PVRecord pvRecord) {
            this.pvRecord = pvRecord;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.support.RecordSupport#getRecordProcess()
         */
        public synchronized RecordProcess getRecordProcess() {
            return recordProcess;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.support.RecordSupport#getSupport(org.epics.pvData.pv.PVField)
         */
        public synchronized Support getSupport(PVField pvField) {
            if(pvField==null || pvField==pvRecord) return supportForRecord;
            return supportMap.get(pvField.getFullFieldName());
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.support.RecordSupport#setRecordProcess(org.epics.ioc.support.RecordProcess)
         */
        public synchronized void setRecordProcess(RecordProcess recordProcess) {
            this.recordProcess = recordProcess;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.support.RecordSupport#setSupport(org.epics.pvData.pv.PVField, org.epics.ioc.support.Support)
         */
        public synchronized void setSupport(PVField pvField, Support support) {
            if(pvField==null || pvField==pvRecord) {
                supportForRecord = support;
                return;
            }
            supportMap.put(pvField.getFullFieldName(), support);
        }

    }
}
