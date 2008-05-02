/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pv;

import java.util.regex.Pattern;

import org.epics.ioc.util.MessageType;

/**
 * Abstract base class for a PVField.
 * A factory that implements PVField should extend this class.
 * @author mrk
 *
 */
public abstract class AbstractPVField implements PVField{
    private boolean isMutable = true;
    private String fullFieldName = "";
    private String fullName = "";
    private Field field;
    private PVField parent;
    private PVRecord record;
    private String supportName = null;
    private String createName = null;
    private PVEnumerated pvEnumerated = null;
    
    protected static Convert convert = ConvertFactory.getConvert();
    protected static Pattern commaSpacePattern = Pattern.compile("[, ]");
    protected static  Pattern periodPattern = Pattern.compile("[.]");

    
    private AsynAccessListener asynAccessListener = null;
       
    /**
     * Constructor that must be called by derived classes.
     * @param parent The parent PVField.
     * @param field The introspection interface for the PVField.
     */
    protected AbstractPVField(PVField parent, Field field) {
        this.field = field;
        this.parent = parent;
        supportName = field.getSupportName();
        if(parent==null) return;
        record = parent.getPVRecord();
        createFullFieldAndRequesterNames();
    }
    /**
     * Called by derived classes to replace a field.
     * @param field The new field.
     */
    protected void replaceField(Field field) {
        this.field = field;
    }
    /**
     * Called by derived class to specify the PVRecord interface.
     * @param record The PVRecord interface.
     */
    protected void setRecord(PVRecord record) {
        this.record = record;
        createFullFieldAndRequesterNames();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.util.Requester#getRequesterName()
     */
    public String getRequesterName() {
        return getFullName();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.util.Requester#message(java.lang.String, org.epics.ioc.util.MessageType)
     */
    public void message(String message, MessageType messageType) {
        if(record==null) {
            System.out.println(
                    messageType.toString() + " " + fullFieldName + " " + message);
        } else {
            record.message(fullFieldName + " " + message, messageType);
        }
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVField#isMutable()
     */
    public boolean isMutable() {
        return(isMutable);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVField#setMutable(boolean)
     */
    public void setMutable(boolean value) {
        isMutable = value;
        
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVField#getFullFieldName()
     */
    public String getFullFieldName() {
        return fullFieldName;
    } 
    
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVField#getFullName()
     */
    public String getFullName() {
        return fullName;
    }

    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVField#getField()
     */
    public Field getField() {
        return field;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVField#getParent()
     */
    public PVField getParent() {
        return parent;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVField#getPVRecord()
     */
    public PVRecord getPVRecord() {
       return record;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVField#replacePVField(org.epics.ioc.pv.PVField)
     */
    public void replacePVField(PVField newPVField) {
        if(this.getField().getType()!=newPVField.getField().getType()) {
            throw new IllegalArgumentException(
                "newField is not same type as oldField");
        }
        PVField parent = getParent();
        if(parent==null) throw new IllegalArgumentException("no parent");
        Type parentType = parent.getField().getType();
        if(parentType==Type.pvStructure) {
            PVField[] fields = ((PVStructure)parent).getPVFields();
            for(int i=0; i<fields.length; i++) {
                if(fields[i]==this) {
                    fields[i] = newPVField;
                    return;
                }
            }
        }
        throw new IllegalArgumentException("oldField not found in parent");
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVField#getSubField(java.lang.String)
     */
    public PVField getSubField(String fieldName) {
        return null;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVField#getPVEnumerated()
     */
    public synchronized PVEnumerated getPVEnumerated() {
        if(pvEnumerated!=null) return pvEnumerated;
        if(field.getType()!=Type.pvStructure) return null;
        PVStructure pvStructure = (PVStructure)this;
        PVField[]pvFields = pvStructure.getPVFields();
        Structure structure = pvStructure.getStructure();
        Field[] fields = structure.getFields();
        Field field = fields[0];
        if(!field.getFieldName().equals("index") || field.getType()!=Type.pvInt) {
            pvStructure.message("structure does not have field index of type int", MessageType.error);
            return null;
        } 
        field = fields[1];
        if(!field.getFieldName().equals("choice") || field.getType()!=Type.pvString) {
            pvStructure.message("structure does not have field choice of type string", MessageType.error);
            return null;
        }
        field = fields[2];
        if(!field.getFieldName().equals("choices") || field.getType()!=Type.pvArray) {
            pvStructure.message("structure does not have field choices of type array", MessageType.error);
            return null;
        }
        Array array = (Array)fields[2];
        if(array.getElementType()!=Type.pvString) {
            pvStructure.message("elementType for choices is not string", MessageType.error);
            return null;
        }
        pvEnumerated = new PVEnumeratedImpl(
            this,(PVInt)pvFields[0],(PVString)pvFields[1],(PVStringArray)pvFields[2]);
        return pvEnumerated;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVField#findProperty(java.lang.String)
     */
    public PVField findProperty(String fieldName) {
        if(fieldName==null || fieldName.length()==0) return null;
        String[] names = periodPattern.split(fieldName,2);
        if(names.length<=0) {
            return null;
        }
        PVField currentField = this;
        while(true) {
            String name = names[0];
            int arrayIndex = -1;
            int startBracket = name.indexOf('[');
            String nextBracket = null;
            if(startBracket>=0) {
                String arrayIndexString = name.substring(startBracket+1);
                name = name.substring(0,startBracket);
                int endBracket = arrayIndexString.indexOf(']');
                if(endBracket<0) return null;
                if(endBracket<(arrayIndexString.length()-1)) {
                    nextBracket = arrayIndexString.substring(endBracket+1);
                }
                arrayIndexString = arrayIndexString.substring(0,endBracket);
                arrayIndex = Integer.parseInt(arrayIndexString);
            }
            currentField = findField(currentField,name);
            if(currentField==null) return null;
            while(arrayIndex>=0) {
                Type type = currentField.getField().getType();
                if(type!=Type.pvArray) return null;
                PVArray pvArray = (PVArray)currentField;
                Array field = (Array)pvArray.getField();
                Type elementType = field.getElementType();
                if(elementType==Type.pvStructure) {
                    PVStructureArray pvStructureArray =
                        (PVStructureArray)currentField;
                    if(arrayIndex>=pvStructureArray.getLength()) return null;
                    StructureArrayData data = new StructureArrayData();
                    int n = pvStructureArray.get(arrayIndex,1,data);
                    PVStructure[] structureArray = data.data;
                    int offset = data.offset;
                    if(n<1 || structureArray[offset]==null) return null;
                    currentField = (PVField)structureArray[offset];
                } else if(elementType==Type.pvArray) {
                    PVArrayArray pvArrayArray = (PVArrayArray)currentField;
                    if(arrayIndex>=pvArrayArray.getLength()) break;
                    ArrayArrayData data = new ArrayArrayData();
                    int n = pvArrayArray.get(arrayIndex,1,data);
                    PVArray[] arrayArray = data.data;
                    int offset = data.offset;
                    if(n<1 || arrayArray[offset]==null) return null;
                    currentField = (PVField)arrayArray[offset];
                    if(nextBracket==null) break;
                    startBracket = nextBracket.indexOf('[');
                    if(startBracket<0) return null;
                    String arrayIndexString = nextBracket.substring(startBracket+1);
                    int endBracket = arrayIndexString.indexOf(']');
                    if(endBracket<0) return null;
                    if(endBracket<(arrayIndexString.length()-1)) {
                        nextBracket = arrayIndexString.substring(endBracket+1);
                    }
                    arrayIndexString = arrayIndexString.substring(0,endBracket);
                    arrayIndex = Integer.parseInt(arrayIndexString);
                    continue;
                } else {
                    return null;
                }
                break;
            }
            if(names.length<=1) break;
            names = periodPattern.split(names[1],2);
        }
        return currentField;
    }

    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVField#findPropertyViaParent(java.lang.String)
     */
    public PVField findPropertyViaParent(String propertyName) {
        PVField currentPVField = this;
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
     * @see org.epics.ioc.pv.PVField#getPropertyNames()
     */
    public String[] getPropertyNames() {
        PVField pvField = this;
        boolean skipValue = false;
        Field field = this.getField();
        if(field.getFieldName().equals("value")) {
            if(this.parent!=null) {
                pvField = this.parent;
                skipValue = true;
            }
        }
        field = pvField.getField();
        if(field.getType()!=Type.pvStructure) return null;
        PVStructure pvStructure = (PVStructure)pvField;
        PVField[] pvFields = pvStructure.getPVFields();
        int size = 0;
        for(PVField pvf: pvFields) {
            field = pvf.getField();
            String fieldName = field.getFieldName();
            if(skipValue && fieldName.equals("value")) continue;
            // findProperty will skip null structures
            if(pvField.findProperty(fieldName)!=null) size++;
        }
        if(size==0) return null;
        String[] propertyNames = new String[size];
        int index = 0;
        for(PVField pvf: pvFields) {
            field = pvf.getField();
            String fieldName = field.getFieldName();
            if(skipValue && fieldName.equals("value")) continue;
            // findProperty will skip null structures
            if(pvField.findProperty(fieldName)!=null) {
                propertyNames[index++] = pvf.getField().getFieldName();
            }
        }
        return propertyNames;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVField#getSupportName()
     */
    public String getSupportName() {
        return supportName;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVField#setSupportName(java.lang.String)
     */
    public void setSupportName(String name) {
        supportName = name;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVField#getCreateName()
     */
    public String getCreateName() {
        return createName;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVField#setCreateName(java.lang.String)
     */
    public void setCreateName(String name) {
        createName = name;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVField#asynAccessCallListener(boolean)
     */
    public void asynAccessCallListener(boolean begin) {
        if(asynAccessListener==null) return;       
        if(begin) {
            asynAccessListener.beginSyncAccess();
        } else {
            asynAccessListener.endSyncAccess();
        }
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVField#asynModifyEnd(org.epics.ioc.pv.AsynAccessListener)
     */
    public void asynAccessEnd(AsynAccessListener asynAccessListener) {
        if(asynAccessListener==null || asynAccessListener!=this.asynAccessListener) {
            throw new IllegalStateException("not the accessListener");
        }
        this.asynAccessListener = null;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVField#asynModifyStart(org.epics.ioc.pv.AsynAccessListener)
     */
    public boolean asynAccessStart(AsynAccessListener asynAccessListener) {
        if(this.asynAccessListener!=null) return false;
        this.asynAccessListener = asynAccessListener;
        return true;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVField#isAsynModifyActive()
     */
    public boolean isAsynAccessActive() {
        return (asynAccessListener==null) ? false : true;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVField#toString(int)
     */
    public String toString(int indentLevel) {
        String returnValue = "";
        if(supportName!=null) {
            returnValue = " supportName " + supportName;
        }
        String createName = field.getCreateName();
        if(createName==null) createName = this.createName;
        if(createName!=null) {
            returnValue += " createName " + createName;
        }
        return returnValue;
    }
    
    private void createFullFieldAndRequesterNames() {
        if(this==record) {
            fullFieldName = fullName = record.getRecordName();
            return;
        }
        StringBuilder fieldName = new StringBuilder();
        fieldName.append(getField().getFieldName());
        if(parent.getField().getType()!=Type.pvArray) fieldName.insert(0, ".");
        PVField parent = getParent();
        while(parent!=this.record) {
            fieldName.insert(0,parent.getField().getFieldName());
            parent = parent.getParent();
            if(parent!=null && parent.getField().getType()!=Type.pvArray) fieldName.insert(0, ".");
        }
        fullFieldName = fieldName.substring(1); //remove leading "."
        if(record!=null) {
            fullName = record.getRecordName() + "." + fullFieldName;
        }
    }
    
    private PVField findField(PVField pvField,String name) {
        if(pvField==null) return null;
        Field field = pvField.getField();
        String pvFieldName = field.getFieldName();
        if(field.getType()!=Type.pvStructure)  {
            if(pvFieldName.equals("value")) {
                return findField(pvField.getParent(),name);
            }
            return null;
        }
        PVStructure pvStructure = (PVStructure)pvField;
        PVField[] pvFields = pvStructure.getPVFields();
        Structure structure = pvStructure.getStructure();
        int dataIndex = structure.getFieldIndex(name);
        if(dataIndex>=0) {
            PVField pvf = pvFields[dataIndex];
            boolean notNullStructure = true;
            field = pvf.getField();
            if(field.getType()==Type.pvStructure)  {
                pvStructure = (PVStructure)pvf;
                pvFields = pvStructure.getPVFields();
                structure = pvStructure.getStructure();
                String supportName = pvStructure.getSupportName();
                if(supportName==null || supportName.length()<=0) {
                    supportName = structure.getSupportName();
                }
                if(supportName!=null && supportName.length()<=0) supportName = null;
                if(pvFields.length==0 && supportName==null) {
                    notNullStructure = false;
                }
            }
            if(notNullStructure) return pvf;
        }
        if(name.equals("timeStamp")) {
            return pvField.findPropertyViaParent(name);
        }
        if(pvFieldName.equals("value")) {
            pvField = pvField.getParent();
            if(pvField!=null) return findField(pvField,name);
        }
        return null;
    }
    
    private static class PVEnumeratedImpl implements PVEnumerated {
        private PVField pvField;
        private PVInt pvIndex;
        private PVString pvChoice;
        private PVStringArray pvChoices;
        
        private PVEnumeratedImpl(PVField pvField,PVInt pvIndex, PVString pvChoice, PVStringArray pvChoices) {
            this.pvField = pvField;
            this.pvIndex = pvIndex;
            this.pvChoice = pvChoice;
            this .pvChoices = pvChoices;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVEnumerated#getChoiceField()
         */
        public PVString getChoiceField() {
            return pvChoice;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVEnumerated#getChoicesField()
         */
        public PVStringArray getChoicesField() {
            return pvChoices;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVEnumerated#getIndexField()
         */
        public PVInt getIndexField() {
            return pvIndex;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVEnumerated#getPVField()
         */
        public PVField getPVField() {
            return pvField;
        }
        
    }
}
