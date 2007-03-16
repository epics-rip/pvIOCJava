/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;
import org.epics.ioc.pv.*;
/**
 * Base class for a CD (Channel Data) Array of Links.
 * @author mrk
 *
 */
public class BaseCDLinkArray extends BaseCDField implements CDNonScalarArray{
    private boolean supportAlso;
    private PVLinkArray pvLinkArray;
    private CDLink[] elementCDLinks;
    private LinkArrayData linkArrayData = new LinkArrayData();
    
    /**
     * Constructor.
     * @param parent The parent cdField.
     * @param cdRecord The cdRecord that contains this field.
     * @param pvLinkArray The pvLinkArray that this CDField references.
     * @param supportAlso Should support be read/written?
     */
    public BaseCDLinkArray(
        CDField parent,CDRecord cdRecord,PVLinkArray pvLinkArray,boolean supportAlso)
    {
        super(parent,cdRecord,pvLinkArray,supportAlso);
        this.supportAlso = supportAlso;
        this.pvLinkArray = pvLinkArray;
        createElementCDBLinks();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.BaseCDField#clearNumPuts()
     */
    public void clearNumPuts() {
        for(CDField cdField : elementCDLinks) cdField.clearNumPuts();
        super.clearNumPuts();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.BaseCDField#dataPut(org.epics.ioc.pv.PVField)
     */
    public void dataPut(PVField targetPVField) {
        if(!supportAlso) return;
        String supportName = targetPVField.getSupportName();
        if(supportName!=null) super.supportNamePut(targetPVField.getSupportName());
        PVLinkArray targetPVLinkArray = (PVLinkArray)targetPVField;
        if(checkPVLinkArray(targetPVLinkArray)) {
            super.incrementNumPuts();
            return;
        }
        int length = targetPVLinkArray.getLength();
        pvLinkArray.get(0, length, linkArrayData);
        PVLink[] pvLinks = linkArrayData.data;
        targetPVLinkArray.get(0, length, linkArrayData);
        PVLink[] targetLinks = linkArrayData.data;
        for(int i=0; i<length; i++) {
            PVLink targetPVLink = targetLinks[i];
            if(targetPVLink==null) continue;
            CDLink cdLink = elementCDLinks[i];   
            cdLink.dataPut(targetPVLink);
        }
        pvLinkArray.put(0, pvLinks.length, pvLinks, 0);
        super.incrementNumPuts();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDNonScalarArray#getElementCDFields()
     */
    public CDLink[] getElementCDFields() {
        return elementCDLinks;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDNonScalarArray#replacePVArray()
     */
    public void replacePVArray() {
        pvLinkArray = (PVLinkArray)super.getPVField();
        createElementCDBLinks();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.BaseCDField#dataPut(org.epics.ioc.pv.PVField, org.epics.ioc.pv.PVField)
     */
    public boolean dataPut(PVField requested,PVField targetPVField) {
        if(!supportAlso) return false;
        PVLinkArray targetPVLinkArray = (PVLinkArray)requested;
        checkPVLinkArray(targetPVLinkArray);
        int length = targetPVLinkArray.getLength();
        targetPVLinkArray.get(0, length, linkArrayData);
        PVLink[] targetLinks = linkArrayData.data;
        for(int i=0; i<length; i++) {
            PVLink targetLink = targetLinks[i];
            if(targetLink==targetPVField) {
                CDLink cdLink = elementCDLinks[i];
                cdLink.dataPut(targetPVField);
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
        PVLinkArray targetPVLinkArray = (PVLinkArray)requested;
        checkPVLinkArray(targetPVLinkArray);
        int length = targetPVLinkArray.getLength();
        targetPVLinkArray.get(0, length, linkArrayData);
        PVLink[] targetLinks = linkArrayData.data;
        for(int i=0; i<length; i++) {
            PVLink targetLink = targetLinks[i];
            if(targetLink==targetPVField) {
                CDLink cdLink = elementCDLinks[i];
                cdLink.supportNamePut(targetPVField.getSupportName());
                return true;
            }
        }
        throw new IllegalStateException("Logic error.");
    }    
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.BaseCDField#configurationStructurePut(org.epics.ioc.pv.PVField, org.epics.ioc.pv.PVLink)
     */
    public boolean configurationStructurePut(PVField requested,PVLink targetPVLink) {
        if(!supportAlso) return false;
        PVLinkArray targetPVLinkArray = (PVLinkArray)requested;
        checkPVLinkArray(targetPVLinkArray);
        int length = targetPVLinkArray.getLength();
        targetPVLinkArray.get(0, length, linkArrayData);
        PVLink[] targetLinks = linkArrayData.data;
        for(int i=0; i<length; i++) {
            PVLink targetLink = targetLinks[i];
            if(targetLink==targetPVLink) {
                CDLink cdLink = elementCDLinks[i];
                cdLink.configurationStructurePut(targetPVLink.getConfigurationStructure());
                return true;
            }
        }
        throw new IllegalStateException("Logic error.");
    }
    
    private void createElementCDBLinks() {
        int length = pvLinkArray.getLength();
        elementCDLinks = new CDLink[length];
        CDRecord dbRecord = super.getCDRecord();
        pvLinkArray.get(0, length, linkArrayData);
        PVLink[] pvLinks = linkArrayData.data;
        for(int i=0; i<length; i++) {
            PVLink pvLink = pvLinks[i];
            if(pvLink==null) {
                elementCDLinks[i] = null;
            } else {
                elementCDLinks[i] = new BaseCDLink(this,dbRecord,pvLink,supportAlso);
            }
        }
    }
    
    
    private boolean checkPVLinkArray(PVLinkArray targetPVLinkArray) {
        boolean madeChanges = false;
        int length = targetPVLinkArray.getLength();
        if(elementCDLinks.length<length) {
            madeChanges = true;
            CDLink[] newDatas = new CDLink[length];
            for(int i=0;i<elementCDLinks.length; i++) {
                newDatas[i] = elementCDLinks[i];
            }
            elementCDLinks = newDatas;
        }
        CDRecord cdRecord = super.getCDRecord();
        PVDataCreate pvDataCreate = cdRecord.getPVDataCreate();
        pvLinkArray.get(0, length, linkArrayData);
        PVLink[] pvLinks = linkArrayData.data;
        targetPVLinkArray.get(0, length, linkArrayData);
        PVLink[] targetLinks = linkArrayData.data;
        for(int i=0; i<length; i++) {
            PVLink targetPVLink = targetLinks[i];
            if(targetPVLink==null) {
                if(pvLinks[i]!=null) {
                    madeChanges = true;
                    pvLinks[i] = null;
                    elementCDLinks[i] = null;
                }
                continue;
            }
            if(elementCDLinks[i]==null) {
                madeChanges = true;
                Field newField = cdRecord.createField(targetPVLink.getField());
                PVLink newLink = (PVLink)pvDataCreate.createPVField(pvLinkArray, newField);
                pvLinks[i] = newLink;
                elementCDLinks[i] = new BaseCDLink(this,cdRecord,newLink,supportAlso);
                CDLink cdLink = elementCDLinks[i];
                cdLink.supportNamePut(targetPVLink.getSupportName());
                cdLink.configurationStructurePut(targetPVLink.getConfigurationStructure());
            }
        }
        if(madeChanges) {
            pvLinkArray.put(0, pvLinks.length, pvLinks, 0);
        }
        return madeChanges;
    }
}
