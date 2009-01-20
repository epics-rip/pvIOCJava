/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.calc;

import org.epics.pvData.pv.*;
import org.epics.pvData.misc.*;
import org.epics.pvData.factory.*;
import org.epics.pvData.property.*;
import org.epics.ioc.support.*;
import org.epics.ioc.support.alarm.*;

import org.epics.ioc.util.*;


import org.epics.ioc.ca.*;

/**
 * Support for an array of calcArg structures.
 * @author mrk
 *
 */
public class CalcArgsFactory {
    /**
     * Create support for an array of calcArg structures.
     * @param pvField The array which must be an array of links.
     * @return An interface to the support or null if the supportName was not "linkArray".
     */
    public static Support create(PVStructure pvStructure) {
        return new CalcArgsImpl(pvStructure);
    }
    
    
    private static class CalcArgsImpl extends AbstractSupport
    implements CalcArgs,SupportProcessRequester
    {
        private String processRequesterName = null;
        private PVStructure pvStructure;
        private PVField[] valuePVFields;
        private PVString[] namePVFields;
        private Support[] supports = null;
        private int numSupports = 0;
              
        private SupportProcessRequester supportProcessRequester;
        private int numberWait;
        private RequestResult finalResult;
       
        private CalcArgsImpl(PVStructure pvStructure) {
            super("calcArgArray",pvStructure);
            this.pvStructure = pvStructure;
            processRequesterName = pvStructure.getFullName();
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.support.CalcArgArraySupport#getPVField(java.lang.String)
         */
        public PVField getPVField(String argName) {
            if(super.getSupportState()==SupportState.readyForInitialize) {
                throw new IllegalStateException("getPVField called but not initialized");
            }
            for(int i=0; i<namePVFields.length; i++) {
                PVString pvString = (PVString)namePVFields[i];
                String name = pvString.get();
                if(name.equals(argName)) {
                    return valuePVFields[i];
                }
            }
            return null;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.SupportProcessRequester#getProcessRequesterName()
         */
        public String getRequesterName() {
            return processRequesterName;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.support.AbstractSupport#initialize(org.epics.ioc.support.RecordSupport)
         */
        public void initialize(RecordSupport recordSupport) {
            if(!super.checkSupportState(SupportState.readyForInitialize,supportName)) return;
            SupportState supportState = SupportState.readyForStart;
            PVField[] pvFields = pvStructure.getPVFields();
            int length = pvFields.length;
            valuePVFields = new PVField[length];
            namePVFields = new PVString[length];
            supports = new Support[length];
            numSupports = 0;
            for(int i=0; i< length; i++) {
                PVField pvField = pvFields[i];
                if(pvField.getField().getType()!=Type.structure) {
                    pvField.message("CalcArgs requires this to be a calcArg structure", MessageType.error);
                    return;
                }
                PVStructure pvStructure = (PVStructure)pvField;
                valuePVFields[i] = pvStructure.getSubField("value");
                if(valuePVFields[i]==null) {
                    pvField.message("CalcArgs requires this to have a valuefield", MessageType.error);
                    return;
                }
                namePVFields[i] = pvStructure.getStringField("name");
                if(namePVFields[i]==null) return;
                
                Support support = recordSupport.getSupport(pvField);
                supports[i] = support;
                if(support==null) continue;
                numSupports++;
                support.initialize(recordSupport);
                if(support.getSupportState()!=SupportState.readyForStart) {
                    supportState = SupportState.readyForInitialize;
                    for(int j=0; j<i; j++) {
                        if(supports[j]!=null) supports[j].uninitialize();
                    }
                    return;
                }
            }
            setSupportState(supportState);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#start()
         */
        public void start() {
            if(!super.checkSupportState(SupportState.readyForStart,supportName)) return;
            for(Support support: supports) {
                if(support!=null) support.start();
            }
            setSupportState(SupportState.ready);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#stop()
         */
        public void stop() {
            if(super.getSupportState()!=SupportState.ready) return;
            for(Support support: supports) {
                if(support!=null) support.stop();
            }
            setSupportState(SupportState.readyForStart);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#uninitialize()
         */
        public void uninitialize() {
            if(super.getSupportState()==SupportState.ready) {
                stop();
            }
            if(super.getSupportState()!=SupportState.readyForStart) return;
            for(Support support: supports) {
                if(support!=null) support.stop();
            }
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
            if(numSupports<=0) {
                supportProcessRequester.supportProcessDone(RequestResult.success);
                return;
            }
            this.supportProcessRequester = supportProcessRequester;
            numberWait = numSupports;
            finalResult = RequestResult.success;
            for(Support support: supports) {
                if(support!=null) support.process(this);
            }
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
            supportProcessRequester.supportProcessDone(finalResult);
        }
    }
}
