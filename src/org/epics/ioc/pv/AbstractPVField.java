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
        createFullNameANDFullFieldName();
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
        createFullNameANDFullFieldName();
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
    
    private void createFullNameANDFullFieldName() {
        if(this==record) {
            fullName = record.getRecordName();
            return;
        }
        StringBuilder fieldName = new StringBuilder();
        Field field = getField();
        String name = field.getFieldName();
        if(name!=null && name.length()>0) {
            fieldName.append(name);
            if(parent.getField().getType()!=Type.pvArray) fieldName.insert(0, ".");
        }
        PVField parent = getParent();
        while(parent!=this.record) {
            field = parent.getField();
            name = field.getFieldName();
            boolean gotName = false;
            if(name!=null && name.length()>0) {
                fieldName.insert(0,parent.getField().getFieldName());
                gotName = true;
            }
            parent = parent.getParent();
            if(gotName && parent!=null && parent.getField().getType()!=Type.pvArray) fieldName.insert(0, ".");
        }
        if(fieldName.length()>0 && fieldName.charAt(0)=='.') {
            fullFieldName = fieldName.substring(1); //remove leading "."
        }
        if(record!=null) {
            fullName = record.getRecordName() + "." + fullFieldName;
        }
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
