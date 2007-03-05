/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;

import org.epics.ioc.pv.*;

/**
 * Base class for a CDStructure (Channel Data Structure).
 * @author mrk
 *
 */
public class BaseCDStructure extends BaseCDField implements CDStructure {
    private CDField[] cdFields;
    private PVStructure pvStructure;
    private CDRecord cdRecord;
    
    /**
     * Constructor.
     * @param parent The parent cdField.
     * @param cdRecord The cdRecord that contains this field.
     * @param pvStructure The pvStructure that this CDField references.
     */
    public BaseCDStructure(
        CDField parent,CDRecord cdRecord,
        PVStructure pvStructure)
    {
        super(parent,cdRecord,pvStructure);
        this.pvStructure = pvStructure;
        this.cdRecord = cdRecord;
        createFields();        
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.BaseCDField#clearNumPuts()
     */
    public void clearNumPuts() {
        for(CDField cdField : cdFields) cdField.clearNumPuts();
        super.clearNumPuts();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDStructure#getFieldCDFields()
     */
    public CDField[] getFieldCDFields() {
        return cdFields;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDStructure#getPVStructure()
     */
    public PVStructure getPVStructure() {
        return pvStructure;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDStructure#replacePVStructure()
     */
    public void replacePVStructure() {
        this.pvStructure = (PVStructure)super.getPVField();
        createFields();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.BaseCDField#dataPut(org.epics.ioc.pv.PVField)
     */
    public void dataPut(PVField target) {
        PVStructure targetPVStructure = (PVStructure)target;
        PVField[] targetPVFields = targetPVStructure.getFieldPVFields();
        for(int i=0; i<targetPVFields.length; i++) {
            PVField targetPVField = targetPVFields[i];
            CDField cdField = cdFields[i];
            cdField.dataPut(targetPVField);
        }
        super.incrementNumPuts();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.BaseCDField#configurationStructurePut(org.epics.ioc.pv.PVField, org.epics.ioc.pv.PVLink)
     */
    public boolean configurationStructurePut(PVField requested, PVLink target) {
        PVStructure targetPVStructure = (PVStructure)requested;
        PVField[] targetPVFields = targetPVStructure.getFieldPVFields();
        for(int i=0; i<targetPVFields.length; i++) {
            PVField targetPVField = targetPVFields[i];
            if(targetPVField==target) {
                CDLink cdLink = (CDLink)cdFields[i];
                cdLink.configurationStructurePut(target);
                return true;
            }
        }
        // Try each structure or nonScalarArray subfield. Note that this is recursive.
        for(int i=0; i<targetPVFields.length; i++) {
            PVField targetPVField = targetPVFields[i];
            CDField cdField = cdFields[i];
            Field field = targetPVField.getField();
            Type type = field.getType();
            if(type.isScalar()) continue;
            if(type==Type.pvArray) {
                Array array = (Array)field;
                Type elementType = array.getElementType();
                if(elementType.isScalar()) continue;
                if(!cdField.configurationStructurePut(targetPVField, target)) continue;
            } else if(type!=Type.pvStructure) {
                throw new IllegalStateException("Logic error.");
            }
            if(cdField.dataPut(targetPVField, target)) return true;
        }
        return false;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDField#dataPut(org.epics.ioc.pv.PVField, org.epics.ioc.pv.PVField)
     */
    public boolean dataPut(PVField requested, PVField target) {
        PVStructure targetPVStructure = (PVStructure)requested;
        PVField[] targetPVFields = targetPVStructure.getFieldPVFields();
        for(int i=0; i<targetPVFields.length; i++) {
            PVField targetPVField = targetPVFields[i];
            if(targetPVField==target) {
                CDField cdField = cdFields[i];
                cdField.dataPut(target);
                return true;
            }
        }
        // Try each structure or nonScalarArray subfield. Note that this is recursive.
        for(int i=0; i<targetPVFields.length; i++) {
            PVField targetPVField = targetPVFields[i];
            CDField cdField = cdFields[i];
            Field field = targetPVField.getField();
            Type type = field.getType();
            if(type.isScalar()) continue;
            if(type==Type.pvArray) {
                Array array = (Array)field;
                Type elementType = array.getElementType();
                if(elementType.isScalar()) continue;
                if(!cdField.dataPut(targetPVField, target))continue;
            } else if(type!=Type.pvStructure) {
                throw new IllegalStateException("Logic error.");
            }
            if(cdField.dataPut(targetPVField, target)) return true;
        }
        return false;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDField#enumChoicesPut(org.epics.ioc.pv.PVField, org.epics.ioc.pv.PVEnum)
     */
    public boolean enumChoicesPut(PVField requested, PVEnum target) {
        PVStructure targetPVStructure = (PVStructure)requested;
        PVField[] targetPVFields = targetPVStructure.getFieldPVFields();
        for(int i=0; i<targetPVFields.length; i++) {
            PVField targetPVField = targetPVFields[i];
            if(targetPVField==target) {
                CDEnum cdEnum = (CDEnum)cdFields[i];
                cdEnum.enumChoicesPut(target);
                return true;
            }
        }
        // Try each structure or nonScalarArray subfield. Note that this is recursive.
        for(int i=0; i<targetPVFields.length; i++) {
            PVField targetPVField = targetPVFields[i];
            CDField cdField = cdFields[i];
            Field field = targetPVField.getField();
            Type type = field.getType();
            if(type.isScalar()) continue;
            if(type==Type.pvArray) {
                Array array = (Array)field;
                Type elementType = array.getElementType();
                if(elementType.isScalar()) continue;
                if(!cdField.enumChoicesPut(targetPVField, target)) continue;
            } else if(type!=Type.pvStructure) {
                throw new IllegalStateException("Logic error.");
            }
            if(cdField.dataPut(targetPVField, target)) return true;
        }
        return false;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDField#enumIndexPut(org.epics.ioc.pv.PVField, org.epics.ioc.pv.PVEnum)
     */
    public boolean enumIndexPut(PVField requested, PVEnum target) {
        PVStructure targetPVStructure = (PVStructure)requested;
        PVField[] targetPVFields = targetPVStructure.getFieldPVFields();
        for(int i=0; i<targetPVFields.length; i++) {
            PVField targetPVField = targetPVFields[i];
            if(targetPVField==target) {
                CDEnum cdEnum = (CDEnum)cdFields[i];
                cdEnum.enumIndexPut(target);
                return true;
            }
        }
        // Try each structure or nonScalarArray subfield. Note that this is recursive.
        for(int i=0; i<targetPVFields.length; i++) {
            PVField targetPVField = targetPVFields[i];
            CDField cdField = cdFields[i];
            Field field = targetPVField.getField();
            Type type = field.getType();
            if(type.isScalar()) continue;
            if(type==Type.pvArray) {
                Array array = (Array)field;
                Type elementType = array.getElementType();
                if(elementType.isScalar()) continue;
                if(!cdField.enumChoicesPut(targetPVField, target)) continue;
            } else if(type!=Type.pvStructure) {
                throw new IllegalStateException("Logic error.");
            }
            if(cdField.dataPut(targetPVField, target)) return true;
        }
        return false;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDField#supportNamePut(org.epics.ioc.pv.PVField, org.epics.ioc.pv.PVField)
     */
    public boolean supportNamePut(PVField requested, PVField target) {
        PVStructure targetPVStructure = (PVStructure)requested;
        PVField[] targetPVFields = targetPVStructure.getFieldPVFields();
        for(int i=0; i<targetPVFields.length; i++) {
            PVField targetPVField = targetPVFields[i];
            if(targetPVField==target) {
                CDField cdField = cdFields[i];
                cdField.supportNamePut(target);
                return true;
            }
        }
        // Try each structure or nonScalarArray subfield. Note that this is recursive.
        for(int i=0; i<targetPVFields.length; i++) {
            PVField targetPVField = targetPVFields[i];
            CDField cdField = cdFields[i];
            Field field = targetPVField.getField();
            Type type = field.getType();
            if(type.isScalar()) continue;
            if(type==Type.pvArray) {
                Array array = (Array)field;
                Type elementType = array.getElementType();
                if(elementType.isScalar()) continue;
                if(!cdField.supportNamePut(targetPVField, target)) continue;
            } else if(type!=Type.pvStructure) {
                throw new IllegalStateException("Logic error.");
            }
            if(cdField.dataPut(targetPVField, target)) return true;
        }
        return false;
    }
    
    private void createFields() {
        PVField[] pvFields = pvStructure.getFieldPVFields();
        int length = pvFields.length;
        cdFields = new CDField[length];
        for(int i=0; i<length; i++) {
            PVField pvField = pvFields[i];
            Field field = pvField.getField();
            Type type = field.getType();
            if(type.isScalar()) {
                cdFields[i] = new BaseCDField(this,cdRecord,pvField);
                continue;
            }
            switch(type) {
            case pvEnum:
                cdFields[i] = new BaseCDEnum(this,cdRecord,pvField);
                break;
            case pvMenu:
                cdFields[i] = new BaseCDMenu(this,cdRecord,pvField);
                break;
            case pvLink:
                cdFields[i] = new BaseCDLink(this,cdRecord,pvField);
                break;
            case pvArray: {
                Array array = (Array)field;
                Type elementType = array.getElementType();
                switch(elementType) {
                case pvArray:
                    cdFields[i] = new BaseCDArrayArray(this,cdRecord,(PVArrayArray)pvField);
                    break;
                case pvEnum:
                    cdFields[i] = new BaseCDEnumArray(this,cdRecord,(PVEnumArray)pvField);
                    break;
                case pvMenu:
                    cdFields[i] = new BaseCDMenuArray(this,cdRecord,(PVMenuArray)pvField);
                    break;
                case pvLink:
                    cdFields[i] = new BaseCDLinkArray(this,cdRecord,(PVLinkArray)pvField);
                    break;
                case pvStructure:
                    cdFields[i] = new BaseCDStructureArray(this,cdRecord,(PVStructureArray)pvField);
                    break;
                default:
                    cdFields[i] = new BaseCDField(this,cdRecord,pvField);
                    break;
                }
                break;
            }
            case pvStructure:
                cdFields[i] = new BaseCDStructure(
                    this,cdRecord,(PVStructure)pvField);
                break;
            default:
                throw new IllegalStateException("Logic error");
            }
        }
    }
}
