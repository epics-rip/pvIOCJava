/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pdrv;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Factory for creating implementations of Trace.
 * @author mrk
 *
 */
public class TraceFactory {
    
    /**
     * Create an implementation of Trace.
     * @return The Trace interface.
     */
    public static Trace create() {
        return new TraceImpl();
    }
    
    private static class TraceImpl implements Trace {
        
        /**
         * The constructor
         */
        public TraceImpl() {

        }

        private static final int DEFAULT_TRACE_TRUNCATE_SIZE = 80;
        private ReentrantLock traceLock = new ReentrantLock();
        private Writer file = new BufferedWriter(new OutputStreamWriter(System.out));
        private int mask = Trace.ERROR;//|Trace.SUPPORT|Trace.DRIVER;//|Trace.FLOW;
        private int iomask = Trace.IO_NODATA;//|Trace.IO_ASCII;
        private int truncateSize = DEFAULT_TRACE_TRUNCATE_SIZE;

        private TraceOptionChangeList optionChangeList = new TraceOptionChangeList();
        private boolean optionChangeActive = false;

        /* (non-Javadoc)
         * @see org.epics.ioc.asyn.Trace#getTraceFile()
         */
        public Writer getFile() {
            traceLock.lock();
            try {
                return file;
            } finally {
                traceLock.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.asyn.Trace#getTraceIOMask()
         */
        public int getIOMask() {
            traceLock.lock();
            try {
                return iomask;
            } finally {
                traceLock.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.asyn.Trace#getTraceIOTruncateSize()
         */
        public int getIOTruncateSize() {
            traceLock.lock();
            try {
                return truncateSize;
            } finally {
                traceLock.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.asyn.Trace#getTraceMask()
         */
        public int getMask() {
            traceLock.lock();
            try {
                return mask;
            } finally {
                traceLock.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.asyn.Trace#setTraceFile(java.io.Writer)
         */
        public void setFile(Writer file) {
            traceLock.lock();
            try {
                this.file = file;
            } finally {
                traceLock.unlock();
            }
            raiseException();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.asyn.Trace#setTraceIOMask(int)
         */
        public void setIOMask(int mask) {
            traceLock.lock();
            try {
                iomask = mask;
            } finally {
                traceLock.unlock();
            }
            raiseException();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.asyn.Trace#setTraceIOTruncateSize(long)
         */
        public void setIOTruncateSize(int size) {
            traceLock.lock();
            try {
                truncateSize = size;
            } finally {
                traceLock.unlock();
            }
            raiseException();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.asyn.Trace#setTraceMask(int)
         */
        public void setMask(int mask) {
            traceLock.lock();
            try {
                this.mask = mask;
            } finally {
                traceLock.unlock();
            }
            raiseException();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.Trace#optionChangeListenerAdd(org.epics.ioc.pdrv.User, org.epics.ioc.pdrv.TraceOptionChangeListener)
         */
        public Status optionChangeListenerAdd(User user,TraceOptionChangeListener listener) {
            traceLock.lock();
            try {
                if(!optionChangeActive) {
                    return optionChangeList.add(user, listener);
                } else {
                    return optionChangeList.addNew(user, listener);
                }
            } finally {
                traceLock.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.Trace#optionChangeListenerRemove(org.epics.ioc.pdrv.User)
         */
        public void optionChangeListenerRemove(User user) {
            traceLock.lock();
            try {
                optionChangeList.remove(user);
            } finally {
                traceLock.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.Trace#print(int, java.lang.String)
         */
        public void print(int reason, String message) {
            print(reason," %s",message);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.asyn.Trace#print(int, java.lang.String, java.lang.Object[])
         */
        public void print(int reason, String format, Object... args) {
            if((reason&mask)==0) return;
            traceLock.lock();
            try {
                file.write(getTime() + String.format(format, args) +  String.format("%n"));
                file.flush();
            }catch (IOException e) {
                System.err.println(e.getMessage());
            } finally {
                traceLock.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.Trace#printIO(int, byte[], long, java.lang.String)
         */
        public void printIO(int reason, byte[] buffer, long len, String message) {
            printIO(reason,buffer,len,"%s",message);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.asyn.Trace#printIO(int, byte[], long, java.lang.String, java.lang.Object[])
         */
        public void printIO(int reason, byte[] buffer, long len, String format, Object... args) {
            if((reason&mask)==0) return;
            traceLock.lock();
            try {
                file.write(getTime() + String.format(format, args));
                if(iomask!=0) {
                    int index = 0;
                    StringBuilder builder = new StringBuilder();
                    builder.append(' ');
                    while(builder.length()<truncateSize && index<len) {
                        if((iomask&Trace.IO_ASCII)!=0) {
                            char value = (char)buffer[index];
                            builder.append(String.valueOf(value));
                        } else if((iomask&Trace.IO_ESCAPE)!=0) {
                            char value = (char)buffer[index];
                            if(Character.isISOControl(value)) {
                                builder.append(getEscaped(value));
                            } else {
                                builder.append(String.valueOf(value));
                            }
                        } else if((iomask&Trace.IO_HEX)!=0) {
                            builder.append(String.format("%x ", buffer[index]));
                        }
                        ++index;
                    }
                    file.write(builder.toString());
                }
                file.write(String.format("%n"));
                file.flush();
            }catch (IOException e) {
                System.err.println(e.getMessage());
            }  finally {
                traceLock.unlock();
            }
        }

        private void raiseException() {
            traceLock.lock();
            try {
                optionChangeActive = true; 
            } finally {
                traceLock.unlock();
            }
            optionChangeList.raiseException();
            traceLock.lock();
            try {
                optionChangeList.merge();
                optionChangeActive = false; 
            } finally {
                traceLock.unlock();
            }
        }

        private String getTime() {
            Date date = new Date(System.currentTimeMillis());
            return String.format("%tF %tT.%tL ", date,date,date);
        }
        private static HashMap<Character,String> escapedMap = new HashMap<Character,String> ();

        static {
            escapedMap.put('\b', "\\b");
            escapedMap.put('\t', "\\t");
            escapedMap.put('\n', "\\n");
            escapedMap.put('\f', "\\f");
            escapedMap.put('\r', "\\r");
            escapedMap.put('\"', "\\\"");
            escapedMap.put('\'', "\\\'");
            escapedMap.put('\\', "\\\\");
        }

        String getEscaped(char key) {
            String value = escapedMap.get(key);
            if(value==null) {
                value = "\\" + key;
               
            }
            return value;
        }

        private static class TraceOptionChangeList {
            Status add(User user, TraceOptionChangeListener listener) {
                for(TraceOptionChangeNode node : list) {
                    if(node.user==user) {
                        user.setMessage("already a TraceOptionChangeListener");
                        return Status.error;
                    }
                }
                TraceOptionChangeNode node = new TraceOptionChangeNode(user,listener);
                list.add(node);
                return Status.success;
            }
            Status addNew(User user, TraceOptionChangeListener listener) {
                for(TraceOptionChangeNode node : list) {
                    if(node.user==user) {
                        user.setMessage("already a TraceOptionChangeListener");
                        return Status.error;
                    }
                }
                if(listNew==null) {
                    listNew = new LinkedList<TraceOptionChangeNode>();
                } else {
                    for(TraceOptionChangeNode node : listNew) {
                        if(node.user==user) {
                            user.setMessage("already a TraceOptionChangeListener");
                            return Status.error;
                        }
                    }
                }
                TraceOptionChangeNode node = new TraceOptionChangeNode(user,listener);
                listNew.add(node);
                return Status.success;
            }
            void raiseException() {
                ListIterator<TraceOptionChangeNode> iter =  list.listIterator();
                while(iter.hasNext()) {
                    TraceOptionChangeNode node = iter.next();
                    node.listener.optionChange();
                }
            }
            void merge() {
                if(listNew!=null) {
                    list.addAll(listNew);
                    listNew = null;
                }
            }
            void remove(User user) {
                ListIterator<TraceOptionChangeNode> iter =  list.listIterator();
                while(iter.hasNext()) {
                    TraceOptionChangeNode node = iter.next();
                    if(node.user==user) {
                        iter.remove();
                        return;
                    }
                }
                if(listNew==null) return;
                iter =  listNew.listIterator();
                while(iter.hasNext()) {
                    TraceOptionChangeNode node = iter.next();
                    if(node.user==user) {
                        iter.remove();
                        return;
                    }
                }
            }
            private static class TraceOptionChangeNode {
                User user;
                TraceOptionChangeListener listener;
                TraceOptionChangeNode(User user,TraceOptionChangeListener listener) {
                    this.user = user;
                    this.listener = listener;
                }
            }
            private List<TraceOptionChangeNode> list = new LinkedList<TraceOptionChangeNode>();
            private List<TraceOptionChangeNode> listNew = null;
        }
    }
}
