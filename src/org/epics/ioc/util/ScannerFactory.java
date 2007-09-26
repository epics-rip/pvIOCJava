/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.util;

import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.epics.ioc.pv.*;
import org.epics.ioc.db.*;
import org.epics.ioc.process.*;

/**
 * Factory for periodic and event scanning.
 * All methods are thread safe.
 * @author mrk
 *
 */
public class ScannerFactory {
    private static PeriodicScanner periodicScanner = new PeriodicScannerImpl();
    private static EventScanner eventScanner = new EventScannerImpl();

    /**
     * Get the interface for the periodic scanner.
     * @return The interface.
     */
    public static PeriodicScanner getPeriodicScanner() {
        return periodicScanner;
    }
    /**
     * Get the interface for the event scanner.
     * @return The interface.
     */
    public static EventScanner getEventScanner() {
        return eventScanner;
    }
     
    private static String lineBreak = System.getProperty("line.separator");
    private static final int maxNumberConsecutiveActive = 1;

    private static class RecordExecutor implements RecordProcessRequester {
        private String name;
        private RecordProcess recordProcess;
        private DBRecord dbRecord;
        private PVRecord pvRecord;
        private boolean isActive = false;
        private int numberConsecutiveActive = 0;
        private boolean release = false;
        private ReentrantLock lock = new ReentrantLock();
        private Condition waitForNotActive = lock.newCondition();

        private RecordExecutor(String name,RecordProcess recordProcess) {
            this.name = name;
            this.recordProcess = recordProcess;
            dbRecord = recordProcess.getRecord();
            pvRecord = dbRecord.getPVRecord();
        }
        
        private void execute(TimeStamp timeStamp) {
            if(release) return;
            if(isActive) {
                if(++numberConsecutiveActive == maxNumberConsecutiveActive) {
                    dbRecord.lock();
                    try {
                        pvRecord.message("record active too long", MessageType.warning);
                    } finally {
                        dbRecord.unlock();
                    }
                }
            } else {
                isActive = true;
                numberConsecutiveActive = 0;
                recordProcess.process(this, false,timeStamp);
            }
        }
        
        private DBRecord getDBRecord() {
            return dbRecord;
        }
        
        private void release() {
            try {
                lock.lock();
                try {
                    release = true;
                    if(isActive) {
                        waitForNotActive.await();
                    }
                } finally {
                    lock.unlock();
                }
            } catch(InterruptedException e) {}
            recordProcess.releaseRecordProcessRequester(this);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcessRequester#getRecordProcessRequesterName()
         */
        public String getRequesterName() {
            return name;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requester#message(java.lang.String, org.epics.ioc.util.MessageType)
         */
        public void message(String message, MessageType messageType) {
            pvRecord.message(message, messageType);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcessRequester#recordProcessComplete(org.epics.ioc.process.RequestResult)
         */
        public void recordProcessComplete() {
            lock.lock();
            try {
                isActive = false;
                if(release) waitForNotActive.signal();
            } finally {
                lock.unlock();
            }
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcessRequester#recordProcessResult(org.epics.ioc.util.AlarmSeverity, java.lang.String, org.epics.ioc.util.TimeStamp)
         */
        public void recordProcessResult(RequestResult requestResult) {
            // nothing to do.    
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcessRequester#ready()
         */
        public RequestResult ready() {
            throw new IllegalStateException("Why was this called?"); 
        }
    }

    private static class Executor {
        private String name;
        private RecordExecutor[] recordExecutors = new RecordExecutor[0];
        
        private TimeStamp timeStamp = new TimeStamp();
        
        private boolean isActive = false;
        private boolean listModify = false;
        private ReentrantLock lock = new ReentrantLock();
        private Condition waitForNotActive = lock.newCondition();
        private Condition waitForModify = lock.newCondition();

        private Executor(String name) {
            this.name = name;
        }

        private void runList(long startTime) {                      
            try {
                lock.lock();
                try {
                    if(listModify) {
                        waitForModify.await();
                    }
                    isActive = true;
                } finally {
                    lock.unlock();
                }
            } catch(InterruptedException e) {}
            TimeUtility.set(timeStamp,startTime);
            for(int i=0; i< recordExecutors.length; i++) {
                RecordExecutor recordExecutor = recordExecutors[i];
                if(recordExecutor!=null) recordExecutor.execute(timeStamp);
            }
            lock.lock();
            try {
                isActive = false;
                if(listModify) {
                    waitForNotActive.signal();
                }               
            } finally {
                lock.unlock();
            }
        }

        boolean remove(DBRecord dbRecord) {
            RecordExecutor recordExecutor = null;
            int index = 0;
            for(int i=0; i< recordExecutors.length; i++) {
                if(recordExecutors[i].getDBRecord()==dbRecord) {
                    recordExecutor = recordExecutors[i];
                    index = i;
                    break;
                }
            }
            if(recordExecutor==null) return false;
            try {
                lock.lock();
                try {
                    listModify = true;
                    if(isActive) {
                        waitForNotActive.await();
                    }
                    recordExecutors[index] = null;
                    recordExecutor.release();
                    listModify = false;
                    waitForModify.signal();
                } finally {
                    lock.unlock();
                }
            } catch(InterruptedException e) {}
            return true;
        }
        
        void add(RecordExecutor recordExecutor) {
            try {
                lock.lock();
                try {
                    listModify = true;
                    if(isActive) {
                        waitForNotActive.await();
                    }
                    int index = -1;
                    for(int i=0; i< recordExecutors.length; i++) {
                        if(recordExecutors[i]==null) {
                            index = i;
                            break;
                        }
                    }
                    if(index<0) {
                        RecordExecutor[] executors = new RecordExecutor[recordExecutors.length+5];
                        for(int i=0; i< recordExecutors.length; i++) {
                            executors[i] = recordExecutors[i];
                        }
                        for(int i=recordExecutors.length; i<executors.length -1; i++) {
                            executors[i] = null;
                        }
                        index = recordExecutors.length;
                        recordExecutors = executors;
                    }
                    recordExecutors[index] = recordExecutor;
                    listModify = false;
                    waitForModify.signal();
                } finally {
                    lock.unlock();
                }
            } catch(InterruptedException e) {}
        }
    }

    private static class PeriodiocExecutor extends Executor implements Runnable {  
        private long period;
        private Thread thread;
        private boolean isRunning = false;

        private PeriodiocExecutor(String name,long period, int priority) {
            super(name);
            this.period = period;
            thread = new Thread(this,name);
            thread.setPriority(priority);
            thread.start();
            while(!isRunning) {
                try {
                Thread.sleep(1);
                } catch(InterruptedException e) {}
            }
        }

        /* (non-Javadoc)
         * @see java.lang.Runnable#run()
         */
        public void run() {
            isRunning = true;
            try {
                while(true) {
                    long startTime = System.currentTimeMillis();
                    super.runList(startTime);
                    long endTime = System.currentTimeMillis();
                    long delay = period - (endTime - startTime);
                    if(delay<1) delay = 1;
                    Thread.sleep(delay);
                }
            } catch(InterruptedException e) {}
        }
        private long getPeriod() {
            return period;
        }
        private Thread getThread() {
            return thread;
        }
        private String getName() {
            return super.name;
        }
    }

    private static class PeriodNode {
        private long period;
        private LinkedList<PeriodiocExecutor> priorityList = new LinkedList<PeriodiocExecutor>();
        
        PeriodNode(long period) {
            this.period = period;
        }
        LinkedList<PeriodiocExecutor> getPriorityList() {
            return priorityList;
        }
        
        private PeriodiocExecutor getPeriodiocExecutor(int priority,boolean addNew,String name) {
            ListIterator<PeriodiocExecutor> iter = priorityList.listIterator();
            while(iter.hasNext()) {
                PeriodiocExecutor next = iter.next();
                int threadPriority = next.getThread().getPriority();
                if(priority>threadPriority) continue;
                if(priority==threadPriority) return next;
                if(!addNew) return null;
                next = new PeriodiocExecutor(name,period,priority);
                iter.previous();
                iter.add(next);
                return next;
            }
            if(!addNew) return null;
            PeriodiocExecutor periodiocExecutor = new PeriodiocExecutor(name,period,priority);
            priorityList.add(periodiocExecutor);
            return periodiocExecutor;
        }
    }
    
    private static class PeriodicScannerImpl implements PeriodicScanner {
        private LinkedList<PeriodNode> periodList = new LinkedList<PeriodNode>();
        private ReentrantLock lock = new ReentrantLock();
        private long minPeriod = 10;
        private int deltaPeriod = 10;
                
        private PeriodicScannerImpl() {
            String envValue = System.getenv("IOCPeriodicScanPeriodMinimum");
            if(envValue!=null) {
                double value = Double.parseDouble(envValue);
                minPeriod = (long)(value*1000.0);
            }
            envValue = System.getenv("IOCPeriodicScanPeriodDelta");
            if(envValue!=null) {
                double value = Double.parseDouble(envValue);
                deltaPeriod = (int)(value*1000.0);
            }
        }
        
        
        /* (non-Javadoc)
         * @see org.epics.ioc.util.PeriodicScanner#schedule(org.epics.ioc.db.DBRecord)
         */
        public boolean schedule(DBRecord dbRecord) {
            ScanField scanField = ScanFieldFactory.create(dbRecord);
            PVRecord pvRecord = dbRecord.getPVRecord();
            if(scanField==null) {
                pvRecord.message(
                        "PeriodicScanner: ScanFieldFactory.create failed",
                        MessageType.fatalError);;
                        return false;
            }
            double rate = scanField.getRate();
            int priority = scanField.getPriority().getJavaPriority();
            long period = rateToPeriod(rate);
            RecordExecutor recordExecutor = null;
            PeriodiocExecutor executor = null;
            String name = "periodic(" + String.valueOf(period)
            + "," + String.valueOf(priority) + ")";
            lock.lock();
            try {
                PeriodNode periodNode = getPeriodNode(period,true);
                executor = periodNode.getPeriodiocExecutor(priority, true, name);
            } finally {
                lock.unlock();
            }
            RecordProcess recordProcess = dbRecord.getRecordProcess();
            recordExecutor = new RecordExecutor(executor.getName(),recordProcess);
            if(!recordProcess.setRecordProcessRequester(recordExecutor)){
                return false;
            }
            executor.add(recordExecutor);
            return true;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.util.PeriodicScanner#unschedule(org.epics.ioc.db.DBRecord, double, org.epics.ioc.util.ScanPriority)
         */
        public boolean unschedule(DBRecord dbRecord, double rate, ScanPriority scanPriority) {
            int priority = scanPriority.getJavaPriority();
            long period = rateToPeriod(rate);
            PeriodiocExecutor executor = null;
            lock.lock();
            try {
                PeriodNode periodNode = getPeriodNode(period,false);
                if(periodNode!=null) {
                    executor = periodNode.getPeriodiocExecutor(priority, false, null);
                }
            } finally {
                lock.unlock();
            }
            if(executor==null) {
                dbRecord.getPVRecord().message(
                        "PeriodicScanner.unschedule but not in list",
                        MessageType.error);
                return false;
            }
            executor.remove(dbRecord);
            return true;
        }
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() {
            StringBuilder builder = new StringBuilder();
            lock.lock();
            try {
                ListIterator<PeriodNode> iter = periodList.listIterator();
                while(iter.hasNext()) {
                    PeriodNode periodNode = iter.next();
                    ListIterator<PeriodiocExecutor> iter1 = periodNode.priorityList.listIterator();
                    while(iter1.hasNext()) {
                        PeriodiocExecutor next = iter1.next();
                        builder.append(showExecutor(next) + lineBreak);
                    }
                }
            }finally {
                lock.unlock();
            }
            return builder.toString();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.util.PeriodicScanner#show(double, org.epics.ioc.util.ScanPriority)
         */
        public String show(double rate, ScanPriority scanPriority) {
            StringBuilder builder = new StringBuilder();
            int priority = scanPriority.getJavaPriority();
            long period = rateToPeriod(rate);
            lock.lock();
            try {
                PeriodiocExecutor executor = null;
                PeriodNode periodNode = getPeriodNode(period,false);
                if(periodNode!=null) {
                    executor = periodNode.getPeriodiocExecutor(priority, false, null);
                }
                if(executor!=null) {
                    builder.append(showExecutor(executor) + lineBreak);
                }
            }finally {
                lock.unlock();
            }
            return builder.toString();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.util.PeriodicScanner#show(double)
         */
        public String show(double rate) {
            StringBuilder builder = new StringBuilder();
            long period = rateToPeriod(rate);
            lock.lock();
            try {
                PeriodNode periodNode = getPeriodNode(period,false);
                if(periodNode!=null) {
                    ListIterator<PeriodiocExecutor> iter1 = periodNode.priorityList.listIterator();
                    while(iter1.hasNext()) {
                        PeriodiocExecutor next = iter1.next();
                        builder.append(showExecutor(next) + lineBreak);
                    }
                }
            }finally {
                lock.unlock();
            }
            return builder.toString();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.util.PeriodicScanner#show(org.epics.ioc.util.ScanPriority)
         */
        public String show(ScanPriority scanPriority) {
            StringBuilder builder = new StringBuilder();
            int priority = scanPriority.getJavaPriority();
            lock.lock();
            try {
                ListIterator<PeriodNode> iter = periodList.listIterator();
                while(iter.hasNext()) {
                    PeriodNode periodNode = iter.next();
                    PeriodiocExecutor executor = periodNode.getPeriodiocExecutor(priority, false, null);
                    if(executor!=null) {
                        builder.append(showExecutor(executor) + lineBreak);
                    }
                }
            }finally {
                lock.unlock();
            }
            return builder.toString();
        }
        
        private PeriodNode getPeriodNode(long period,boolean addNew) {
            ListIterator<PeriodNode> iter = periodList.listIterator();
            while(iter.hasNext()) {
                PeriodNode next = iter.next();
                if(period>next.period) continue;
                if(period==next.period) return next;
                if(!addNew) return null;
                next = new PeriodNode(period);
                iter.previous();
                iter.add(next);
                return next;
            }
            if(!addNew) return null;
            PeriodNode periodNode = new PeriodNode(period);
            periodList.add(periodNode);
            return periodNode;
        }
        
        private long rateToPeriod(double rate) {
            long period = (long)(rate*1000);
            if(period<minPeriod) period = minPeriod;
            long n = (period - minPeriod)/deltaPeriod;
            period = minPeriod + n*deltaPeriod;
            return period;
        }        

        private String showExecutor(PeriodiocExecutor periodicExecutor) {
            StringBuilder builder = new StringBuilder();
            Executor executor = periodicExecutor;
            RecordExecutor[] recordExecutors = executor.recordExecutors;
            Thread thread = periodicExecutor.getThread();
            builder.append(String.format("thread %s period %d priority %d record list{",
                    thread.getName(),
                    periodicExecutor.getPeriod(),
                    thread.getPriority()));
            for(int i=0; i<recordExecutors.length; i++) {
                RecordExecutor recordExecutor = recordExecutors[i];
                if(recordExecutor==null) continue;
                RecordProcess recordProcess = recordExecutor.recordProcess;
                String name = recordProcess.getRecord().getPVRecord().getRecordName();
                builder.append(lineBreak + "    " + name);
            }
            builder.append(lineBreak + "}");
            return builder.toString();
        }
    }

    private static class EventExecutor extends Executor implements EventAnnounce, Runnable {
        private ReentrantLock lock = new ReentrantLock();
        private Condition waitForWork = lock.newCondition();
        private Thread thread;
        private long startTime = 0;
        private boolean isRunning = false;

        private EventExecutor(String name,int priority) {
            super(name);
            thread = new Thread(this,name);
            thread.setPriority(priority);
            thread.start();
            while(!isRunning) {
                try {
                Thread.sleep(1);
                } catch(InterruptedException e) {}
            }
        }

        public void run() {
            isRunning = true;
            try {
                while(true) {
                    lock.lock();
                    try {
                        waitForWork.await();
                    } finally {
                        lock.unlock();
                    }
                    super.runList(startTime);
                }
            } catch(InterruptedException e) {}
        }
        public void announce() {
            lock.lock();
            try {
                waitForWork.signal();
            } finally {
                lock.unlock();
            }
        }

        private void setStartTime(long startTime) {
            lock.lock();
            try {
                this.startTime = startTime;
            } finally {
                lock.unlock();
            }
        }
        private Thread getThread() {
            return thread;
        }
        private String getName() {
            return super.name;
        }
    }

    private static class Announce implements EventAnnounce, Runnable { 
        // announcerList kept for diagnostic purpose only
        private LinkedList<String> announcerList = new LinkedList<String>();
        
        private EventExecutor[] eventExecutors = new EventExecutor[0];
        
        private boolean isActive = false;
        private boolean listModify = false;
        private ReentrantLock lock = new ReentrantLock();
        private Condition waitForWork = lock.newCondition();
        private Condition waitForModify = lock.newCondition();
        private Condition waitForNotActive = lock.newCondition();
        private String eventName;
        private Thread thread;
        private boolean isRunning = false;

        private Announce(String name) {
            super();
            eventName = name;
            thread = new Thread(this,"event(" + name + ")");
            thread.setPriority(ScanPriority.valueOf("higher").getJavaPriority());
            thread.start();
            while(!isRunning) {
                try {
                Thread.sleep(1);
                } catch(InterruptedException e) {}
            }
        }

        public void run() {
            isRunning = true;
            try {
                while(true) {
                    lock.lock();
                    try {
                        waitForWork.await();
                        if(listModify) {
                            waitForModify.await();
                        }
                        isActive = true;
                    } finally {
                        lock.unlock();
                    }
                    long startTime = System.currentTimeMillis();
                    for(int i=0; i< eventExecutors.length; i++) {
                        EventExecutor eventExecutor = eventExecutors[i];
                        if(eventExecutor==null) continue;
                        eventExecutor.setStartTime(startTime);
                        eventExecutor.announce();
                    }
                    lock.lock();
                    try {
                        isActive = false;
                        if(listModify) {
                            waitForNotActive.signal();
                        }
                    } finally {
                        lock.unlock();
                    }
                }
            } catch(InterruptedException e) {}
        }
        
        public void announce() {
            lock.lock();
            try {
                waitForWork.signal();
            } finally {
                lock.unlock();
            }
        }

        private Thread getThread() {
            return thread;
        }

        private String getEventName() {
            return eventName;
        }

        void addEventExecutor(EventExecutor eventExecutor) {
            try {
                lock.lock();
                try {
                    listModify = true;
                    if(isActive) {
                        waitForNotActive.await();
                    }
                    int index = -1;
                    for(int i=0; i< eventExecutors.length; i++) {
                        if(eventExecutors[i]==null) {
                            index = i;
                            break;
                        }
                    }
                    if(index<0) {
                        EventExecutor[] executors = new EventExecutor[eventExecutors.length+1];
                        for(int i=0; i< eventExecutors.length; i++) {
                            executors[i] = eventExecutors[i];
                        }
                        index = executors.length -1;
                        eventExecutors = executors;
                    }
                    eventExecutors[index] = eventExecutor;
                    listModify = false;
                    waitForModify.signal();
                } finally {
                    lock.unlock();
                }
            } catch(InterruptedException e) {}
        }
        
        boolean removeEventExecutor(EventExecutor eventExecutor) {
            int index = -1;
            for(int i=0; i< eventExecutors.length; i++) {
                if(eventExecutors[i]==eventExecutor) {
                    index = i;
                    break;
                }
            }
            if(index<0) return false;
            try {
                lock.lock();
                try {
                    listModify = true;
                    if(isActive) {
                        waitForNotActive.await();
                    }
                    eventExecutors[index] = null;
                    listModify = false;
                    waitForModify.signal();
                } finally {
                    lock.unlock();
                }
            } catch(InterruptedException e) {}
            return true;
        }
       
        private void addAnnouncer(String name) {
            lock.lock();
            try {
                announcerList.add(name);
            } finally {
                lock.unlock();
            }
        }
        private void removeAnnouncer(String name) {
            lock.lock();
            try {
                announcerList.remove(name);
            } finally {
                lock.unlock();
            }
        }
        private List<String> getAnnouncerList() {
            lock.lock();
            try {
                return (List<String>)announcerList.clone();
            } finally {
                lock.unlock();
            }
        }
    }

    private static class EventScannerImpl implements EventScanner {
        private ReentrantLock lock = new ReentrantLock();
        private ArrayList<Announce> eventAnnouncerList = new ArrayList<Announce>();

        private EventScannerImpl() {}

        private Announce getAnnounce(String name) {
            Announce announce = null;
            ListIterator<Announce> iter = eventAnnouncerList.listIterator();
            while(iter.hasNext()) {
                announce = iter.next();
                int compare = name.compareTo(announce.getEventName());
                if(compare>0) continue;
                if(compare==0) return announce;
                announce = new Announce(name);
                iter.previous();
                iter.add(announce);
                break;
            }
            if(announce==null) {
                announce = new Announce(name);
                eventAnnouncerList.add(announce);
            }
            return announce;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.util.EventScanner#addEventAnnouncer(java.lang.String, java.lang.String)
         */
        public EventAnnounce addEventAnnouncer(String eventName, String announcer) {
            lock.lock();
            try {
                Announce announce = getAnnounce(eventName);
                announce.addAnnouncer(announcer);
                return announce;
            } finally {
                lock.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.util.EventScanner#addRecord(org.epics.ioc.db.DBRecord)
         */
        public boolean addRecord(DBRecord dbRecord) {
            lock.lock();
            try {
                ScanField scanField = ScanFieldFactory.create(dbRecord);
                int priority = scanField.getPriority().getJavaPriority();
                String eventName = scanField.getEventName();
                String threadName = "event(" + eventName + "," + String.valueOf(priority) + ")";
                Announce announce = getAnnounce(eventName);
                EventExecutor eventExecutor = null;
                EventExecutor[] eventExecutors = announce.eventExecutors;
                for(int i=0; i<eventExecutors.length; i++) {
                    EventExecutor eventExecutorNow = eventExecutors[i];
                    int threadPriority = eventExecutorNow.getThread().getPriority();
                    if(priority<threadPriority) continue;
                    if(priority==threadPriority) {
                        eventExecutor = eventExecutorNow;
                        break;
                    }
                }
                if(eventExecutor==null) {
                    eventExecutor = new EventExecutor(threadName,priority);
                    announce.addEventExecutor(eventExecutor);
                }
                RecordProcess recordProcess = dbRecord.getRecordProcess();
                RecordExecutor recordExecutor = new RecordExecutor(eventExecutor.getName(),recordProcess);
                if(!recordProcess.setRecordProcessRequester(recordExecutor)){
                    return false;
                }         
                eventExecutor.add(recordExecutor);
            } finally {
                lock.unlock();
            }
            return true;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.util.EventScanner#removeEventAnnouncer(org.epics.ioc.util.EventAnnounce, java.lang.String)
         */
        public void removeEventAnnouncer(EventAnnounce eventAnnounce, String announcer) {
            lock.lock();
            try {
                Announce announce = (Announce)eventAnnounce;
                announce.removeAnnouncer(announcer);
            } finally {
                lock.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.util.EventScanner#removeRecord(org.epics.ioc.db.DBRecord, java.lang.String, org.epics.ioc.util.ScanPriority)
         */
        public boolean removeRecord(DBRecord dbRecord, String eventName, ScanPriority scanPriority) {
            lock.lock();
            try {
                Announce announce = getAnnounce(eventName);
                int priority = scanPriority.getJavaPriority();
                EventExecutor eventExecutor = null;
                EventExecutor[] eventExecutors = announce.eventExecutors;
                for(int i=0; i<eventExecutors.length; i++) {
                    EventExecutor eventExecutorNow = eventExecutors[i];
                    int threadPriority = eventExecutorNow.getThread().getPriority();
                    if(priority<threadPriority) continue;
                    if(priority==threadPriority) {
                        eventExecutor = eventExecutorNow;
                        break;
                    }
                }
                if(eventExecutor==null || !eventExecutor.remove(dbRecord)) {
                    dbRecord.getPVRecord().message(
                            "EventScanner.removeRecord but not in list",
                            MessageType.error);
                }
            } finally {
                lock.unlock();
            }
            return true;
        }
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() {
            StringBuilder builder = new StringBuilder();
            lock.lock();
            try {
                ListIterator<Announce> iter = eventAnnouncerList.listIterator();
                while(iter.hasNext()) {
                    Announce announce = iter.next();
                    builder.append(showAnnounce(announce));
                }
                return builder.toString();
            } finally {
                lock.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.util.EventScanner#show(java.lang.String)
         */
        public String show(String eventName) {
            lock.lock();
            try {
                StringBuilder builder = new StringBuilder();
                ListIterator<Announce> iter = eventAnnouncerList.listIterator();
                while(iter.hasNext()) {
                    Announce announce = iter.next();
                    if(eventName.equals(announce.getEventName())) {
                        builder.append(showAnnounce(announce));
                    }
                }
                return builder.toString();
            } finally {
                lock.unlock();
            }
        }        
        private String showAnnounce(Announce announce) {
            StringBuilder builder = new StringBuilder();
            Thread thread = announce.getThread();
            builder.append(String.format(
                    "thread %s eventName %s priority %d announcers {",
                    thread.getName(),announce.getEventName(),thread.getPriority()));
            List<String> announcerList = announce.getAnnouncerList();
            ListIterator<String> iter = announcerList.listIterator();
            while(iter.hasNext()) {
                String announcer = iter.next();
                builder.append(announcer);
                if(iter.hasNext()) builder.append(',');
            }
            builder.append("}");
            EventExecutor[] eventExecutors = announce.eventExecutors;
            for(int i=0; i< eventExecutors.length; i++) {
                EventExecutor eventExecutor = eventExecutors[i];
                if(eventExecutor==null) continue;
                thread = eventExecutor.getThread();
                builder.append(String.format(lineBreak + "    thread %s priority %d record list{",
                        thread.getName(),
                        thread.getPriority()));
                
                Executor executor = eventExecutor;
                RecordExecutor[] recordExecutors = executor.recordExecutors;
                for(int j=0; j<recordExecutors.length; j++) {
                    RecordExecutor recordExecutor = recordExecutors[j];
                    if(recordExecutor==null) continue;
                    RecordProcess recordProcess = recordExecutor.recordProcess;
                    String name = recordProcess.getRecord().getPVRecord().getRecordName();
                    builder.append(lineBreak + "        " + name);
                }
                builder.append(lineBreak + "    }");
            }
            builder.append(lineBreak + "}");
            return builder.toString();
        }
    }
}
