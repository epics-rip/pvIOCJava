/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;

import org.epics.ioc.pv.*;

/**
 * @author mrk
 *
 */
public class BaseCDBEnum extends BaseCDBData implements CDBEnum {
    private PVEnum pvEnum;
    private int numIndexPut;
    private int numChoicesPut;
    
    public BaseCDBEnum(
        CDBData parent,CDBRecord cdbRecord,PVData pvData)
    {
        super(parent,cdbRecord,pvData);
        pvEnum = (PVEnum)pvData;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDBEnum#enumChoicesPut(org.epics.ioc.pv.PVEnum)
     */
    public void enumChoicesPut(PVEnum targetPVEnum) {
        pvEnum.setChoices(targetPVEnum.getChoices());
        numChoicesPut++;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDBEnum#enumIndexPut(org.epics.ioc.pv.PVEnum)
     */
    public void enumIndexPut(PVEnum targetPVEnum) {
        pvEnum.setIndex(targetPVEnum.getIndex());
        numIndexPut++;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDBEnum#getNumChoicesPut()
     */
    public int getNumChoicesPut() {
        int num = numChoicesPut;
        numChoicesPut = 0;
        return num;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDBEnum#getNumIndexPuts()
     */
    public int getNumIndexPuts() {
        int num = numIndexPut;
        numIndexPut = 0;
        return num;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDBEnum#getPVEnum()
     */
    public PVEnum getPVEnum() {
        return pvEnum;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.BaseCDBData#dataPut(org.epics.ioc.pv.PVData)
     */
    public void dataPut(PVData targetPVData) {
        PVEnum targetPVEnum = (PVEnum)targetPVData;
        pvEnum.setIndex(targetPVEnum.getIndex());
        if(targetPVData.getField().getType()==Type.pvMenu) return;
        numIndexPut++;
        pvEnum.setChoices(targetPVEnum.getChoices());
        numChoicesPut++;
    }
}
