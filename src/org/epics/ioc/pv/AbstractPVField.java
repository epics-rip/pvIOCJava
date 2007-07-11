/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pv;

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
        if(this.getField().getType()!=newPVField.getField().getType()) {
            throw new IllegalArgumentException(
                "newField is not same type as oldField");
        }
        if(!(newPVField instanceof PVField)) {
            throw new IllegalArgumentException(
            "newField is not a PVField");
        }
        PVField parent = getParent();
        if(parent==null) throw new IllegalArgumentException("no parent");
        Type parentType = parent.getField().getType();
        if(parentType==Type.pvStructure) {
            PVField[] fields = ((PVStructure)parent).getFieldPVFields();
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
        if(supportName!=null) {
            return " supportName " + supportName;
        }
        return "";
    }
    
    private void createFullFieldAndRequesterNames() {
        if(this==record) {
            fullFieldName = fullName = "";
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
}
