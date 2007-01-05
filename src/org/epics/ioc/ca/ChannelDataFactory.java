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
     private static FieldCreate fieldCreate = FieldFactory.getFieldCreate();
     
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
         private Channel channel;
         private ChannelFieldGroup channelFieldGroup;
         private ChannelRecord channelRecord;
         private PVData[] pvDatas;
         private ChannelField[] channelFields;
         private ArrayList<ChannelDataPV> channelDataPVList;
         private ArrayList<ChannelDataPV> channelDataPVFreeList;
        
         private ChannelDataImpl(Channel channel, ChannelFieldGroup channelFieldGroup)
         {
             this.channel = channel;
             this.channelFieldGroup = channelFieldGroup;
         }
         
         private boolean createData() {
             List<ChannelField> channelFieldList = channelFieldGroup.getList();
             int size = channelFieldList.size();
             pvDatas = new PVData[size];
             channelFields = new ChannelField[size];
             Field[] fields = new Field[size];
             for(int i=0; i<size; i++) {
                 fields[i] = channelFieldList.get(i).getField();
             }
             Structure structure = fieldCreate.createStructure(
                 "channelData", "channelData", fields);
             channelRecord = new ChannelRecord(channel,structure);
             for(int i=0; i<size; i++) {
                 ChannelField channelField = channelFieldList.get(i);
                 Field field = channelField.getField();
                 PVData pvData = null;
                 switch(field.getType()) {
                 case pvBoolean:  pvData = new BooleanData(channelRecord,field); break;
                 case pvByte:     pvData = new ByteData(channelRecord,field); break;
                 case pvShort:    pvData = new ShortData(channelRecord,field); break;
                 case pvInt:      pvData = new IntData(channelRecord,field); break;
                 case pvLong:     pvData = new LongData(channelRecord,field); break;
                 case pvFloat:    pvData = new FloatData(channelRecord,field); break;
                 case pvDouble:   pvData = new DoubleData(channelRecord,field); break;
                 case pvString:   pvData = new StringData(channelRecord,field); break;
                 case pvEnum:     pvData = new EnumData(channelRecord,field); break;
                 case pvMenu:     pvData = new EnumData(channelRecord,field); break;
                 case pvLink:     pvData = new LinkData(channelRecord,field); break;
                 case pvArray:    pvData = createArrayData(channelRecord,field); break;
                 case pvStructure: pvData = createStructureData(channelRecord,field); break;
                 }
                 if(pvData==null) return false;
                 pvDatas[i] = pvData;
                 channelFields[i] = channelField;
             }
             channelRecord.setPVDatas(pvDatas);
             channelDataPVList = new ArrayList<ChannelDataPV>(size);
             channelDataPVFreeList = new ArrayList<ChannelDataPV>(size);
             return true;
         }        
         /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelData#getChannelFieldGroup()
         */
         public ChannelFieldGroup getChannelFieldGroup() {
             return channelFieldGroup;
         }
         /* (non-Javadoc)
          * @see org.epics.ioc.ca.ChannelData#clear()
          */
         public void clear() {
             channelDataPVFreeList.addAll(channelDataPVList);
             channelDataPVList.clear();
         }
         /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelData#initData(org.epics.ioc.ca.ChannelField, org.epics.ioc.pv.PVData)
         */
        public void initData(ChannelField channelField, PVData pvData) {
            ChannelDataPVImpl channelDataPV = getChannelDataPV();
            channelDataPV.setChannelField(channelField);
            channelDataPV.setPVdata(pvData);
            channelDataPV.setInitial();
            channelDataPVList.add(channelDataPV);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelData#dataPut(org.epics.ioc.pv.PVData)
         */
        public void dataPut(PVData fromData) {
             int index = pvDatasIndex(fromData);
             PVData toData = pvDatas[index];
             Type type = fromData.getField().getType();
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
             } else if(type==Type.pvLink) {
                 PVLink from = (PVLink)fromData;
                 PVLink to = (PVLink)toData;
                 to.setConfigurationStructure(from.getConfigurationStructure());
             } else {
                 throw new IllegalStateException("Logic error");
             }
             ChannelDataPVImpl channelDataPV = getChannelDataPV();
             channelDataPV.setChannelField(channelFields[index]);
             channelDataPV.setPVdata(toData);
             channelDataPVList.add(channelDataPV);
         }

        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelData#enumChoicesPut(org.epics.ioc.pv.PVEnum)
         */
        public void enumChoicesPut(PVEnum pvEnum) {
            int index = pvDatasIndex(pvEnum);
            PVEnum toEnum = (PVEnum)pvDatas[index];
            toEnum.setChoices(pvEnum.getChoices());
            ChannelDataPVImpl channelDataPV = getChannelDataPV();
            channelDataPV.setChannelField(channelFields[index]);
            channelDataPV.setPVdata(toEnum);
            channelDataPV.setEumChoicesChange();
            channelDataPVList.add(channelDataPV);
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelData#enumIndexPut(org.epics.ioc.pv.PVEnum)
         */
        public void enumIndexPut(PVEnum pvEnum) {
            int index = pvDatasIndex(pvEnum);
            PVEnum toEnum = (PVEnum)pvDatas[index];
            toEnum.setIndex(pvEnum.getIndex());
            ChannelDataPVImpl channelDataPV = getChannelDataPV();
            channelDataPV.setChannelField(channelFields[index]);
            channelDataPV.setPVdata(toEnum);
            channelDataPV.setEnumIndexChange();
            channelDataPVList.add(channelDataPV);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelData#supportNamePut(org.epics.ioc.pv.PVData)
         */
        public void supportNamePut(PVData pvData) {
            int index = pvDatasIndex(pvData);
            PVData toData = pvDatas[index];
            toData.setSupportName(pvData.getSupportName());
            ChannelDataPVImpl channelDataPV = getChannelDataPV();
            channelDataPV.setChannelField(channelFields[index]);
            channelDataPV.setPVdata(toData);
            channelDataPV.setSupportNameChange();
            channelDataPVList.add(channelDataPV);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelData#configurationStructurePut(org.epics.ioc.pv.PVLink)
         */
        public void configurationStructurePut(PVLink pvLink) {
            int index = pvDatasIndex(pvLink);
            PVLink toLink = (PVLink)pvDatas[index];
            toLink.setConfigurationStructure(pvLink.getConfigurationStructure());
            ChannelDataPVImpl channelDataPV = getChannelDataPV();
            channelDataPV.setChannelField(channelFields[index]);
            channelDataPV.setPVdata(toLink);
            channelDataPV.setConfigurationStructureChange();
            channelDataPVList.add(channelDataPV);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelData#beginPut(org.epics.ioc.pv.PVStructure)
         */
        public void beginPut(PVStructure pvStructure) {
            // nothing to do
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelData#endPut(org.epics.ioc.pv.PVStructure)
         */
        public void endPut(PVStructure pvStructure) {
            // nothing to do
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelData#structurePut(org.epics.ioc.pv.PVStructure, org.epics.ioc.pv.PVData)
         */
        public void dataPut(PVStructure pvStructure, PVData pvData) {
            int index = pvDatasIndex(pvStructure);
            PVStructure toStructure = (PVStructure)pvDatas[index];
            PVData data = findField(toStructure,pvData);
            Type type = data.getField().getType();
            if(type.isScalar()) {
                convert.copyScalar(pvData, data);
            } else if(type==Type.pvArray) {
                PVArray from = (PVArray)pvData;
                PVArray to = (PVArray)data;
                convert.copyArray(from,0, to, 0, from.getLength());
            } else {
                throw new IllegalStateException("Logic error");
            }
            ChannelDataPVImpl channelDataPV = getChannelDataPV();
            channelDataPV.setChannelField(channelFields[index]);
            channelDataPV.setPVdata(data);
            channelDataPVList.add(channelDataPV);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelData#enumChoicesPut(org.epics.ioc.pv.PVStructure, org.epics.ioc.pv.PVEnum)
         */
        public void enumChoicesPut(PVStructure pvStructure, PVEnum pvEnum) {
            int index = pvDatasIndex(pvStructure);
            PVStructure toStructure = (PVStructure)pvDatas[index];
            PVEnum toEnum = (PVEnum)findField(toStructure,pvEnum);
            toEnum.setChoices(pvEnum.getChoices());
            ChannelDataPVImpl channelDataPV = getChannelDataPV();
            channelDataPV.setChannelField(channelFields[index]);
            channelDataPV.setPVdata(toEnum);
            channelDataPV.setEumChoicesChange();
            channelDataPVList.add(channelDataPV);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelData#enumIndexPut(org.epics.ioc.pv.PVStructure, org.epics.ioc.pv.PVEnum)
         */
        public void enumIndexPut(PVStructure pvStructure, PVEnum pvEnum) {
            int index = pvDatasIndex(pvStructure);
            PVStructure toStructure = (PVStructure)pvDatas[index];
            PVEnum toEnum = (PVEnum)findField(toStructure,pvEnum);
            toEnum.setIndex(pvEnum.getIndex());
            ChannelDataPVImpl channelDataPV = getChannelDataPV();
            channelDataPV.setChannelField(channelFields[index]);
            channelDataPV.setPVdata(toEnum);
            channelDataPV.setEnumIndexChange();
            channelDataPVList.add(channelDataPV);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelData#supportNamePut(org.epics.ioc.pv.PVStructure, org.epics.ioc.pv.PVData)
         */
        public void supportNamePut(PVStructure pvStructure, PVData pvData) {
            int index = pvDatasIndex(pvStructure);
            PVStructure toStructure = (PVStructure)pvDatas[index];
            PVData toData = findField(toStructure,pvData);
            toData.setSupportName(pvData.getSupportName());
            ChannelDataPVImpl channelDataPV = getChannelDataPV();
            channelDataPV.setChannelField(channelFields[index]);
            channelDataPV.setPVdata(toData);
            channelDataPV.setSupportNameChange();
            channelDataPVList.add(channelDataPV);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelData#configurationStructurePut(org.epics.ioc.pv.PVStructure, org.epics.ioc.pv.PVLink)
         */
        public void configurationStructurePut(PVStructure pvStructure, PVLink pvLink) {
            int index = pvDatasIndex(pvStructure);
            PVStructure toStructure = (PVStructure)pvDatas[index];
            PVLink toLink = (PVLink)findField(toStructure,pvLink);
            toLink.setConfigurationStructure(pvLink.getConfigurationStructure());
            ChannelDataPVImpl channelDataPV = getChannelDataPV();
            channelDataPV.setChannelField(channelFields[index]);
            channelDataPV.setPVdata(toLink);
            channelDataPV.setConfigurationStructureChange();
            channelDataPVList.add(channelDataPV);
        }
         /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelData#getChannelDataPVList()
         */
        public List<ChannelDataPV> getChannelDataPVList() {
            return channelDataPVList;
        }
 
         private PVData createArrayData(PVData parent,Field field) {
             Array array = (Array)field;
             switch(array.getElementType()) {
             case pvBoolean:  return new BooleanArray(parent,array);
             case pvByte:     return new ByteArray(parent,array);
             case pvShort:    return new ShortArray(parent,array);
             case pvInt:      return new IntArray(parent,array);
             case pvLong:     return new LongArray(parent,array);
             case pvFloat:    return new FloatArray(parent,array);
             case pvDouble:   return new DoubleArray(parent,array);
             case pvString:   return new StringArray(parent,array);
             case pvMenu:
             case pvEnum:     return new EnumArray(parent,array);
             case pvLink:     return new LinkArray(parent,array);
             case pvArray:    return new ArrayArray(parent,array);
             case pvStructure: return new StructureArray(parent,array);
             }
             return null;
         }
         
         private PVData createStructureData(PVData parent,Field structureField) {
             Structure structure = (Structure)structureField;
             Field[] fields = structure.getFields();
             int length = fields.length;
             if(length<=0) return null;
             PVData[] pvDataArray = new PVData[length];
             StructureData structureData = new StructureData(parent,structureField,pvDataArray);
             for(int i=0; i<length; i++) {
                 PVData pvData = null;
                 Field field = fields[i];
                 switch(field.getType()) {
                 case pvBoolean:  pvData = new BooleanData(structureData,field); break;
                 case pvByte:     pvData = new ByteData(structureData,field); break;
                 case pvShort:    pvData = new ShortData(structureData,field); break;
                 case pvInt:      pvData = new IntData(structureData,field); break;
                 case pvLong:     pvData = new LongData(structureData,field); break;
                 case pvFloat:    pvData = new FloatData(structureData,field); break;
                 case pvDouble:   pvData = new DoubleData(structureData,field); break;
                 case pvString:   pvData = new StringData(structureData,field); break;
                 case pvEnum:     pvData = new EnumData(structureData,field); break;
                 case pvArray:    pvData = createArrayData(structureData,field); break;
                 case pvStructure: pvData = createStructureData(structureData,field); break;
                 }
                 if(pvData==null) {
                     throw new IllegalStateException("unsupported type");
                 }
                 pvDataArray[i] = pvData;
             }
             return structureData;
         }
         // find the pvData[i] that corresponds to pvData
         private int pvDatasIndex(PVData pvData) {
             Field fromField = pvData.getField();
             PVData toData = null;
             for(int i=0; i< pvDatas.length; i++) {
                 toData = pvDatas[i];
                 if(toData.getField()==fromField) {
                     return i;
                 }
             }
             throw new IllegalStateException("Logic error");
         }
         // recursively look for fromData
         private PVData findField(PVStructure pvStructure,PVData fromData) {
             PVData[] datas = pvStructure.getFieldPVDatas();
             Field fromField = fromData.getField();
             for(int i=0; i<datas.length; i++) {
                 PVData data = datas[i];
                 if(data.getField()==fromField) return data;
                 if(data.getField().getType()!=Type.pvStructure) continue;
                 PVStructure dataStruct = (PVStructure)data;
                 data = findField(dataStruct,fromData);
                 if(data!=null) return data;
             }
             throw new IllegalStateException("Logic error");
         }
         // get a free ChannelDataPVImpl or allocate one
         private ChannelDataPVImpl getChannelDataPV() {
             ChannelDataPVImpl channelDataPV;
             if(channelDataPVFreeList.isEmpty()) {
                 channelDataPV = new ChannelDataPVImpl(this);
             } else {
                 channelDataPV = (ChannelDataPVImpl)
                     channelDataPVFreeList.remove(channelDataPVFreeList.size()-1);
             }
             return channelDataPV;
         }
     }
     
     private static class ChannelDataPVImpl implements ChannelDataPV {
        private ChannelData channelData;
        private ChannelField channelField;
        private PVData pvData;
        private boolean isInitial;
        private boolean configurationStructureChange;
        private boolean enumChoicesChange;
        private boolean enumIndexChange;
        private boolean supportNameChange;
        
        private ChannelDataPVImpl(ChannelData channelData) {
            this.channelData = channelData;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelDataPV#getChannelData()
         */
        public ChannelData getChannelData() {
            return channelData;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelDataPV#getChannelField()
         */
        public ChannelField getChannelField() {
            return channelField;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelDataPV#getPVData()
         */
        public PVData getPVData() {
            return pvData;
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelDataPV#isInitial()
         */
        public boolean isInitial() {
            return isInitial;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelDataPV#enumChoicesChange()
         */
        public boolean enumChoicesChange() {
            return enumChoicesChange;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelDataPV#enumIndexChange()
         */
        public boolean enumIndexChange() {
            return enumIndexChange;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelDataPV#supportNameChange()
         */
        public boolean supportNameChange() {
            return supportNameChange;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelDataPV#configurationStructureChange()
         */
        public boolean configurationStructureChange() {
            return configurationStructureChange;
        }
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() {
            StringBuilder builder = new StringBuilder();
            if(channelField!=null) {
                builder.append("channelField " + channelField.getField().getFieldName() + " ");
            }
            if(pvData!=null) {
                builder.append("pvField " + pvData.getField().getFieldName() + " ");
            }
            if(isInitial) builder.append("isInitial " + Boolean.toString(isInitial) + " ");
            if(enumChoicesChange) builder.append("enumChoicesChange " + Boolean.toString(enumChoicesChange) + " ");
            if(enumIndexChange) builder.append("enumIndexChange " + Boolean.toString(enumIndexChange) + " ");
            if(supportNameChange) builder.append(
                "supportNameChange " + Boolean.toString(supportNameChange) + " ");
            if(configurationStructureChange) builder.append(
                "configurationStructureChange " + Boolean.toString(configurationStructureChange) + " ");
            return builder.toString();
        }
        
        private void setChannelField(ChannelField channelField) {
            this.channelField = channelField;
            isInitial = false;
            configurationStructureChange = false;
            enumChoicesChange = false;
            enumIndexChange = false;
            supportNameChange = false;
        }
        
        private void setPVdata(PVData pvData) {
            this.pvData = pvData;
        }
        
        private void setInitial() {
            isInitial = true;
        }
        private void setConfigurationStructureChange() {
            configurationStructureChange = true;
        }
        
        private void setEumChoicesChange() {
            enumChoicesChange = true;
        }
        
        private void setEnumIndexChange() {
            enumIndexChange = true;
        }
        
        private void setSupportNameChange() {
            supportNameChange = true;
        }
         
     }
     
     private static class ChannelRecord extends AbstractPVData implements PVRecord {
         private Channel channel;
         private String recordName;
         private PVData[] pvDatas;
         
         private ChannelRecord(Channel channel,Structure structure) {
             super(null,structure);
             this.channel = channel;
             recordName = channel.getChannelName();
         }
         
         /* (non-Javadoc)
         * @see org.epics.ioc.pv.AbstractPVData#message(java.lang.String, org.epics.ioc.util.MessageType)
         */
        @Override
        public void message(String message, MessageType messageType) {
            channel.message(message, messageType);
        }

        private void setPVDatas(PVData[] pvDatas) {
             this.pvDatas = pvDatas;
         }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVRecord#getRecordName()
         */
        public String getRecordName() {
            return recordName;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVStructure#beginPut()
         */
        public void beginPut() {
            // Nothing to do
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVStructure#endPut()
         */
        public void endPut() {
            // Nothing to do
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVStructure#getFieldPVDatas()
         */
        public PVData[] getFieldPVDatas() {
            return pvDatas;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVStructure#replaceStructureField(java.lang.String, java.lang.String)
         */
        public boolean replaceStructureField(String fieldName, String structureName) {
            // nothing to do
            return false;
        }
     }
     
     private static abstract class Data implements PVData {
         private Field field;
         private PVData parent;
         private PVRecord pvRecord;
         private String fullFieldName;
         private String supportName = null;
         
         private Data(PVData parent,Field field) {
             this.parent = parent;
             this.field = field;
             pvRecord = parent.getPVRecord();
             fullFieldName = "." + field.getFieldName();
             PVData newParent = parent;
             while(newParent!=null) {
                 fullFieldName = "." + newParent.getField().getFieldName() + fullFieldName;
                 newParent = newParent.getParent();
             }
         }
         
         /* (non-Javadoc)
          * @see org.epics.ioc.util.Requestor#getRequestorName()
          */
         public String getRequestorName() {
             return getFullFieldName();
         }     
         /* (non-Javadoc)
          * @see org.epics.ioc.util.Requestor#message(java.lang.String, org.epics.ioc.util.MessageType)
          */
         public void message(String message, MessageType messageType) {
             System.out.printf("%s %s",messageType.toString(),message);
         }
         /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVData#getFullFieldName()
         */
        public String getFullFieldName() {
            return fullFieldName;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVData#getParent()
         */
        public PVData getParent() {
            return parent;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVData#getRecord()
         */
        public PVRecord getPVRecord() {
            return pvRecord;
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

         private BooleanData(PVData parent,Field field) {
             super(parent,field);
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
         
         private ByteData(PVData parent,Field field) {
             super(parent,field);
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

         private ShortData(PVData parent,Field field) {
             super(parent,field);
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
         
         private IntData(PVData parent,Field field) {
             super(parent,field);
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

         private LongData(PVData parent,Field field) {
             super(parent,field);
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

         private FloatData(PVData parent,Field field) {
             super(parent,field);
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

         private DoubleData(PVData parent,Field field) {
             super(parent,field);
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

         private StringData(PVData parent,Field field) {
             super(parent,field);
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

         private EnumData(PVData parent,Field field) {
             super(parent,field);
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
         
         private LinkData(PVData parent,Field field) {
             super(parent,field);
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
     
         private DataArray(PVData parent,Array array) {
             super(parent,array);
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
     
         private BooleanArray(PVData parent,Array array) {
             super(parent,array);
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
     
         private ByteArray(PVData parent,Array array) {
             super(parent,array);
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
     
         private ShortArray(PVData parent,Array array) {
             super(parent,array);
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
     
         private IntArray(PVData parent,Array array) {
             super(parent,array);
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
     
         private LongArray(PVData parent,Array array) {
             super(parent,array);
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
     
         private FloatArray(PVData parent,Array array) {
             super(parent,array);
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
     
         private DoubleArray(PVData parent,Array array) {
             super(parent,array);
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
     
         private StringArray(PVData parent,Array array) {
             super(parent,array);
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
     
         private EnumArray(PVData parent,Array array) {
             super(parent,array);
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
     
         private LinkArray(PVData parent,Array array) {
             super(parent,array);
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
     
         private StructureArray(PVData parent,Array array) {
             super(parent,array);
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
     
         ArrayArray(PVData parent,Array array) {
             super(parent,array);
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
         
         private StructureData(PVData parent,Field field,PVData[] pvDatas) {
             super(parent,field);
             fieldPVDatas = pvDatas;
         }
         /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVStructure#replaceField(java.lang.String, org.epics.ioc.pv.PVData)
         */
        public boolean replaceStructureField(String fieldName, String structureName) {
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
