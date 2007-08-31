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
         * @see org.epics.ioc.pv.PVAccess#setField(java.lang.String)
         */
        public PVField findField(String fieldName) {
            if(fieldName==null || fieldName.length()==0) {
                pvFieldSetField = pvRecord;
                return pvFieldSetField;
            }
            String[] names = periodPattern.split(fieldName,2);
            if(names.length<=0) {
                return null;
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
                    } else {
                        currentField = null;
                        break;
                    }
                }
                if(currentField==null) break;
                if(names.length<=1) break;
                names = periodPattern.split(names[1],2);
            }
            if(currentField==null) {
                return null;
            }
            pvFieldSetField = currentField;
            return pvFieldSetField;
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
       
        private PVField findField(PVField pvField,String name) {
            PVField pvStructureField = getPVStructureField(pvField,name);
            if(pvStructureField!=null) return pvStructureField;
            Property property = pvField.getField().getProperty(name);
            PVField pvPropertyField = findPropertyField(pvField,property);
            if(pvPropertyField!=null) return pvPropertyField;
            PVField pvParent = pvField.getParent();
            while(pvParent!=null) {
                pvStructureField = getPVStructureField(pvParent,name);
                if(pvStructureField!=null) return pvStructureField;
                pvParent = pvParent.getParent();
            }
            return null;
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
    }
}
