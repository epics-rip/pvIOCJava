/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pv;

import java.util.regex.Pattern;


/**
 * @author mrk
 *
 */
public class PVPropertyFactory {
    private PVPropertyFactory() {} // don't create
    private static PVPropertyImpl pvProperty = new PVPropertyImpl();
    /**
     * Get the interface for PVDataCreate.
     * @return The interface.
     */
    public static PVProperty getPVProperty() {
        return pvProperty;
    }
    
    private static final class PVPropertyImpl implements PVProperty{
        protected static Pattern commaSpacePattern = Pattern.compile("[, ]");
        protected static  Pattern periodPattern = Pattern.compile("[.]");
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVProperty#findProperty(org.epics.ioc.pv.PVField, java.lang.String)
         */
        public PVField findProperty(PVField pvField,String fieldName) {
            if(fieldName==null || fieldName.length()==0) return null;
            String[] names = periodPattern.split(fieldName,2);
            if(names.length<=0) {
                return null;
            }
            PVField currentField = pvField;
            while(true) {
                String name = names[0];
                int startBracket = name.indexOf('[');
                if(startBracket<0) {
                    currentField = findField(currentField,name);
                } else {
                    if(startBracket==0) {
                        currentField = getArray(currentField,name);
                    } else {
                        String currentName = name.substring(0,startBracket);
                        currentField = findField(currentField,currentName);
                        name = name.substring(startBracket);
                        currentField = getArray(currentField,name);
                    }
                }
                if(currentField==null) break;;
                if(names.length<=1) break;
                names = periodPattern.split(names[1],2);
            }
            if(currentField==null) {
                if(fieldName.equals("timeStamp")) {
                    return findPropertyViaParent(pvField,fieldName);
                }
                String name = pvField.getField().getFieldName(); 
                if(name.equals("value")) {
                    return findField(pvField.getParent(),fieldName);
                }
            }
            return currentField;
        }
        
        
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVProperty#findPropertyViaParent(org.epics.ioc.pv.PVField, java.lang.String)
         */
        public PVField findPropertyViaParent(PVField pvf,String propertyName) {
            PVField currentPVField = pvf;
            PVField parentPVField = currentPVField.getParent();
            while(parentPVField!=null) {
                if(parentPVField.getField().getType()==Type.pvStructure) {
                    PVStructure parentPVStructure = (PVStructure)parentPVField;
                    for(PVField pvField : parentPVStructure.getPVFields()) {
                        Field field = pvField.getField();
                        if(field.getFieldName().equals(propertyName)) {
                            if(field.getType()==Type.pvStructure) {
                                PVStructure pvStructure = (PVStructure)pvField;
                                if(pvStructure.getSupportName()==null
                                && pvStructure.getStructure().getStructureName().equals("null")) break;
                            }
                            return pvField;
                        }
                    }
                }
                parentPVField = parentPVField.getParent();
            }
            return null;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVProperty#getPropertyNames(org.epics.ioc.pv.PVField)
         */
        public String[] getPropertyNames(PVField pv) {
            PVField pvField = pv;
            boolean skipValue = false;
            Field field = pvField.getField();
            String timeStamp = null;
            if(field.getFieldName().equals("value")) {
                if(pvField.getParent()!=null) {
                    pvField = pvField.getParent();
                    skipValue = true;
                }
            }
            field = pvField.getField();
            if(field.getType()!=Type.pvStructure) return null;
            PVStructure pvStructure = (PVStructure)pvField;
            PVField[] pvFields = pvStructure.getPVFields();
            int size = 0;
            boolean addTimeStamp = true;
            for(PVField pvf: pvFields) {
                field = pvf.getField();
                String fieldName = field.getFieldName();
                if(fieldName.equals("timeStamp")) addTimeStamp = false;
                if(skipValue && fieldName.equals("value")) continue;
                size++;
            }
            if(timeStamp==null) {
                
            }
            PVField pvTimeStamp = null;
            if(addTimeStamp) {
                pvTimeStamp = findPropertyViaParent(pv,"timeStamp");
                if(pvTimeStamp!=null) size++;
            }
            if(size==0) return null;
            String[] propertyNames = new String[size];
            int index = 0;
            if(pvTimeStamp!=null) {
                propertyNames[index++] = pvTimeStamp.getField().getFieldName();
            }
            for(PVField pvf: pvFields) {
                field = pvf.getField();
                String fieldName = field.getFieldName();
                if(skipValue && fieldName.equals("value")) continue;
                propertyNames[index++] = pvf.getField().getFieldName();
            }
            return propertyNames;
        }
        
        private static PVField getArray(PVField pvField,String fieldName) {
            int startBracket = fieldName.indexOf('[');
            int endBracket = fieldName.indexOf(']');
            if(startBracket!=0 || endBracket<=startBracket) {
                throw new IllegalStateException(fieldName + " mismatched []");
            }
            if(pvField.getField().getType()!=Type.pvArray) {
                throw new IllegalStateException("[] but pvField = not PVArray");
            }
            PVArray pvArray = (PVArray)pvField;
            Type elementType = pvArray.getArray().getElementType();
            if(elementType==Type.pvStructure) {
                PVStructureArray pvStructureArray = (PVStructureArray)pvArray;
                StructureArrayData data = new StructureArrayData();
                int len = pvStructureArray.get(0,pvArray.getLength(), data);
                String name = fieldName.substring(0, endBracket+1);
                PVField currentField = null;
                for(int index =0; index<len; index++) {
                    PVStructure now = data.data[index];
                    if(now==null) continue;
                    if(now.getField().getFieldName().equals(name)) {
                        currentField = now; break;
                    }
                }
                if(currentField==null) return null;
                fieldName = fieldName.substring(endBracket+1);
                if(fieldName.length()<=0) return currentField;
                return getArray(currentField,fieldName);
            } else if(elementType==Type.pvArray) {
                PVArrayArray pvArrayArray = (PVArrayArray)pvArray;
                ArrayArrayData data = new ArrayArrayData();
                int len = pvArrayArray.get(0,pvArray.getLength(), data);
                String name = fieldName.substring(0, endBracket+1);
                PVField currentField = null;
                for(int index =0; index<len; index++) {
                    PVArray now = data.data[index];
                    if(now==null) continue;
                    if(now.getField().getFieldName().equals(name)) {
                        currentField = now; break;
                    }
                }
                if(currentField==null) return null;
                fieldName = fieldName.substring(endBracket+1);
                if(fieldName.length()<=0) return currentField;
                return getArray(currentField,fieldName);
            }
            throw new IllegalStateException("[] Logic Error");
        }
        
        private PVField findField(PVField pvField,String name) {
            if(pvField==null) return null;
            Field field = pvField.getField();
            Type type = field.getType();
            if(type.isScalar()) {
                return null;
            }
            if(type==Type.pvStructure) {
                PVStructure pvStructure = (PVStructure)pvField;
                PVField[] pvFields = pvStructure.getPVFields();
                Structure structure = pvStructure.getStructure();
                int dataIndex = structure.getFieldIndex(name);
                if(dataIndex>=0) return pvFields[dataIndex];
                return null;
            } else if(type==Type.pvArray) {
                PVArray pvArray = (PVArray)pvField;
                Type elementType = pvArray.getArray().getElementType();
                if(elementType.isScalar()) return null;
                if(elementType==Type.pvStructure) {
                    PVStructureArray pvStructureArray = (PVStructureArray)pvArray;
                    StructureArrayData data = new StructureArrayData();
                    int len = pvStructureArray.get(0, pvArray.getLength(), data);
                    PVStructure[] pvStructures = data.data;
                    for(int index=0; index<len; index++) {
                        PVStructure next = pvStructures[index];
                        if(next==null) continue;
                        if(next.getField().getFieldName().equals(name)) return next;
                    }
                    return null;
                } else if(elementType==Type.pvArray) {
                    PVArrayArray pvArrayArray = (PVArrayArray)pvArray;
                    ArrayArrayData data = new ArrayArrayData();
                    int len = pvArrayArray.get(0, pvArray.getLength(), data);
                    PVArray[] pvArrays = data.data;
                    for(int index=0; index<len; index++) {
                        PVArray next = pvArrays[index];
                        if(next==null) continue;
                        if(next.getField().getFieldName().equals(name)) return next;
                    }
                    return null;
                }
            }
            throw new IllegalStateException("Logic error");
        }
    }
}
