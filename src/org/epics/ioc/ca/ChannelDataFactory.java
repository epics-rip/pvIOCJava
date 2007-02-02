/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;

import java.util.*;
import java.util.concurrent.locks.*;

import org.epics.ioc.pv.*;
import org.epics.ioc.pv.Enum;
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
     * @param channelFieldGroup The field group defining what should be in each channelData.
     * @return The ChannelDataQueue interface.
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
     
     /**
     * CAData is required because for the PVData in ChannelData
     * 1) The Field must have no properties.
     * 2) Given a ChannelField it must be possible to find the Channeldata
     *
     */
    private interface CAData extends PVData {
        public Field getChannelDataField();
        public void setChannelDataField(Field field);
     }
     
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
         private CAData[] caDatas;
         private ChannelField[] channelFields;
         private ArrayList<ChannelDataPV> channelDataPVList;
         private ArrayList<ChannelDataPV> channelDataPVFreeList;
        
         private ChannelDataImpl(Channel channel, ChannelFieldGroup channelFieldGroup)
         {
             this.channel = channel;
             this.channelFieldGroup = channelFieldGroup;
         }
         

         
         public boolean createData() {
             List<ChannelField> channelFieldList = channelFieldGroup.getList();
             int size = channelFieldList.size();
             caDatas = new CAData[size];
             channelFields = new ChannelField[size];
             Field[] oldFields = new Field[size];
             Field[] newFields = new Field[size];
             for(int i=0; i<size; i++) {
                 oldFields[i] = channelFieldList.get(i).getField();
                 newFields[i] = createField(oldFields[i]);
             }
             Structure structure = fieldCreate.createStructure(
                 "channelData", "channelData", oldFields);
             channelRecord = new ChannelRecord(channel,structure);
             for(int i=0; i<size; i++) {
                 Field field = newFields[i];
                 CAData caData = null;
                 switch(field.getType()) {
                 case pvBoolean:  caData = new BooleanData(channelRecord,field); break;
                 case pvByte:     caData = new ByteData(channelRecord,field); break;
                 case pvShort:    caData = new ShortData(channelRecord,field); break;
                 case pvInt:      caData = new IntData(channelRecord,field); break;
                 case pvLong:     caData = new LongData(channelRecord,field); break;
                 case pvFloat:    caData = new FloatData(channelRecord,field); break;
                 case pvDouble:   caData = new DoubleData(channelRecord,field); break;
                 case pvString:   caData = new StringData(channelRecord,field); break;
                 case pvEnum:     caData = new EnumData(channelRecord,field,null); break;
                 case pvMenu:     caData = new MenuData(channelRecord,(Menu)field); break;
                 case pvLink:     caData = new LinkData(channelRecord,field); break;
                 case pvArray:    caData = createArrayData(channelRecord,field); break;
                 case pvStructure: caData = createStructureData(channelRecord,field,oldFields[i]); break;
                 }
                 if(caData==null) return false;
                 caData.setChannelDataField(oldFields[i]);
                 caDatas[i] = caData;
                 channelFields[i] = channelFieldList.get(i);
             }
             channelRecord.setCADatas(caDatas);
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
            ChannelDataPVImpl channelDataPV = getChannelDataPV(channelField,pvData);
            channelDataPV.setInitial(true);
            channelDataPVList.add(channelDataPV);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelData#dataPut(org.epics.ioc.pv.PVData)
         */
        public void dataPut(PVData fromData) {
             int index = caDatasIndex(fromData);
             PVData toData = caDatas[index];
             getChannelDataPV(channelFields[index],toData);
             Type type = fromData.getField().getType();
             if(type.isScalar()) {
                 convert.copyScalar(fromData, toData);
             } else if(type==Type.pvArray) {
                 PVArray from = (PVArray)fromData;
                 PVArray to = (PVArray)toData;
                 convert.copyArray(from,0, to, 0, from.getLength());
             } else if(type==Type.pvEnum) {
                 PVEnum from = (PVEnum)fromData;
                 PVEnum to = (PVEnum)toData;
                 to.setChoices(from.getChoices());
                 to.setIndex(from.getIndex());
             } else if(type==Type.pvMenu) {
                 PVMenu from = (PVMenu)fromData;
                 PVMenu to = (PVMenu)toData;
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
         }

        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelData#enumChoicesPut(org.epics.ioc.pv.PVEnum)
         */
        public void enumChoicesPut(PVEnum pvEnum) {
            int index = caDatasIndex(pvEnum);
            PVEnum toEnum = (PVEnum)caDatas[index];
            toEnum.setChoices(pvEnum.getChoices());
            ChannelDataPVImpl channelDataPV = getChannelDataPV(channelFields[index],toEnum);
            channelDataPV.setEumChoicesChange();
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelData#enumIndexPut(org.epics.ioc.pv.PVEnum)
         */
        public void enumIndexPut(PVEnum pvEnum) {
            int index = caDatasIndex(pvEnum);
            PVEnum toEnum = (PVEnum)caDatas[index];
            toEnum.setIndex(pvEnum.getIndex());
            ChannelDataPVImpl channelDataPV = getChannelDataPV(channelFields[index],toEnum);
            channelDataPV.setEnumIndexChange();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelData#supportNamePut(org.epics.ioc.pv.PVData)
         */
        public void supportNamePut(PVData pvData) {
            int index = caDatasIndex(pvData);
            PVData toData = caDatas[index];
            toData.setSupportName(pvData.getSupportName());
            ChannelDataPVImpl channelDataPV = getChannelDataPV(channelFields[index],toData);
            channelDataPV.setSupportNameChange();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelData#configurationStructurePut(org.epics.ioc.pv.PVLink)
         */
        public void configurationStructurePut(PVLink pvLink) {
            int index = caDatasIndex(pvLink);
            PVLink toLink = (PVLink)caDatas[index];
            toLink.setConfigurationStructure(pvLink.getConfigurationStructure());
            ChannelDataPVImpl channelDataPV = getChannelDataPV(channelFields[index],toLink);
            channelDataPV.setConfigurationStructureChange();
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
            int index = caDatasIndex(pvStructure);
            PVStructure toStructure = (PVStructure)caDatas[index];
            PVData toData = findField(toStructure,pvData);
            getChannelDataPV(channelFields[index],toData);
            Type type = toData.getField().getType();
            if(type.isScalar()) {
                convert.copyScalar(pvData, toData);
            } else if(type==Type.pvArray) {
                PVArray from = (PVArray)pvData;
                PVArray to = (PVArray)toData;
                convert.copyArray(from,0, to, 0, from.getLength());
            } else {
                throw new IllegalStateException("Logic error");
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelData#enumChoicesPut(org.epics.ioc.pv.PVStructure, org.epics.ioc.pv.PVEnum)
         */
        public void enumChoicesPut(PVStructure pvStructure, PVEnum pvEnum) {
            int index = caDatasIndex(pvStructure);
            PVStructure toStructure = (PVStructure)caDatas[index];
            PVEnum toEnum = (PVEnum)findField(toStructure,pvEnum);
            toEnum.setChoices(pvEnum.getChoices());
            ChannelDataPVImpl channelDataPV = getChannelDataPV(channelFields[index],toEnum);
            channelDataPV.setEumChoicesChange();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelData#enumIndexPut(org.epics.ioc.pv.PVStructure, org.epics.ioc.pv.PVEnum)
         */
        public void enumIndexPut(PVStructure pvStructure, PVEnum pvEnum) {
            int index = caDatasIndex(pvStructure);
            PVStructure toStructure = (PVStructure)caDatas[index];
            PVEnum toEnum = (PVEnum)findField(toStructure,pvEnum);
            toEnum.setIndex(pvEnum.getIndex());
            ChannelDataPVImpl channelDataPV = getChannelDataPV(channelFields[index],toEnum);
            channelDataPV.setEnumIndexChange();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelData#supportNamePut(org.epics.ioc.pv.PVStructure, org.epics.ioc.pv.PVData)
         */
        public void supportNamePut(PVStructure pvStructure, PVData pvData) {
            int index = caDatasIndex(pvStructure);
            PVStructure toStructure = (PVStructure)caDatas[index];
            PVData toData = findField(toStructure,pvData);
            toData.setSupportName(pvData.getSupportName());
            ChannelDataPVImpl channelDataPV = getChannelDataPV(channelFields[index],toData);
            channelDataPV.setSupportNameChange();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelData#configurationStructurePut(org.epics.ioc.pv.PVStructure, org.epics.ioc.pv.PVLink)
         */
        public void configurationStructurePut(PVStructure pvStructure, PVLink pvLink) {
            int index = caDatasIndex(pvStructure);
            PVStructure toStructure = (PVStructure)caDatas[index];
            PVLink toLink = (PVLink)findField(toStructure,pvLink);
            toLink.setConfigurationStructure(pvLink.getConfigurationStructure());
            ChannelDataPVImpl channelDataPV = getChannelDataPV(channelFields[index],toLink);
            channelDataPV.setConfigurationStructureChange();
        }
         /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelData#getChannelDataPVList()
         */
        public List<ChannelDataPV> getChannelDataPVList() {
            return channelDataPVList;
        }
        
        // create a Field like original except no properties
        private static Field createField(Field oldField) {
            Field newField = null;
            Type type = oldField.getType();
            FieldAttribute fieldAttribute = oldField.getFieldAttribute();
            Property[] property = new Property[0];
            String fieldName = oldField.getFieldName();
            if(type==Type.pvArray) {
                newField = fieldCreate.createArray(
                    fieldName,((Array)oldField).getElementType(),
                    property,fieldAttribute);
            } else if(type==Type.pvEnum) {
                Enum enumField = (Enum)oldField;
                newField = fieldCreate.createEnum(
                    fieldName, enumField.isChoicesMutable(),
                    property,fieldAttribute);
            } else if(type==Type.pvMenu) {
                Menu menu = (Menu)oldField;
                newField = fieldCreate.createMenu(
                    fieldName, menu.getMenuName(),
                    menu.getMenuChoices(),property, fieldAttribute);
            } else if(type==Type.pvStructure) {
                Structure structure = (Structure)oldField;
                Field[] oldFields = structure.getFields();
                Field[] newFields = new Field[oldFields.length];
                for(int i=0; i<oldFields.length; i++) {
                    newFields[i] = createField(oldFields[i]);
                }
                newField = fieldCreate.createStructure(
                    fieldName, structure.getStructureName(),newFields,
                    property,fieldAttribute);
            } else {
                newField = fieldCreate.createField(
                    fieldName, type,
                    property,fieldAttribute);
            }
            return newField;
        }
        
        private CAData createArrayData(PVData parent,Field field) {
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
            case pvMenu:     return new MenuArray(parent,array);
            case pvEnum:     return new EnumArray(parent,array);
            case pvLink:     return new LinkArray(parent,array);
            case pvArray:    return new ArrayArray(parent,array);
            case pvStructure: return new StructureArray(parent,array);
            }
            return null;
        }
         
         private CAData createStructureData(PVData parent,Field newField,Field oldField) {
             Structure newStructure = (Structure)newField;
             Field[] newFields = newStructure.getFields();
             Structure oldStructure = (Structure)oldField;
             Field[] oldFields = oldStructure.getFields();
             int length = newFields.length;
             if(length<=0) return null;
             CAData[] pvDataArray = new CAData[length];
             StructureData structureData = new StructureData(parent,newField,pvDataArray);
             for(int i=0; i<length; i++) {
                 CAData pvData = null;
                 Field field = newFields[i];
                 switch(field.getType()) {
                 case pvBoolean:  pvData = new BooleanData(structureData,field); break;
                 case pvByte:     pvData = new ByteData(structureData,field); break;
                 case pvShort:    pvData = new ShortData(structureData,field); break;
                 case pvInt:      pvData = new IntData(structureData,field); break;
                 case pvLong:     pvData = new LongData(structureData,field); break;
                 case pvFloat:    pvData = new FloatData(structureData,field); break;
                 case pvDouble:   pvData = new DoubleData(structureData,field); break;
                 case pvString:   pvData = new StringData(structureData,field); break;
                 case pvMenu:     pvData = new MenuData(structureData,(Menu)field); break;
                 case pvEnum:     pvData = new EnumData(structureData,field,null); break;
                 case pvArray:    pvData = createArrayData(structureData,field); break;
                 case pvStructure:
                     pvData = createStructureData(structureData,field,oldFields[i]);
                     break;
                 }
                 if(pvData==null) {
                     throw new IllegalStateException("unsupported type");
                 }
                 pvData.setChannelDataField(oldFields[i]);
                 pvDataArray[i] = pvData;
             }
             return structureData;
         }
         // find the caData[i] that corresponds to pvData
         private int caDatasIndex(PVData pvData) {
             Field fromField = pvData.getField();
             CAData toData = null;
             for(int i=0; i< caDatas.length; i++) {
                 toData = caDatas[i];
                 if(toData.getChannelDataField()==fromField) {
                     return i;
                 }
             }
             throw new IllegalStateException("Logic error");
         }
         // recursively look for fromData
         private CAData findField(PVStructure pvStructure,PVData fromData) {
             PVData[] datas = pvStructure.getFieldPVDatas();
             Field fromField = fromData.getField();
             for(int i=0; i<datas.length; i++) {
                 CAData data = (CAData)datas[i];
                 if(data.getChannelDataField()==fromField) return data;
                 if(data.getField().getType()!=Type.pvStructure) continue;
                 PVStructure dataStruct = (PVStructure)data;
                 data = findField(dataStruct,fromData);
                 if(data!=null) return data;
             }
             throw new IllegalStateException("Logic error");
         }
         // get a free ChannelDataPVImpl or allocate one
         private ChannelDataPVImpl getChannelDataPV(ChannelField channelField,PVData pvData) {
             ChannelDataPVImpl channelDataPV;
             for(ChannelDataPV dataPV: channelDataPVList)  {
                 if(dataPV.getChannelField()==channelField && dataPV.getPVData()==pvData) {
                     return (ChannelDataPVImpl)dataPV;
                 }
             }
             if(channelDataPVFreeList.isEmpty()) {
                 channelDataPV = new ChannelDataPVImpl(this,channelField,pvData);
             } else {
                 channelDataPV = (ChannelDataPVImpl)
                     channelDataPVFreeList.remove(channelDataPVFreeList.size()-1);
                 channelDataPV.setChannelField(channelField);
                 channelDataPV.setPVdata(pvData);
                 channelDataPV.setInitial(false);
             }
             channelDataPVList.add(channelDataPV);
             return channelDataPV;
         }
     }
     
     private static class ChannelDataPVImpl implements ChannelDataPV {
        private ChannelData channelData;
        private ChannelField channelField;
        private PVData pvData;
        private boolean isInitial = false;
        private boolean configurationStructureChange;
        private boolean enumChoicesChange;
        private boolean enumIndexChange;
        private boolean supportNameChange;
        
        private ChannelDataPVImpl(ChannelData channelData,ChannelField channelField,PVData pvData) {
            this.channelData = channelData;
            this.channelField = channelField;
            this.pvData = pvData;
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
        
        private void setInitial(boolean value) {
            isInitial = value;
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
         private CAData[] caDatas;
         
         private ChannelRecord(Channel channel,Structure structure) {
             super(null,structure);
             this.channel = channel;
             recordName = channel.getChannelName();
         }
         
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.AbstractPVData#replacePVData(org.epics.ioc.pv.PVData)
         */
        @Override
        public void replacePVData(PVData newPVData) {
            // for now do nothing
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pv.AbstractPVData#message(java.lang.String, org.epics.ioc.util.MessageType)
         */
        @Override
        public void message(String message, MessageType messageType) {
            channel.message(message, messageType);
        }

        public void setCADatas(CAData[] caDatas) {
             this.caDatas = caDatas;
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
            return caDatas;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVStructure#replaceStructureField(java.lang.String, java.lang.String)
         */
        public boolean replaceStructureField(String fieldName, String structureName) {
            // nothing to do
            return false;
        }
     }
     
     private static abstract class Data implements CAData {
         private Field field;
         private Field pvDataField;
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
             while(newParent!=pvRecord) {
                 fullFieldName = "." + newParent.getField().getFieldName() + fullFieldName;
                 newParent = newParent.getParent();
             }
         }
         
         public Field getChannelDataField() {
             return pvDataField;
         }
         
         public void setChannelDataField(Field field) {
             pvDataField = field;
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
         * @see org.epics.ioc.pv.PVData#replacePVData(org.epics.ioc.pv.PVData)
         */
        public void replacePVData(PVData newPVData) {
            // for now do nothing
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

         private EnumData(PVData parent,Field field, String[]choice) {
             super(parent,field);
             index = 0;
             if(choice==null) choice = EMPTY_STRING_ARRAY;
             this.choice = choice;
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
     
     
     private static class MenuData extends EnumData implements PVMenu {
         
         public MenuData(PVData parent,Menu menu) {
             super(parent,menu,menu.getMenuChoices());
         }

         public boolean setChoices(String[] choice) {
             throw new UnsupportedOperationException(
                 "Menu choices can not be modified");
         }    
         /* (non-Javadoc)
          * @see java.lang.Object#toString()
          */
         public String toString() { return getString(0);}
         /* (non-Javadoc)
          * @see org.epics.ioc.db.AbstractDBData#toString(int)
          */
         public String toString(int indentLevel) {
             return getString(indentLevel);
         }

         private String getString(int indentLevel) {
             StringBuilder builder = new StringBuilder();
             convert.newLine(builder,indentLevel);
             Menu menu = (Menu)super.getField();
             builder.append("menu(" + menu.getMenuName() + ")" + " {");
             convert.newLine(builder,indentLevel+1);
             builder.append(super.toString(indentLevel+1));
             convert.newLine(builder,indentLevel);
             builder.append("}");
             return builder.toString();
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
     
     
     private static class MenuArray extends DataArray implements PVMenuArray {
         private PVMenu[] value;
     
         private MenuArray(PVData parent,Array array) {
             super(parent,array);
             value = new PVMenu[0];
         }
         /* (non-Javadoc)
          * @see org.epics.ioc.pv.PVMenuArray#get(int, int, org.epics.ioc.pv.MenuArrayData)
          */
         public int get(int offset, int len, MenuArrayData data) {
             int n = len;
             if(offset+len > length) n = length;
             data.data = value;
             data.offset = offset;
             return n;
         }
         /* (non-Javadoc)
          * @see org.epics.ioc.pv.PVMenuArray#put(int, int, org.epics.ioc.pv.MenuArrayData)
          */
         public int put(int offset, int len, PVMenu[] from, int fromOffset) {
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
         public Type getElementType() {return Type.pvMenu;}
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
