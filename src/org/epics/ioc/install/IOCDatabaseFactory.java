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
 * IOCDatabaseFactory.
 * This is a factory that creates an IOCDatabase.
 * It allows only two IOCDatabases: master and beingInstalled.
 * Once the beingInstalled database is merged into master then it no longer exists.
 * @author mrk
 *
 */
public class IOCDatabaseFactory {
    /**
     * Create an IOCDatabase.
     * The pvDatabase must be either master or beingInstalled.
     * The master is permanent. beingInstalled only exists until it is merged into master.
     * @param pvDatabase The PVDatabase that IOCDatabase refernces.
     * @return The IOCDatabase.
     */
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
    /**
     * Get the IOCDatabase for the pvDatabase.
     * @param pvDatabase The PVDatabase.
     * This must be either master or beingInstalled.
     * @return The IOCDatabase or null if no IOCDatabase exists for the pvDatabase.
     */
    public static synchronized IOCDatabase get(PVDatabase pvDatabase) {
        if(pvDatabase==PVDatabaseFactory.getMaster()) {
            return masterIOCDatabase;
        }
        if(beingInstalledIOCDatabase!=null) return beingInstalledIOCDatabase;
        throw new IllegalStateException("database must be master or beingInstalled");
    }
    /**
     * Get the LocateSupport for the specified record.
     * The record must be in either master or beingInstalled.
     * @param pvRecord The record for which to find LocateSupport.
     * @return The LocateSupport or null if an IOCDatabase does not exist for the pvRecord.
     */
    public static synchronized LocateSupport getLocateSupport(PVRecord pvRecord) {
        IOCDatabase iocDatabase = masterIOCDatabase;
        LocateSupport locateSupport = iocDatabase.getLocateSupport(pvRecord);
        if(locateSupport!=null) return locateSupport;
        if(beingInstalledIOCDatabase==null) return null;
        iocDatabase = beingInstalledIOCDatabase;
        return iocDatabase.getLocateSupport(pvRecord);
    }

    private static IOCDatabaseImpl masterIOCDatabase = new IOCDatabaseImpl(PVDatabaseFactory.getMaster());
    private static IOCDatabaseImpl beingInstalledIOCDatabase = null;
    
    private static class IOCDatabaseImpl implements IOCDatabase {
        private PVDatabase pvDatabase;
        private TreeMap<String,LocateSupport> locateSupportMap = new TreeMap<String,LocateSupport>();

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
            LocateSupport locateSupport = locateSupportMap.get(pvRecord.getRecordName());
            if(locateSupport==null) {
                locateSupport = new LocateSupportImpl(pvRecord);
                locateSupportMap.put(pvRecord.getRecordName(), locateSupport);
            }
            return locateSupport;
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
            keys = locateSupportMap.keySet();
            for(String key: keys) {
                LocateSupport recordSupport = locateSupportMap.get(key);
                masterIOCDatabase.merge(key, recordSupport);
            }
            locateSupportMap.clear();
            IOCDatabaseFactory.beingInstalledIOCDatabase = null;
        }
        
        private synchronized void merge(String recordSupportName, LocateSupport recordSupport) {
            this.locateSupportMap.put(recordSupportName, recordSupport);
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
