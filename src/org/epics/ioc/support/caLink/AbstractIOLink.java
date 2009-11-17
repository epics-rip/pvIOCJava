/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.caLink;

import org.epics.ioc.install.AfterStart;
import org.epics.ioc.install.LocateSupport;
import org.epics.ioc.support.SupportState;
import org.epics.pvData.factory.ConvertFactory;
import org.epics.pvData.pv.Convert;
import org.epics.pvData.pv.Field;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVField;
import org.epics.pvData.pv.PVInt;
import org.epics.pvData.pv.PVString;
import org.epics.pvData.pv.PVStructure;
import org.epics.pvData.pv.Scalar;
import org.epics.pvData.pv.ScalarType;
import org.epics.pvData.pv.Type;


/**
 * Abstract support for channel access link that transfers data.
 * @author mrk
 *
 */
abstract class AbstractIOLink extends AbstractLink {
    /**
     * The convert implementation.
     */
    protected static final Convert convert = ConvertFactory.getConvert();
    protected PVStructure pvRequest = null;
    protected PVStructure linkPVStructure = null;
    protected PVField[] linkPVFields = null;
    protected PVField[] pvFields = null;
    /**
     * If alarm is a requested field this is the index.
     */
    protected int indexAlarmLinkField = -1;;
    /**
     * If alarm is a request the interface for the alarm message.
     */
    protected PVString pvAlarmMessage = null;
    /**
     * If alarm is a request the interface for the alarm severity index.
     */
    protected PVInt pvAlarmSeverityIndex = null;
    /**
     * request is a string that can be passed to channelAccess.createRequest.
     */
    private PVString requestPVString = null;
    /**
     * Constructor.
     * @param supportName The support name.
     * @param pvField The field which is supported.
     */
    public AbstractIOLink(String supportName,PVField pvField) {
        super(supportName,pvField);
    }
    
    public void initialize(LocateSupport recordSupport) {
        super.initialize(recordSupport);
        if(!super.checkSupportState(SupportState.readyForStart,null)) return;
        PVField pvRequest = pvStructure.getSubField("request");
        if(pvRequest==null) {
            return; // if no request then done
        }
        Field field = pvRequest.getField();
        Type type = field.getType();
        if(type==Type.scalar) {
            if(((Scalar)field).getScalarType()==ScalarType.pvString) {
                requestPVString = (PVString)pvRequest;
                return;
            }
            pvStructure.message("request is invalid scalarType", MessageType.error);
            super.uninitialize();
            return;
        } else if(type==Type.structure) {
            this.pvRequest = (PVStructure) pvRequest;
        }
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.support.AbstractSupport#start()
     */
    public void start(AfterStart afterStart) {
        if(!super.checkSupportState(SupportState.readyForStart,null)) return;
        if(pvRequest==null) {
            if(requestPVString==null) {
                pvRequest = channelAccess.createRequest("value");
            } else {
                pvRequest = channelAccess.createRequest(requestPVString.get());
            }
        }
        PVField[] pvRequestFields = pvRequest.getPVFields();
        for(int i=0; i<pvRequestFields.length; i++) {
            PVField pvRequestField = pvRequestFields[i];
            String fieldName = pvRequestField.getField().getFieldName();
            PVField pvField = null;
            PVStructure pvParent = super.pvStructure;
            while(pvParent!=null) {
                pvField = pvParent.getSubField(fieldName);
                if(pvField!=null) break;
                pvParent = pvParent.getParent();
            }
            if(pvField==null) {
                pvStructure.message("request for field " + fieldName + " is not a parent of this field", MessageType.error);
                super.uninitialize();
                return;
            }
        }
        super.start(afterStart);
    }
    
    protected boolean setLinkPVStructure(PVStructure linkPVStructure) {
        PVField[] linkPVFields = linkPVStructure.getPVFields();
        pvFields = new PVField[linkPVFields.length];
        for(int i=0; i<linkPVFields.length; i++) {
            PVField pvLinkField = linkPVFields[i];
            String fieldName = pvLinkField.getField().getFieldName();
            PVField pvField = null;
            PVStructure pvParent = super.pvStructure;
            while(pvParent!=null) {
                pvField = pvParent.getSubField(fieldName);
                if(pvField!=null) break;
                pvParent = pvParent.getParent();
            }
            if(pvField==null) {
                pvStructure.message("request for field " + fieldName + " is not a parent of this field", MessageType.error);
                return false;
            }
            if(fieldName.equals("alarm") && alarmSupport!=null) {
                PVStructure pvStructure = (PVStructure)pvLinkField;
                pvAlarmMessage = pvStructure.getStringField("message");
                pvAlarmSeverityIndex = pvStructure.getIntField("severity.index");
                if(pvAlarmMessage==null || pvAlarmSeverityIndex==null) return false;
                indexAlarmLinkField = i;
            } else {
                if(!convert.isCopyCompatible(pvLinkField.getField(), pvField.getField())) {
                    pvStructure.message(
                        "field " + fieldName +" is not copy compatible with link field",
                        MessageType.error);
                    return false;
                }
            }
            pvFields[i] = pvField;
        }
        this.linkPVStructure = linkPVStructure;
        this.linkPVFields = linkPVFields;
        return true;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.support.AbstractSupport#uninitialize()
     */
    public void uninitialize() {
        requestPVString = null;
        pvAlarmSeverityIndex = null;
        pvAlarmMessage = null;
        indexAlarmLinkField = -1;
        linkPVFields = null;
        linkPVStructure = null;
        pvRequest = null;
    }
}
