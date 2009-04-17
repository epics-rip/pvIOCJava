/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.dbLink;

import org.epics.ioc.install.AfterStart;
import org.epics.ioc.install.LocateSupport;
import org.epics.ioc.support.SupportState;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVArray;
import org.epics.pvData.pv.PVBoolean;
import org.epics.pvData.pv.PVField;
import org.epics.pvData.pv.PVScalar;
import org.epics.pvData.pv.PVStructure;
import org.epics.pvData.pv.Type;

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
    protected PVArray valuePVArray = null;
    protected PVStructure valuePVStructure = null;
    // The following are all for other record.
    protected PVField linkValuePVField = null;
    protected PVScalar linkValuePVScalar = null;
    protected PVArray linkValuePVArray = null;
    protected PVStructure linkValuePVStructure = null;
    /**
     * Constructor.
     * @param supportName The support name.
     * @param pvField The field being supported.
     */
    public AbstractIOLink(String supportName,PVField pvField) {
        super(supportName,pvField);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.support.AbstractSupport#initialize(org.epics.ioc.support.RecordSupport)
     */
    public void initialize(LocateSupport recordSupport) {
        super.initialize(recordSupport);
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
            valuePVArray = (PVArray)valuePVField; break;
        case structure:
            valuePVStructure = (PVStructure)valuePVField; break;
        }
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.support.AbstractSupport#start()
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
        linkValuePVField = super.linkPVRecord.getSubField(name);
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
            linkValuePVArray = (PVArray)linkValuePVField;
            if(!convert.isCopyArrayCompatible(valuePVArray.getArray(),linkValuePVArray.getArray())) {
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
        }
    }
}
