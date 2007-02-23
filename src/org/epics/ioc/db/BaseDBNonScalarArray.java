/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.db;

import org.epics.ioc.pv.*;

/**
 * @author mrk
 *
 */
public class BaseDBNonScalarArray extends BaseDBData implements DBNonScalarArray {
    private PVArray pvArray;
    private DBData[] elementDBDatas;
    
    /**
     * Constructuor.
     * @param parent The parent DBData.
     * @param record The DBRecord to which this field belongs.
     * @param pvArray The pvArray interface.
     */
    public BaseDBNonScalarArray(DBData parent,DBRecord record, PVArray pvArray) {
        super(parent,record,pvArray);
        this.pvArray = pvArray;
        createElementDBDatas();
        
    }

    /* (non-Javadoc)
     * @see org.epics.ioc.db.DBNonScalarArray#getElementDBDatas()
     */
    public DBData[] getElementDBDatas() {
        return elementDBDatas;
    }
    public void replacePVArray() {
        pvArray = (PVArray)super.getPVData();
        createElementDBDatas();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.db.BaseDBData#postPut()
     */
    public void postPut() {
        createElementDBDatas();
        super.postPut();
    }
    
    private void createElementDBDatas() {
        int length = pvArray.getLength();
        elementDBDatas = new DBData[length];
        Type elementType = ((Array)pvArray.getField()).getElementType();
        DBRecord dbRecord = super.getDBRecord();
        switch(elementType) {
        case pvEnum:
            PVEnumArray pvEnumArray = (PVEnumArray)pvArray;
            EnumArrayData enumArrayData = new EnumArrayData();
            pvEnumArray.get(0, length, enumArrayData);
            PVEnum[] pvEnums = enumArrayData.data;
            for(int i=0; i<length; i++) {
                PVEnum pvEnum = pvEnums[i];
                if(pvEnum==null) {
                    elementDBDatas[i] = null;
                } else {
                    elementDBDatas[i] = new BaseDBEnum(this,dbRecord,pvEnum);
                }
            }
            return;
        case pvStructure:
            PVStructureArray pvStructureArray = (PVStructureArray)pvArray;
            StructureArrayData structureArrayData = new StructureArrayData();
            pvStructureArray.get(0, length, structureArrayData);
            PVStructure[] pvStructures = structureArrayData.data;
            for(int i=0; i<length; i++) {
                PVStructure pvStructure = pvStructures[i];
                if(pvStructure==null) {
                    elementDBDatas[i] = null;
                } else {
                    elementDBDatas[i] = new BaseDBStructure(this,dbRecord,pvStructure);
                }
            }
            return;
        case pvArray:
            PVArrayArray pvArrayArray = (PVArrayArray)pvArray;
            ArrayArrayData arrayArrayData = new ArrayArrayData();
            pvArrayArray.get(0, length, arrayArrayData);
            PVArray[] pvArrays = arrayArrayData.data;
            for(int i=0; i<length; i++) {
                PVArray elementArray = pvArrays[i];
                if(elementArray==null) {
                    elementDBDatas[i] = null;
                } else {
                    if(((Array)elementArray.getField()).getElementType().isScalar()) {
                        elementDBDatas[i] = new BaseDBData(this,dbRecord,pvArray);
                    } else {
                        elementDBDatas[i] = new BaseDBNonScalarArray(this,dbRecord,elementArray);
                    }
                }
            }
            return;
        case pvMenu:
            PVMenuArray pvMenuArray = (PVMenuArray)pvArray;
            MenuArrayData menuArrayData = new MenuArrayData();
            pvMenuArray.get(0, length, menuArrayData);
            PVMenu[] pvMenus = menuArrayData.data;
            for(int i=0; i<length; i++) {
                PVMenu pvMenu = pvMenus[i];
                if(pvMenu==null) {
                    elementDBDatas[i] = null;
                } else {
                    elementDBDatas[i] = new BaseDBMenu(this,dbRecord,pvMenu);
                }
            }
            return;
        case pvLink:
            PVLinkArray pvLinkArray = (PVLinkArray)pvArray;
            LinkArrayData linkArrayData = new LinkArrayData();
            pvLinkArray.get(0, length, linkArrayData);
            PVLink[] pvLinks = linkArrayData.data;
            for(int i=0; i<length; i++) {
                PVLink pvLink = pvLinks[i];
                if(pvLink==null) {
                    elementDBDatas[i] = null;
                } else {
                    elementDBDatas[i] = new BaseDBLink(this,dbRecord,pvLink);
                }
            }
            return;
        default: throw new IllegalStateException("not valid for scalars");
        }
    }
}
