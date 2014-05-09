/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS pvData is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.support.caLink;

import org.epics.pvaccess.client.CreateRequest;
import org.epics.pvdata.factory.ConvertFactory;
import org.epics.pvdata.property.AlarmSeverity;
import org.epics.pvdata.property.AlarmStatus;
import org.epics.pvdata.pv.*;
import org.epics.pvdata.pv.MessageType;
import org.epics.pvdata.pv.PVField;
import org.epics.pvdata.pv.PVInt;
import org.epics.pvdata.pv.PVString;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvioc.database.PVRecordField;
import org.epics.pvioc.install.AfterStart;
import org.epics.pvioc.support.SupportState;


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
    /**
     * request is a string that can be passed to CreateRequestFactory.createRequest.
     */
    protected PVString requestPVString = null;
    /**
     * pvRequest is passed to one of the channel.createXXX methods.
     */
    protected PVStructure pvRequest = null;
    /**
     * pvFields are the fields in PVRecord
     */
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
    protected PVInt pvAlarmSeverity = null;
   
    /**
     * Constructor.
     * @param supportName The support name.
     * @param pvField The field which is supported.
     */
    public AbstractIOLink(String supportName,PVRecordField pvRecordField) {
        super(supportName,pvRecordField);
    }
    /* (non-Javadoc)
     * @see org.epics.pvioc.support.AbstractSupport#initialize()
     */
    @Override
    public void initialize() {
        super.initialize();
        if(!super.checkSupportState(SupportState.readyForStart,null)) return;
        requestPVString = pvStructure.getStringField("request");
        if(requestPVString==null) {
            pvStructure.message("request is invalid scalarType", MessageType.error);
            super.uninitialize();
            return;
        }
    }
    /* (non-Javadoc)
     * @see org.epics.pvioc.support.AbstractSupport#start()
     */
    @Override
    public void start(AfterStart afterStart) {
    	if(!super.checkSupportState(SupportState.readyForStart,null)) return;
    	CreateRequest createRequest = CreateRequest.create();
    	if(pvRequest==null) {
    		String request = requestPVString.get();
    		if(request==null || request.length()==0) {
    			pvRequest = createRequest.createRequest("field(value)");
    		} else {
    			int index = request.indexOf("field(");
    			if(index<0) {
    				int indRecord = request.indexOf("record[");
    				if(indRecord<0) {
    					request = "field(" + request + ")";
    				} else {
    					request = request + "field(value)";
    				}
    			}
    			pvRequest = createRequest.createRequest(request);
    		}
    	}
    	if(pvRequest==null) {
    		message(createRequest.getMessage(),MessageType.error);
    		return;
    	}
    	super.start(afterStart);
    }
    
    protected boolean findPVFields(Structure structure) {
        Field[] fields = structure.getFields();
        String[] names = structure.getFieldNames();
        pvFields = new PVField[fields.length];
        for(int i=0; i<fields.length; i++) {
            Field field = fields[i];
            String fieldName = names[i];
            PVField pvField = null;
            PVStructure pvParent = super.pvStructure;
            while(pvParent!=null) {
                pvField = pvParent.getSubField(fieldName);
                if(pvField!=null) break;
                pvParent = pvParent.getParent();
            }
            if(pvField==null && fieldName.equals("index")) {
            	// enumerated structure is a special case
            	pvParent = super.pvStructure;
                while(pvParent!=null) {
                    pvField = pvParent.getSubField("value");
                    if(pvField!=null) break;
                    pvParent = pvParent.getParent();
                }
            }
            if(pvField==null) {
                String message = pvRecordField.getFullName() + " request for field " + fieldName + " is not a parent of this field";
                if(alarmSupport!=null) {
                    alarmSupport.setAlarm(message, AlarmSeverity.INVALID,AlarmStatus.DB);
                }
                super.message(message, MessageType.error);
                return false;
            }
            if(fieldName.equals("alarm") && alarmSupport!=null) {
                Structure struct = (Structure)field;
                String[] subnames = struct.getFieldNames();
                int found=0;
                for(int j=0; i<subnames.length; ++i) {
                    if(subnames[j].equals("message")) ++j;
                    if(subnames[j].equals("severity")) ++j;
                }
                if(found!=2) {
                    String message = "link field does not have alarm info";
                    if(alarmSupport!=null) {
                        alarmSupport.setAlarm(message, AlarmSeverity.INVALID,AlarmStatus.DB);
                    }
                    super.message(message, MessageType.error);
                    return false;
                }
                indexAlarmLinkField = i;
            } else {
                if(!convert.isCopyCompatible(field, pvField.getField())) {
                    String message = "pvLinkField " + names[i];
                    message += " pvField " + pvField;
                    message += "field " + fieldName +" is not copy compatible with link field";
                    if(alarmSupport!=null) {
                        alarmSupport.setAlarm(message, AlarmSeverity.INVALID,AlarmStatus.DB);
                    }
                    super.message(message,MessageType.error);
                    return false;
                }
            }
            pvFields[i] = pvField;
        }
        return true;
    }
    /* (non-Javadoc)
     * @see org.epics.pvioc.support.AbstractSupport#uninitialize()
     */
    public void uninitialize() {
        requestPVString = null;
        pvAlarmSeverity = null;
        pvAlarmMessage = null;
        indexAlarmLinkField = -1;
        pvRequest = null;
        super.uninitialize();
    }
}
