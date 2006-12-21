/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;

import java.util.*;
import java.util.concurrent.locks.*;

import org.epics.ioc.pv.*;
import org.epics.ioc.util.MessageType;

/**
 * A factory for creating ChannelData and ChannelDataQueue.
 * @author mrk
 *
 */
public class ChannelDataFactory {
     /**
      * Create a ChannelData for the specified channel and fieldGroupImpl.
     * @param channel The channel.
     * @param channelFieldGroup The field group defining what should be in the channelData.
     * @return The channelData interface.
     */
    public static ChannelData createData(
         Channel channel,ChannelFieldGroup channelFieldGroup)
     {
         ChannelDataImpl channelData = 
             new ChannelDataImpl(channel,channelFieldGroup);
         if(channelData.createData()) {
             return channelData;
         }
         return null;
     }
     
     /**
      * Create a queue of ChannelData.
     * @param queueSize The queueSize. This is can not be changed after creation.
     * @param channel The channel.
     * @param fieldGroup The field group defining what should be in each channelData.
     * @return The channelDataQueue interface.
     */
    public static ChannelDataQueue createQueue(
             int queueSize,
             Channel channel,ChannelFieldGroup channelFieldGroup)
     {
          ChannelData[] queue = new ChannelData[queueSize];
          for(int i = 0; i<queueSize; i++) {
              queue[i] = createData(channel,channelFieldGroup);
          }
          return new ChannelDataQueueImpl(queue);
     }
     
     private static Convert convert = ConvertFactory.getConvert();
     
     private static class ChannelDataQueueImpl implements ChannelDataQueue {
         private ReentrantLock lock = new ReentrantLock();
         private int queueSize;
         private ArrayList<ChannelData> freeList;
         private ArrayList<ChannelData> inUseList;
         private ChannelData next = null;
         private int numberMissed = 0;

         private ChannelDataQueueImpl(ChannelData[] queue) {
             queueSize = queue.length;
             freeList = new ArrayList<ChannelData>(queueSize);
             for(ChannelData data : queue) freeList.add(data);
             inUseList = new ArrayList<ChannelData>(queueSize);
         }

        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelDataQueue#capacity()
         */
        public int capacity() {
            return queueSize;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelDataQueue#getFree()
         */
        public ChannelData getFree(boolean forceFree) {
            lock.lock();
            try {
                if(freeList.size()>0) {
                    ChannelData data = freeList.remove(0);
                    inUseList.add(data);
                    return data;
                }
                numberMissed++;
                if(!forceFree) return null;
                if(inUseList.size()<=0) return null;
                ChannelData data = inUseList.remove(0);
                inUseList.add(data);
                return data;
            } finally {
                lock.unlock();
            }
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelDataQueue#getNext()
         */
        public ChannelData getNext() {
            lock.lock();
            try {
                if(next!=null) {
                    throw new IllegalStateException("already have next");
                }
                if(inUseList.size()<=0) return null;
                next = inUseList.remove(0);
                return next;
            } finally {
                lock.unlock();
            }
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelDataQueue#getNumberMissed()
         */
        public int getNumberMissed() {
            int number = numberMissed;
            numberMissed = 0;
            return number;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelDataQueue#getNumberFree()
         */
        public int getNumberFree() {
            lock.lock();
            try {
                return freeList.size();
            } finally {
                lock.unlock();
            }
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelDataQueue#releaseNext(org.epics.ioc.ca.ChannelData)
         */
        public void releaseNext(ChannelData channelData) {
            lock.lock();
            try {
                if(next!=channelData) {
                    throw new IllegalStateException("channelData is not that returned by getNext");
                }
                channelData.clear();
                freeList.add(next);
                next = null;
            } finally {
                lock.unlock();
            }
        }
        
     }
     
     private static class ChannelDataImpl implements ChannelData {
         private ChannelFieldGroup channelFieldGroup;
         private boolean[] hasData;
         private PVData[] pvDataArray;
         private ChannelField[] channelFieldArray;
         private ArrayList<PVData> pvDataList;
         private ArrayList<ChannelField> channelFieldList;
        
         private ChannelDataImpl(Channel channel, ChannelFieldGroup channelFieldGroup)
         {
             super();
             this.channelFieldGroup = channelFieldGroup;
         }
         
         private boolean createData() {
             List<ChannelField> channelFieldList = channelFieldGroup.getList();
             int size = channelFieldList.size();
             pvDataArray = new PVData[size];
             channelFieldArray = new ChannelField[size];
             for(int i=0; i<size; i++) {
                 ChannelField channelField = channelFieldList.get(i);
                 Field field = channelField.getField();
                 PVData pvData = null;
                 switch(field.getType()) {
                 case pvBoolean:  pvData = new BooleanData(field); break;
                 case pvByte:     pvData = new ByteData(field); break;
                 case pvShort:    pvData = new ShortData(field); break;
                 case pvInt:      pvData = new IntData(field); break;
                 case pvLong:     pvData = new LongData(field); break;
                 case pvFloat:    pvData = new FloatData(field); break;
                 case pvDouble:   pvData = new DoubleData(field); break;
                 case pvString:   pvData = new StringData(field); break;
                 case pvEnum:     pvData = new EnumData(field); break;
                 case pvMenu:     pvData = new EnumData(field); break;
                 case pvLink:     pvData = new LinkData(field); break;
                 case pvArray:    pvData = createArrayData(field); break;
                 case pvStructure: pvData = createStructureData(field); break;
                 }
                 if(pvData==null) return false;
                 pvDataArray[i] = pvData;
                 channelFieldArray[i] = channelField;
             }
             hasData = new boolean[size];
             pvDataList = new ArrayList<PVData>(size);
             this.channelFieldList = new ArrayList<ChannelField>(size);
             return true;
         }
         
         /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelData#getChannelFieldGroup()
         */
        public ChannelFieldGroup getChannelFieldGroup() {
            return channelFieldGroup;
        }

        /* (non-Javadoc)
          * @see org.epics.ioc.ca.ChannelData#add(org.epics.ioc.pv.PVData)
          */
         public boolean add(PVData fromData) {
             Field fromField = fromData.getField();
             for(int i=0; i< pvDataArray.length; i++) {
                 PVData toData = pvDataArray[i];
                 if(toData.getField()==fromField) {
                     Type type = fromField.getType();
                     if(type.isScalar()) {
                         convert.copyScalar(fromData, toData);
                     } else if(type==Type.pvArray) {
                         PVArray from = (PVArray)fromData;
                         PVArray to = (PVArray)toData;
                         convert.copyArray(from,0, to, 0, from.getLength());
                     } else if(type==Type.pvEnum || type==Type.pvMenu) {
                         PVEnum from = (PVEnum)fromData;
                         PVEnum to = (PVEnum)toData;
                         to.setChoices(from.getChoices());
                         to.setIndex(from.getIndex());
                     } else if(type==Type.pvStructure) {
                         PVStructure from = (PVStructure)fromData;
                         PVStructure to = (PVStructure)toData;
                         convert.copyStructure(from, to);
                     } else {
                         throw new IllegalStateException("unsupported type");
                     }
                     hasData[i] = true;
                     return true;
                 }
             }
             return false;
         }

        /* (non-Javadoc)
          * @see org.epics.ioc.ca.ChannelData#clear()
          */
         public void clear() {
             for(int i=0; i< hasData.length; i++) hasData[i] = false;
             pvDataList.clear();
             channelFieldList.clear();
         }
         /* (non-Javadoc)
          * @see org.epics.ioc.ca.ChannelData#getPVDataList()
          */
         public List<PVData> getPVDataList() {
             if(pvDataList.size()<=0) {
                 for(int i=0; i< hasData.length; i++) {
                     if(hasData[i]) pvDataList.add(pvDataArray[i]);
                 }
             }
             return pvDataList;
         } 
         
         /* (non-Javadoc)
          * @see org.epics.ioc.ca.ChannelData#getChannelFieldList()
          */
         public List<ChannelField> getChannelFieldList() {
             if(channelFieldList.size()<=0) {
                 for(int i=0; i< hasData.length; i++) {
                     if(hasData[i]) channelFieldList.add(channelFieldArray[i]);
                 }
             }
             return channelFieldList;
         }
         
         private PVData createArrayData(Field field) {
             Array array = (Array)field;
             switch(array.getElementType()) {
             case pvBoolean:  return new BooleanArray(array);
             case pvByte:     return new ByteArray(array);
             case pvShort:    return new ShortArray(array);
             case pvInt:      return new IntArray(array);
             case pvLong:     return new LongArray(array);
             case pvFloat:    return new FloatArray(array);
             case pvDouble:   return new DoubleArray(array);
             case pvString:   return new StringArray(array);
             case pvMenu:
             case pvEnum:     return new EnumArray(array);
             case pvLink:     return new LinkArray(array);
             case pvArray:    return new ArrayArray(array);
             case pvStructure: return new StructureArray(array);
             }
             return null;
         }
         
         private PVData createStructureData(Field structureField) {
             Structure structure = (Structure)structureField;
             Field[] fields = structure.getFields();
             int length = fields.length;
             if(length<=0) return null;
             PVData[] pvDataArray = new PVData[length];
             for(int i=0; i<length; i++) {
                 PVData pvData = null;
                 Field field = fields[i];
                 switch(field.getType()) {
                 case pvBoolean:  pvData = new BooleanData(field); break;
                 case pvByte:     pvData = new ByteData(field); break;
                 case pvShort:    pvData = new ShortData(field); break;
                 case pvInt:      pvData = new IntData(field); break;
                 case pvLong:     pvData = new LongData(field); break;
                 case pvFloat:    pvData = new FloatData(field); break;
                 case pvDouble:   pvData = new DoubleData(field); break;
                 case pvString:   pvData = new StringData(field); break;
                 case pvEnum:     pvData = new EnumData(field); break;
                 case pvArray:    pvData = createArrayData(field); break;
                 case pvStructure: pvData = createStructureData(field); break;
                 }
                 if(pvData==null) {
                     throw new IllegalStateException("unsupported type");
                 }
                 pvDataArray[i] = pvData;
             }
             return new StructureData(structureField,pvDataArray);
         }
     }
     
     private static abstract class Data implements PVData {
         private Field field;
         private String supportName = null;
         
         private Data(Field field) {
             this.field = field;
         }
         /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVData#getFullFieldName()
         */
        public String getFullFieldName() {
            return "." + field.getFieldName();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVData#getParent()
         */
        public PVData getParent() {
            return null;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVData#message(java.lang.String, org.epics.ioc.util.MessageType)
         */
        public void message(String message, MessageType messageType) {
            System.out.printf("%s %s",message,messageType.toString());
        }
        /* (non-Javadoc)
          * @see org.epics.ioc.pv.PVData#getField()
          */
         public Field getField() {
             return field;
         }
         /* (non-Javadoc)
          * @see org.epics.ioc.pv.PVData#getSupportName()
          */
         public String getSupportName() {
             return supportName;
         }
         
         /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVData#setSupportName(java.lang.String)
         */
        public String setSupportName(String name) {
             supportName = name;
             return null;
         }
         /* (non-Javadoc)
          * @see org.epics.ioc.pv.PVData#toString(int)
          */
         public String toString(int indentLevel) {
             if(supportName!=null) return " supportName " + supportName;
             return "";
         }
         
     }

     private static class BooleanData extends Data implements PVBoolean {
         private boolean value;

         private BooleanData(Field field) {
             super(field);
             value = false;
         }
         /* (non-Javadoc)
          * @see org.epics.ioc.pv.PVBoolean#get()
          */
         public boolean get() { return value; }
         /* (non-Javadoc)
          * @see org.epics.ioc.pv.PVBoolean#put(boolean)
          */
         public void put(boolean value) { this.value = value;}
         /* (non-Javadoc)
          * @see org.epics.ioc.pv.PVData#getField()
          */
         public String toString() {
             return toString(0);
         }
         /* (non-Javadoc)
          * @see org.epics.ioc.pv.PVData#toString(int)
          */
         public String toString(int indentLevel) {
             return convert.getString(this,indentLevel)
                 + super.toString(indentLevel);
         }
     }

     private static class ByteData extends Data implements PVByte {
         private byte value;
         
         private ByteData(Field field) {
             super(field);
             value = 0;
         }
         /* (non-Javadoc)
          * @see org.epics.ioc.pv.PVByte#get()
          */
         public byte get() { return value; }
         /* (non-Javadoc)
          * @see org.epics.ioc.pv.PVByte#put(byte)
          */
         public void put(byte value) { this.value = value;}
         /* (non-Javadoc)
          * @see java.lang.Object#toString()
          */
         public String toString() {
             return toString(0);
         }
         /* (non-Javadoc)
          * @see org.epics.ioc.pv.PVData#toString(int)
          */
         public String toString(int indentLevel) {
             return convert.getString(this,indentLevel)
                 + super.toString(indentLevel);
         }
     }
     
     private static class ShortData extends Data implements PVShort {
         private short value;

         private ShortData(Field field) {
             super(field);
             value = 0;
         }
         /* (non-Javadoc)
          * @see org.epics.ioc.pv.PVShort#get()
          */
         public short get() { return value; }
         /* (non-Javadoc)
          * @see org.epics.ioc.pv.PVShort#put(short)
          */
         public void put(short value) { this.value = value;}
         /* (non-Javadoc)
          * @see java.lang.Object#toString()
          */
         public String toString() {
             return toString(0);
         }
         /* (non-Javadoc)
          * @see org.epics.ioc.pv.PVData#toString(int)
          */
         public String toString(int indentLevel) {
             return convert.getString(this,indentLevel)
                 + super.toString(indentLevel);
         }
     }
     
     private static class IntData extends Data implements PVInt {
         private int value;
         
         private IntData(Field field) {
             super(field);
             value = 0;
         }
         /* (non-Javadoc)
          * @see org.epics.ioc.pv.PVInt#get()
          */
         public int get() { return value; }
         /* (non-Javadoc)
          * @see org.epics.ioc.pv.PVInt#put(int)
          */
         public void put(int value) { this.value = value;}
         /* (non-Javadoc)
          * @see java.lang.Object#toString()
          */
         public String toString() {
             return toString(0);
         }
         /* (non-Javadoc)
          * @see org.epics.ioc.pv.PVData#toString(int)
          */
         public String toString(int indentLevel) {
             return convert.getString(this,indentLevel)
                 + super.toString(indentLevel);
         }
     }
     
     private static class LongData extends Data implements PVLong {
         private long value;

         private LongData(Field field) {
             super(field);
             value = 0;
         }
         /* (non-Javadoc)
          * @see org.epics.ioc.pv.PVLong#get()
          */
         public long get() { return value; }
         /* (non-Javadoc)
          * @see org.epics.ioc.pv.PVLong#put(long)
          */
         public void put(long value) { this.value = value;}
         /* (non-Javadoc)
          * @see java.lang.Object#toString()
          */
         public String toString() {
             return toString(0);
         }
         /* (non-Javadoc)
          * @see org.epics.ioc.pv.PVData#toString(int)
          */
         public String toString(int indentLevel) {
             return convert.getString(this,indentLevel)
                 + super.toString(indentLevel);
         }
     }
     
     private static class FloatData extends Data implements PVFloat {
         private float value;

         private FloatData(Field field) {
             super(field);
             value = 0;
         }
         /* (non-Javadoc)
          * @see org.epics.ioc.pv.PVFloat#get()
          */
         public float get() { return value; }
         /* (non-Javadoc)
          * @see org.epics.ioc.pv.PVFloat#put(float)
          */
         public void put(float value) { this.value = value;}
         /* (non-Javadoc)
          * @see java.lang.Object#toString()
          */
         public String toString() {
             return toString(0);
         }
         /* (non-Javadoc)
          * @see org.epics.ioc.pv.PVData#toString(int)
          */
         public String toString(int indentLevel) {
             return convert.getString(this,indentLevel)
                 + super.toString(indentLevel);
         }
     }
     
     private static class DoubleData extends Data implements PVDouble {
         private double value;

         private DoubleData(Field field) {
             super(field);
             value = 0;
         }
         /* (non-Javadoc)
          * @see org.epics.ioc.pv.PVDouble#get()
          */
         public double get() { return value; }
         /* (non-Javadoc)
          * @see org.epics.ioc.pv.PVDouble#put(double)
          */
         public void put(double value) { this.value = value;}
         /* (non-Javadoc)
          * @see java.lang.Object#toString()
          */
         public String toString() {
             return toString(0);
         }
         /* (non-Javadoc)
          * @see org.epics.ioc.pv.PVData#toString(int)
          */
         public String toString(int indentLevel) {
             return convert.getString(this,indentLevel)
                 + super.toString(indentLevel);
         }
     }
     
     private static class StringData extends Data implements PVString {
         private String value;

         private StringData(Field field) {
             super(field);
             value = null;
         }
         /* (non-Javadoc)
          * @see org.epics.ioc.pv.PVString#get()
          */
         public String get() { return value; }
         /* (non-Javadoc)
          * @see org.epics.ioc.pv.PVString#put(java.lang.String)
          */
         public void put(String value) { this.value = value;}
         /* (non-Javadoc)
          * @see java.lang.Object#toString()
          */
         public String toString() {
             return toString(0);
         }
         /* (non-Javadoc)
          * @see org.epics.ioc.pv.PVData#toString(int)
          */
         public String toString(int indentLevel) {
             return convert.getString(this,indentLevel)
                 + super.toString(indentLevel);
         }
     }
     
     private static class EnumData extends Data implements PVEnum {
         private int index;
         private String[] choice;

         private final static String[] EMPTY_STRING_ARRAY = new String[0];

         private EnumData(Field field) {
             super(field);
             index = 0;
             choice = EMPTY_STRING_ARRAY;
         }

         /* (non-Javadoc)
          * @see org.epics.ioc.pv.PVEnum#getChoices()
          */
         public String[] getChoices() {
             return choice;
         }

         /* (non-Javadoc)
          * @see org.epics.ioc.pv.PVEnum#getIndex()
          */
         public int getIndex() {
             return index;
         }

         /* (non-Javadoc)
          * @see org.epics.ioc.pv.PVEnum#setChoices(java.lang.String[])
          */
         public boolean setChoices(String[] choice) {
             this.choice = choice;
             return true;
         }

         /* (non-Javadoc)
          * @see org.epics.ioc.pv.PVEnum#setIndex(int)
          */
         public void setIndex(int index) {
             this.index = index;
         }
         /* (non-Javadoc)
          * @see java.lang.Object#toString()
          */
         public String toString() {
             return toString(0);
         }
         /* (non-Javadoc)
          * @see org.epics.ioc.pv.PVData#toString(int)
          */
         public String toString(int indentLevel) {
             return convert.getString(this,indentLevel)
                 + super.toString(indentLevel);
         }
     }
     
     private static class LinkData extends Data implements PVLink {
         PVStructure configStructure = null;
         
         private LinkData(Field field) {
             super(field);
         }       
         /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVLink#getConfigurationStructure()
         */
        public PVStructure getConfigurationStructure() {
            return configStructure;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVLink#setConfigurationStructure(org.epics.ioc.pv.PVStructure)
         */
        public boolean setConfigurationStructure(PVStructure pvStructure) {
            configStructure = pvStructure;
            return true;
        }

     }

     private static class DataArray extends Data implements PVArray {
         protected int length = 0;
         protected int capacity = 0;
     
         private DataArray(Array array) {
             super(array);
         }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVArray#getCapacity()
         */
        public int getCapacity() {
            return capacity;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVArray#getLength()
         */
        public int getLength() {
            return length;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVArray#isCapacityMutable()
         */
        public boolean isCapacityMutable() {
            return true;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVArray#setCapacity(int)
         */
        public void setCapacity(int len) {
            capacity = len;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVArray#setLength(int)
         */
        public void setLength(int len) {
            length = len;
        }
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() {
            return toString(0);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVData#toString(int)
         */
        public String toString(int indentLevel) {
            return convert.getString(this,indentLevel)
                + super.toString(indentLevel);
        }
     }
     private static class BooleanArray extends DataArray implements PVBooleanArray {
         private boolean[] value;
     
         private BooleanArray(Array array) {
             super(array);
             value = new boolean[0];
         }
              
         /* (non-Javadoc)
          * @see org.epics.ioc.pv.PVBooleanArray#get(int, int, org.epics.ioc.pv.BooleanArrayData)
          */
         public int get(int offset, int len, BooleanArrayData data) {
             int n = len;
             if(offset+len > length) n = length;
             data.data = value;
             data.offset = offset;
             return n;
         }
         /* (non-Javadoc)
          * @see org.epics.ioc.pv.PVBooleanArray#put(int, int, org.epics.ioc.pv.BooleanArrayData)
          */
         public int put(int offset, int len, boolean[]from, int fromOffset) {
             if(offset+len > length) {
                  int newlength = offset + len;
                  if(newlength>capacity) setCapacity(newlength);
                  length = newlength;
             }
             System.arraycopy(from,fromOffset,value,offset,len);
             return len;
         }
         /* (non-Javadoc)
          * @see org.epics.ioc.pv.PVArray#getElementType()
          */
         public Type getElementType() {return Type.pvBoolean;}
     }
     
     private static class ByteArray extends DataArray implements PVByteArray {
         private byte[] value;
     
         private ByteArray(Array array) {
             super(array);
             value = new byte[0];
         }
         /* (non-Javadoc)
          * @see org.epics.ioc.pv.PVByteArray#get(int, int, org.epics.ioc.pv.ByteArrayData)
          */
         public int get(int offset, int len, ByteArrayData data) {
             int n = len;
             if(offset+len > length) n = length;
             data.data = value;
             data.offset = offset;
             return n;
         }
         /* (non-Javadoc)
          * @see org.epics.ioc.pv.PVByteArray#put(int, int, org.epics.ioc.pv.ByteArrayData)
          */
         public int put(int offset, int len, byte[]from, int fromOffset) {
             if(offset+len > length) {
                  int newlength = offset + len;
                  if(newlength>capacity) setCapacity(newlength);
                  length = newlength;
             }
             System.arraycopy(from,fromOffset,value,offset,len);
             return len;
         }
         /* (non-Javadoc)
          * @see org.epics.ioc.pv.PVArray#getElementType()
          */
         public Type getElementType() {return Type.pvByte;}
     }
     
     private static class ShortArray extends DataArray implements PVShortArray {
         private short[] value;
     
         private ShortArray(Array array) {
             super(array);
             value = new short[0];
         }
         /* (non-Javadoc)
          * @see org.epics.ioc.pv.PVShortArray#get(int, int, org.epics.ioc.pv.ShortArrayData)
          */
         public int get(int offset, int len, ShortArrayData data) {
             int n = len;
             if(offset+len > length) n = length;
             data.data = value;
             data.offset = offset;
             return n;
         }
         /* (non-Javadoc)
          * @see org.epics.ioc.pv.PVShortArray#put(int, int, org.epics.ioc.pv.ShortArrayData)
          */
         public int put(int offset, int len, short[]from, int fromOffset) {
             if(offset+len > length) {
                  int newlength = offset + len;
                  if(newlength>capacity) setCapacity(newlength);
                  length = newlength;
             }
             System.arraycopy(from,fromOffset,value,offset,len);
             return len;
         }
         /* (non-Javadoc)
          * @see org.epics.ioc.pv.PVArray#getElementType()
          */
         public Type getElementType() {return Type.pvShort;}
     }
     
     private static class IntArray extends DataArray implements PVIntArray {
         private int[] value;
     
         private IntArray(Array array) {
             super(array);
             value = new int[0];
         }
         /* (non-Javadoc)
          * @see org.epics.ioc.pv.PVIntArray#get(int, int, org.epics.ioc.pv.IntArrayData)
          */
         public int get(int offset, int len, IntArrayData data) {
             int n = len;
             if(offset+len > length) n = length;
             data.data = value;
             data.offset = offset;
             return n;
         }
         /* (non-Javadoc)
          * @see org.epics.ioc.pv.PVIntArray#put(int, int, org.epics.ioc.pv.IntArrayData)
          */
         public int put(int offset, int len, int[]from, int fromOffset) {
             if(offset+len > length) {
                  int newlength = offset + len;
                  if(newlength>capacity) setCapacity(newlength);
                  length = newlength;
             }
             System.arraycopy(from,fromOffset,value,offset,len);
             return len;
         }
         /* (non-Javadoc)
          * @see org.epics.ioc.pv.PVArray#getElementType()
          */
         public Type getElementType() {return Type.pvInt;}
     }
     
     private static class LongArray extends DataArray implements PVLongArray {
         private int length = 0;
         private int capacity = 0;
         private long[] value;
     
         private LongArray(Array array) {
             super(array);
             value = new long[0];
         }
         /* (non-Javadoc)
          * @see org.epics.ioc.pv.PVArray#getLength()
          */
         public int getLength(){ return length;}
         /* (non-Javadoc)
          * @see org.epics.ioc.pv.PVArray#getCapacity()
          */
         public int getCapacity(){ return capacity;}
         /* (non-Javadoc)
          * @see org.epics.ioc.pv.PVArray#setLength(int)
          */
         public void setLength(int len) {
             if(len>capacity) setCapacity(len);
             length = len;
         }
         /* (non-Javadoc)
          * @see org.epics.ioc.pv.PVArray#setCapacity(int)
          */
         public void setCapacity(int len) {
             if(len<=capacity) return;
             long[] newarray = new long[len];
             if(length>0) System.arraycopy(value,0,newarray,0,length);
             value = newarray;
             capacity = len;
         }
         /* (non-Javadoc)
          * @see org.epics.ioc.pv.PVLongArray#get(int, int, org.epics.ioc.pv.LongArrayData)
          */
         public int get(int offset, int len, LongArrayData data) {
             int n = len;
             if(offset+len > length) n = length;
             data.data = value;
             data.offset = offset;
             return n;
         }
         /* (non-Javadoc)
          * @see org.epics.ioc.pv.PVLongArray#put(int, int, org.epics.ioc.pv.LongArrayData)
          */
         public int put(int offset, int len, long[]from, int fromOffset) {
             if(offset+len > length) {
                  int newlength = offset + len;
                  if(newlength>capacity) setCapacity(newlength);
                  length = newlength;
             }
             System.arraycopy(from,fromOffset,value,offset,len);
             return len;
         }
         /* (non-Javadoc)
          * @see org.epics.ioc.pv.PVArray#getElementType()
          */
         public Type getElementType() {return Type.pvLong;}
     }
     
     private static class FloatArray extends DataArray implements PVFloatArray {
         private float[] value;
     
         private FloatArray(Array array) {
             super(array);
             value = new float[0];
         }
         /* (non-Javadoc)
          * @see org.epics.ioc.pv.PVFloatArray#get(int, int, org.epics.ioc.pv.FloatArrayData)
          */
         public int get(int offset, int len, FloatArrayData data) {
             int n = len;
             if(offset+len > length) n = length;
             data.data = value;
             data.offset = offset;
             return n;
         }
         /* (non-Javadoc)
          * @see org.epics.ioc.pv.PVFloatArray#put(int, int, org.epics.ioc.pv.FloatArrayData)
          */
         public int put(int offset, int len, float[]from, int fromOffset) {
             if(offset+len > length) {
                  int newlength = offset + len;
                  if(newlength>capacity) setCapacity(newlength);
                  length = newlength;
             }
             System.arraycopy(from,fromOffset,value,offset,len);
             return len;
         }
         /* (non-Javadoc)
          * @see org.epics.ioc.pv.PVArray#getElementType()
          */
         public Type getElementType() {return Type.pvFloat;}
     }
     
     private static class DoubleArray extends DataArray implements PVDoubleArray {
         private double[] value;
     
         private DoubleArray(Array array) {
             super(array);
             value = new double[0];
         }
         /* (non-Javadoc)
          * @see org.epics.ioc.pv.PVDoubleArray#get(int, int, org.epics.ioc.pv.DoubleArrayData)
          */
         public int get(int offset, int len, DoubleArrayData data) {
             data.data = value;
             data.offset = offset;
             if(offset+len>length) {
                 len = length - offset;
                 if(len<0) len =0;
             }
             return len;
         }
         
         /* (non-Javadoc)
          * @see org.epics.ioc.pv.PVDoubleArray#put(int, int, org.epics.ioc.pv.DoubleArrayData)
          */
         public int put(int offset, int len, double[] from, int fromOffset) {
             if(offset+len > length) {
                 int newlength = offset + len;
                 if(newlength>capacity) setCapacity(newlength);
                 length = newlength;
            }
            System.arraycopy(from,fromOffset,value,offset,len);
            return len;
         }
         /* (non-Javadoc)
          * @see org.epics.ioc.pv.PVArray#getElementType()
          */
         public Type getElementType() {return Type.pvDouble;}
         /* (non-Javadoc)
          * @see java.lang.Object#toString()
          */
     }
     
     private static class StringArray extends DataArray implements PVStringArray {
         private String[] value;
     
         private StringArray(Array array) {
             super(array);
             value = new String[0];
         }
         /* (non-Javadoc)
          * @see org.epics.ioc.pv.PVStringArray#get(int, int, org.epics.ioc.pv.StringArrayData)
          */
         public int get(int offset, int len, StringArrayData data) {
             int n = len;
             if(offset+len > length) n = length;
             data.data = value;
             data.offset = offset;
             return n;
         }
         /* (non-Javadoc)
          * @see org.epics.ioc.pv.PVStringArray#put(int, int, org.epics.ioc.pv.StringArrayData)
          */
         public int put(int offset, int len, String[] from, int fromOffset) {
             if(offset+len > length) {
                  int newlength = offset + len;
                  if(newlength>capacity) setCapacity(newlength);
                  length = newlength;
             }
             System.arraycopy(from,fromOffset,value,offset,len);
             return len;
         }
         /* (non-Javadoc)
          * @see org.epics.ioc.pv.PVArray#getElementType()
          */
         public Type getElementType() {return Type.pvString;}
     }
     
     private static class EnumArray extends DataArray implements PVEnumArray {
         private PVEnum[] value;
     
         private EnumArray(Array array) {
             super(array);
             value = new PVEnum[0];
         }
         /* (non-Javadoc)
          * @see org.epics.ioc.pv.PVEnumArray#get(int, int, org.epics.ioc.pv.EnumArrayData)
          */
         public int get(int offset, int len, EnumArrayData data) {
             int n = len;
             if(offset+len > length) n = length;
             data.data = value;
             data.offset = offset;
             return n;
         }
         /* (non-Javadoc)
          * @see org.epics.ioc.pv.PVEnumArray#put(int, int, org.epics.ioc.pv.EnumArrayData)
          */
         public int put(int offset, int len, PVEnum[] from, int fromOffset) {
             if(offset+len > length) {
                  int newlength = offset + len;
                  if(newlength>capacity) setCapacity(newlength);
                  length = newlength;
             }
             System.arraycopy(from,fromOffset,value,offset,len);
             return len;
         }
         /* (non-Javadoc)
          * @see org.epics.ioc.pv.PVArray#getElementType()
          */
         public Type getElementType() {return Type.pvEnum;}
     }
     
     private static class LinkArray extends DataArray implements PVLinkArray {
         private PVLink[] value;
     
         private LinkArray(Array array) {
             super(array);
             value = new PVLink[0];
         }
         
         public int get(int offset, int len, LinkArrayData data) {
             int n = len;
             if(offset+len > length) n = length;
             data.data = value;
             data.offset = offset;
             return n;
         }
         /* (non-Javadoc)
          * @see org.epics.ioc.pv.PVEnumArray#put(int, int, org.epics.ioc.pv.EnumArrayData)
          */
         public int put(int offset, int len, PVLink[] from, int fromOffset) {
             if(offset+len > length) {
                  int newlength = offset + len;
                  if(newlength>capacity) setCapacity(newlength);
                  length = newlength;
             }
             System.arraycopy(from,fromOffset,value,offset,len);
             return len;
         }
         /* (non-Javadoc)
          * @see org.epics.ioc.pv.PVArray#getElementType()
          */
         public Type getElementType() {return Type.pvLink;}
     }
     
     private static class StructureArray extends Data implements PVStructureArray {
         private int length = 0;
         private int capacity = 0;
         private PVStructure[] value;
     
         private StructureArray(Array array) {
             super(array);
             value = new PVStructure[0];
         }
         /* (non-Javadoc)
          * @see org.epics.ioc.pv.PVArray#getLength()
          */
         public int getLength(){ return length;}
         /* (non-Javadoc)
          * @see org.epics.ioc.pv.PVArray#getCapacity()
          */
         public int getCapacity(){ return capacity;}
         /* (non-Javadoc)
          * @see org.epics.ioc.pv.PVArray#setLength(int)
          */
         public void setLength(int len) {
             if(len>capacity) setCapacity(len);
             length = len;
         }
         /* (non-Javadoc)
          * @see org.epics.ioc.pv.PVArray#setCapacity(int)
          */
         public void setCapacity(int len) {
             if(len<=capacity) return;
             PVStructure[] newarray = new PVStructure[len];
             if(length>0) System.arraycopy(value,0,newarray,0,length);
             value = newarray;
             capacity = len;
         }
         /* (non-Javadoc)
          * @see org.epics.ioc.pv.PVStructureArray#get(int, int, org.epics.ioc.pv.StructureArrayData)
          */
         public int get(int offset, int len, StructureArrayData data) {
             int n = len;
             if(offset+len > length) n = length;
             data.data = value;
             data.offset = offset;
             return n;
         }
         /* (non-Javadoc)
          * @see org.epics.ioc.pv.PVStructureArray#put(int, int, org.epics.ioc.pv.StructureArrayData)
          */
         public int put(int offset, int len,PVStructure[] from,int fromOffset) {
             if(offset+len > length) {
                  int newlength = offset + len;
                  if(newlength>capacity) setCapacity(newlength);
                  length = newlength;
             }
             System.arraycopy(from,fromOffset,value,offset,len);
             return len;
         }
         /* (non-Javadoc)
          * @see org.epics.ioc.pv.PVArray#isCapacityMutable()
          */
         public boolean isCapacityMutable() {
             return true;
         }
         /* (non-Javadoc)
          * @see org.epics.ioc.pv.PVArray#getElementType()
          */
         public Type getElementType() {return Type.pvStructure;}
         /* (non-Javadoc)
          * @see java.lang.Object#toString()
          */
         public String toString() {
             return toString(0);
         }
         /* (non-Javadoc)
          * @see org.epics.ioc.pv.PVData#toString(int)
          */
         public String toString(int indentLevel) {
             return convert.getString(this,indentLevel)
                 + super.toString(indentLevel);
         }
     }
     
     private static class ArrayArray extends Data implements PVArrayArray {
         private int length = 0;
         private int capacity = 0;
         private PVArray[] value;
     
         ArrayArray(Array array) {
             super(array);
             value = new PVArray[0];
         }
         /* (non-Javadoc)
          * @see org.epics.ioc.pv.PVArray#getLength()
          */
         public int getLength(){ return length;}
         /* (non-Javadoc)
          * @see org.epics.ioc.pv.PVArray#getCapacity()
          */
         public int getCapacity(){ return capacity;}
         /* (non-Javadoc)
          * @see org.epics.ioc.pv.PVArray#setLength(int)
          */
         public void setLength(int len) {
             if(len>capacity) setCapacity(len);
             length = len;
         }
         /* (non-Javadoc)
          * @see org.epics.ioc.pv.PVArray#setCapacity(int)
          */
         public void setCapacity(int len) {
             if(len<=capacity) return;
             PVArray[] newarray = new PVArray[len];
             if(length>0) System.arraycopy(value,0,newarray,0,length);
             value = newarray;
             capacity = len;
         }
         /* (non-Javadoc)
          * @see org.epics.ioc.pv.PVArrayArray#get(int, int, org.epics.ioc.pv.ArrayArrayData)
          */
         public int get(int offset, int len, ArrayArrayData data) {
             int n = len;
             if(offset+len > length) n = length;
             data.data = value;
             data.offset = offset;
             return n;
         }
         /* (non-Javadoc)
          * @see org.epics.ioc.pv.PVArrayArray#put(int, int, org.epics.ioc.pv.ArrayArrayData)
          */
         public int put(int offset, int len, PVArray[] from, int fromOffset) {
             if(offset+len > length) {
                  int newlength = offset + len;
                  if(newlength>capacity) setCapacity(newlength);
                  length = newlength;
             }
             System.arraycopy(from,fromOffset,value,offset,len);
             return len;
         }
         /* (non-Javadoc)
          * @see org.epics.ioc.pv.PVArray#isCapacityMutable()
          */
         public boolean isCapacityMutable() {
             return true;
         }
         /* (non-Javadoc)
          * @see org.epics.ioc.pv.PVArray#getElementType()
          */
         public Type getElementType() {return Type.pvArray;}
         /* (non-Javadoc)
          * @see java.lang.Object#toString()
          */
         public String toString() {
             return toString(0);
         }
         /* (non-Javadoc)
          * @see org.epics.ioc.pv.PVData#toString(int)
          */
         public String toString(int indentLevel) {
             return convert.getString(this,indentLevel)
                 + super.toString(indentLevel);
         }
     }
     
     private static class StructureData extends Data implements PVStructure {
         
         private PVData[] fieldPVDatas;
         
         private StructureData(Field field,PVData[] pvDatas) {
             super(field);
             fieldPVDatas = pvDatas;
         }
         /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVStructure#replaceField(java.lang.String, org.epics.ioc.pv.PVData)
         */
        public boolean replaceField(String fieldName, PVData pvData) {
            return false;
        }
        /* (non-Javadoc)
          * @see org.epics.ioc.pv.PVStructure#getFieldPVDatas()
          */
         public PVData[] getFieldPVDatas() {
             return fieldPVDatas;
         }
         /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVStructure#beginPut()
         */
        public void beginPut() {
            // nothing to do
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVStructure#endPut()
         */
        public void endPut() {
            // nothing to do
        }
        /* (non-Javadoc)
          * @see java.lang.Object#toString()
          */
         public String toString() {
             return toString(0);
         }
         /* (non-Javadoc)
          * @see org.epics.ioc.pv.PVData#toString(int)
          */
         public String toString(int indentLevel) {
             return convert.getString(this,indentLevel)
                 + super.toString(indentLevel);
         }
          
      }
}
