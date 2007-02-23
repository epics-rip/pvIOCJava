/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;

import org.epics.ioc.pv.*;

/**
 * Implementation of CDBData.
 * @author mrk
 *
 */
public class BaseCDBData implements CDBData {
    private static Convert convert = ConvertFactory.getConvert();
    private CDBData parent;
    private CDBRecord cdbRecord;
    private PVData pvData;
    private int numPuts = 0;
    private int numSupportNamePuts = 0;
    
    public BaseCDBData(
        CDBData parent,CDBRecord cdbRecord,PVData pvData)
    {
        this.parent = parent;
        this.cdbRecord = cdbRecord;
        this.pvData = pvData;
    }
    protected void setNumPuts(int numPuts) {
        if(this.numPuts<numPuts) {
            this.numPuts = numPuts;
        }
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDBData#getParent()
     */
    public CDBData getParent() {
        return parent;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDBData#getCDBRecord()
     */
    public CDBRecord getCDBRecord() {
        return cdbRecord;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDBData#getPVData()
     */
    public PVData getPVData() {
        return pvData;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDBData#replacePVData(org.epics.ioc.pv.PVData)
     */
    public void replacePVData(PVData newPVData) {
        pvData.replacePVData(newPVData);
        pvData = newPVData;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDBData#getNumPuts()
     */
    public int getNumPuts() {
        return numPuts;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDBData#supportNameChange()
     */
    public int getNumSupportNamePuts() {
        return numSupportNamePuts;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDBData#clearNumPuts()
     */
    public void clearNumPuts() {
        numPuts = 0;
        numSupportNamePuts = 0;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDBData#dataPut(org.epics.ioc.pv.PVData)
     */
    public void dataPut(PVData targetPVData) {
        Field field = targetPVData.getField();
        Type type = field.getType();
        if(type!=targetPVData.getField().getType()) {
            throw new IllegalStateException("Logic error.");
        }
        if(type.isScalar()) {
            convert.copyScalar(targetPVData, pvData);
        } else if(type==Type.pvArray) {
            Array array = (Array)field;
            Type elementType = array.getElementType();
            if(!elementType.isScalar()) {
                throw new IllegalStateException("Logic error.");
            }
            PVArray pvArray = (PVArray)pvData;
            PVArray targetPVArray = (PVArray)targetPVData;
            convert.copyArray(targetPVArray, 0, pvArray, 0, targetPVArray.getLength());
            numPuts++;
        } else {
            throw new IllegalStateException("Logic error.");
        }
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDBData#supportNamePut(org.epics.ioc.pv.PVData)
     */
    public void supportNamePut(PVData targetPVData) {
        PVData toPVData = this.getPVData();
        toPVData.setSupportName(targetPVData.getSupportName());
        numSupportNamePuts++;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDBData#configurationStructurePut(org.epics.ioc.pv.PVData, org.epics.ioc.pv.PVLink)
     */
    public int configurationStructurePut(PVData requested, PVLink targetPVLink) {
        throw new IllegalStateException("Logic error.");
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDBData#dataPut(org.epics.ioc.pv.PVData, org.epics.ioc.pv.PVData)
     */
    public int dataPut(PVData requested, PVData targetPVData) {
        throw new IllegalStateException("Logic error.");
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDBData#enumChoicesPut(org.epics.ioc.pv.PVData, org.epics.ioc.pv.PVEnum)
     */
    public int enumChoicesPut(PVData requested, PVEnum targetPVEnum) {
        throw new IllegalStateException("Logic error.");
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDBData#enumIndexPut(org.epics.ioc.pv.PVData, org.epics.ioc.pv.PVEnum)
     */
    public int enumIndexPut(PVData requested, PVEnum targetPVEnum) {
        throw new IllegalStateException("Logic error.");
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDBData#supportNamePut(org.epics.ioc.pv.PVData, org.epics.ioc.pv.PVData)
     */
    public int supportNamePut(PVData requested, PVData targetPVData) {
        throw new IllegalStateException("Logic error.");
    }
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return toString(0);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDBData#toString(int)
     */
    public String toString(int indentLevel) {
        return String.format(
            "numPuts %d numSupportNamePuts %b ",
            numPuts,numSupportNamePuts);
    }

}
