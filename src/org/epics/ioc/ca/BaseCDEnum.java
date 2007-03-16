/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;

import org.epics.ioc.pv.*;

/**
 * Base class for a CDEnum (Channel Data Enum).
 * @author mrk
 *
 */
public class BaseCDEnum extends BaseCDField implements CDEnum {
    private boolean supportAlso;
    private PVEnum pvEnum;
    private int numIndexPuts;
    private int numChoicesPut;
    
    /**
     * Constructor.
     * @param parent The parent cdField.
     * @param cdRecord The cdRecord that contains this field.
     * @param pvField The pvEnum that this CDField references.
     * @param supportAlso Should support be read/written?
     */
    public BaseCDEnum(
        CDField parent,CDRecord cdRecord,PVField pvField,boolean supportAlso)
    {
        super(parent,cdRecord,pvField,supportAlso);
        this.supportAlso = supportAlso;
        pvEnum = (PVEnum)pvField;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDEnum#enumChoicesPut(org.epics.ioc.pv.PVEnum)
     */
    public void enumChoicesPut(String[] choices) {
        pvEnum.setChoices(choices);
        numChoicesPut++;
        super.setMaxNumPuts(numChoicesPut);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDEnum#enumIndexPut(org.epics.ioc.pv.PVEnum)
     */
    public void enumIndexPut(int index) {
        pvEnum.setIndex(index);
        numIndexPuts++;
        super.setMaxNumPuts(numIndexPuts);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDEnum#getNumChoicesPut()
     */
    public int getNumChoicesPut() {
        return numChoicesPut;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDEnum#getNumIndexPuts()
     */
    public int getNumIndexPuts() {
        return numIndexPuts;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.BaseCDField#clearNumPuts()
     */
    @Override
    public void clearNumPuts() {
        numChoicesPut = 0;
        numIndexPuts = 0;
        super.clearNumPuts();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDEnum#getPVEnum()
     */
    public PVEnum getPVEnum() {
        return pvEnum;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.BaseCDField#dataPut(org.epics.ioc.pv.PVField)
     */
    public void dataPut(PVField targetPVField) {
        if(supportAlso) {
            String supportName = targetPVField.getSupportName();
            if(supportName!=null) super.supportNamePut(targetPVField.getSupportName());
        }
        PVEnum targetPVEnum = (PVEnum)targetPVField;
        pvEnum.setIndex(targetPVEnum.getIndex());
        numIndexPuts++;
        super.setMaxNumPuts(numIndexPuts);
        if(targetPVField.getField().getType()==Type.pvMenu) return;
        pvEnum.setChoices(targetPVEnum.getChoices());
        numChoicesPut++;
        super.setMaxNumPuts(numChoicesPut);
        super.incrementNumPuts();
    }
}
