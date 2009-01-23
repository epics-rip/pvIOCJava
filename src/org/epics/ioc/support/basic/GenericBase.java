/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.basic;

import org.epics.ioc.support.AbstractSupport;
import org.epics.ioc.support.RecordSupport;
import org.epics.ioc.support.Support;
import org.epics.ioc.support.SupportProcessRequester;
import org.epics.ioc.support.SupportState;
import org.epics.ioc.support.alarm.*;
import org.epics.ioc.util.RequestResult;
import org.epics.pvData.pv.PVBoolean;
import org.epics.pvData.pv.PVField;
import org.epics.pvData.pv.PVStructure;
import org.epics.pvData.pv.Type;

/**
 * @author mrk
 *
 */
public class GenericBase extends AbstractSupport implements SupportProcessRequester{
    private PVStructure pvStructure;
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
     * @param pvStructure The structure being supported.
     */
    public GenericBase(String supportName,PVStructure pvStructure) {
        super(supportName,pvStructure);
        this.pvStructure = pvStructure;
        processRequesterName = pvStructure.getFullName();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.process.SupportProcessRequester#getProcessRequesterName()
     */
    public String getRequesterName() {
        return processRequesterName;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.support.Support#initialize(org.epics.ioc.support.RecordSupport)
     */
    public void initialize(RecordSupport recordSupport) {
        if(!super.checkSupportState(SupportState.readyForInitialize,supportName)) return;
        processRequesterName = pvStructure.getFullName();
        SupportState supportState = SupportState.readyForStart;
        PVField[] pvFields = pvStructure.getPVFields();
        int n = pvFields.length;
        int numberSupport = 0;
        for(int i=0; i< n; i++) {
            PVField pvField = pvFields[i];
            String fieldName = pvField.getField().getFieldName();
            if(fieldName.equals("scan")) continue;
            if(fieldName.equals("timeStamp")) continue;
            // alarm is a special case
            if(fieldName.equals("alarm")) {
                Support support = recordSupport.getSupport(pvField);
                if(support!=null && support instanceof AlarmSupport) {
                    alarmSupport = (AlarmSupport)support;
                }
                continue;
            }
            if(recordSupport.getSupport(pvFields[i])!=null) numberSupport++;
        }
        if(alarmSupport!=null) {
            alarmSupport.initialize(recordSupport);
            if(alarmSupport.getSupportState()!=SupportState.readyForStart) {
                return;
            }
        }
        pvWaits = new PVBoolean[numberSupport];
        supports = new Support[numberSupport];
        int indSupport = 0;
        for(int i=0; i< n; i++) {
            PVField pvField = pvFields[i];
            String fieldName = pvField.getField().getFieldName();
            if(fieldName.equals("scan")) continue;
            if(fieldName.equals("timeStamp")) continue;
            if(fieldName.equals("alarm")) continue;
            Support support = recordSupport.getSupport(pvField);
            if(support==null) continue;
            pvWaits[indSupport] = null;
            supports[indSupport] = support;
            if(pvField.getField().getType()==Type.structure) {
                PVStructure pvStruct = (PVStructure)pvField;
                pvWaits[indSupport] = pvStruct.getBooleanField("wait");
            }
            support.initialize(recordSupport);
            if(support.getSupportState()!=SupportState.readyForStart) {
                supportState = SupportState.readyForInitialize;
                for(int j=0; j<indSupport; j++) {
                    if(supports[j]!=null) supports[j].uninitialize();
                }
                break;
            }
            indSupport++;
        }
        setSupportState(supportState);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.process.Support#start()
     */
    public void start() {
        if(!super.checkSupportState(SupportState.readyForStart,supportName)) return;
        if(alarmSupport!=null) {
            alarmSupport.start();
            if(alarmSupport.getSupportState()!=SupportState.ready) return;
        }
        for(Support support : supports) support.start();
        setSupportState(SupportState.ready);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.process.Support#stop()
     */
    public void stop() {
        if(super.getSupportState()!=SupportState.ready) return;
        if(alarmSupport!=null) alarmSupport.stop();
        for(Support support : supports) support.stop();
        setSupportState(SupportState.readyForStart);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.process.Support#uninitialize()
     */
    public void uninitialize() {
        if(super.getSupportState()==SupportState.ready) stop();
        if(alarmSupport!=null) alarmSupport.uninitialize();
        if(supports!=null) for(Support support: supports) support.uninitialize();
        setSupportState(SupportState.readyForInitialize);
    }       
    /* (non-Javadoc)
     * @see org.epics.ioc.process.Support#process(org.epics.ioc.process.RecordProcessRequester)
     */
    public void process(SupportProcessRequester supportProcessRequester) {
        if(supportProcessRequester==null) {
            throw new IllegalStateException("no processRequestListener");
        }
        if(!super.checkSupportState(SupportState.ready,supportName + ".process")) {
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
     * @see org.epics.ioc.process.SupportProcessRequester#supportProcessDone(org.epics.ioc.util.RequestResult)
     */
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
        if(!super.checkSupportState(SupportState.ready,supportName + ".processContinue")) {
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
