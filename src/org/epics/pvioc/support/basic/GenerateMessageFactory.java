/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS pvData is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.support.basic;


import java.util.Date;

import org.epics.pvdata.property.TimeStamp;
import org.epics.pvdata.property.TimeStampFactory;
import org.epics.pvdata.pv.MessageType;
import org.epics.pvdata.pv.PVField;
import org.epics.pvdata.pv.PVScalar;
import org.epics.pvdata.pv.PVString;
import org.epics.pvdata.pv.ScalarType;
import org.epics.pvdata.pv.Type;
import org.epics.pvioc.database.PVRecordField;
import org.epics.pvioc.support.AbstractSupport;
import org.epics.pvioc.support.Support;
import org.epics.pvioc.support.SupportProcessRequester;
import org.epics.pvioc.support.SupportState;
import org.epics.pvioc.util.RequestResult;



/**
 * @author mrk
 *
 */
public class GenerateMessageFactory {
    
    public static Support create(PVRecordField pvRecordField) {
        PVField pvField = pvRecordField.getPVField();
        if(!pvField.getFieldName().equals("value")) {
            pvField.message("illegal field name. Must be value", MessageType.error);
            return null;
        }
        if(pvField.getField().getType()!=Type.scalar) {
            pvField.message("illegal field type. Must be scalar string", MessageType.error);
            return null;
        }
        PVScalar pvScalar = (PVScalar)pvField;
        if(pvScalar.getScalar().getScalarType()!=ScalarType.pvString) {
            pvField.message("illegal field type. Must be scalar string", MessageType.error);
            return null;
        }
        return new GenerateMessage(pvRecordField);
    }
    
    private static class GenerateMessage extends AbstractSupport
    {
        private TimeStamp timeStamp = TimeStampFactory.create();
        private static final String supportName = "org.epics.pvioc.generateMessage";
        private PVString pvValue = null;

        private GenerateMessage(PVRecordField pvRecordField) {
            super(supportName,pvRecordField);
            pvValue = (PVString)pvRecordField.getPVField();
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.support.Support#initialize(org.epics.pvioc.support.RecordProcess)
         */
        
        /* (non-Javadoc)
         * @see org.epics.pvioc.support.AbstractSupport#initialize(org.epics.pvioc.support.RecordSupport)
         */
        @Override
        public void initialize() {
            if(!super.checkSupportState(SupportState.readyForInitialize,supportName)) return;
            setSupportState(SupportState.readyForStart);
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.process.AbstractSupport#uninitialize()
         */
        public void uninitialize() {
            if(super.getSupportState()==SupportState.ready) {
                stop();
            }
            if(super.getSupportState()!=SupportState.readyForStart) return;
            setSupportState(SupportState.readyForInitialize);
        }
        @Override
        public void process(SupportProcessRequester supportProcessRequester) {
            timeStamp.getCurrentTime();
            long milliPastEpoch = timeStamp.getMilliSeconds();
            Date date = new Date(milliPastEpoch);
            String value = String.format("%tF %tT.%tL %s",date,date,date,pvValue.get());
            pvValue.message(value, MessageType.info);
            supportProcessRequester.supportProcessDone(RequestResult.success);
        }
    }
}
