/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pv;

import org.epics.ioc.util.MessageType;

/**
 * Abstract base class for a PVData.
 * A factory that implements PVData should extend this class.
 * @author mrk
 *
 */
public abstract class AbstractPVData implements PVData{
    private String fullFieldName = "";
    private String requestorName = "";
    private Field field;
    private PVData parent;
    private PVRecord record;
    private String supportName = null;
       
    /**
     * Constructor that must be called by derived classes.
     * @param parent The parent PVData.
     * @param field The introspection interface for the PVData.
     */
    protected AbstractPVData(PVData parent, Field field) {
        this.field = field;
        this.parent = parent;
        supportName = field.getSupportName();
        if(parent==null) return;
        record = parent.getPVRecord();
        createFullFieldAndRequestorNames();
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
    public void setRecord(PVRecord record) {
        this.record = record;
        createFullFieldAndRequestorNames();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVData#replacePVData(org.epics.ioc.pv.PVData, org.epics.ioc.pv.PVData)
     */
    public void replacePVData(PVData newPVData) {
        if(this.getField().getType()!=newPVData.getField().getType()) {
            throw new IllegalArgumentException(
                "newField is not same type as oldField");
        }
        if(this.getField().getType()!=newPVData.getField().getType()) {
            throw new IllegalArgumentException(
                "newField is not same type as oldField");
        }
        if(!(newPVData instanceof PVData)) {
            throw new IllegalArgumentException(
            "newField is not a PVData");
        }
        PVData parent = getParent();
        if(parent==null) throw new IllegalArgumentException("no parent");
        Type parentType = parent.getField().getType();
        if(parentType==Type.pvStructure) {
            PVData[] fields = ((PVStructure)parent).getFieldPVDatas();
            for(int i=0; i<fields.length; i++) {
                if(fields[i]==this) {
                    fields[i] = newPVData;
                    return;
                }
            }
        }
        throw new IllegalArgumentException("oldField not found in parent");
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.util.Requestor#getRequestorName()
     */
    public String getRequestorName() {
        return requestorName;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.util.Requestor#message(java.lang.String, org.epics.ioc.util.MessageType)
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
     * @see org.epics.ioc.pv.PVData#getFullFieldName()
     */
    public String getFullFieldName() {
        return fullFieldName;
    } 
    
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVData#getField()
     */
    public Field getField() {
        return field;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVData#getParent()
     */
    public PVData getParent() {
        return parent;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVData#getPVRecord()
     */
    public PVRecord getPVRecord() {
       return record;
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
    public void setSupportName(String name) {
        supportName = name;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVData#toString(int)
     */
    public String toString(int indentLevel) {
        if(supportName!=null) {
            return " supportName " + supportName;
        }
        return "";
    }
    
    protected void createFullFieldAndRequestorNames() {
        if(this==record) {
            fullFieldName = "";
            return;
        }
        StringBuilder fieldName = new StringBuilder();
        fieldName.insert(0,getField().getFieldName());
        if(parent.getField().getType()!=Type.pvArray) fieldName.insert(0,".");
        PVData parent = getParent();
        while(parent!=null && parent!=this.record) {
            PVData now = parent;
            fieldName.insert(0,now.getField().getFieldName());
            if(now.getParent()==null
            || now.getParent().getField().getType()!=Type.pvArray) fieldName.insert(0,".");
            parent = now.getParent();
        }
        fullFieldName = fieldName.toString();
        if(record!=null) {
            requestorName = record.getRecordName() + fullFieldName;
        }
    }
}
