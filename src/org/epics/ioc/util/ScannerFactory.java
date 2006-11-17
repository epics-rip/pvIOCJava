/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.util;

import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.epics.ioc.dbAccess.*;
import org.epics.ioc.dbProcess.*;

/**
 * Factory for periodic and event scanning.
 * All methods are thread safe.
 * @author mrk
 *
 */
public class ScannerFactory {
     private static PeriodicScanner periodicScanner = new Periodic();
     private static EventScanner eventScanner = new Event();
     
     public static PeriodicScanner getPeriodicScanner() {
         return periodicScanner;
     }
     public static EventScanner getEventScanner() {
         return eventScanner;
     }
     
     private static String lineBreak = System.getProperty("line.separator");
     private static final int maxNumberConsecutiveActive = 10;
     
     private static class RecordExecutor implements RecordProcessRequestor {
         private String name;
         private RecordProcess recordProcess;
         private DBRecord dbRecord;
         private boolean isActive = false;
         private int numberConsecutiveActive = 0;
         
         private RecordExecutor(String name,RecordProcess recordProcess) {
             this.name = name;
             this.recordProcess = recordProcess;
             dbRecord = recordProcess.getRecord();   
         }
         void execute(TimeStamp timeStamp) {
             if(isActive) {
                 if(++numberConsecutiveActive == maxNumberConsecutiveActive) {
                     dbRecord.lock();
                     try {
                         dbRecord.message("record active too long", MessageType.warning);
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
         /* (non-Javadoc)
          * @see org.epics.ioc.dbProcess.RecordProcessRequestor#getRecordProcessRequestorName()
          */
         public String getRequestorName() {
             return name;
         }

         /* (non-Javadoc)
         * @see org.epics.ioc.util.Requestor#message(java.lang.String, org.epics.ioc.util.MessageType)
         */
        public void message(String message, MessageType messageType) {
            dbRecord.message(message, messageType);
        }
        /* (non-Javadoc)
          * @see org.epics.ioc.dbProcess.RecordProcessRequestor#recordProcessComplete(org.epics.ioc.dbProcess.RequestResult)
          */
         public void recordProcessComplete() {
             isActive = false;
         }

         /* (non-Javadoc)
          * @see org.epics.ioc.dbProcess.RecordProcessRequestor#recordProcessResult(org.epics.ioc.util.AlarmSeverity, java.lang.String, org.epics.ioc.util.TimeStamp)
          */
         public void recordProcessResult(RequestResult requestResult) {
             // nothing to do.    
         }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcessRequestor#ready()
         */
        public RequestResult ready() {
            throw new IllegalStateException("Why was this called?"); 
        }
     }
     
     private static class Executor {
         private String name;
         private RecordExecutor[] recordExecutors = new RecordExecutor[0];
         private ArrayList<RecordProcess> processList = new ArrayList<RecordProcess>();
         private boolean listModified = false;
         private ReentrantLock lock = new ReentrantLock();
         private TimeStamp timeStamp = new TimeStamp();
         
         private Executor(String name) {
             this.name = name;
         }
         
         private void runList(long startTime) {
             lock.lock();
             try {                
                 if(listModified) {
                     for(int i=0; i< recordExecutors.length; i++) {
                         RecordExecutor recordExecutor = recordExecutors[i];
                         RecordProcess recordProcess = recordExecutor.recordProcess;
                         recordProcess.releaseRecordProcessRequestor(recordExecutor);
                     }
                     recordExecutors = new RecordExecutor[processList.size()];
                     int nextGood = 0;
                     for(int i=0; i< recordExecutors.length; i++) {
                         RecordProcess recordProcess = processList.get(i);
                         RecordExecutor recordExecutor = new RecordExecutor(name,recordProcess);
                         if(recordProcess.setRecordProcessRequestor(recordExecutor)) {
                             recordExecutors[nextGood++] = recordExecutor;
                         }
                     }
                     if(nextGood<recordExecutors.length) {
                         RecordExecutor[] temp = new RecordExecutor[nextGood];
                         processList.clear();
                         for(int i=0; i< temp.length; i++) {
                             temp[i] = recordExecutors[i];
                             processList.add(temp[i].recordProcess);
                         }
                         recordExecutors = temp;
                     }
                     listModified = false;
                 }
             } finally {
                 lock.unlock();
             }
             TimeUtility.set(timeStamp,startTime);
             for(int i=0; i< recordExecutors.length; i++) {
                 RecordExecutor recordExecutor = recordExecutors[i];
                 recordExecutor.execute(timeStamp);
             }
         }
         
         private void setList(ArrayList<RecordProcess> list) {
             lock.lock();
             try {
               processList = list;
               listModified = true;
             } finally {
                 lock.unlock();
             }
         }
         private ArrayList<RecordProcess> getList() {
             lock.lock();
             try {
                 return (ArrayList<RecordProcess>)processList.clone();
             } finally {
                 lock.unlock();
             }
         }

        
     }
     
     private static class PeriodiocExecutor extends Executor implements Runnable {  
         private long period;
         private Thread thread;
         
         private PeriodiocExecutor(String name,long period, int priority) {
             super(name);
             this.period = period;
             thread = new Thread(this,name);
             thread.setPriority(priority);
             thread.start();
         }
         
         public void run() {
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
         private void setList(ArrayList<RecordProcess> list) {
             super.setList(list);
         }
         private ArrayList<RecordProcess> getList() {
             return super.getList();
         }
     }
     
     private static class Periodic implements PeriodicScanner {
         private LinkedList<PeriodiocExecutor> executorList = new LinkedList<PeriodiocExecutor>();
         private ReentrantLock lock = new ReentrantLock();
         private long minPeriod = 10;
         private int deltaPeriod = 10;
         
         private Periodic() {
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
         
         public void schedule(DBRecord dbRecord) {
             ScanField scanField = ScanFieldFactory.create(dbRecord);
             if(scanField==null) {
                 dbRecord.message(
                     "PeriodicScanner: ScanFieldFactory.create failed",
                     MessageType.fatalError);;
                 return;
             }
             double rate = scanField.getRate();
             int priority = scanField.getPriority().getJavaPriority();
             long period = rateToPeriod(rate);
             lock.lock();
             try {
                 ListIterator<PeriodiocExecutor> iter = executorList.listIterator();
                 String name = "periodic(" + String.valueOf(period)
                     + "," + String.valueOf(priority) + ")";
                 PeriodiocExecutor executor = null;
                 outer:
                 while(iter.hasNext()) {
                     PeriodiocExecutor executorNext = iter.next();
                     long executorPeriod = executorNext.getPeriod();
                     if(period>executorPeriod) continue;
                     if(period<executorPeriod) {
                         executor = new PeriodiocExecutor(name,period,priority);
                         iter.previous();
                         iter.add(executor);
                         break;
                     }
                     // found correct period. Now look for priority
                     iter.previous();
                     while(iter.hasNext()) {
                         executorNext = iter.next();
                         executorPeriod = executorNext.getPeriod();
                         if(period<executorPeriod) {
                             executor = new PeriodiocExecutor(name,period,priority);
                             iter.previous();
                             iter.add(executor);
                             break;
                         }
                         int executorPriority = executorNext.getThread().getPriority();
                         if(priority>executorPriority) continue;
                         if(priority==executorPriority) {
                             executor = executorNext;
                             break outer;
                         }
                         executor = new PeriodiocExecutor(name,period,priority);
                         iter.previous();
                         iter.add(executor);
                         break outer;
                     }
                 }
                 if(executor==null) {
                     executor = new PeriodiocExecutor(name,period,priority);
                     executorList.add(executor);
                 }
                 ArrayList<RecordProcess> processList = executor.getList();
                 processList.add(dbRecord.getRecordProcess());
                 executor.setList(processList);
             } finally {
                 lock.unlock();
             }
         }
         public void unschedule(DBRecord dbRecord) {
             ScanField scanField = ScanFieldFactory.create(dbRecord);
             double rate = scanField.getRate();
             int priority = scanField.getPriority().getJavaPriority();
             long period = rateToPeriod(rate);
             lock.lock();
             try {
                 ListIterator<PeriodiocExecutor> iter = executorList.listIterator();
                 PeriodiocExecutor executor = null;
                 while(iter.hasNext()) {
                     PeriodiocExecutor executorNext = iter.next();
                     long executorPeriod = executorNext.getPeriod();
                     int executorPriority = executorNext.getThread().getPriority();
                     if(period==executorPeriod && priority==executorPriority) {
                         executor = executorNext;
                         break;
                     }
                 }
                 if(executor==null) return;
                 RecordProcess recordProcess = dbRecord.getRecordProcess();
                 ArrayList<RecordProcess> processList = executor.getList();
                 processList.remove(recordProcess);
                 executor.setList(processList);
             } finally {
                 lock.unlock();
             }
         }
               
        public String toString() {
            StringBuilder builder = new StringBuilder();
            lock.lock();
            try {
                ListIterator<PeriodiocExecutor> iter = executorList.listIterator();
                while(iter.hasNext()) {
                    PeriodiocExecutor executor = iter.next();
                    builder.append(showExecutor(executor) + lineBreak);
                }
            }finally {
                lock.unlock();
            }
            return builder.toString();
        }
        public String show(double rate, ScanPriority scanPriority) {
            StringBuilder builder = new StringBuilder();
            int priority = scanPriority.getJavaPriority();
            long period = rateToPeriod(rate);
            lock.lock();
            try {
                ListIterator<PeriodiocExecutor> iter = executorList.listIterator();
                while(iter.hasNext()) {
                    PeriodiocExecutor executor = iter.next();
                    long executorPeriod = executor.getPeriod();
                    int executorPriority = executor.getThread().getPriority();
                    if(priority==executorPriority && period==executorPeriod) {
                        builder.append(showExecutor(executor) + lineBreak);
                    }
                    
                }
            }finally {
                lock.unlock();
            }
            return builder.toString();
        }

        public String show(double rate) {
            StringBuilder builder = new StringBuilder();
            long period = rateToPeriod(rate);
            lock.lock();
            try {
                ListIterator<PeriodiocExecutor> iter = executorList.listIterator();
                while(iter.hasNext()) {
                    PeriodiocExecutor executor = iter.next();
                    long executorPeriod = executor.getPeriod();
                    if(period==executorPeriod) {
                        builder.append(showExecutor(executor) + lineBreak);
                    }
                    
                }
            }finally {
                lock.unlock();
            }
            return builder.toString();
        }

        public String show(ScanPriority scanPriority) {
            StringBuilder builder = new StringBuilder();
            int priority = scanPriority.getJavaPriority();
            lock.lock();
            try {
                ListIterator<PeriodiocExecutor> iter = executorList.listIterator();
                while(iter.hasNext()) {
                    PeriodiocExecutor executor = iter.next();
                    int executorPriority = executor.getThread().getPriority();
                    if(priority==executorPriority) {
                        builder.append(showExecutor(executor) + lineBreak);
                    }
                    
                }
            }finally {
                lock.unlock();
            }
            return builder.toString();
        }
        
        private long rateToPeriod(double rate) {
            long period = (long)(rate*1000);
            if(period<minPeriod) period = minPeriod;
            long n = (period - minPeriod)/deltaPeriod;
            period = minPeriod + n*deltaPeriod;
            return period;
        }        
        
       private String showExecutor(PeriodiocExecutor executor) {
           StringBuilder builder = new StringBuilder();
           List<RecordProcess> processList = executor.getList();
           Thread thread = executor.getThread();
           builder.append(String.format("thread %s period %d priority %d record list{",
                   thread.getName(),
                   executor.getPeriod(),
                   thread.getPriority()));
           ListIterator<RecordProcess> iter = processList.listIterator();
           while(iter.hasNext()) {
               RecordProcess recordProcess = iter.next();
               String name = recordProcess.getRecord().getRecordName();
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
         
         private EventExecutor(String name,int priority) {
             super(name);
             thread = new Thread(this,name);
             thread.setPriority(priority);
             thread.start();
         }
         
         public void run() {
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
         private void setList(ArrayList<RecordProcess> list) {
             super.setList(list);
         }
         private ArrayList<RecordProcess> getList() {
             return super.getList();
         }
     }
     
     private static class Announce implements EventAnnounce, Runnable {  
         private LinkedList<String> announcerList = new LinkedList<String>();
         private ArrayList<EventExecutor> workingList = new ArrayList<EventExecutor>();
         private ArrayList<EventExecutor> eventExecutorList = new ArrayList<EventExecutor>();
         private boolean listModified = false;
         private ReentrantLock lock = new ReentrantLock();
         private Condition waitForWork = lock.newCondition();
         private String eventName;
         private Thread thread;
         
         private Announce(String name) {
             super();
             eventName = name;
             thread = new Thread(this,"event(" + name + ")");
             thread.setPriority(ScanPriority.valueOf("higher").getJavaPriority());
             thread.start();
         }
         
         public void run() {
             try {
                 while(true) {
                     lock.lock();
                     try {
                         waitForWork.await();
                         if(listModified) {
                             workingList = new ArrayList<EventExecutor>(eventExecutorList);
                             listModified = false;
                         }
                     } finally {
                         lock.unlock();
                     }
                     long startTime = System.currentTimeMillis();
                     ListIterator<EventExecutor> iter = workingList.listIterator();
                     while(iter.hasNext()) {
                         EventExecutor eventExecutor = iter.next();
                         eventExecutor.setStartTime(startTime);
                         eventExecutor.announce();
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
         
         private void setEventExecutorList(ArrayList<EventExecutor> list) {
             lock.lock();
             try {
               eventExecutorList = list;
               listModified = true;
             } finally {
                 lock.unlock();
             }
         }
         private ArrayList<EventExecutor> getEventExecutorList() {
             lock.lock();
             try {
                 return (ArrayList<EventExecutor>)eventExecutorList.clone();
             } finally {
                 lock.unlock();
             }
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
     
     
     
     private static class Event implements EventScanner {
         private ReentrantLock lock = new ReentrantLock();
         private ArrayList<Announce> eventAnnouncerList = new ArrayList<Announce>();

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

        public void addRecord(DBRecord dbRecord) {
            lock.lock();
            try {
                ScanField scanField = ScanFieldFactory.create(dbRecord);
                int priority = scanField.getPriority().getJavaPriority();
                String eventName = scanField.getEventName();
                String threadName = "event(" + eventName + "," + String.valueOf(priority) + ")";
                Announce announce = getAnnounce(eventName);
                EventExecutor eventExecutor = null;
                ArrayList<EventExecutor> eventExecuterList = announce.getEventExecutorList();
                ListIterator<EventExecutor> iter = eventExecuterList.listIterator();
                while(iter.hasNext()) {
                    EventExecutor eventExecutorNow = iter.next();
                    int threadPriority = eventExecutorNow.getThread().getPriority();
                    if(priority<threadPriority) continue;
                    if(priority==threadPriority) {
                        eventExecutor = eventExecutorNow;
                        break;
                    }
                    if(iter.hasNext()) {
                        eventExecutor = new EventExecutor(threadName,priority);
                        iter.previous();
                        iter.add(eventExecutor);
                        break;
                    }
                }
                if(eventExecutor==null) {
                    eventExecutor = new EventExecutor(threadName,priority);
                    eventExecuterList.add(eventExecutor);
                    announce.setEventExecutorList(eventExecuterList);
                }
                ArrayList<RecordProcess> recordProcessList = eventExecutor.getList();
                recordProcessList.add(dbRecord.getRecordProcess());
                eventExecutor.setList(recordProcessList);
            } finally {
                lock.unlock();
            }
        }

        public void removeEventAnnouncer(EventAnnounce eventAnnounce, String announcer) {
            lock.lock();
            try {
                Announce announce = (Announce)eventAnnounce;
                announce.removeAnnouncer(announcer);
            } finally {
                lock.unlock();
            }
        }

        public void removeRecord(DBRecord dbRecord) {
            lock.lock();
            try {
                ScanField scanField = ScanFieldFactory.create(dbRecord);
                int priority = scanField.getPriority().getJavaPriority();
                String eventName = scanField.getEventName();
                Announce announce = getAnnounce(eventName);
                ArrayList<EventExecutor> eventExecuterList = announce.getEventExecutorList();
                ListIterator<EventExecutor> iter = eventExecuterList.listIterator();
                while(iter.hasNext()) {
                    EventExecutor eventExecutor = iter.next();
                    int threadPriority = eventExecutor.getThread().getPriority();
                    if(priority<threadPriority) continue;
                    if(priority==threadPriority) {
                        ArrayList<RecordProcess> recordProcessList = eventExecutor.getList();
                        recordProcessList.remove(dbRecord.getRecordProcess());
                        return;
                    }
                    break;
                }
                return;
            } finally {
                lock.unlock();
            }
        }

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
            List<EventExecutor> eventExecutorList = announce.getEventExecutorList();
            ListIterator<EventExecutor> iter1 = eventExecutorList.listIterator();
            while(iter1.hasNext()) {
                EventExecutor eventExecutor = iter1.next();
                thread = eventExecutor.getThread();
                builder.append(String.format(lineBreak + "    thread %s priority %d record list{",
                        thread.getName(),
                        thread.getPriority()));
                List<RecordProcess> processList = eventExecutor.getList();
                ListIterator<RecordProcess> iter2 = processList.listIterator();
                while(iter2.hasNext()) {
                    RecordProcess recordProcess = iter2.next();
                    String name = recordProcess.getRecord().getRecordName();
                    builder.append(lineBreak + "        " + name);
                }
                builder.append(lineBreak + "    }");
            }
            builder.append(lineBreak + "}");
            return builder.toString();
        }
     }
}
