/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS pvData is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.support.calc;

import org.epics.pvdata.pv.MessageType;
import org.epics.pvdata.pv.PVField;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.Type;
import org.epics.pvioc.database.PVRecordStructure;
import org.epics.pvioc.install.AfterStart;
import org.epics.pvioc.support.AbstractSupport;
import org.epics.pvioc.support.Support;
import org.epics.pvioc.support.SupportProcessRequester;
import org.epics.pvioc.support.SupportState;
import org.epics.pvioc.util.RequestResult;

/**
 * Support for an array of calcArg structures.
 * @author mrk
 *
 */
public class CalcArgsFactory {
    /**
     * Create support for structure which has  calcArg structure fields.
     * @param pvRecordStructure The structure which mist have calcArg structure fields.
     * @return An interface to the support or null if the supportName was not "linkArray".
     */
    public static Support create(PVRecordStructure pvRecordStructure) {
        return new CalcArgsImpl(pvRecordStructure);
    }
    
    
    private static class CalcArgsImpl extends AbstractSupport
    implements CalcArgs,SupportProcessRequester
    {
        private static final String supportName = "org.epics.pvioc.calcArgArray";
        private final String processRequesterName;
        private final PVRecordStructure pvRecordStructure;
        private PVField[] valuePVFields;
        private String[] argNames;
        private Support[] supports = null;
        private int numSupports = 0;
              
        private SupportProcessRequester supportProcessRequester;
        private int numberWait;
        private RequestResult finalResult;
       
        private CalcArgsImpl(PVRecordStructure pvRecordStructure) {
            super(supportName,pvRecordStructure);
            this.pvRecordStructure = pvRecordStructure;
            processRequesterName = pvRecordStructure.getFullName();
        }
        
        /* (non-Javadoc)
         * @see org.epics.pvioc.support.CalcArgArraySupport#getPVField(java.lang.String)
         */
        public PVField getPVField(String argName) {
            if(super.getSupportState()==SupportState.readyForInitialize) {
                throw new IllegalStateException("getPVField called but not initialized");
            }
            for(int i=0; i<argNames.length; i++) {
                String name = argNames[i];
                if(name.equals(argName)) {
                    return valuePVFields[i];
                }
            }
            return null;
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.process.SupportProcessRequester#getProcessRequesterName()
         */
        @Override
        public String getRequesterName() {
            return processRequesterName;
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.support.AbstractSupport#initialize()
         */
        @Override
        public void initialize() {
            if(!super.checkSupportState(SupportState.readyForInitialize,supportName)) return;
            PVField[] pvFields = pvRecordStructure.getPVStructure().getPVFields();
            int length = pvFields.length;
            valuePVFields = new PVField[length];
            argNames = new String[length];
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
                    pvField.message("CalcArgs requires this to have a value field", MessageType.error);
                    return;
                }
                argNames[i] = valuePVFields[i].getParent().getFieldName();
                Support support = pvRecordStructure.getPVRecord().findPVRecordField(pvField).getSupport();
                supports[i] = support;
                if(support==null) continue;
                numSupports++;
                support.initialize();
                if(support.getSupportState()!=SupportState.readyForStart) {
                    for(int j=0; j<i; j++) {
                        if(supports[j]!=null) supports[j].uninitialize();
                    }
                    return;
                }
            }
            setSupportState(SupportState.readyForStart);
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.support.AbstractSupport#start(org.epics.pvioc.install.AfterStart)
         */
        @Override
        public void start(AfterStart afterStart) {
            if(!super.checkSupportState(SupportState.readyForStart,supportName)) return;
            for(Support support: supports) {
                if(support!=null) support.start(afterStart);
            }
            setSupportState(SupportState.ready);
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.process.Support#stop()
         */
        @Override
        public void stop() {
            if(super.getSupportState()!=SupportState.ready) return;
            for(Support support: supports) {
                if(support!=null) support.stop();
            }
            setSupportState(SupportState.readyForStart);
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.process.Support#uninitialize()
         */
        @Override
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
         * @see org.epics.pvioc.process.Support#process(org.epics.pvioc.process.RecordProcessRequester)
         */
        @Override
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
         * @see org.epics.pvioc.process.SupportProcessRequester#supportProcessDone(org.epics.pvioc.util.RequestResult)
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
