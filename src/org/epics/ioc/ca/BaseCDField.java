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
    private int maxNumPuts = 0;
    private int numPuts = 0;
    private int numSupportNamePuts = 0;
    
    /**
     * Constructor.
     * @param parent The parent cdField.
     * @param cdRecord The cdRecord that contains this field.
     * @param pvField The pvField that this CDField references.
     */
    public BaseCDField(
        CDField parent,CDRecord cdRecord,PVField pvField)
    {
        this.parent = parent;
        this.cdRecord = cdRecord;
        this.pvField = pvField;
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
     * @see org.epics.ioc.ca.CDField#replacePVField(org.epics.ioc.pv.PVField)
     */
    public void replacePVField(PVField newPVField) {
        pvField.replacePVField(newPVField);
        pvField = newPVField;
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
     * @see org.epics.ioc.ca.CDField#supportNameChange()
     */
    public int getNumSupportNamePuts() {
        return numSupportNamePuts;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDField#clearNumPuts()
     */
    public void clearNumPuts() {
        numPuts = 0;
        maxNumPuts = 0;
        numSupportNamePuts = 0;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDField#dataPut(org.epics.ioc.pv.PVField)
     */
    public void dataPut(PVField targetPVField) {
        Field field = targetPVField.getField();
        Type type = field.getType();
        if(type!=targetPVField.getField().getType()) {
            throw new IllegalStateException("Logic error.");
        }
        if(type.isScalar()) {
            convert.copyScalar(targetPVField, pvField);
        } else if(type==Type.pvArray) {
            Array array = (Array)field;
            Type elementType = array.getElementType();
            if(!elementType.isScalar()) {
                throw new IllegalStateException("Logic error.");
            }
            PVArray pvArray = (PVArray)pvField;
            PVArray targetPVArray = (PVArray)targetPVField;
            convert.copyArray(targetPVArray, 0, pvArray, 0, targetPVArray.getLength());
        } else {
            throw new IllegalStateException("Logic error.");
        }
        numPuts++;
        setMaxNumPuts(numPuts);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDField#supportNamePut(org.epics.ioc.pv.PVField)
     */
    public void supportNamePut(PVField targetPVField) {
        PVField toPVField = this.getPVField();
        toPVField.setSupportName(targetPVField.getSupportName());
        numSupportNamePuts++;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDField#configurationStructurePut(org.epics.ioc.pv.PVField, org.epics.ioc.pv.PVLink)
     */
    public boolean configurationStructurePut(PVField requested, PVLink targetPVLink) {
        throw new IllegalStateException("Logic error.");
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDField#dataPut(org.epics.ioc.pv.PVField, org.epics.ioc.pv.PVField)
     */
    public boolean dataPut(PVField requested, PVField targetPVField) {
        throw new IllegalStateException("Logic error.");
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDField#enumChoicesPut(org.epics.ioc.pv.PVField, org.epics.ioc.pv.PVEnum)
     */
    public boolean enumChoicesPut(PVField requested, PVEnum targetPVEnum) {
        throw new IllegalStateException("Logic error.");
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDField#enumIndexPut(org.epics.ioc.pv.PVField, org.epics.ioc.pv.PVEnum)
     */
    public boolean enumIndexPut(PVField requested, PVEnum targetPVEnum) {
        throw new IllegalStateException("Logic error.");
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDField#supportNamePut(org.epics.ioc.pv.PVField, org.epics.ioc.pv.PVField)
     */
    public boolean supportNamePut(PVField requested, PVField targetPVField) {
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
        return String.format(
            "maxNumPuts %d numSupportNamePuts %b ",
            maxNumPuts,numSupportNamePuts);
    }

}
