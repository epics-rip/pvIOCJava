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
public class BaseCDBLinkArray extends BaseCDBData implements CDBLinkArray{
    private PVLinkArray pvLinkArray;
    private CDBLink[] elementCDBLinks;
    
    public BaseCDBLinkArray(
            CDBData parent,CDBRecord cdbRecord,PVLinkArray pvLinkArray)
        {
            super(parent,cdbRecord,pvLinkArray);
            this.pvLinkArray = pvLinkArray;
            createElementCDBLinks();
        }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDBLinkArray#dataPut(org.epics.ioc.pv.PVLinkArray)
     */
    public void dataPut(PVLinkArray targetPVLinkArray) {
        int length = targetPVLinkArray.getLength();
        if(elementCDBLinks.length<length) {
            CDBLink[] newDatas = new CDBLink[length];
            for(int i=0;i<elementCDBLinks.length; i++) {
                newDatas[i] = elementCDBLinks[i];
            }
            elementCDBLinks = newDatas;
        }
        CDBRecord cdbRecord = super.getCDBRecord();
        PVDataCreate pvDataCreate = cdbRecord.getPVDataCreate();
        LinkArrayData linkArrayData = new LinkArrayData();
        pvLinkArray.get(0, length, linkArrayData);
        PVLink[] pvLinks = linkArrayData.data;
        targetPVLinkArray.get(0, length, linkArrayData);
        PVLink[] targetLinks = linkArrayData.data;
        for(int i=0; i<length; i++) {
            PVLink targetPVLink = targetLinks[i];
            if(targetPVLink==null) {
                elementCDBLinks[i] = null;
                continue;
            }
            if(elementCDBLinks[i]==null) {
                Field newField = cdbRecord.createField(targetPVLink.getField());
                PVLink newLink = (PVLink)pvDataCreate.createData(pvLinkArray, newField);
                pvLinks[i] = newLink;
                elementCDBLinks[i] = new BaseCDBLink(this,cdbRecord,newLink);
            }
            CDBLink cdbLink = elementCDBLinks[i];   
            cdbLink.configurationStructurePut(targetPVLink);
            super.setNumPuts(cdbLink.getNumConfigurationStructurePuts());
        }
        pvLinkArray.put(0, pvLinks.length, pvLinks, 0);
    }

    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDBLinkArray#getElementCDBLinks()
     */
    public CDBLink[] getElementCDBLinks() {
        return elementCDBLinks;
    }

    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDBLinkArray#replacePVArray()
     */
    public void replacePVLinkArray() {
        pvLinkArray = (PVLinkArray)super.getPVData();
        createElementCDBLinks();
    }

    public int dataPut(PVData requested,PVData targetPVData) {
        PVLinkArray targetPVLinkArray = (PVLinkArray)requested;
        int length = targetPVLinkArray.getLength();
        LinkArrayData linkArrayData = new LinkArrayData();
        targetPVLinkArray.get(0, length, linkArrayData);
        PVLink[] targetLinks = linkArrayData.data;
        for(int i=0; i<length; i++) {
            PVLink targetLink = targetLinks[i];
            if(targetLink==targetPVData) {
                CDBLink cdbLink = elementCDBLinks[i];
                cdbLink.dataPut(targetPVData);
                return cdbLink.getNumPuts();
            }
        }
        throw new IllegalStateException("Logic error.");
    }
    
    public int supportNamePut(PVData requested,PVData targetPVData) {
        PVLinkArray targetPVLinkArray = (PVLinkArray)requested;
        int length = targetPVLinkArray.getLength();
        LinkArrayData linkArrayData = new LinkArrayData();
        targetPVLinkArray.get(0, length, linkArrayData);
        PVLink[] targetLinks = linkArrayData.data;
        for(int i=0; i<length; i++) {
            PVLink targetLink = targetLinks[i];
            if(targetLink==targetPVData) {
                CDBLink cdbLink = elementCDBLinks[i];
                cdbLink.supportNamePut(targetPVData);
                return cdbLink.getNumSupportNamePuts();
            }
        }
        throw new IllegalStateException("Logic error.");
    }
    
    public int configurationStructurePut(PVData requested,PVLink targetPVLink) {
        PVLinkArray targetPVLinkArray = (PVLinkArray)requested;
        int length = targetPVLinkArray.getLength();
        LinkArrayData linkArrayData = new LinkArrayData();
        targetPVLinkArray.get(0, length, linkArrayData);
        PVLink[] targetLinks = linkArrayData.data;
        for(int i=0; i<length; i++) {
            PVLink targetLink = targetLinks[i];
            if(targetLink==targetPVLink) {
                CDBLink cdbLink = elementCDBLinks[i];
                cdbLink.configurationStructurePut(targetPVLink);
                return cdbLink.getNumConfigurationStructurePuts();
            }
        }
        throw new IllegalStateException("Logic error.");
    }
    
    private void createElementCDBLinks() {
        int length = pvLinkArray.getLength();
        elementCDBLinks = new CDBLink[length];
        CDBRecord dbRecord = super.getCDBRecord();
        LinkArrayData linkArrayData = new LinkArrayData();
        pvLinkArray.get(0, length, linkArrayData);
        PVLink[] pvLinks = linkArrayData.data;
        for(int i=0; i<length; i++) {
            PVLink pvLink = pvLinks[i];
            if(pvLink==null) {
                elementCDBLinks[i] = null;
            } else {
                elementCDBLinks[i] = new BaseCDBLink(this,dbRecord,pvLink);
            }
        }
    }
}
