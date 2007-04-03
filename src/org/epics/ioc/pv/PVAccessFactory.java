/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pv;

import java.util.regex.Pattern;

import org.epics.ioc.util.*;



/**
 * Factory for creating PVAccess instances.
 * @author mrk
 *
 */
public class PVAccessFactory {
    /**
     * Create a PVAccess instance.
     * @param pvRecord The record to be accessed.
     * @return The PVAccess interface.
     */
    public static PVAccess createPVAccess(PVRecord pvRecord) {
        return new Access(pvRecord);
    }
    
    private static class Access implements PVAccess {
        private PVRecord pvRecord;
        private PVField pvFieldSetField;
        static private Pattern periodPattern = Pattern.compile("[.]");
        //following are for setName(String name)
        private String otherRecord = null;
        private String otherField = null;

        
        private Access(PVRecord pvRecord) {
            this.pvRecord = pvRecord;
            pvFieldSetField = null;
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
        public PVField getField() {
            return pvFieldSetField;
        }   
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVAccess#setField(java.lang.String)
         */
        public AccessSetResult findField(String fieldName) {
            if(fieldName==null || fieldName.length()==0) {
                pvFieldSetField = pvRecord;
                return AccessSetResult.thisRecord;
            }
            otherRecord = null;
            String[] names = periodPattern.split(fieldName,2);
            if(names.length<=0) {
                return AccessSetResult.notFound;
            }
            PVField currentField = pvFieldSetField;
            if(currentField==null) currentField = pvRecord;
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
                PVField newField = findField(currentField,name);
                currentField = newField;
                if(currentField==null) break;
                if(arrayIndex>=0) {
                    Type type = currentField.getField().getType();
                    if(type!=Type.pvArray) break;
                    PVArray pvArray = (PVArray)currentField;
                    Array field = (Array)pvArray.getField();
                    Type elementType = field.getElementType();
                    if(elementType==Type.pvStructure) {
                        PVStructureArray pvStructureArray =
                            (PVStructureArray)currentField;
                        if(arrayIndex>=pvStructureArray.getLength()) break;
                        StructureArrayData data = new StructureArrayData();
                        int n = pvStructureArray.get(arrayIndex,1,data);
                        PVStructure[] structureArray = data.data;
                        int offset = data.offset;
                        if(n<1 || structureArray[offset]==null) {
                            currentField = null;
                            break;
                        }
                        currentField = (PVField)structureArray[offset];
                    } else if(elementType==Type.pvArray) {
                        PVArrayArray pvArrayArray = (PVArrayArray)currentField;
                        if(arrayIndex>=pvArrayArray.getLength()) break;
                        ArrayArrayData data = new ArrayArrayData();
                        int n = pvArrayArray.get(arrayIndex,1,data);
                        PVArray[] arrayArray = data.data;
                        int offset = data.offset;
                        if(n<1 || arrayArray[offset]==null) {
                            currentField = null;
                            break;
                        }
                        currentField = (PVField)arrayArray[offset];
                        break;
                    } else if(elementType==Type.pvLink) {
                        PVLinkArray pvLinkArray = (PVLinkArray)currentField;
                        if(arrayIndex>=pvLinkArray.getLength()) break;
                        LinkArrayData data = new LinkArrayData();
                        int n = pvLinkArray.get(arrayIndex,1,data);
                        PVLink[] linkArray = data.data;
                        int offset = data.offset;
                        if(n<1 || linkArray[offset]==null) {
                            currentField = null;
                            break;
                        }
                        currentField = (PVField)linkArray[offset];
                        break;
                    } else if(elementType==Type.pvMenu) {
                        PVMenuArray pvMenuArray = (PVMenuArray)currentField;
                        if(arrayIndex>=pvMenuArray.getLength()) break;
                        MenuArrayData data = new MenuArrayData();
                        int n = pvMenuArray.get(arrayIndex,1,data);
                        PVMenu[] menuArray = data.data;
                        int offset = data.offset;
                        if(n<1 || menuArray[offset]==null) {
                            currentField = null;
                            break;
                        }
                        currentField = (PVField)menuArray[offset];
                        break;
                    } else if(elementType==Type.pvEnum){
                        PVEnumArray pvEnumArray = (PVEnumArray)currentField;
                        if(arrayIndex>=pvEnumArray.getLength()) break;
                        EnumArrayData data = new EnumArrayData();
                        int n = pvEnumArray.get(arrayIndex,1,data);
                        PVEnum[] enumArray = data.data;
                        int offset = data.offset;
                        if(n<1 || enumArray[offset]==null) {
                            currentField = null;
                            break;
                        }
                        currentField = (PVField)enumArray[offset];
                        break;
                    } else {
                        currentField = null;
                        break;
                    }
                }
                if(currentField==null) break;
                if(currentField.getField().getType()==Type.pvLink) {
                    if(otherRecord!=null) {
                        if(names.length>1) otherField += "." + names[1];
                        currentField = null;
                        break;
                    }
                    if(names.length<=1) break;
                    PVLink pvLink = (PVLink)currentField;
                    lookForRemote(pvLink,names[1]);
                    currentField = null;
                    break;
                }
                if(names.length<=1) break;
                names = periodPattern.split(names[1],2);
            }
            if(currentField==null) {
                if(otherRecord==null) return AccessSetResult.notFound;
                return AccessSetResult.otherRecord;
            }
            pvFieldSetField = currentField;
            return AccessSetResult.thisRecord;
        }                
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVAccess#setField(org.epics.ioc.pv.PVField)
         */
        public void setPVField(PVField pvField) {
            if(pvField==null) {
                pvFieldSetField = pvRecord;
                return;
            }
            if(pvField.getPVRecord()!=pvRecord) 
                throw new IllegalArgumentException (
                    "field is not in this record instance");
            pvFieldSetField = (PVField)pvField;
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
       
        private PVField findField(PVField pvField,String name) {
            PVField newField = getPVStructureField(pvField,name);
            if(newField!=null) return newField;
            Property property = pvField.getField().getProperty(name);
            return findPropertyField(pvField,property);
            
        }
        
        private PVField  findPropertyField(PVField pvField,
            Property property)
        {
            if(property==null) return null;
            PVRecord pvRecord = pvField.getPVRecord();
            String propertyFieldName = property.getAssociatedFieldName();
            if(propertyFieldName.charAt(0)=='/') {
                propertyFieldName = propertyFieldName.substring(1);
                pvField = pvRecord;
            }
            String[] names = periodPattern.split(propertyFieldName,0);
            int length = names.length;
            if(length<1 || length>2) {
                pvField.message(
                        " recordType "
                     + ((Structure)pvRecord.getField()).getStructureName()
                     + " has a bad property definition "
                     + property.toString(),
                     MessageType.error);
                return null;
            }
            PVField newField = null;
            if(pvField.getField().getType()==Type.pvStructure) {
                newField = getPVStructureField(pvField,names[0]);
            } else {
                newField = getPVStructureField(pvField.getParent(),names[0]);
            }
            if(newField==pvField) {
                pvField.message(
                        " recordType "
                     + ((Structure)pvRecord.getField()).getStructureName()
                     + " has a recursive property  = "
                     + property.toString(),
                     MessageType.error);
                return null;
            }
            pvField = newField;
            if(pvField==null) return null;
            if(length==2) {
                Type type = pvField.getField().getType();
                if(type==Type.pvLink) {
                    PVLink pvLink = (PVLink)pvField;
                    lookForRemote(pvLink,names[1]);
                    return pvField;
                }
                if(type==Type.pvStructure) {
                    newField = getPVStructureField(pvField,names[1]);
                } else {
                    newField = getPVStructureField(pvField.getParent(),names[1]);
                }
                if(newField!=null) {
                    pvField = newField;
                } else {
                    property = pvField.getField().getProperty(names[1]);
                    if(property!=null) {
                        pvField = findPropertyField(pvField,property);
                    }
                }
            }
            return pvField;            
        }
        
        private PVField getPVStructureField(PVField pvField, String fieldName) {
            if(pvField.getField().getType()!=Type.pvStructure)  return null;
            PVStructure pvStructure = (PVStructure)pvField;
            PVField[] pvDatas = pvStructure.getFieldPVFields();
            Structure structure = (Structure)pvStructure.getField();
            int dataIndex = structure.getFieldIndex(fieldName);
            if(dataIndex>=0) {
                return (PVField)pvDatas[dataIndex];
            }
            return null;
        }
        
        private void lookForRemote(PVLink pvLink,String fieldName)
        {
            PVStructure config = pvLink.getConfigurationStructure();
            if(config==null) return;
            PVString pvname = null;
            for(PVField pvdata: config.getFieldPVFields()) {
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