/**
 * Copyright - See the COPYRIGHT that is included with this distibution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbProcess;

import java.lang.reflect.Constructor;
import java.util.*;
import java.util.concurrent.*;

import org.epics.ioc.dbAccess.*;
import org.epics.ioc.dbDefinition.DBDAttribute;
import org.epics.ioc.dbDefinition.DBType;
import org.epics.ioc.pvAccess.*;
import org.epics.ioc.util.*;

/**
 * A factory for creating RecordProcess support for record instances.
 * @author mrk
 *
 */
public class ProcessFactory {
    /**
     * create a process database.
     * @param iocdb the iocdb associated with the record processing.
     * @return the ProcessDB.
     */
    static public ProcessDB createProcessDB(IOCDB iocdb) {
        return new ProcessDatabase(iocdb);
    }
    
    static private class ProcessDatabase implements ProcessDB{
        
        private IOCDB iocdb;
        private static Map<String,RecordProcess> recordProcessMap;
        static {
            recordProcessMap = new HashMap<String,RecordProcess>();
        }

        /**
         * Constructor for ProcessDatabase.
         * @param iocdb the IOCDB for which RecordProcess will be created.
         */
        ProcessDatabase(IOCDB iocdb) {
            this.iocdb = iocdb;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.ProcessDB#createRecordProcess(java.lang.String)
         */
        public boolean createRecordProcess(String recordName) {
            if(recordProcessMap.containsKey(recordName)) return false;
            DBRecord dbRecord = iocdb.findRecord(recordName);
            RecordProcess recordProcess = new Process(this,dbRecord);
            if(recordProcessMap.put(recordName,recordProcess)==null) {
                dbRecord.setRecordProcess(recordProcess);
                return true;
            }
            System.out.printf("ProcessDB.createRecordProcess failure. Why??\n");
            return true;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.ProcessDB#removeRecordProcess(java.lang.String)
         */
        public void removeRecordProcess(String recordName) {
            recordProcessMap.remove(recordName);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.ProcessDB#findRecordProcess(java.lang.String)
         */
        public RecordProcess getRecordProcess(String recordName) {
            return recordProcessMap.get(recordName);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.ProcessDB#getIOCDB()
         */
        public IOCDB getIOCDB() {
            return iocdb;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.ProcessDB#createSupport(java.lang.String)
         */
        public boolean createSupport(String recordName) {
            boolean result = true;
            DBRecord record = iocdb.findRecord(recordName);
            String recordSupportName = record.getRecordSupportName();
            if(recordSupportName==null) {
                result = false;
            } else {
                if(!createRecordSupport(record,recordSupportName)) result = false; 
            }
            if(!createStructureSupport(record)) result = false;
            if(!recordProcessMap.containsKey(recordName)) {
                if(!createRecordProcess(recordName)) result = false;
            }
            RecordProcess recordProcess = getRecordProcess(recordName);
            if(recordProcess!=null) {
                recordProcess.setRecordSupport(record.getRecordSupport());
            }
            return result;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.ProcessDB#createSupport()
         */
        public boolean createSupport() {
            boolean result = true;
            Map<String,DBRecord> recordMap = iocdb.getRecordMap();
            Set<String> keys = recordMap.keySet();
            for(String key: keys) {
                if(!createSupport(key)) result = false;
            }
            return result;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.ProcessDB#getRecordProcessMap()
         */
        public Map<String, RecordProcess> getRecordProcessMap() {
            return recordProcessMap;
        }
        private void printError(DBData dbData,String message) {
            String name = dbData.getField().getName();
            DBData parent = dbData.getParent();
            DBRecord record = dbData.getRecord();
            while(parent!=null && parent!=record) {
                name = parent.getField().getName() + "." + name;
                parent = parent.getParent();
            }
            name = record.getRecordName() + "." + name;
            System.err.printf("%s %s\n",name,message);
        }
        
        private boolean createRecordSupport(DBRecord dbRecord, String supportName) {
            Class supportClass;
            RecordSupport recordSupport = null;
            Class[] argClass = null;
            Object[] args = null;
            Constructor constructor = null;
            try {
                supportClass = Class.forName(supportName);
            }catch (ClassNotFoundException e) {
                System.err.printf("%s recordSupport %s does not exist\n",
                        dbRecord.getRecordName(),supportName);
                return false;
            }
            try {
                argClass = new Class[] {Class.forName("org.epics.ioc.dbAccess.DBRecord")};
            }catch (ClassNotFoundException e) {
                printError(dbRecord,"class DBRecord does not exist");
                return false;
            }
            try {
                args = new Object[] {dbRecord};
                constructor = supportClass.getConstructor(argClass);
            } catch (NoSuchMethodException e) {
                printError(dbRecord,
                        "recordSupport "
                        + supportName
                        + " does not have a valid constructor");
                return false;
            } catch(Exception e) {
                printError(dbRecord,
                        "recordSupport "
                        + supportName
                        + " getConstructor failed "
                        + e.getMessage());
                return false;
            }
            try {
                recordSupport = (RecordSupport)constructor.newInstance(args);
            } catch(Exception e) {
                printError(dbRecord,
                        "recordSupport "
                        + supportName
                        + " constructor failed "
                        + e.getMessage());
                return false;
            }
            return dbRecord.setRecordSupport(recordSupport);
        }
        
        private boolean createStructureSupport(DBStructure dbStructure) {
            boolean result = true;
            String supportName = dbStructure.getStructureSupportName();
            if(supportName!=null) {
                Class supportClass = null;
                RecordSupport structureSupport = null;
                Class[] argClass = null;
                Object[] args = null;
                Constructor constructor = null;
                try {
                    supportClass = Class.forName(supportName);
                }catch (ClassNotFoundException e) {
                    printError(dbStructure,
                            " structureSupport "
                            + supportName +
                            " does not exist");
                    result = false;
                }
                try {
                    argClass = new Class[] {Class.forName("org.epics.ioc.dbAccess.DBStructure")};
                }catch (ClassNotFoundException e) {
                    printError(dbStructure,"class DBStructure does not exist");
                    return false;
                }
                try {
                    args = new Object[] {dbStructure};
                    constructor = supportClass.getConstructor(argClass);
                } catch (NoSuchMethodException e) {
                    printError(dbStructure,
                            "structureSupport "
                            + supportName
                            + " does not have a valid constructor");
                    return false;
                }
                try {
                    structureSupport = (RecordSupport)constructor.newInstance(args);
                } catch(Exception e) {
                    printError(dbStructure,
                            "structureSupport "
                            + supportName
                            + " constructor failed");
                    return false;
                }
                if(!dbStructure.setStructureSupport(structureSupport)) return false;
            }
            DBData[] dbDatas = dbStructure.getFieldDBDatas();
            for(DBData dbData : dbDatas) {
                DBType dbType = dbData.getDBDField().getDBType();
                if(dbType==DBType.dbStructure) {
                    if(!createStructureSupport((DBStructure)dbData)) result = false;
                } else if(dbType==DBType.dbLink) {
                    if(!createLinkSupport((DBLink)dbData)) result = false;
                } else if(dbType==DBType.dbArray) {
                    if(!createArraySupport((DBArray)dbData)) result = false;
                }
            }
            return result;
        }
        
        private boolean createLinkSupport(DBLink dbLink) {
            String supportName = dbLink.getLinkSupportName();
            if(supportName==null) return false;
            Class supportClass;
            LinkSupport linkSupport = null;
            Class[] argClass = null;
            Object[] args = null;
            Constructor constructor = null;
            try {
                supportClass = Class.forName(supportName);
            }catch (ClassNotFoundException e) {
                printError(dbLink,
                        " linkSupport "
                        + supportName +
                        " does not exist");
                return false;
            }
            try {
                argClass = new Class[] {Class.forName("org.epics.ioc.dbAccess.DBLink")};
            }catch (ClassNotFoundException e) {
                printError(dbLink,"class DBLink does not exist");
                return false;
            }
            try {
                args = new Object[] {dbLink};
                constructor = supportClass.getConstructor(argClass);
            } catch (NoSuchMethodException e) {
                printError(dbLink,
                        "linkSupport "
                        + supportName
                        + " does not have a valid constructor");
                return false;
            }
            try {
                linkSupport = (LinkSupport)constructor.newInstance(args);
            } catch(Exception e) {
                printError(dbLink,
                        "linkSupport "
                        + supportName
                        + " constructor failed");
                return false;
            }
            return dbLink.setLinkSupport(linkSupport);
        }
        
        private boolean createArraySupport(DBArray dbArray) {
            boolean result = true;
            DBDAttribute attribute = dbArray.getDBDField().getAttribute();
            DBType elementType = attribute.getElementDBType();
            if(elementType==DBType.dbStructure) {
                DBStructureArray dbStructureArray = (DBStructureArray)dbArray;
                int len = dbStructureArray.getLength();
                DBStructureArrayData data = new DBStructureArrayData();
                int nsofar = 0;
                int offset = 0;
                while(nsofar<len) {
                    int n = dbStructureArray.get(offset,len-nsofar,data);
                    if(n<=0) break;
                    DBStructure[] dbStructures = data.data;
                    for(int i=0; i<n; i++) {
                        if(dbStructures[i]==null) continue;
                        if(!createStructureSupport(dbStructures[i])) result = false;
                    }
                    nsofar += n; offset += n;
                }
            } else if(elementType==DBType.dbLink) {
                DBLinkArray dbLinkArray = (DBLinkArray)dbArray;
                int len = dbLinkArray.getLength();
                LinkArrayData data = new LinkArrayData();
                int nsofar = 0;
                int offset = 0;
                while(nsofar<len) {
                    int n = dbLinkArray.get(offset,len-nsofar,data);
                    if(n<=0) break;
                    DBLink[] dbLink = data.data;
                    for(int i=0; i<n; i++) {
                        if(dbLink[i]==null) continue;
                        if(!createLinkSupport(dbLink[i])) result = false;
                    }
                    nsofar += n; offset += n;
                }
            } else if(elementType==DBType.dbArray) {
                DBArrayArray dbArrayArray = (DBArrayArray)dbArray;
                int len = dbArrayArray.getLength();
                DBArrayArrayData data = new DBArrayArrayData();
                int nsofar = 0;
                int offset = 0;
                while(nsofar<len) {
                    int n = dbArrayArray.get(offset,len-nsofar,data);
                    if(n<=0) break;
                    DBArray[] db = data.data;
                    for(int i=0; i<n; i++) {
                        if(db[i]==null) continue;
                        if(!createArraySupport(db[i])) result = false;
                    }
                    nsofar += n; offset += n;
                }
            }
            return result;
        }
    }
    
    static private class Process implements RecordStateListener,RecordProcess {
        private ProcessDB processDB;
        private DBRecord dbRecord;
        private boolean constructed = true;
        private boolean disabled = false;
        private RecordSupport recordSupport = null;
        
        private boolean active = false;
        private ConcurrentLinkedQueue<ProcessComplete> completionListener =
            new ConcurrentLinkedQueue<ProcessComplete>();
        
        private PVString status = null;
        private PVEnum severity = null;
        
        private String startStatus;
        private String newStatus;
        private int startSeverity;
        private int newSeverity;
        private boolean timeStampSet;
        private TimeStamp timeStamp = new TimeStamp();
        
        private List<RecordProcess> linkedRecordList = new ArrayList<RecordProcess>();
        private List<ProcessComplete> linkedProcessCompleteList = new ArrayList<ProcessComplete>();

        Process(ProcessDB processDB,DBRecord record) {
            this.processDB = processDB;
            dbRecord = record;
            PVData[] pvData = dbRecord.getFieldPVDatas();
            PVData data;
            int index = record.getFieldDBDataIndex("status");
            if(index>=0) {
                data = pvData[index];
                if(data.getField().getType()==Type.pvString)
                    status = (PVString)data;
            }
            index = record.getFieldDBDataIndex("alarmSeverity");
            if(index>=0) {
                data = pvData[index];
                if(data.getField().getType()==Type.pvEnum)
                    severity = (PVEnum)data;
            }
            timeStampSet = false;
            TimeUtility.set(timeStamp,System.currentTimeMillis());
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbAccess.RecordStateListener#newState(org.epics.ioc.dbAccess.RecordState)
         */
        public void newState(RecordState newState) {
            switch(newState) {
            case constructing:
                active = false;
                constructed = false;
                removeAndCallLinkedProcessListeners(ProcessReturn.abort);
                break;
            case constructed:
                constructed = true;
                break;
            case zombie:
                active = false;
                constructed = false;
                removeAndCallLinkedProcessListeners(ProcessReturn.zombie);
                processDB.removeRecordProcess(dbRecord.getRecordName());
                break;
            }
        }
        // Record Process
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcess#isDisabled()
         */
        public boolean isDisabled() {
            dbRecord.lock();
            try {
                boolean result = disabled;
                return result;
            } finally {
                dbRecord.unlock();
            }
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcess#setDisabled(boolean)
         */
        public boolean setDisabled(boolean value) {
            dbRecord.lock();
            try {
                boolean oldValue = disabled;
                disabled = value;
                return (oldValue==value) ? false : true;
            } finally {
                dbRecord.unlock();
            }
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcess#isActive()
         */
        public boolean isActive() {
            dbRecord.lock();
            try {
                return active;
            } finally {
                dbRecord.unlock();
            }
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcess#getRecord()
         */
        public DBRecord getRecord() {
            dbRecord.lock();
            try {
                DBRecord result = dbRecord;
                return result;
            } finally {
                dbRecord.unlock();
            }
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcess#getRecordSupport()
         */
        public RecordSupport getRecordSupport() {
            dbRecord.lock();
            try {
                RecordSupport result = recordSupport;
                return result;
            } finally {
                dbRecord.unlock();
            }
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcess#setRecordSupport(org.epics.ioc.dbProcess.RecordSupport)
         */
        public void setRecordSupport(RecordSupport support) {
            dbRecord.lock();
            try {
                recordSupport = support;
            } finally {
                dbRecord.unlock();
            }
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcess#requestProcess(org.epics.ioc.dbProcess.ProcessComplete)
         */
        public RequestProcessReturn requestProcess(ProcessComplete listener) {
            dbRecord.lock();
            try {
                RecordState recordState = dbRecord.getRecordState();
                if(recordState!=RecordState.constructed) {
                    if(recordState==RecordState.constructing)
                        return RequestProcessReturn.failure;
                    return RequestProcessReturn.zombie;
                }
                if(active) {
                    if(listener!=null) {
                        if(completionListener.add(listener)) {
                            return RequestProcessReturn.listenerAdded;
                        } else {
                            return RequestProcessReturn.failure;
                        }
                    }
                    return RequestProcessReturn.alreadyActive;
                }
                active = true;
                timeStampSet = false;
                return RequestProcessReturn.success;
            } finally {
                dbRecord.unlock();
            }
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcess#process(org.epics.ioc.dbProcess.ProcessComplete)
         */
        public ProcessReturn process(ProcessComplete listener) {
            ProcessReturn result = ProcessReturn.abort;
            int numberLinkedRecords = 0;
            dbRecord.lock();
            try {
                RecordState recordState = dbRecord.getRecordState();
                if(recordState!=RecordState.constructed) {
                    if(recordState==RecordState.constructing)
                        return ProcessReturn.abort;
                    return ProcessReturn.zombie;
                }
                if(isDisabled()) {
                    active = false;
                    return ProcessReturn.abort;
                }
                if(recordSupport==null) {
                    active = false;
                    return ProcessReturn.noop;
                }
                dbRecord.beginSynchronous();
                if(status!=null) startStatus = status.get();
                if(severity!=null) startSeverity = severity.getIndex();
                newStatus = null;
                newSeverity = -1;
                result = recordSupport.process(this);
                numberLinkedRecords = linkedRecordList.size();
                recordSupportDone(result);
                if(result==ProcessReturn.active) {
                    if(listener!=null) completionListener.add(listener);
                } else if(numberLinkedRecords>0) {
                    active = false;
                }
                dbRecord.endSynchronous();
            } finally {
                dbRecord.unlock();
            }
            if(numberLinkedRecords>0) {
                for(int i=0; i < numberLinkedRecords; i++) {
                    RecordProcess linkedRecord = linkedRecordList.get(i);
                    ProcessComplete linkedListener = linkedProcessCompleteList.get(i);
                    ProcessReturn linkedResult = linkedRecord.process(linkedListener);
                    if(linkedResult!=ProcessReturn.active) {
                        linkedListener.complete(linkedResult);
                    }
                }
                linkedRecordList.clear();
                linkedProcessCompleteList.clear();
                if(result!=ProcessReturn.active) {
                    dbRecord.lock();
                    active = false;
                    dbRecord.unlock();
                }
            }
            return result;
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcess#removeCompletionListener(org.epics.ioc.dbProcess.ProcessComplete)
         */
        public void removeCompletionListener(ProcessComplete listener) {
            completionListener.remove(listener);
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcess#requestProcessLinkedRecord(org.epics.ioc.dbAccess.DBRecord, org.epics.ioc.dbProcess.ProcessComplete)
         */
        public RequestProcessReturn requestProcessLinkedRecord(DBRecord record, ProcessComplete listener) {
            RecordProcess linkedRecordProcess = record.getRecordProcess();
            dbRecord.lockOtherRecord(record);
            try {
                RequestProcessReturn result = linkedRecordProcess.requestProcess(listener);
                if(result==RequestProcessReturn.success) {
                    linkedRecordList.add(record.getRecordProcess());
                    linkedProcessCompleteList.add(listener);
                }
                return result;
            } finally {
                record.unlock();
            }
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcess#removeLinkedCompletionListener(org.epics.ioc.dbProcess.ProcessComplete)
         */
        public void removeLinkedCompletionListener(ProcessComplete listener) {
            dbRecord.lock();
            try {
                int index = linkedProcessCompleteList.indexOf(listener);
                linkedRecordList.remove(index);
                linkedProcessCompleteList.remove(index);
            } finally {
                dbRecord.unlock();
            }
            
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcess#recordSupportDone(org.epics.ioc.dbProcess.ProcessReturn)
         */
        public void recordSupportDone(ProcessReturn result) {
            if(newSeverity!=startSeverity) {
                if(newSeverity<0) newSeverity = 0;
                if(severity!=null) severity.setIndex(newSeverity);
            }
            if(newStatus!=startStatus) {
                if(status!=null) status.put(newStatus);
            }
            if(result==ProcessReturn.active) {
                startSeverity = newSeverity;
                startStatus = newStatus;
                newStatus = null;
                newSeverity = -1;
                if(!timeStampSet) TimeUtility.set(timeStamp,System.currentTimeMillis());
                timeStampSet = false;
                callLinkedProcessListeners(result);
            } else {
                removeAndCallLinkedProcessListeners(result);
                active = false;
            }
        }
        

        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcess#setStatusSeverity(java.lang.String, org.epics.ioc.dbProcess.AlarmSeverity)
         */
        public boolean setStatusSeverity(String status, AlarmSeverity alarmSeverity) {
            if(newSeverity<0 || alarmSeverity.ordinal()>newSeverity) {  
                newStatus = status;
                newSeverity = alarmSeverity.ordinal();
                return true;
            }
            return false;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcess#getAlarmSeverity()
         */
        public AlarmSeverity getAlarmSeverity() {
            if(newSeverity<0) return AlarmSeverity.none;
            return AlarmSeverity.getSeverity(newSeverity);
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcess#getStatus()
         */
        public String getStatus() {
            return newStatus;
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcess#setTimeStamp(org.epics.ioc.util.TimeStamp)
         */
        public void setTimeStamp(TimeStamp newTimeStamp) {
            timeStamp.seconds = newTimeStamp.seconds;
            timeStamp.nanoSeconds = newTimeStamp.nanoSeconds;
            timeStampSet = true;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcess#getTimeStamp(org.epics.ioc.util.TimeStamp)
         */
        public void getTimeStamp(TimeStamp timeStamp) {
            timeStamp.seconds = this.timeStamp.seconds;
            timeStamp.nanoSeconds = this.timeStamp.nanoSeconds;
            
        }

        private void callLinkedProcessListeners(ProcessReturn result)
        {
            if(completionListener.isEmpty()) return;
            Iterator<ProcessComplete> iter = completionListener.iterator();
            while(iter.hasNext()) {
                iter.next().complete(result);
            }
        }
    
        private void removeAndCallLinkedProcessListeners(ProcessReturn result)
        {
            if(completionListener.isEmpty()) return;
            ProcessComplete processComplete;
            while((processComplete = completionListener.poll())!=null) {
                processComplete.complete(result);
            }
        }
        
    }
}
