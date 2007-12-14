/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;

import org.epics.ioc.pv.*;

/**
 * Implementation of CDField.
 * @author mrk
 *
 */
public class BaseCDField implements CDField {
    private static Convert convert = ConvertFactory.getConvert();
    private CDField parent;
    private CDRecord cdRecord;
    private PVField pvField;
    private ChannelField channelField;
    private int maxNumPuts = 0;
    private int numPuts = 0;
    
    /**
     * Constructor.
     * @param parent The parent cdField.
     * @param cdRecord The cdRecord that contains this field.
     * @param pvField The pvField that this CDField.
     * @param channelField The channelField.
     */
    public BaseCDField(
        CDField parent,CDRecord cdRecord,PVField pvField,ChannelField channelField)
    {
        this.parent = parent;
        this.cdRecord = cdRecord;
        this.pvField = pvField;
        this.channelField = channelField;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDField#getParent()
     */
    public CDField getParent() {
        return parent;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDField#getCDBRecord()
     */
    public CDRecord getCDRecord() {
        return cdRecord;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDField#getPVField()
     */
    public PVField getPVField() {
        return pvField;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDField#getChannelField()
     */
    public ChannelField getChannelField() {
        return channelField;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDField#getNumPuts()
     */
    public int getNumPuts() {
        return numPuts;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDField#incrementNumPuts()
     */
    public void incrementNumPuts() {
        numPuts++;
        setMaxNumPuts(numPuts);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDField#getNumPuts()
     */
    public int getMaxNumPuts() {
        return maxNumPuts;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDField#setMaxNumPuts(int)
     */
    public void setMaxNumPuts(int numPuts) {
        if(this.maxNumPuts<numPuts) {
            this.maxNumPuts = numPuts;
        }
        if(parent!=null) {
            parent.setMaxNumPuts(numPuts);
        }
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDField#clearNumPuts()
     */
    public void clearNumPuts() {
        numPuts = 0;
        maxNumPuts = 0;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDField#get(org.epics.ioc.pv.PVField, boolean)
     */
    public void get(PVField toPVField,boolean postPut) {
        if(numPuts<=0) return;
        Field field = toPVField.getField();
        Type type = field.getType();
        if(type!=pvField.getField().getType()) {
            throw new IllegalStateException("Logic error.");
        }
        if(type.isScalar()) {
            convert.copyScalar(pvField,toPVField);
            if(postPut) channelField.postPut();
        } else if(type==Type.pvArray) {
            Array array = (Array)field;
            Type elementType = array.getElementType();
            if(!elementType.isScalar()) {
                throw new IllegalStateException("Logic error.");
            }
            PVArray pvArray = (PVArray)pvField;
            PVArray toPVArray = (PVArray)toPVField;
            convert.copyArray(pvArray, 0, toPVArray, 0, pvArray.getLength());
            if(postPut) channelField.postPut();
        } else {
            throw new IllegalStateException("Logic error.");
        }
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDField#put(org.epics.ioc.pv.PVField)
     */
    public void put(PVField fromPVField) {
        Field field = fromPVField.getField();
        Type type = field.getType();
        if(type!=pvField.getField().getType()) {
            throw new IllegalStateException("Logic error.");
        }
        if(type.isScalar()) {
            convert.copyScalar(fromPVField, pvField);
        } else if(type==Type.pvArray) {
            Array array = (Array)field;
            Type elementType = array.getElementType();
            if(!elementType.isScalar()) {
                throw new IllegalStateException("Logic error.");
            }
            PVArray pvArray = (PVArray)pvField;
            PVArray fromPVArray = (PVArray)fromPVField;
            convert.copyArray(fromPVArray, 0, pvArray, 0, fromPVArray.getLength());
        } else {
            throw new IllegalStateException("Logic error.");
        }
        numPuts++;
        setMaxNumPuts(numPuts);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDField#dataPut(org.epics.ioc.pv.PVField, org.epics.ioc.pv.PVField)
     */
    public void put(PVField pvField, PVField pvSubField) {
        throw new IllegalStateException("Logic error.");
    }
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return toString(0);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDField#toString(int)
     */
    public String toString(int indentLevel) {
        return String.format("maxNumPuts %d ",maxNumPuts);
    }

}
