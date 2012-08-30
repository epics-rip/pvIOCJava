/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS pvData is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.database;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.epics.pvdata.factory.FieldFactory;
import org.epics.pvdata.factory.PVDataFactory;
import org.epics.pvdata.factory.StandardFieldFactory;
import org.epics.pvdata.misc.Executor;
import org.epics.pvdata.misc.ExecutorFactory;
import org.epics.pvdata.misc.ExecutorNode;
import org.epics.pvdata.misc.LinkedList;
import org.epics.pvdata.misc.LinkedListArray;
import org.epics.pvdata.misc.LinkedListCreate;
import org.epics.pvdata.misc.LinkedListNode;
import org.epics.pvdata.misc.MessageNode;
import org.epics.pvdata.misc.MessageQueue;
import org.epics.pvdata.misc.MessageQueueFactory;
import org.epics.pvdata.misc.ThreadPriority;
import org.epics.pvdata.pv.Field;
import org.epics.pvdata.pv.FieldCreate;
import org.epics.pvdata.pv.MessageType;
import org.epics.pvdata.pv.PVAuxInfo;
import org.epics.pvdata.pv.PVDataCreate;
import org.epics.pvdata.pv.PVString;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.Requester;
import org.epics.pvdata.pv.ScalarType;
import org.epics.pvdata.pv.StandardField;
import org.epics.pvdata.pv.Structure;



/**
 * Factory for PVDatabase.
 * @author mrk
 *
 */
public class PVDatabaseFactory {
    /**
     * Create a PVDatabase.
     * @param name Name for the database.
     * @return PVDatabase interface.
     */
    public static PVDatabase create(String name) {
        if(name.equals("master")) return master;
        if(name.equals("beingInstalled") && beingInstalled!=null) {
            throw new IllegalStateException("beingInstalled already present");
        }
        Database database = new Database(name);
        if(name.equals("beingInstalled")) beingInstalled = database;
        return database;
    }
    /**
     * Get the master database.
     * @return PVDatabase interface.
     */
    public static PVDatabase getMaster() {
        return master;
    }
    /**
     * Get the beingInstalled database.
     * @return The beingInstalled database or null if no database is currently being installed.
     */
    public static PVDatabase getBeingInstalled() {
        return beingInstalled;
    }
    
    private static Database master;
    private static Database beingInstalled = null;
    private static LinkedListCreate<Requester> linkedListCreate = new LinkedListCreate<Requester>();
    
    static {
        master = new Database("master");
        PVDataCreate pvDataCreate = PVDataFactory.getPVDataCreate();
        FieldCreate fieldCreate = FieldFactory.getFieldCreate();
        String prefix = "org.epics.pvioc.";
        StandardField standardField = StandardFieldFactory.getStandardField();
        String[] supportNames = new String[1];
        Field[] supportFields = new Field[1];
        supportNames[0] = "supportFactory";
        supportFields[0] = fieldCreate.createScalar(ScalarType.pvString);
        Structure supportStructure =fieldCreate.createStructure("supportFactory_t", supportNames, supportFields);
        PVAuxInfo pvAuxInfo = null;
        PVString auxValue = null;
        PVStructure pvSupportStructure =  null;
        PVString pvSupportName = null;
        
        Structure structure = standardField.timeStamp();
        PVStructure pvStructure = pvDataCreate.createPVStructure(structure);
        master.addStructure(pvStructure,prefix + "timeStamp");
        
        structure = standardField.display();
        pvStructure = pvDataCreate.createPVStructure(structure);
        master.addStructure(pvStructure,prefix + "display");

        structure = standardField.control();
        pvStructure = pvDataCreate.createPVStructure(structure);
        master.addStructure(pvStructure,prefix + "control");

        structure = standardField.enumerated();
        pvStructure = pvDataCreate.createPVStructure(structure);
        master.addStructure(pvStructure,prefix + "enumerated");
        
        structure = standardField.alarm();
        pvStructure = pvDataCreate.createPVStructure(structure);
        pvAuxInfo = pvStructure.getPVAuxInfo();
        auxValue = (PVString)pvAuxInfo.createInfo("supportFactory", ScalarType.pvString);
        auxValue.put("org.epics.pvioc.alarmSupportFactory");
        master.addStructure(pvStructure,prefix + "alarm");
        pvSupportStructure = pvDataCreate.createPVStructure(supportStructure);
        pvSupportName = pvSupportStructure.getStringField("supportFactory");
        pvSupportName.put("org.epics.pvioc.support.alarm.AlarmSupportFactory");
        master.addStructure(pvSupportStructure,prefix +"alarmSupportFactory");
        
        structure = standardField.enumeratedAlarm();
        pvStructure = pvDataCreate.createPVStructure(structure);
        pvAuxInfo = pvStructure.getPVAuxInfo();
        auxValue = (PVString)pvAuxInfo.createInfo("supportFactory", ScalarType.pvString);
        auxValue.put("org.epics.pvioc.enumeratedAlarmFactory");
        master.addStructure(pvStructure,prefix + "enumeratedAlarm");
        pvSupportStructure = pvDataCreate.createPVStructure(supportStructure);
        pvSupportName = pvSupportStructure.getStringField("supportFactory");
        pvSupportName.put("org.epics.pvioc.support.alarm.EnumeratedAlarmFactory");
        master.addStructure(pvSupportStructure,prefix +"enumeratedAlarmFactory");
        
        structure = standardField.booleanAlarm();
        pvStructure = pvDataCreate.createPVStructure(structure);
        pvAuxInfo = pvStructure.getPVAuxInfo();
        auxValue = (PVString)pvAuxInfo.createInfo("supportFactory", ScalarType.pvString);
        auxValue.put("org.epics.pvioc.booleanAlarmFactory");
        master.addStructure(pvStructure,prefix + "booleanAlarm");
        pvSupportStructure = pvDataCreate.createPVStructure(supportStructure);
        pvSupportName = pvSupportStructure.getStringField("supportFactory");
        pvSupportName.put("org.epics.pvioc.support.alarm.BooleanAlarmFactory");
        master.addStructure(pvSupportStructure,prefix +"booleanAlarmFactory");
        
        structure = standardField.byteAlarm();
        pvStructure = pvDataCreate.createPVStructure(structure);
        pvAuxInfo = pvStructure.getPVAuxInfo();
        auxValue = (PVString)pvAuxInfo.createInfo("supportFactory", ScalarType.pvString);
        auxValue.put("org.epics.pvioc.byteAlarmFactory");
        master.addStructure(pvStructure,prefix + "byteAlarm");
        pvSupportStructure = pvDataCreate.createPVStructure(supportStructure);
        pvSupportName = pvSupportStructure.getStringField("supportFactory");
        pvSupportName.put("org.epics.pvioc.support.alarm.ByteAlarmFactory");
        master.addStructure(pvSupportStructure,prefix +"byteAlarmFactory");
        
        structure = standardField.shortAlarm();
        pvStructure = pvDataCreate.createPVStructure(structure);
        pvAuxInfo = pvStructure.getPVAuxInfo();
        auxValue = (PVString)pvAuxInfo.createInfo("supportFactory", ScalarType.pvString);
        auxValue.put("org.epics.pvioc.shortAlarmFactory");
        master.addStructure(pvStructure,prefix + "shortAlarm");
        pvSupportStructure = pvDataCreate.createPVStructure(supportStructure);
        pvSupportName = pvSupportStructure.getStringField("supportFactory");
        pvSupportName.put("org.epics.pvioc.support.alarm.ShortAlarmFactory");
        master.addStructure(pvSupportStructure,prefix +"shortAlarmFactory");
        
        structure = standardField.intAlarm();
        pvStructure = pvDataCreate.createPVStructure(structure);
        pvAuxInfo = pvStructure.getPVAuxInfo();
        auxValue = (PVString)pvAuxInfo.createInfo("supportFactory", ScalarType.pvString);
        auxValue.put("org.epics.pvioc.intAlarmFactory");
        master.addStructure(pvStructure,prefix + "intAlarm");
        pvSupportStructure = pvDataCreate.createPVStructure(supportStructure);
        pvSupportName = pvSupportStructure.getStringField("supportFactory");
        pvSupportName.put("org.epics.pvioc.support.alarm.IntAlarmFactory");
        master.addStructure(pvSupportStructure,prefix +"intAlarmFactory");
        
        structure = standardField.longAlarm();
        pvStructure = pvDataCreate.createPVStructure(structure);
        pvAuxInfo = pvStructure.getPVAuxInfo();
        auxValue = (PVString)pvAuxInfo.createInfo("supportFactory", ScalarType.pvString);
        auxValue.put("org.epics.pvioc.longAlarmFactory");
        master.addStructure(pvStructure,prefix + "longAlarm");
        pvSupportStructure = pvDataCreate.createPVStructure(supportStructure);
        pvSupportName = pvSupportStructure.getStringField("supportFactory");
        pvSupportName.put("org.epics.pvioc.support.alarm.LongAlarmFactory");
        master.addStructure(pvSupportStructure,prefix +"longAlarmFactory");
        
        structure = standardField.floatAlarm();
        pvStructure = pvDataCreate.createPVStructure(structure);
        pvAuxInfo = pvStructure.getPVAuxInfo();
        auxValue = (PVString)pvAuxInfo.createInfo("supportFactory", ScalarType.pvString);
        auxValue.put("org.epics.pvioc.floatAlarmFactory");
        master.addStructure(pvStructure,prefix + "floatAlarm");
        pvSupportStructure = pvDataCreate.createPVStructure(supportStructure);
        pvSupportName = pvSupportStructure.getStringField("supportFactory");
        pvSupportName.put("org.epics.pvioc.support.alarm.FloatAlarmFactory");
        master.addStructure(pvSupportStructure,prefix +"floatAlarmFactory");
        
        structure = standardField.doubleAlarm();
        pvStructure = pvDataCreate.createPVStructure(structure);
        pvAuxInfo = pvStructure.getPVAuxInfo();
        auxValue = (PVString)pvAuxInfo.createInfo("supportFactory", ScalarType.pvString);
        auxValue.put("org.epics.pvioc.doubleAlarmFactory");
        master.addStructure(pvStructure,prefix + "doubleAlarm");
        pvSupportStructure = pvDataCreate.createPVStructure(supportStructure);
        pvSupportName = pvSupportStructure.getStringField("supportFactory");
        pvSupportName.put("org.epics.pvioc.support.alarm.DoubleAlarmFactory");
        master.addStructure(pvSupportStructure,prefix +"doubleAlarmFactory");
    }
    
    private static class Database implements PVDatabase,Runnable {
        private String name;
        private TreeMap<String,PVStructure> structureMap = new TreeMap<String,PVStructure>();
        private boolean isMaster = false;
        private LinkedHashMap<String,PVRecord> recordMap = new LinkedHashMap<String,PVRecord>();
        private ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
        private LinkedList<Requester> messageRequesterList = linkedListCreate.create();
        private LinkedListArray<Requester> messageRequesterArray = linkedListCreate.createArray();
        // following are only used by master
        private static final int messageQueueSize = 300;
        private MessageQueue messageQueue = MessageQueueFactory.create(messageQueueSize);
        private Executor executor = null;
        private ExecutorNode executorNode = null;
        
        private Database(String name) {
            this.name = name;
            if(name.equals("master")) {
                isMaster = true;
                executor = ExecutorFactory.create(
                        "PVDatabaseMessage", ThreadPriority.lowest);
                executorNode = executor.createNode(this);
            }
        }
        /* (non-Javadoc)
         * @see org.epics.pvdata.pv.Requester#getRequesterName()
         */
        public String getRequesterName() {
            return name;
        }
        /* (non-Javadoc)
         * @see org.epics.pvdata.pv.PVDatabase#addRecord(org.epics.pvdata.pv.PVRecord)
         */
        public boolean addRecord(PVRecord record) {
            rwLock.writeLock().lock();
            try {
                String key = record.getRecordName();
                if(recordMap.containsKey(key)) {
                    message("record already exists",MessageType.warning);
                    return false;
                }
                if(this!=master && master.findRecord(key)!=null) {
                    message("record already exists in master",MessageType.warning);
                    return false;
                }
                recordMap.put(key,record);
            } finally {
                rwLock.writeLock().unlock();
            }
            if(isMaster) {
                record.addRequester(this);
            }
            return true;
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.database.PVDatabase#addStructure(org.epics.pvdata.pv.PVStructure, java.lang.String)
         */
        @Override
        public boolean addStructure(PVStructure pvStructure, String structureName) {
            rwLock.writeLock().lock();
            try {
                String key = structureName;
                if(structureMap.containsKey(key)) return false;
                if(this!=master && master.findStructure(key)!=null) return false;
                structureMap.put(key,pvStructure);
                return true;
            } finally {
                rwLock.writeLock().unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.pvdata.pv.PVDatabase#findRecord(java.lang.String)
         */
        public PVRecord findRecord(String recordName) {
            rwLock.readLock().lock();
            try {
                PVRecord record = null;
                record = recordMap.get(recordName);
                if(record==null && this!=master) record = master.findRecord(recordName);
                return record;
            } finally {
                rwLock.readLock().unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.pvdata.pv.PVDatabase#findStructure(java.lang.String)
         */
        public PVStructure findStructure(String structureName) {
            rwLock.readLock().lock();
            try {
                PVStructure pvStructure = null;
                pvStructure = structureMap.get(structureName);
                if(pvStructure==null && this!=master) {
                    pvStructure = master.findStructure(structureName);
                }
                return pvStructure;
            } finally {
                rwLock.readLock().unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.pvdata.pv.PVDatabase#getMaster()
         */
        public PVDatabase getMaster() {
            return master;
        }
        /* (non-Javadoc)
         * @see org.epics.pvdata.pv.PVDatabase#getName()
         */
        public String getName() {
            return name;
        }
        /* (non-Javadoc)
         * @see org.epics.pvdata.pv.PVDatabase#getRecordNames()
         */
        public String[] getRecordNames() {
            rwLock.readLock().lock();
            try {
                String[] array = new String[recordMap.size()];
                int index = 0;
                Set<String> keys = recordMap.keySet();
                for(String key: keys) {
                    array[index++] = key;
                }
                return array;
            } finally {
                rwLock.readLock().unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.pvdata.pv.PVDatabase#getRecords()
         */
        public PVRecord[] getRecords() {
            rwLock.readLock().lock();
            try {
                PVRecord[] array = new PVRecord[recordMap.size()];
                recordMap.values().toArray(array);
                return array;
            } finally {
                rwLock.readLock().unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.pvdata.pv.PVDatabase#getStructureNames()
         */
        public String[] getStructureNames() {
            rwLock.readLock().lock();
            try {
                String[] array = new String[structureMap.size()];
                int index = 0;
                Set<String> keys = structureMap.keySet();
                for(String key: keys) {
                    array[index++] = key;
                }
                return array;
            } finally {
                rwLock.readLock().unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.pvdata.pv.PVDatabase#getStructures()
         */
        public PVStructure[] getStructures() {
            rwLock.readLock().lock();
            try {
                PVStructure[] array = new PVStructure[structureMap.size()];
                structureMap.values().toArray(array);
                return array;
            } finally {
                rwLock.readLock().unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.pvdata.pv.PVDatabase#mergeIntoMaster()
         */
        public void mergeIntoMaster() {
            if(getMaster()==this) return;
            rwLock.writeLock().lock();
            try {
                master.merge(structureMap,recordMap);
                structureMap.clear();
                recordMap.clear();
                if(name.equals("beingInstalled")) PVDatabaseFactory.beingInstalled = null;
            } finally {
                rwLock.writeLock().unlock();
            }
        }
        
        // merge allows master to be locked once
        private void merge(
                TreeMap<String,PVStructure> structure,
                LinkedHashMap<String,PVRecord> from)
        {
            
            rwLock.writeLock().lock();
            try {
                Set<Map.Entry<String, PVStructure>> set0 = structure.entrySet();
                for(Map.Entry<String,PVStructure> entry : set0) {
                    structureMap.put(entry.getKey(), entry.getValue());
                }
                Set<Map.Entry<String, PVRecord>> set1 = from.entrySet();
                for(Map.Entry<String,PVRecord> entry : set1) {
                    String key = entry.getKey();
                    PVRecord pvRecord = from.get(key);
                    pvRecord.addRequester(this);
                    recordMap.put(key,pvRecord);
                }
            } finally {
                rwLock.writeLock().unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.pvdata.pv.PVDatabase#recordList(java.lang.String)
         */
        public String[] recordList(String regularExpression) {
            ArrayList<String> list = new ArrayList<String>();
            if(regularExpression==null) regularExpression = ".*";
            Pattern pattern;
            try {
                pattern = Pattern.compile(regularExpression);
            } catch (PatternSyntaxException e) {
                return new String[0];
            }
            rwLock.readLock().lock();
            try {
                Set<Map.Entry<String,PVRecord>> recordSet = recordMap.entrySet();
                Iterator<Map.Entry<String,PVRecord>> iter = recordSet.iterator();
                while(iter.hasNext()) {
                    Map.Entry<String,PVRecord> entry = iter.next();
                    String name = entry.getKey();
                    if(pattern.matcher(name).matches()) {
                        list.add(name);
                    }
                }
                String[] result = new String[list.size()];
                for(int i=0; i< list.size(); i++) result[i] = list.get(i);
                return result;
            } finally {
                rwLock.readLock().unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.pvdata.pv.PVDatabase#recordToString(java.lang.String)
         */
        public String recordToString(String regularExpression) {
            StringBuilder result = new StringBuilder();
            if(regularExpression==null) regularExpression = ".*";
            Pattern pattern;
            try {
                pattern = Pattern.compile(regularExpression);
            } catch (PatternSyntaxException e) {
                return "PatternSyntaxException: " + e;
            }
            rwLock.readLock().lock();
            try {
                Set<Map.Entry<String,PVRecord>> recordSet = recordMap.entrySet();
                Iterator<Map.Entry<String,PVRecord>> iter = recordSet.iterator();
                while(iter.hasNext()) {
                    Map.Entry<String,PVRecord> entry = iter.next();
                    String name = entry.getKey();
                    if(pattern.matcher(name).matches()) {
                        result.append(String.format("%nrecord %s%s",name,entry.getValue().toString()));
                    }
                }
                return result.toString();
            } finally {
                rwLock.readLock().unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.pvdata.pv.PVDatabase#removeRecord(org.epics.pvdata.pv.PVRecord)
         */
        public boolean removeRecord(PVRecord record) {
            if(isMaster) {
                record.removeRequester(this);
            }
            rwLock.writeLock().lock();
            try {
                String key = record.getRecordName();
                if(recordMap.remove(key)!=null) return true;
                return false;
            } finally {
                rwLock.writeLock().unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.database.PVDatabase#removeStructure(java.lang.String)
         */
        @Override
        public boolean removeStructure(String structureName) {
            rwLock.writeLock().lock();
            try {
                if(structureMap.remove(structureName)!=null) return true;
                return false;
            } finally {
                rwLock.writeLock().unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.pvdata.pv.PVDatabase#structureList(java.lang.String)
         */
        public String[] structureList(String regularExpression) {
            ArrayList<String> list = new ArrayList<String>();
            if(regularExpression==null) regularExpression = ".*";
            Pattern pattern;
            try {
                pattern = Pattern.compile(regularExpression);
            } catch (PatternSyntaxException e) {
                return new String[0];
            }
            rwLock.readLock().lock();
            try {
                Set<Map.Entry<String,PVStructure>> recordSet = structureMap.entrySet();
                Iterator<Map.Entry<String,PVStructure>> iter = recordSet.iterator();
                while(iter.hasNext()) {
                    Map.Entry<String,PVStructure> entry = iter.next();
                    String name = entry.getKey();
                    if(pattern.matcher(name).matches()) {
                        list.add(name);
                    }
                }
                String[] result = new String[list.size()];
                for(int i=0; i< list.size(); i++) result[i] = list.get(i);
                return result;
            } finally {
                rwLock.readLock().unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.pvdata.pv.PVDatabase#structureToString(java.lang.String)
         */
        public String structureToString(String regularExpression) {
            StringBuilder result = new StringBuilder();
            if(regularExpression==null) regularExpression = ".*";
            Pattern pattern;
            try {
                pattern = Pattern.compile(regularExpression);
            } catch (PatternSyntaxException e) {
                return "PatternSyntaxException: " + e;
            }
            rwLock.readLock().lock();
            try {
                Set<String> keys = structureMap.keySet();
                for(String key: keys) {
                    PVStructure pvStructure = structureMap.get(key);
                    if(pattern.matcher(key).matches()) {
                        result.append(" " + pvStructure.toString());
                    }
                }
                return result.toString();
            } finally {
                rwLock.readLock().unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.pvdata.pv.PVDatabase#addRequester(org.epics.pvdata.pv.Requester)
         */
        public void addRequester(Requester requester) {
            synchronized(messageRequesterList){
                if(messageRequesterList.contains(requester)) {
                    requester.message("already on requesterList", MessageType.warning);
                }
                LinkedListNode<Requester> listNode = linkedListCreate.createNode(requester);
                messageRequesterList.addTail(listNode);
                messageRequesterArray.setNodes(messageRequesterList);
            }
        }
        /* (non-Javadoc)
         * @see org.epics.pvdata.pv.PVDatabase#removeRequester(org.epics.pvdata.pv.Requester)
         */
        public void removeRequester(Requester requester) {
            synchronized(messageRequesterList){
                LinkedListNode<Requester> listNode = messageRequesterList.getHead();
                while(listNode!=null) {
                    Requester req = (Requester)listNode.getObject();
                    if(req==requester) {
                        messageRequesterList.remove(listNode);
                        messageRequesterArray.setNodes(messageRequesterList);
                        return;
                    }
                    listNode = messageRequesterList.getNext(listNode);
                }
                return;
            }
        }
        /* (non-Javadoc)
         * @see org.epics.pvdata.pv.PVDatabase#message(java.lang.String, org.epics.pvdata.pv.MessageType)
         */
        public void message(String message, MessageType messageType) {
            if(!isMaster) {
                LinkedListNode<Requester>[] listNodes = null;
                int length = 0;
                synchronized(messageRequesterList) {
                    listNodes = messageRequesterArray.getNodes();
                    length = messageRequesterArray.getLength();
                }
                if(length<=0) {
                    PrintStream printStream;
                    if(messageType==MessageType.info) {
                        printStream = System.out;
                    } else {
                        printStream = System.err;
                    }
                    printStream.println(messageType.toString() + " " + message);

                } else {
                    for(int i=0; i<length; i++) {
                        LinkedListNode<Requester> listNode = listNodes[i];
                        Requester requester = listNode.getObject();
                        requester.message(message, messageType);
                    }
                }
                return;
            }
            boolean execute = false;
            synchronized(messageQueue) {
                if(messageQueue.isEmpty()) execute = true;
                messageQueue.put(message, messageType,true);
            }
            if(execute) {
                executor.execute(executorNode);
            }
        }
        
        /* (non-Javadoc)
         * @see java.lang.Runnable#run()
         */
        public void run() { // handles messages
            while(true) {
                String message = null;
                MessageType messageType = null;
                int numOverrun = 0;
                synchronized(messageQueue) {
                    MessageNode messageNode = messageQueue.get();
                    numOverrun = messageQueue.getClearOverrun();
                    if(messageNode==null) break;
                    message = messageNode.message;
                    messageType = messageNode.messageType;
                    messageNode.message = null;
                }
                LinkedListNode<Requester>[] listNodes = null;
                int length = 0;
                synchronized(messageRequesterList) {
                    listNodes = messageRequesterArray.getNodes();
                    length = messageRequesterArray.getLength();
                }
                if(length<=0) {
                    PrintStream printStream;
                    if(messageType==MessageType.info) {
                        printStream = System.out;
                    } else {
                        printStream = System.err;
                    }
                    if(numOverrun>0) {
                        System.err.println(MessageType.error.toString() + " " + numOverrun + " dropped messages ");
                    }
                    if(message!=null) {
                        printStream.println(messageType.toString() + " " + message);
                    }

                } else {
                    for(int i=0; i<length; i++) {
                        LinkedListNode<Requester> listNode = listNodes[i];
                        Requester requester = listNode.getObject();
                        requester.message(message, messageType);
                        if(numOverrun>0) {
                            requester.message(numOverrun + " dropped messages",MessageType.error);
                        }
                    }
                }
            }
        }
    }
}
