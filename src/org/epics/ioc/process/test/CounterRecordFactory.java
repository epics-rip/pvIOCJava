/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.process.test;

import org.epics.ioc.pv.*;
import org.epics.ioc.db.*;
import org.epics.ioc.process.*;
import org.epics.ioc.util.*;

/**
 * Counter record acts as a counter from a min to max value with a specified increment.
 * @author mrk
 *
 */
public class CounterRecordFactory {
    /**
     * Create support.
     * @param pvStructure The structure for which the support will be created.
     * @return The newly created support.
     */
    public static Support create(PVStructure pvStructure) {
        return new CounterRecord(pvStructure);
    }
    
    private static class CounterRecord extends AbstractSupport implements SupportProcessRequestor
    {
        private static String supportName = "counterRecord";
        private DBRecord dbRecord = null;
        private SupportProcessRequestor supportProcessRequestor = null;
        private PVDouble pvMin = null;
        private PVDouble pvMax = null;
        private PVDouble pvInc = null;
        private PVDouble pvValue = null;
        private PVArray pvLinkArray = null;
        
        private LinkSupport linkArraySupport = null;
        
        private CounterRecord(PVStructure pvStructure) {
            super(supportName,(DBData)pvStructure);
            DBData dbData = (DBData)pvStructure;
            Structure structure = (Structure)pvStructure.getField();
            dbRecord = dbData.getDBRecord();
            PVData[] pvData = pvStructure.getFieldPVDatas();
            int index;
            index = structure.getFieldIndex("min");
            if(index<0) throw new IllegalStateException("field min does not exist");
            pvMin = (PVDouble)pvData[index];
            index = structure.getFieldIndex("max");
            if(index<0) throw new IllegalStateException("field max does not exist");
            pvMax = (PVDouble)pvData[index];
            index = structure.getFieldIndex("inc");
            if(index<0) throw new IllegalStateException("field inc does not exist");
            pvInc = (PVDouble)pvData[index];
            index = structure.getFieldIndex("value");
            if(index<0) throw new IllegalStateException("field value does not exist");
            pvValue = (PVDouble)pvData[index];
            index = structure.getFieldIndex("linkArray");
            if(index<0) throw new IllegalStateException("field linkArray does not exist");
            pvLinkArray = (PVArray)pvData[index];
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.SupportProcessRequestor#getProcessRequestorName()
         */
        public String getRequestorName() {
            return dbRecord.getRecordName();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requestor#message(java.lang.String, org.epics.ioc.util.MessageType)
         */
        public void message(String message, MessageType messageType) {
            dbRecord.message(message, messageType);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbLinkArray.Support#initialize()
         */
        public void initialize() {
            if(!super.checkSupportState(SupportState.readyForInitialize,"initialize")) return;
            linkArraySupport = (LinkSupport)((DBData)pvLinkArray).getSupport();
            if(linkArraySupport!=null) {
                linkArraySupport.setField(pvValue);
                linkArraySupport.initialize();
                setSupportState(linkArraySupport.getSupportState());
            } else {
                setSupportState(SupportState.readyForStart);
            }
        }   
        /* (non-Javadoc)
         * @see org.epics.ioc.dbLinkArray.Support#start()
         */
        public void start() {
            if(!super.checkSupportState(SupportState.readyForStart,supportName)) return;
            if(linkArraySupport!=null) {
                linkArraySupport.start();
                setSupportState(linkArraySupport.getSupportState());
            } else {
                setSupportState(SupportState.ready);
            }
        }  
        /* (non-Javadoc)
         * @see org.epics.ioc.dbLinkArray.Support#stop()
         */
        public void stop() {
            if(!super.checkSupportState(SupportState.ready,supportName)) return;
            if(linkArraySupport!=null) linkArraySupport.stop();
            setSupportState(SupportState.readyForStart);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbLinkArray.Support#uninitialize()
         */
        public void uninitialize() {
            if(super.getSupportState()==SupportState.ready) {
                stop();
            }
            if(super.getSupportState()!=SupportState.readyForStart) return;
            if(linkArraySupport!=null) linkArraySupport.uninitialize();
            linkArraySupport = null;
            setSupportState(SupportState.readyForInitialize);
        }   
        /* (non-Javadoc)
         * @see org.epics.ioc.dbLinkArray.Support#process(org.epics.ioc.dbLinkArray.ProcessRequestListener)
         */
        public void process(SupportProcessRequestor supportProcessRequestor) {
            if(!super.checkSupportState(SupportState.ready,"process")) {
                supportProcessRequestor.supportProcessDone(RequestResult.failure);
                return;
            }
            if(supportProcessRequestor==null) {
                throw new IllegalStateException("supportProcessRequestor is null");
            }
            this.supportProcessRequestor = supportProcessRequestor;
            double min = pvMin.get();
            double max = pvMax.get();
            double inc = pvInc.get();
            double value = pvValue.get();
            value += inc;
            if(value>max) value = min;
            pvValue.put(value);
            if(linkArraySupport!=null) {
                linkArraySupport.process(this);
            } else {
                supportProcessRequestor.supportProcessDone(RequestResult.success);
            }
        }      
        /* (non-Javadoc)
         * @see org.epics.ioc.process.SupportProcessRequestor#supportProcessDone(org.epics.ioc.util.RequestResult)
         */
        public void supportProcessDone(RequestResult requestResult) {
            supportProcessRequestor.supportProcessDone(requestResult);
        }
    }
}
