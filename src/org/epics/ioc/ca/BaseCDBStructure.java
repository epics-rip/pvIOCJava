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
public class BaseCDBStructure extends BaseCDBData implements CDBStructure {
    private CDBData[] cdbDatas;
    private PVStructure pvStructure;
    private CDBRecord cdbRecord;
    
    public BaseCDBStructure(
        CDBData parent,CDBRecord cdbRecord,
        PVStructure pvStructure)
    {
        super(parent,cdbRecord,pvStructure);
        this.pvStructure = pvStructure;
        this.cdbRecord = cdbRecord;
        createFields();        
    }  
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDBStructure#getFieldChannelDatas()
     */
    public CDBData[] getFieldCDBDatas() {
        return cdbDatas;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDBStructure#getPVStructure()
     */
    public PVStructure getPVStructure() {
        return pvStructure;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDBStructure#replacePVStructure()
     */
    public void replacePVStructure() {
        this.pvStructure = (PVStructure)super.getPVData();
        createFields();
    }
    
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.BaseCDBData#dataPut(org.epics.ioc.pv.PVData)
     */
    @Override
    public void dataPut(PVData target) {
        PVStructure targetPVStructure = (PVStructure)target;
        PVData[] targetPVDatas = targetPVStructure.getFieldPVDatas();
        for(int i=0; i<targetPVDatas.length; i++) {
            PVData targetPVData = targetPVDatas[i];
            CDBData cdbData = cdbDatas[i];
            cdbData.dataPut(targetPVData);
            super.setNumPuts(cdbData.getNumPuts());
        }
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDBData#configurationStructurePut(org.epics.ioc.pv.PVData, org.epics.ioc.pv.PVLink)
     */
    public int configurationStructurePut(PVData requested, PVLink target) {
        PVStructure targetPVStructure = (PVStructure)requested;
        PVData[] targetPVDatas = targetPVStructure.getFieldPVDatas();
        for(int i=0; i<targetPVDatas.length; i++) {
            PVData targetPVData = targetPVDatas[i];
            if(targetPVData==target) {
                CDBLink cdbLink = (CDBLink)cdbDatas[i];
                cdbLink.configurationStructurePut(target);
                int num = cdbLink.getNumConfigurationStructurePuts();
                super.setNumPuts(num);
                return num;
            }
        }
        // Try each structure or nonScalarArray subfield. Note that this is recursive.
        for(int i=0; i<targetPVDatas.length; i++) {
            PVData targetPVData = targetPVDatas[i];
            CDBData cdbData = cdbDatas[i];
            Field field = targetPVData.getField();
            Type type = field.getType();
            if(type.isScalar()) continue;
            if(type==Type.pvArray) {
                Array array = (Array)field;
                Type elementType = array.getElementType();
                if(elementType.isScalar()) continue;
                int num = cdbData.configurationStructurePut(targetPVData, target);
                if(num==0) continue;
            } else if(type!=Type.pvStructure) {
                throw new IllegalStateException("Logic error.");
            }
            int num = cdbData.dataPut(targetPVData, target);
            if(num>0) {
                super.setNumPuts(num);
                return num;
            }
        }
        return 0;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDBData#dataPut(org.epics.ioc.pv.PVData, org.epics.ioc.pv.PVData)
     */
    public int dataPut(PVData requested, PVData target) {
        PVStructure targetPVStructure = (PVStructure)requested;
        PVData[] targetPVDatas = targetPVStructure.getFieldPVDatas();
        for(int i=0; i<targetPVDatas.length; i++) {
            PVData targetPVData = targetPVDatas[i];
            if(targetPVData==target) {
                CDBData cdbData = cdbDatas[i];
                cdbData.dataPut(target);
                int num = cdbData.getNumPuts();
                super.setNumPuts(num);
                return num;
            }
        }
        // Try each structure or nonScalarArray subfield. Note that this is recursive.
        for(int i=0; i<targetPVDatas.length; i++) {
            PVData targetPVData = targetPVDatas[i];
            CDBData cdbData = cdbDatas[i];
            Field field = targetPVData.getField();
            Type type = field.getType();
            if(type.isScalar()) continue;
            if(type==Type.pvArray) {
                Array array = (Array)field;
                Type elementType = array.getElementType();
                if(elementType.isScalar()) continue;
                int num = cdbData.dataPut(targetPVData, target);
                if(num==0) continue;
            } else if(type!=Type.pvStructure) {
                throw new IllegalStateException("Logic error.");
            }
            int num = cdbData.dataPut(targetPVData, target);
            if(num>0) {
                super.setNumPuts(num);
                return num;
            }
        }
        return 0;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDBData#enumChoicesPut(org.epics.ioc.pv.PVData, org.epics.ioc.pv.PVEnum)
     */
    public int enumChoicesPut(PVData requested, PVEnum target) {
        PVStructure targetPVStructure = (PVStructure)requested;
        PVData[] targetPVDatas = targetPVStructure.getFieldPVDatas();
        for(int i=0; i<targetPVDatas.length; i++) {
            PVData targetPVData = targetPVDatas[i];
            if(targetPVData==target) {
                CDBEnum cdbEnum = (CDBEnum)cdbDatas[i];
                cdbEnum.enumChoicesPut(target);
                int num = cdbEnum.getNumChoicesPut();
                super.setNumPuts(num);
                return num;
            }
        }
        // Try each structure or nonScalarArray subfield. Note that this is recursive.
        for(int i=0; i<targetPVDatas.length; i++) {
            PVData targetPVData = targetPVDatas[i];
            CDBData cdbData = cdbDatas[i];
            Field field = targetPVData.getField();
            Type type = field.getType();
            if(type.isScalar()) continue;
            if(type==Type.pvArray) {
                Array array = (Array)field;
                Type elementType = array.getElementType();
                if(elementType.isScalar()) continue;
                int num = cdbData.enumChoicesPut(targetPVData, target);
                if(num==0) continue;
            } else if(type!=Type.pvStructure) {
                throw new IllegalStateException("Logic error.");
            }
            int num = cdbData.dataPut(targetPVData, target);
            if(num>0) {
                super.setNumPuts(num);
                return num;
            }
        }
        return 0;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDBData#enumIndexPut(org.epics.ioc.pv.PVData, org.epics.ioc.pv.PVEnum)
     */
    public int enumIndexPut(PVData requested, PVEnum target) {
        PVStructure targetPVStructure = (PVStructure)requested;
        PVData[] targetPVDatas = targetPVStructure.getFieldPVDatas();
        for(int i=0; i<targetPVDatas.length; i++) {
            PVData targetPVData = targetPVDatas[i];
            if(targetPVData==target) {
                CDBEnum cdbEnum = (CDBEnum)cdbDatas[i];
                cdbEnum.enumIndexPut(target);
                int num = cdbEnum.getNumIndexPuts();
                super.setNumPuts(num);
                return num;
            }
        }
        // Try each structure or nonScalarArray subfield. Note that this is recursive.
        for(int i=0; i<targetPVDatas.length; i++) {
            PVData targetPVData = targetPVDatas[i];
            CDBData cdbData = cdbDatas[i];
            Field field = targetPVData.getField();
            Type type = field.getType();
            if(type.isScalar()) continue;
            if(type==Type.pvArray) {
                Array array = (Array)field;
                Type elementType = array.getElementType();
                if(elementType.isScalar()) continue;
                int num = cdbData.enumChoicesPut(targetPVData, target);
                if(num==0) continue;
            } else if(type!=Type.pvStructure) {
                throw new IllegalStateException("Logic error.");
            }
            int num = cdbData.dataPut(targetPVData, target);
            if(num>0) {
                super.setNumPuts(num);
                return num;
            }
        }
        return 0;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDBData#supportNamePut(org.epics.ioc.pv.PVData, org.epics.ioc.pv.PVData)
     */
    public int supportNamePut(PVData requested, PVData target) {
        PVStructure targetPVStructure = (PVStructure)requested;
        PVData[] targetPVDatas = targetPVStructure.getFieldPVDatas();
        for(int i=0; i<targetPVDatas.length; i++) {
            PVData targetPVData = targetPVDatas[i];
            if(targetPVData==target) {
                CDBData cdbData = cdbDatas[i];
                cdbData.supportNamePut(target);
                int num = cdbData.getNumSupportNamePuts();
                super.setNumPuts(num);
                return num;
            }
        }
        // Try each structure or nonScalarArray subfield. Note that this is recursive.
        for(int i=0; i<targetPVDatas.length; i++) {
            PVData targetPVData = targetPVDatas[i];
            CDBData cdbData = cdbDatas[i];
            Field field = targetPVData.getField();
            Type type = field.getType();
            if(type.isScalar()) continue;
            if(type==Type.pvArray) {
                Array array = (Array)field;
                Type elementType = array.getElementType();
                if(elementType.isScalar()) continue;
                int num = cdbData.supportNamePut(targetPVData, target);
                if(num==0) continue;
            } else if(type!=Type.pvStructure) {
                throw new IllegalStateException("Logic error.");
            }
            int num = cdbData.dataPut(targetPVData, target);
            if(num>0) {
                super.setNumPuts(num);
                return num;
            }
        }
        return 0;
    }
    private void createFields() {
        PVData[] pvDatas = pvStructure.getFieldPVDatas();
        int length = pvDatas.length;
        cdbDatas = new CDBData[length];
        for(int i=0; i<length; i++) {
            PVData pvData = pvDatas[i];
            Field field = pvData.getField();
            Type type = field.getType();
            if(type.isScalar()) {
                cdbDatas[i] = new BaseCDBData(this,cdbRecord,pvData);
                continue;
            }
            switch(type) {
            case pvEnum:
                cdbDatas[i] = new BaseCDBEnum(this,cdbRecord,pvData);
                break;
            case pvMenu:
                cdbDatas[i] = new BaseCDBMenu(this,cdbRecord,pvData);
                break;
            case pvLink:
                cdbDatas[i] = new BaseCDBLink(this,cdbRecord,pvData);
                break;
            case pvArray: {
                Array array = (Array)field;
                Type elementType = array.getElementType();
                switch(elementType) {
                case pvArray:
                    cdbDatas[i] = new BaseCDBArrayArray(this,cdbRecord,(PVArrayArray)pvData);
                    break;
                case pvEnum:
                    cdbDatas[i] = new BaseCDBEnumArray(this,cdbRecord,(PVEnumArray)pvData);
                    break;
                case pvMenu:
                    cdbDatas[i] = new BaseCDBMenuArray(this,cdbRecord,(PVMenuArray)pvData);
                    break;
                case pvLink:
                    cdbDatas[i] = new BaseCDBLinkArray(this,cdbRecord,(PVLinkArray)pvData);
                    break;
                case pvStructure:
                    cdbDatas[i] = new BaseCDBStructureArray(this,cdbRecord,(PVStructureArray)pvData);
                    break;
                default:
                    cdbDatas[i] = new BaseCDBData(this,cdbRecord,pvData);
                    break;
                }
                break;
            }
            case pvStructure:
                cdbDatas[i] = new BaseCDBStructure(
                    this,cdbRecord,(PVStructure)pvData);
                break;
            default:
                throw new IllegalStateException("Logic error");
            }
        }
    }
}
