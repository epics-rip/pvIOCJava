/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS pvData is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.support.caLink;

import org.epics.pvdata.copy.CreateRequest;
import org.epics.pvdata.factory.ConvertFactory;
import org.epics.pvdata.property.AlarmSeverity;
import org.epics.pvdata.property.AlarmStatus;
import org.epics.pvdata.pv.Convert;
import org.epics.pvdata.pv.Field;
import org.epics.pvdata.pv.MessageType;
import org.epics.pvdata.pv.PVField;
import org.epics.pvdata.pv.PVString;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.Structure;
import org.epics.pvdata.pv.Type;
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
    protected String[] nameInRemote = null;
    /**
     * If alarm is a requested field this is the index.
     */
    protected int indexAlarmLinkField = -1;
   
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
            pvRecordField.message("request is invalid scalarType", MessageType.error);
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
        nameInRemote = new String[fields.length];
        for(int i=0; i<fields.length; i++) {
            Field field = fields[i];
            String fieldName = names[i];
            PVStructure pvParent = super.pvStructure;
            PVField pvField = null;
            if(fieldName.equals("alarm") && alarmSupport!=null) {
                // alarm is a special case
                // look up parent tree for first alarm field
                Structure struct = (Structure)field;
                String[] subnames = struct.getFieldNames();
                int found=0;
                for(int j=0; j<subnames.length; ++j) {
                    if(subnames[j].equals("message")) ++found;
                    if(subnames[j].equals("severity")) ++found;
                    if(subnames[j].equals("status")) ++found;
                }
                if(found!=3) {
                    String message = "link field does not have alarm info";
                    if(alarmSupport!=null) {
                        alarmSupport.setAlarm(message, AlarmSeverity.INVALID,AlarmStatus.DB);
                    }
                    super.message(message, MessageType.error);
                    return false;
                }
                indexAlarmLinkField = i;
                while(pvParent!=null) {
                    pvField = pvParent.getSubField("alarm");
                    if(pvField!=null) break;
                    pvParent = pvParent.getParent();
                }
                if(pvField!=null) {
                    pvFields[i] = pvField;
                    nameInRemote[i] = "alarm";
                    continue;
                }
                String message = pvRecordField.getFullName() + " request for alarm but no alarm in this record";
                super.message(message, MessageType.error);
                return false;
            }
            // look first for exact name match
            while(pvParent!=null) {
                pvField = pvParent.getSubField(fieldName);
                if(pvField!=null) break;
                pvParent = pvParent.getParent();
            }
            if(pvField!=null) {
                // check for compatibility
                if(convert.isCopyCompatible(field, pvField.getField())) {
                    pvFields[i] = pvField;
                    nameInRemote[i] = fieldName;
                    continue;
                }
                pvField = null;
            }
            // look up parent tree for value field
            pvParent = super.pvStructure;
            while(pvParent!=null) {
                pvField = pvParent.getSubField("value");
                if(pvField!=null) break;
                pvParent = pvParent.getParent();
            }
            if(pvField==null) {
                String message = pvRecordField.getFullName() + " request " + requestPVString.get() + " is valid for this record";
                if(alarmSupport!=null) {
                    alarmSupport.setAlarm(message, AlarmSeverity.INVALID,AlarmStatus.DB);
                }
                super.message(message, MessageType.error);
                return false;
            }
            // look for first compatible field in linked record that is compatible with pvField
            nameInRemote[i] = fieldName;
            pvFields[i] = null;
            while(true) {
                if(convert.isCopyCompatible(field, pvField.getField())) {
                    pvFields[i] = pvField;
                    break;
                }
                if(field.getType()!=Type.structure) break;
                Structure struct = (Structure)field;
                if(struct.getFields().length!=1) break;
                field = struct.getField(0);
                fieldName = struct.getFieldName(0);
                nameInRemote[i] += "." + fieldName;
            }
            if(pvFields[i]!=null) continue;
            String message = pvRecordField.getFullName() + " request " + requestPVString.get() + " is valid for this record";
            if(alarmSupport!=null) {
                alarmSupport.setAlarm(message, AlarmSeverity.INVALID,AlarmStatus.DB);
            }
            super.message(message, MessageType.error);
            return false;
        }
        return true;
    }
    /* (non-Javadoc)
     * @see org.epics.pvioc.support.AbstractSupport#uninitialize()
     */
    public void uninitialize() {
        requestPVString = null;
        indexAlarmLinkField = -1;
        pvRequest = null;
        pvFields = null;
        nameInRemote = null;
        super.uninitialize();
    }
}
