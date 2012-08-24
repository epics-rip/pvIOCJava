/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS pvData is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.support.basic;

import org.epics.pvdata.pv.MessageType;
import org.epics.pvdata.pv.PVBoolean;
import org.epics.pvdata.pv.PVField;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.Type;
import org.epics.pvioc.database.PVRecordField;
import org.epics.pvioc.database.PVRecordStructure;
import org.epics.pvioc.install.AfterStart;
import org.epics.pvioc.support.AbstractSupport;
import org.epics.pvioc.support.Support;
import org.epics.pvioc.support.SupportProcessRequester;
import org.epics.pvioc.support.SupportState;
import org.epics.pvioc.support.alarm.AlarmSupport;
import org.epics.pvioc.support.alarm.AlarmSupportFactory;
import org.epics.pvioc.util.RequestResult;

/**
 * @author mrk
 *
 */
public class GenericBase extends AbstractSupport implements SupportProcessRequester{
    private PVRecordStructure pvRecordStructure = null;
    private String processRequesterName = null;
    private PVBoolean[] pvWaits = null;
    private Support[] supports = null;
    private AlarmSupport alarmSupport = null;
    private SupportProcessRequester supportProcessRequester;
    private int nextLink;
    private int numberWait;
    private RequestResult finalResult;
   
    /**
     * Constructor for GenericBase
     * @param supportName The name of the support.
     * @param pvRecordStructure The structure being supported.
     */
    public GenericBase(String supportName,PVRecordStructure pvRecordStructure) {
        super(supportName,pvRecordStructure);
        this.pvRecordStructure = pvRecordStructure;
      
        processRequesterName = pvRecordStructure.getFullName();
    }
    /**
     * Get the supports for the fields in this structure.
     * @return
     */
    protected Support[] getSupports() {
        return supports;
    }
    /* (non-Javadoc)
     * @see org.epics.pvioc.process.SupportProcessRequester#getProcessRequesterName()
     */
    @Override
    public String getRequesterName() {
        return processRequesterName;
    }
    @Override
    public void initialize() {
        if(!super.checkSupportState(SupportState.readyForInitialize,super.getSupportName())) return;
        if(pvRecordStructure==null) {
        	super.getPVRecordField().message("generic support attached to a non-structure field", MessageType.fatalError);
        	return;
        }
        PVRecordField[] pvRecordFields = pvRecordStructure.getPVRecordFields();
        int n = pvRecordFields.length;
        int numberSupport = 0;
        for(int i=0; i< n; i++) {
            PVRecordField pvRecordField = pvRecordFields[i];
            String fieldName = pvRecordField.getPVField().getFieldName();
            if(fieldName.equals("scan")) continue;
            if(fieldName.equals("timeStamp")) continue;
            // alarm is a special case
            if(fieldName.equals("alarm")) {
                alarmSupport = AlarmSupportFactory.findAlarmSupport(pvRecordField);
                if(alarmSupport==null) {
                    super.getPVRecordField().message("illegal alarm field", MessageType.error);
                    return;
                }
                continue;
            }
            if(pvRecordField.getSupport()!=null) numberSupport++;
        }
        pvWaits = new PVBoolean[numberSupport];
        supports = new Support[numberSupport];
        int indSupport = 0;
        for(int i=0; i< n; i++) {
            PVRecordField pvRecordField = pvRecordFields[i];
            String fieldName = pvRecordField.getPVField().getFieldName();
            if(fieldName.equals("scan")) continue;
            if(fieldName.equals("timeStamp")) continue;
            if(fieldName.equals("alarm")) continue;
            Support support = pvRecordField.getSupport();
            if(support==null) continue;
            pvWaits[indSupport] = null;
            supports[indSupport] = support;
            PVField pvField = pvRecordField.getPVField();
            if(pvField.getField().getType()==Type.structure) {
                PVStructure pvStruct = (PVStructure)pvField;
                if(pvStruct.getSubField("wait")!=null) {
                    pvWaits[indSupport] = pvStruct.getBooleanField("wait");
                }
            }
            support.initialize();
            if(support.getSupportState()!=SupportState.readyForStart) {
                for(int j=0; j<indSupport-1; j++) {
                    supports[j].uninitialize();
                }
                return;
            }
            indSupport++;
        }
        if(alarmSupport!=null) {
            alarmSupport.initialize();
            if(alarmSupport.getSupportState()!=SupportState.readyForStart) {
                for(Support support : supports) support.uninitialize();
                return;
            }
        }
        setSupportState(SupportState.readyForStart);
    }
    /* (non-Javadoc)
     * @see org.epics.pvioc.process.Support#start()
     */
    @Override
    public void start(AfterStart afterStart) {
        if(!super.checkSupportState(SupportState.readyForStart,super.getSupportName())) return;
        if(alarmSupport!=null) {
            alarmSupport.start(null);
            if(alarmSupport.getSupportState()!=SupportState.ready) return;
        }
        for(int i=0; i<supports.length; i++) {
            Support support = supports[i];
            support.start(afterStart);
            if(support.getSupportState()!=SupportState.ready) {
                for(int j=0; j<i; j++) {
                    supports[j].stop();
                }
                if(alarmSupport!=null) alarmSupport.stop();
                return;
            }
        }
        setSupportState(SupportState.ready);
    }
    /* (non-Javadoc)
     * @see org.epics.pvioc.process.Support#stop()
     */
    @Override
    public void stop() {
        if(super.getSupportState()!=SupportState.ready) return;
        if(alarmSupport!=null) alarmSupport.stop();
        for(Support support : supports) support.stop();
        setSupportState(SupportState.readyForStart);
    }
    /* (non-Javadoc)
     * @see org.epics.pvioc.process.Support#uninitialize()
     */
    @Override
    public void uninitialize() {
        if(super.getSupportState()==SupportState.ready) stop();
        if(alarmSupport!=null) alarmSupport.uninitialize();
        if(supports!=null) for(Support support: supports) support.uninitialize();
        setSupportState(SupportState.readyForInitialize);
    }       
    /* (non-Javadoc)
     * @see org.epics.pvioc.process.Support#process(org.epics.pvioc.process.RecordProcessRequester)
     */
    @Override
    public void process(SupportProcessRequester supportProcessRequester) {
        if(supportProcessRequester==null) {
            throw new IllegalStateException("no processRequestListener");
        }
        if(!super.checkSupportState(SupportState.ready,super.getSupportName() + ".process")) {
            supportProcessRequester.supportProcessDone(RequestResult.failure);
            return;
        }
        if(supports.length<=0) {
            supportProcessRequester.supportProcessDone(RequestResult.success);
            return;
        }
        if(alarmSupport!=null) alarmSupport.beginProcess();
        this.supportProcessRequester = supportProcessRequester;
        nextLink = 0;
        finalResult = RequestResult.success;
        callSupport();
    }                
    /* (non-Javadoc)
     * @see org.epics.pvioc.process.SupportProcessRequester#supportProcessDone(org.epics.pvioc.util.RequestResult)
     */
    @Override
    public void supportProcessDone(RequestResult requestResult) {
        if(requestResult!=RequestResult.success) {
            if(finalResult!=RequestResult.zombie) {
                finalResult = requestResult;
            }
        }
        numberWait--;
        if(numberWait>0) return;
        if(nextLink<supports.length) {
            callSupport();
        } else {
            if(alarmSupport!=null) alarmSupport.endProcess();
            supportProcessRequester.supportProcessDone(finalResult);
        }
    }
    
    private void callSupport() {
        if(!super.checkSupportState(SupportState.ready,super.getSupportName() + ".processContinue")) {
            throw new IllegalStateException("processContinue but not ready");
        }
        while(nextLink<supports.length) {    
            Support support = supports[nextLink];
            boolean wait = true; 
            if(pvWaits[nextLink]!=null) wait = pvWaits[nextLink].get();
            nextLink++;
            if(support.getSupportState()!=SupportState.ready) {
                if(finalResult==RequestResult.success) {
                    finalResult = RequestResult.failure;
                }
            } else {
                numberWait++;
                support.process(this);
            }
            if(wait) return;
        }
    }
}
