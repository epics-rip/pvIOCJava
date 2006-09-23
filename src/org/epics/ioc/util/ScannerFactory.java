/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.util;

import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.epics.ioc.dbAccess.DBMenu;
import org.epics.ioc.dbAccess.DBRecord;
import org.epics.ioc.dbProcess.*;

/**
 * @author mrk
 *
 */
public class ScannerFactory {
     private static PeriodicScanner periodicScanner = new Periodic();
     
     public static PeriodicScanner getPeriodicScanner() {
         return periodicScanner;
     }
     public static EventScanner getEventScanner() {
         return null;
     }
     
     private static String lineBreak = System.getProperty("line.separator");
     
     private static class Executor {
         private List<RecordProcess> workingList = new ArrayList<RecordProcess>();
         private ArrayList<RecordProcess> processList = new ArrayList<RecordProcess>();
         private boolean listModified = false;
         private ReentrantLock lock = new ReentrantLock();
         
         private void runList() {
             Iterator<RecordProcess> iter = workingList.iterator();
             while(iter.hasNext()) {
                 RecordProcess recordProcess = iter.next();
                 recordProcess.process(null);
             }
             lock.lock();
             try {                
                 if(listModified) {
                     workingList = new ArrayList<RecordProcess>(processList);
                     listModified = false;
                 }
             } finally {
                 lock.unlock();
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
             super();
             this.period = period;
             thread = new Thread(this,name);
             thread.setPriority(priority);
             thread.start();
         }
         
         public void run() {
             try {
                 long startTime = System.currentTimeMillis();
                 while(true) {
                     super.runList();
                     long endTime = System.currentTimeMillis();
                     long delay = period - (endTime - startTime);
                     if(delay<1) delay = 1;
                     Thread.sleep(delay);
                     startTime = System.currentTimeMillis();
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
                 RecordProcess recordProcess = dbRecord.getRecordProcess();
                 ArrayList<RecordProcess> processList = executor.getList();
                 processList.add(recordProcess);
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
            long period = (long)rate*1000;
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
         
         private EventExecutor(String name,int priority) {
             super();
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
                     super.runList();
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
             thread = new Thread(this,name);
             thread.setPriority(ScanPriority.valueOf("high").getJavaPriority());
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
                     ListIterator<EventExecutor> iter = eventExecutorList.listIterator();
                     while(iter.hasNext()) {
                         EventExecutor eventExecutor = iter.next();
                         eventExecutor.announce();
                     }
                     lock.lock();
                     try {                
                         if(listModified) {
                             workingList = new ArrayList<EventExecutor>(eventExecutorList);
                             listModified = false;
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
         
         private ArrayList<Announce> eventAnnouncerList = new ArrayList<Announce>();

         private Announce getAnnounce(String name) {
             ListIterator<Announce> iter = eventAnnouncerList.listIterator();
             while(iter.hasNext()) {
                 Announce announce = iter.next();
                 int compare = name.compareTo(announce.getEventName());
                 if(compare>0) continue;
                 if(compare==0) return announce;
                 return new Announce("event(" + name + ")");
             }
             return null;
         }
        public EventAnnounce addEventAnouncer(String eventName, String announcer) {
            Announce announce = getAnnounce(eventName);
            announce.addAnnouncer(announcer);
            return announce;
        }

        public void addRecord(DBRecord dbRecord) {
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
            }
            ArrayList<RecordProcess> recordProcessList = eventExecutor.getList();
            recordProcessList.add(dbRecord.getRecordProcess());
        }

        public void removeEventAnnouncer(EventAnnounce eventAnnounce, String announcer) {
            Announce announce = (Announce)eventAnnounce;
            announce.removeAnnouncer(announcer);
        }

        public void removeRecord(DBRecord dbRecord) {
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
        }

        public String toString() {
            StringBuilder builder = new StringBuilder();
            ListIterator<Announce> iter = eventAnnouncerList.listIterator();
            while(iter.hasNext()) {
                Announce announce = iter.next();
                builder.append(showAnnounce(announce));
            }
            return builder.toString();
        }

        public String show(String eventName) {
            StringBuilder builder = new StringBuilder();
            ListIterator<Announce> iter = eventAnnouncerList.listIterator();
            while(iter.hasNext()) {
                Announce announce = iter.next();
                if(eventName.equals(announce.getEventName())) {
                    builder.append(showAnnounce(announce));
                }
            }
            return builder.toString();
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
                builder.append(String.format("    thread %s priority %d record list{%n",
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
