/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.support.dbLink;

import org.epics.pvdata.pv.MessageType;
import org.epics.pvdata.pv.PVBoolean;
import org.epics.pvdata.pv.PVField;
import org.epics.pvdata.pv.PVScalar;
import org.epics.pvdata.pv.PVScalarArray;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.PVStructureArray;
import org.epics.pvdata.pv.Type;
import org.epics.pvioc.database.PVRecordField;
import org.epics.pvioc.install.AfterStart;
import org.epics.pvioc.support.SupportState;

/**
 * Abstract support for database link that transfers data.
 * @author mrk
 *
 */
abstract class AbstractIOLink extends AbstractLink {
    protected PVBoolean pvProcess = null;
    protected Type valueType = null;
    // The following is for this field
    protected PVField valuePVField = null;
    protected PVScalar valuePVScalar = null;
    protected PVScalarArray valuePVArray = null;
    protected PVStructure valuePVStructure = null;
    protected PVStructureArray valuePVStructureArray = null;
    // The following are all for other record.
    protected PVField linkValuePVField = null;
    protected PVScalar linkValuePVScalar = null;
    protected PVScalarArray linkValuePVArray = null;
    protected PVStructure linkValuePVStructure = null;
    protected PVStructureArray linkValuePVStructureArray = null;
    /**
     * Constructor.
     * @param supportName The support name.
     * @param pvRecordField The field being supported.
     */
    public AbstractIOLink(String supportName,PVRecordField pvRecordField) {
        super(supportName,pvRecordField);
    }
    /* (non-Javadoc)
     * @see org.epics.pvioc.support.dbLink.AbstractLink#initialize()
     */
    @Override
    public void initialize() {
        super.initialize();
        if(!super.checkSupportState(SupportState.readyForStart,null)) return;
        pvProcess = super.pvDatabaseLink.getBooleanField("process");
        if(pvProcess==null) {
            super.uninitialize();
            return;
        }
        PVStructure pvParent = super.pvDatabaseLink;
        while(pvParent!=null) {
            valuePVField = pvParent.getSubField("value");
            if(valuePVField!=null) break;
            pvParent = pvParent.getParent();
        }
        if(valuePVField==null) {
            super.message("value field not found", MessageType.error);
            super.uninitialize();
            return;
        }
        valueType = valuePVField.getField().getType();
        switch(valueType) {
        case scalar:
            valuePVScalar = (PVScalar)valuePVField; break;
        case scalarArray:
            valuePVArray = (PVScalarArray)valuePVField; break;
        case structure:
            valuePVStructure = (PVStructure)valuePVField; break;
        case structureArray:
        	valuePVStructureArray = (PVStructureArray)valuePVField; break;
        }
    }
    /* (non-Javadoc)
     * @see org.epics.pvioc.support.AbstractSupport#start()
     */
    public void start(AfterStart afterStart) {
        super.start(afterStart);
        if(!super.checkSupportState(SupportState.ready,null)) return;
        String name = super.pvnamePV.get();
        int ind = name.indexOf(".");
        if(ind<0) {
            name = "value";
        } else {
            name = name.substring(ind+1);
        }
        linkValuePVField = super.linkPVRecord.getPVRecordStructure().getPVStructure().getSubField(name);
        if(linkValuePVField==null) {
            super.message("pvname field not found", MessageType.error);
            super.uninitialize();
            return;
        }
        Type type = linkValuePVField.getField().getType();
        if(type!=valueType) {
            super.message("pvname type does not match value type", MessageType.error);
            super.uninitialize();
            return;
        }
        switch(valueType) {
        case scalar:
            linkValuePVScalar = (PVScalar)linkValuePVField;
            if(!convert.isCopyScalarCompatible(valuePVScalar.getScalar(),linkValuePVScalar.getScalar())) {
                super.message(
                        "pvname type and value type are not copy compatible", MessageType.error);
                super.stop();
                return;
            }
            break;
        case scalarArray:
            linkValuePVArray = (PVScalarArray)linkValuePVField;
            if(!convert.isCopyScalarArrayCompatible(valuePVArray.getScalarArray(),linkValuePVArray.getScalarArray())) {
                super.message(
                        "pvname type and value type are not copy compatible", MessageType.error);
                super.stop();
                return;
            }
            break;
        case structure:
            linkValuePVStructure = (PVStructure)linkValuePVField;
            if(!convert.isCopyStructureCompatible(valuePVStructure.getStructure(),linkValuePVStructure.getStructure())) {
                super.message(
                        "pvname type and value type are not copy compatible", MessageType.error);
                super.stop();
                return;
            }
            break;
        case structureArray:
        	linkValuePVStructureArray = (PVStructureArray)linkValuePVField;
            if(!convert.isCopyStructureArrayCompatible(valuePVStructureArray.getStructureArray(),linkValuePVStructureArray.getStructureArray())) {
                super.message(
                        "pvname type and value type are not copy compatible", MessageType.error);
                super.stop();
                return;
            }
            break;
        }
        
        	
    }
}
