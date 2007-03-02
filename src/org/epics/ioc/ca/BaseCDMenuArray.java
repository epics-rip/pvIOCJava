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
public class BaseCDMenuArray extends BaseCDField implements CDMenuArray{
    private PVMenuArray pvMenuArray;
    private CDMenu[] elementCDMenus;
    private MenuArrayData menuArrayData = new MenuArrayData();
    
    public BaseCDMenuArray(
        CDField parent,CDRecord cdRecord,PVMenuArray pvMenuArray)
    {
        super(parent,cdRecord,pvMenuArray);
        this.pvMenuArray = pvMenuArray;
        createElementCDBMenus();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.BaseCDField#clearNumPuts()
     */
    @Override
    public void clearNumPuts() {
        for(CDField cdField : elementCDMenus) cdField.clearNumPuts();
        super.clearNumPuts();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDMenuArray#dataPut(org.epics.ioc.pv.PVMenuArray)
     */
    public void dataPut(PVMenuArray targetPVMenuArray) {
        if(checkPVMenuArray(targetPVMenuArray)) {
            super.incrementNumPuts();
            return;
        }
        int length = targetPVMenuArray.getLength();
        if(elementCDMenus.length<length) {
            CDMenu[] newDatas = new CDMenu[length];
            for(int i=0;i<elementCDMenus.length; i++) {
                newDatas[i] = elementCDMenus[i];
            }
            elementCDMenus = newDatas;
        }
        CDRecord cdRecord = super.getCDRecord();
        PVDataCreate pvDataCreate = cdRecord.getPVDataCreate();
        pvMenuArray.get(0, length, menuArrayData);
        PVMenu[] pvMenus = menuArrayData.data;
        targetPVMenuArray.get(0, length, menuArrayData);
        PVMenu[] targetMenus = menuArrayData.data;
        for(int i=0; i<length; i++) {
            PVMenu targetPVMenu = targetMenus[i];
            if(targetPVMenu==null) {
                elementCDMenus[i] = null;
                continue;
            }
            if(elementCDMenus[i]==null) {
                Field newField = cdRecord.createField(targetPVMenu.getField());
                PVMenu newMenu = (PVMenu)pvDataCreate.createPVField(pvMenuArray, newField);
                pvMenus[i] = newMenu;
                elementCDMenus[i] = new BaseCDMenu(this,cdRecord,newMenu);
            }
            CDMenu cdMenu = elementCDMenus[i];   
            cdMenu.enumIndexPut(targetPVMenu);
        }
        pvMenuArray.put(0, pvMenus.length, pvMenus, 0);
        super.incrementNumPuts();
    }

    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDMenuArray#getElementCDBMenus()
     */
    public CDMenu[] getElementCDMenus() {
        return elementCDMenus;
    }

    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDMenuArray#replacePVArray()
     */
    public void replacePVMenuArray() {
        pvMenuArray = (PVMenuArray)super.getPVField();
        createElementCDBMenus();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.BaseCDField#dataPut(org.epics.ioc.pv.PVField, org.epics.ioc.pv.PVField)
     */
    public boolean fieldPut(PVField requested,PVField targetPVField) {
        PVMenuArray targetPVMenuArray = (PVMenuArray)requested;
        checkPVMenuArray(targetPVMenuArray);
        int length = targetPVMenuArray.getLength();
        targetPVMenuArray.get(0, length, menuArrayData);
        PVMenu[] targetMenus = menuArrayData.data;
        for(int i=0; i<length; i++) {
            PVMenu targetMenu = targetMenus[i];
            if(targetMenu==targetPVField) {
                CDMenu cdMenu = elementCDMenus[i];
                cdMenu.fieldPut(targetPVField);
                return true;
            }
        }
        throw new IllegalStateException("Logic error.");
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.BaseCDField#enumIndexPut(org.epics.ioc.pv.PVField, org.epics.ioc.pv.PVEnum)
     */
    public boolean enumIndexPut(PVField requested,PVEnum targetPVEnum) {
        PVMenuArray targetPVMenuArray = (PVMenuArray)requested;
        checkPVMenuArray(targetPVMenuArray);
        int length = targetPVMenuArray.getLength();
        targetPVMenuArray.get(0, length, menuArrayData);
        PVMenu[] targetMenus = menuArrayData.data;
        for(int i=0; i<length; i++) {
            PVMenu targetMenu = targetMenus[i];
            if(targetMenu==targetPVEnum) {
                CDMenu cdMenu = elementCDMenus[i];
                cdMenu.enumIndexPut(targetPVEnum);
                return true;
            }
        }
        throw new IllegalStateException("Logic error.");
    }    
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.BaseCDField#supportNamePut(org.epics.ioc.pv.PVField, org.epics.ioc.pv.PVField)
     */
    public boolean supportNamePut(PVField requested,PVField targetPVField) {
        PVMenuArray targetPVMenuArray = (PVMenuArray)requested;
        checkPVMenuArray(targetPVMenuArray);
        int length = targetPVMenuArray.getLength();
        targetPVMenuArray.get(0, length, menuArrayData);
        PVMenu[] targetMenus = menuArrayData.data;
        for(int i=0; i<length; i++) {
            PVMenu targetMenu = targetMenus[i];
            if(targetMenu==targetPVField) {
                CDMenu cdMenu = elementCDMenus[i];
                cdMenu.supportNamePut(targetPVField);
                return true;
            }
        }
        throw new IllegalStateException("Logic error.");
    }
    
    private void createElementCDBMenus() {
        int length = pvMenuArray.getLength();
        elementCDMenus = new CDMenu[length];
        CDRecord dbRecord = super.getCDRecord();
        pvMenuArray.get(0, length, menuArrayData);
        PVMenu[] pvMenus = menuArrayData.data;
        for(int i=0; i<length; i++) {
            PVMenu pvMenu = pvMenus[i];
            if(pvMenu==null) {
                elementCDMenus[i] = null;
            } else {
                elementCDMenus[i] = new BaseCDMenu(this,dbRecord,pvMenu);
            }
        }
    }
    
    private boolean checkPVMenuArray(PVMenuArray targetPVMenuArray) {
        boolean madeChanges = false;
        int length = targetPVMenuArray.getLength();
        if(elementCDMenus.length<length) {
            madeChanges = true;
            CDMenu[] newDatas = new CDMenu[length];
            for(int i=0;i<elementCDMenus.length; i++) {
                newDatas[i] = elementCDMenus[i];
            }
            elementCDMenus = newDatas;
        }
        CDRecord cdRecord = super.getCDRecord();
        PVDataCreate pvDataCreate = cdRecord.getPVDataCreate();
        pvMenuArray.get(0, length, menuArrayData);
        PVMenu[] pvMenus = menuArrayData.data;
        targetPVMenuArray.get(0, length, menuArrayData);
        PVMenu[] targetMenus = menuArrayData.data;
        for(int i=0; i<length; i++) {
            PVMenu targetPVMenu = targetMenus[i];
            if(targetPVMenu==null) {
                if(pvMenus[i]!=null) {
                    madeChanges = true;
                    pvMenus[i] = null;
                    elementCDMenus[i] = null;
                }
                continue;
            }
            if(elementCDMenus[i]==null) {
                madeChanges = true;
                Field newField = cdRecord.createField(targetPVMenu.getField());
                PVMenu newMenu = (PVMenu)pvDataCreate.createPVField(pvMenuArray, newField);
                pvMenus[i] = newMenu;
                elementCDMenus[i] = new BaseCDMenu(this,cdRecord,newMenu);
                CDMenu cdMenu = elementCDMenus[i];
                cdMenu.enumIndexPut(targetPVMenu);
                cdMenu.supportNamePut(targetPVMenu);
            }
        }
        if(madeChanges) {
            pvMenuArray.put(0, pvMenus.length, pvMenus, 0);
        }
        return madeChanges;
    }
}
