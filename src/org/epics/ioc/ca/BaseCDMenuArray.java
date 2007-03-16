/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;
import org.epics.ioc.pv.*;
/**
 * Base class for a CD (Channel Data) Array of Menus.
 * @author mrk
 *
 */
public class BaseCDMenuArray extends BaseCDField implements CDNonScalarArray{
    private boolean supportAlso;
    private PVMenuArray pvMenuArray;
    private CDMenu[] elementCDMenus;
    private MenuArrayData menuArrayData = new MenuArrayData();
    
    /**
     * Constructor.
     * @param parent The parent cdField.
     * @param cdRecord The cdRecord that contains this field.
     * @param pvMenuArray The pvMenuArray that this CDField references.
     * @param supportAlso Should support be read/written?
     */
    public BaseCDMenuArray(
        CDField parent,CDRecord cdRecord,PVMenuArray pvMenuArray,boolean supportAlso)
    {
        super(parent,cdRecord,pvMenuArray,supportAlso);
        this.supportAlso = supportAlso;
        this.pvMenuArray = pvMenuArray;
        createElementCDBMenus();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.BaseCDField#clearNumPuts()
     */
    public void clearNumPuts() {
        for(CDField cdField : elementCDMenus) cdField.clearNumPuts();
        super.clearNumPuts();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.BaseCDField#dataPut(org.epics.ioc.pv.PVField)
     */
    public void dataPut(PVField targetPVField) {
        if(supportAlso) {
            String supportName = targetPVField.getSupportName();
            if(supportName!=null) super.supportNamePut(targetPVField.getSupportName());
        }
        PVMenuArray targetPVMenuArray = (PVMenuArray)targetPVField;
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
                elementCDMenus[i] = new BaseCDMenu(this,cdRecord,newMenu,supportAlso);
            }
            CDMenu cdMenu = elementCDMenus[i];   
            cdMenu.enumIndexPut(targetPVMenu.getIndex());
        }
        pvMenuArray.put(0, pvMenus.length, pvMenus, 0);
        super.incrementNumPuts();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDNonScalarArray#getElementCDFields()
     */
    public CDMenu[] getElementCDFields() {
        return elementCDMenus;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDNonScalarArray#replacePVArray()
     */
    public void replacePVArray() {
        pvMenuArray = (PVMenuArray)super.getPVField();
        createElementCDBMenus();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.BaseCDField#dataPut(org.epics.ioc.pv.PVField, org.epics.ioc.pv.PVField)
     */
    public boolean dataPut(PVField requested,PVField targetPVField) {
        PVMenuArray targetPVMenuArray = (PVMenuArray)requested;
        checkPVMenuArray(targetPVMenuArray);
        int length = targetPVMenuArray.getLength();
        targetPVMenuArray.get(0, length, menuArrayData);
        PVMenu[] targetMenus = menuArrayData.data;
        for(int i=0; i<length; i++) {
            PVMenu targetMenu = targetMenus[i];
            if(targetMenu==targetPVField) {
                CDMenu cdMenu = elementCDMenus[i];
                cdMenu.dataPut(targetPVField);
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
                cdMenu.enumIndexPut(targetPVEnum.getIndex());
                return true;
            }
        }
        throw new IllegalStateException("Logic error.");
    }    
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.BaseCDField#supportNamePut(org.epics.ioc.pv.PVField, org.epics.ioc.pv.PVField)
     */
    public boolean supportNamePut(PVField requested,PVField targetPVField) {
        if(!supportAlso) return false;
        PVMenuArray targetPVMenuArray = (PVMenuArray)requested;
        checkPVMenuArray(targetPVMenuArray);
        int length = targetPVMenuArray.getLength();
        targetPVMenuArray.get(0, length, menuArrayData);
        PVMenu[] targetMenus = menuArrayData.data;
        for(int i=0; i<length; i++) {
            PVMenu targetMenu = targetMenus[i];
            if(targetMenu==targetPVField) {
                CDMenu cdMenu = elementCDMenus[i];
                cdMenu.supportNamePut(targetPVField.getSupportName());
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
                elementCDMenus[i] = new BaseCDMenu(this,dbRecord,pvMenu,supportAlso);
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
                elementCDMenus[i] = new BaseCDMenu(this,cdRecord,newMenu,supportAlso);
                CDMenu cdMenu = elementCDMenus[i];
                cdMenu.enumIndexPut(targetPVMenu.getIndex());
                cdMenu.supportNamePut(targetPVMenu.getSupportName());
            }
        }
        if(madeChanges) {
            pvMenuArray.put(0, pvMenus.length, pvMenus, 0);
        }
        return madeChanges;
    }
}
