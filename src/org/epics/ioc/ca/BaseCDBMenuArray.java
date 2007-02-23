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
public class BaseCDBMenuArray extends BaseCDBData implements CDBMenuArray{
    private PVMenuArray pvMenuArray;
    private CDBMenu[] elementCDBMenus;
    
    public BaseCDBMenuArray(
            CDBData parent,CDBRecord cdbRecord,PVMenuArray pvMenuArray)
        {
            super(parent,cdbRecord,pvMenuArray);
            this.pvMenuArray = pvMenuArray;
            createElementCDBMenus();
        }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDBMenuArray#dataPut(org.epics.ioc.pv.PVMenuArray)
     */
    public void dataPut(PVMenuArray targetPVMenuArray) {
        int length = targetPVMenuArray.getLength();
        if(elementCDBMenus.length<length) {
            CDBMenu[] newDatas = new CDBMenu[length];
            for(int i=0;i<elementCDBMenus.length; i++) {
                newDatas[i] = elementCDBMenus[i];
            }
            elementCDBMenus = newDatas;
        }
        CDBRecord cdbRecord = super.getCDBRecord();
        PVDataCreate pvDataCreate = cdbRecord.getPVDataCreate();
        MenuArrayData menuArrayData = new MenuArrayData();
        pvMenuArray.get(0, length, menuArrayData);
        PVMenu[] pvMenus = menuArrayData.data;
        targetPVMenuArray.get(0, length, menuArrayData);
        PVMenu[] targetMenus = menuArrayData.data;
        for(int i=0; i<length; i++) {
            PVMenu targetPVMenu = targetMenus[i];
            if(targetPVMenu==null) {
                elementCDBMenus[i] = null;
                continue;
            }
            if(elementCDBMenus[i]==null) {
                Field newField = cdbRecord.createField(targetPVMenu.getField());
                PVMenu newMenu = (PVMenu)pvDataCreate.createData(pvMenuArray, newField);
                pvMenus[i] = newMenu;
                elementCDBMenus[i] = new BaseCDBMenu(this,cdbRecord,newMenu);
            }
            CDBMenu cdbMenu = elementCDBMenus[i];   
            cdbMenu.enumIndexPut(targetPVMenu);
            super.setNumPuts(cdbMenu.getNumIndexPuts());
        }
        pvMenuArray.put(0, pvMenus.length, pvMenus, 0);
    }

    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDBMenuArray#getElementCDBMenus()
     */
    public CDBMenu[] getElementCDBMenus() {
        return elementCDBMenus;
    }

    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDBMenuArray#replacePVArray()
     */
    public void replacePVMenuArray() {
        pvMenuArray = (PVMenuArray)super.getPVData();
        createElementCDBMenus();
    }

    public int dataPut(PVData requested,PVData targetPVData) {
        PVMenuArray targetPVMenuArray = (PVMenuArray)requested;
        int length = targetPVMenuArray.getLength();
        MenuArrayData menuArrayData = new MenuArrayData();
        targetPVMenuArray.get(0, length, menuArrayData);
        PVMenu[] targetMenus = menuArrayData.data;
        for(int i=0; i<length; i++) {
            PVMenu targetMenu = targetMenus[i];
            if(targetMenu==targetPVData) {
                CDBMenu cdbMenu = elementCDBMenus[i];
                cdbMenu.dataPut(targetPVData);
                return cdbMenu.getNumPuts();
            }
        }
        throw new IllegalStateException("Logic error.");
    }
    
    public int enumIndexPut(PVData requested,PVMenu targetPVMenu) {
        PVMenuArray targetPVMenuArray = (PVMenuArray)requested;
        int length = targetPVMenuArray.getLength();
        MenuArrayData menuArrayData = new MenuArrayData();
        targetPVMenuArray.get(0, length, menuArrayData);
        PVMenu[] targetMenus = menuArrayData.data;
        for(int i=0; i<length; i++) {
            PVMenu targetMenu = targetMenus[i];
            if(targetMenu==targetPVMenu) {
                CDBMenu cdbMenu = elementCDBMenus[i];
                cdbMenu.enumIndexPut(targetPVMenu);
                return cdbMenu.getNumIndexPuts();
            }
        }
        throw new IllegalStateException("Logic error.");
    }
    
    public int supportNamePut(PVData requested,PVData targetPVData) {
        PVMenuArray targetPVMenuArray = (PVMenuArray)requested;
        int length = targetPVMenuArray.getLength();
        MenuArrayData menuArrayData = new MenuArrayData();
        targetPVMenuArray.get(0, length, menuArrayData);
        PVMenu[] targetMenus = menuArrayData.data;
        for(int i=0; i<length; i++) {
            PVMenu targetMenu = targetMenus[i];
            if(targetMenu==targetPVData) {
                CDBMenu cdbMenu = elementCDBMenus[i];
                cdbMenu.supportNamePut(targetPVData);
                return cdbMenu.getNumSupportNamePuts();
            }
        }
        throw new IllegalStateException("Logic error.");
    }
    
    private void createElementCDBMenus() {
        int length = pvMenuArray.getLength();
        elementCDBMenus = new CDBMenu[length];
        CDBRecord dbRecord = super.getCDBRecord();
        MenuArrayData menuArrayData = new MenuArrayData();
        pvMenuArray.get(0, length, menuArrayData);
        PVMenu[] pvMenus = menuArrayData.data;
        for(int i=0; i<length; i++) {
            PVMenu pvMenu = pvMenus[i];
            if(pvMenu==null) {
                elementCDBMenus[i] = null;
            } else {
                elementCDBMenus[i] = new BaseCDBMenu(this,dbRecord,pvMenu);
            }
        }
    }
}
