/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pv;

import java.util.regex.Pattern;

import org.epics.ioc.util.*;



/**
 * @author mrk
 *
 */
public class PVAccessFactory {
    public static PVAccess createPVAccess(PVRecord pvRecord) {
        return new Access(pvRecord);
    }
    
    private static class Access implements PVAccess {
        private PVRecord pvRecord;
        private PVData pvDataSetField;
        static private Pattern periodPattern = Pattern.compile("[.]");
        //following are for setName(String name)
        private String otherRecord = null;
        private String otherField = null;

        
        private Access(PVRecord pvRecord) {
            this.pvRecord = pvRecord;
            pvDataSetField = null;
        }    
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVAccess#getPVRecord()
         */
        public PVRecord getPVRecord() {
            return pvRecord;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVAccess#getField()
         */
        public PVData getField() {
            return pvDataSetField;
        }   
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVAccess#setField(java.lang.String)
         */
        public AccessSetResult findField(String fieldName) {
            if(fieldName==null || fieldName.length()==0) {
                pvDataSetField = pvRecord;
                return AccessSetResult.thisRecord;
            }
            otherRecord = null;
            String[] names = periodPattern.split(fieldName,2);
            if(names.length<=0) {
                return AccessSetResult.notFound;
            }
            PVData currentData = pvDataSetField;
            if(currentData==null) currentData = pvRecord;
            while(true) {
                String name = names[0];
                int arrayIndex = -1;
                int startBracket = name.indexOf('[');
                if(startBracket>=0) {
                    String arrayIndexString = name.substring(startBracket+1);
                    name = name.substring(0,startBracket);
                    int endBracket = arrayIndexString.indexOf(']');
                    if(endBracket<0) break;
                    arrayIndexString = arrayIndexString.substring(0,endBracket);
                    arrayIndex = Integer.parseInt(arrayIndexString);
                }
                PVData newData = findField(currentData,name);
                currentData = newData;
                if(currentData==null) break;
                if(arrayIndex>=0) {
                    Type type = currentData.getField().getType();
                    if(type!=Type.pvArray) break;
                    PVArray pvArray = (PVArray)currentData;
                    Array field = (Array)pvArray.getField();
                    Type elementType = field.getElementType();
                    if(elementType==Type.pvStructure) {
                        PVStructureArray pvStructureArray =
                            (PVStructureArray)currentData;
                        if(arrayIndex>=pvStructureArray.getLength()) break;
                        StructureArrayData data = new StructureArrayData();
                        int n = pvStructureArray.get(arrayIndex,1,data);
                        PVStructure[] structureArray = data.data;
                        int offset = data.offset;
                        if(n<1 || structureArray[offset]==null) {
                            currentData = null;
                            break;
                        }
                        currentData = (PVData)structureArray[offset];
                    } else if(elementType==Type.pvArray) {
                        PVArrayArray pvArrayArray = (PVArrayArray)currentData;
                        if(arrayIndex>=pvArrayArray.getLength()) break;
                        ArrayArrayData data = new ArrayArrayData();
                        int n = pvArrayArray.get(arrayIndex,1,data);
                        PVArray[] arrayArray = data.data;
                        int offset = data.offset;
                        if(n<1 || arrayArray[offset]==null) {
                            currentData = null;
                            break;
                        }
                        currentData = (PVData)arrayArray[offset];
                        break;
                    } else if(elementType==Type.pvLink) {
                        PVLinkArray pvLinkArray = (PVLinkArray)currentData;
                        if(arrayIndex>=pvLinkArray.getLength()) break;
                        LinkArrayData data = new LinkArrayData();
                        int n = pvLinkArray.get(arrayIndex,1,data);
                        PVLink[] linkArray = data.data;
                        int offset = data.offset;
                        if(n<1 || linkArray[offset]==null) {
                            currentData = null;
                            break;
                        }
                        currentData = (PVData)linkArray[offset];
                        break;
                    } else if(elementType==Type.pvMenu) {
                        PVMenuArray pvMenuArray = (PVMenuArray)currentData;
                        if(arrayIndex>=pvMenuArray.getLength()) break;
                        MenuArrayData data = new MenuArrayData();
                        int n = pvMenuArray.get(arrayIndex,1,data);
                        PVMenu[] menuArray = data.data;
                        int offset = data.offset;
                        if(n<1 || menuArray[offset]==null) {
                            currentData = null;
                            break;
                        }
                        currentData = (PVData)menuArray[offset];
                        break;
                    } else if(elementType==Type.pvEnum){
                        PVEnumArray pvEnumArray = (PVEnumArray)currentData;
                        if(arrayIndex>=pvEnumArray.getLength()) break;
                        EnumArrayData data = new EnumArrayData();
                        int n = pvEnumArray.get(arrayIndex,1,data);
                        PVEnum[] enumArray = data.data;
                        int offset = data.offset;
                        if(n<1 || enumArray[offset]==null) {
                            currentData = null;
                            break;
                        }
                        currentData = (PVData)enumArray[offset];
                        break;
                    } else {
                        currentData = null;
                        break;
                    }
                }
                if(currentData==null) break;
                if(currentData.getField().getType()==Type.pvLink) {
                    if(otherRecord!=null) {
                        if(names.length>1) otherField += "." + names[1];
                        currentData = null;
                        break;
                    }
                    if(names.length<=1) break;
                    PVLink pvLink = (PVLink)currentData;
                    lookForRemote(pvLink,names[1]);
                    currentData = null;
                    break;
                }
                if(names.length<=1) break;
                names = periodPattern.split(names[1],2);
            }
            if(currentData==null) {
                if(otherRecord==null) return AccessSetResult.notFound;
                return AccessSetResult.otherRecord;
            }
            pvDataSetField = currentData;
            return AccessSetResult.thisRecord;
        }                
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVAccess#setField(org.epics.ioc.pv.PVData)
         */
        public void setPVField(PVData pvData) {
            if(pvData==null) {
                pvDataSetField = pvRecord;
                return;
            }
            if(pvData.getPVRecord()!=pvRecord) 
                throw new IllegalArgumentException (
                    "field is not in this record instance");
            pvDataSetField = (PVData)pvData;
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVAccess#getOtherField()
         */
        public String getOtherField() {
            return otherField;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVAccess#getOtherRecord()
         */
        public String getOtherRecord() {
            return otherRecord;
        }        

        
        private PVData findField(PVData pvData,String name) {
            PVData newField = getPVStructureField(pvData,name);
            if(newField!=null) return newField;
            Property property = pvData.getField().getProperty(name);
            return findPropertyField(pvData,property);
            
        }
        
        private PVData  findPropertyField(PVData pvData,
            Property property)
        {
            if(property==null) return null;
            PVRecord pvRecord = pvData.getPVRecord();
            String propertyFieldName = property.getAssociatedFieldName();
            if(propertyFieldName.charAt(0)=='/') {
                propertyFieldName = propertyFieldName.substring(1);
                pvData = pvRecord;
            }
            String[] names = periodPattern.split(propertyFieldName,0);
            int length = names.length;
            if(length<1 || length>2) {
                pvData.message(
                        " recordType "
                     + ((Structure)pvRecord.getField()).getStructureName()
                     + " has a bad property definition "
                     + property.toString(),
                     MessageType.error);
                return null;
            }
            PVData newField = null;
            if(pvData.getField().getType()==Type.pvStructure) {
                newField = getPVStructureField(pvData,names[0]);
            } else {
                newField = getPVStructureField(pvData.getParent(),names[0]);
            }
            if(newField==pvData) {
                pvData.message(
                        " recordType "
                     + ((Structure)pvRecord.getField()).getStructureName()
                     + " has a recursive property  = "
                     + property.toString(),
                     MessageType.error);
                return null;
            }
            pvData = newField;
            if(pvData==null) return null;
            if(length==2) {
                Type type = pvData.getField().getType();
                if(type==Type.pvLink) {
                    PVLink pvLink = (PVLink)pvData;
                    lookForRemote(pvLink,names[1]);
                    return pvData;
                }
                if(type==Type.pvStructure) {
                    newField = getPVStructureField(pvData,names[1]);
                } else {
                    newField = getPVStructureField(pvData.getParent(),names[1]);
                }
                if(newField!=null) {
                    pvData = newField;
                } else {
                    property = pvData.getField().getProperty(names[1]);
                    if(property!=null) {
                        pvData = findPropertyField(pvData,property);
                    }
                }
            }
            return pvData;            
        }
        
        private PVData getPVStructureField(PVData pvData, String fieldName) {
            if(pvData.getField().getType()!=Type.pvStructure)  return null;
            PVStructure pvStructure = (PVStructure)pvData;
            PVData[] pvDatas = pvStructure.getFieldPVDatas();
            Structure structure = (Structure)pvStructure.getField();
            int dataIndex = structure.getFieldIndex(fieldName);
            if(dataIndex>=0) {
                return (PVData)pvDatas[dataIndex];
            }
            return null;
        }
        
        private void lookForRemote(PVLink pvLink,String fieldName)
        {
            PVStructure config = pvLink.getConfigurationStructure();
            if(config==null) return;
            PVString pvname = null;
            for(PVData pvdata: config.getFieldPVDatas()) {
                FieldAttribute attribute = pvdata.getField().getFieldAttribute();
                if(attribute.isLink()) {
                    if(pvdata.getField().getType()==Type.pvString) {
                        pvname = (PVString)pvdata;
                        break;
                    }
                }
            }
            if(pvname==null) return;
            String[] subFields = periodPattern.split(pvname.get(),2);
            otherRecord = subFields[0];
            if(subFields.length>1) {
                otherField = subFields[1];
            } else {   
                otherField = fieldName;
            }
        }
    }
}
