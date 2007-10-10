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
    private boolean supportAlso;
    private CDField[] cdFields;
    private PVStructure pvStructure;
    private CDRecord cdRecord;
    
    /**
     * Constructor.
     * @param parent The parent cdField.
     * @param cdRecord The cdRecord that contains this field.
     * @param pvStructure The pvStructure that this CDField references.
     * @param supportAlso Should support be read/written?
     */
    public BaseCDStructure(
        CDField parent,CDRecord cdRecord,
        PVStructure pvStructure,boolean supportAlso)
    {
        super(parent,cdRecord,pvStructure,supportAlso);
        this.supportAlso = supportAlso;
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
    public CDField[] getCDFields() {
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
    public void dataPut(PVField targetPVField) {
        if(supportAlso) {
            String supportName = targetPVField.getSupportName();
            if(supportName!=null) super.supportNamePut(targetPVField.getSupportName());
        }
        PVStructure targetPVStructure = (PVStructure)targetPVField;
        PVField[] targetPVFields = targetPVStructure.getPVFields();
        for(int i=0; i<targetPVFields.length; i++) {
            PVField target = targetPVFields[i];
            CDField cdField = cdFields[i];
            cdField.dataPut(target);
        }
        super.incrementNumPuts();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDField#dataPut(org.epics.ioc.pv.PVField, org.epics.ioc.pv.PVField)
     */
    public boolean dataPut(PVField requested, PVField target) {
        PVStructure targetPVStructure = (PVStructure)requested;
        PVField[] targetPVFields = targetPVStructure.getPVFields();
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
                if(cdField.dataPut(targetPVField, target)) return true;
            } else if(type==Type.pvStructure) {
                if(cdField.dataPut(targetPVField, target)) return true;
            }
        }
        return false;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDField#supportNamePut(org.epics.ioc.pv.PVField, org.epics.ioc.pv.PVField)
     */
    public boolean supportNamePut(PVField requested, PVField target) {
        PVStructure targetPVStructure = (PVStructure)requested;
        PVField[] targetPVFields = targetPVStructure.getPVFields();
        for(int i=0; i<targetPVFields.length; i++) {
            PVField targetPVField = targetPVFields[i];
            if(targetPVField==target) {
                CDField cdField = cdFields[i];
                cdField.supportNamePut(target.getSupportName());
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
                if(cdField.supportNamePut(targetPVField, target)) return true;
            } else if(type==Type.pvStructure) {
                if(cdField.supportNamePut(targetPVField, target)) return true;
            }
        }
        return false;
    }
    
    private void createFields() {
        PVField[] pvFields = pvStructure.getPVFields();
        int length = pvFields.length;
        cdFields = new CDField[length];
        for(int i=0; i<length; i++) {
            PVField pvField = pvFields[i];
            Field field = pvField.getField();
            Type type = field.getType();
            if(type.isScalar()) {
                cdFields[i] = new BaseCDField(this,cdRecord,pvField,supportAlso);
                continue;
            }
            switch(type) {
            case pvArray: {
                Array array = (Array)field;
                Type elementType = array.getElementType();
                switch(elementType) {
                case pvArray:
                    cdFields[i] = new BaseCDArrayArray(this,cdRecord,(PVArrayArray)pvField,supportAlso);
                    break;
                case pvStructure:
                    cdFields[i] = new BaseCDStructureArray(this,cdRecord,(PVStructureArray)pvField,supportAlso);
                    break;
                default:
                    cdFields[i] = new BaseCDField(this,cdRecord,pvField,supportAlso);
                    break;
                }
                break;
            }
            case pvStructure:
                cdFields[i] = new BaseCDStructure(
                    this,cdRecord,(PVStructure)pvField,supportAlso);
                break;
            default:
                throw new IllegalStateException("Logic error");
            }
        }
    }
}
