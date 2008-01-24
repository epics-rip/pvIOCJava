/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support;

import org.epics.ioc.db.DBField;
import org.epics.ioc.db.DBStructure;
import org.epics.ioc.db.DBStructureArray;
import org.epics.ioc.process.SupportProcessRequester;
import org.epics.ioc.process.SupportState;
import org.epics.ioc.pv.Array;
import org.epics.ioc.pv.Field;
import org.epics.ioc.pv.PVBoolean;
import org.epics.ioc.pv.PVField;
import org.epics.ioc.pv.PVStructure;
import org.epics.ioc.pv.PVStructureArray;
import org.epics.ioc.pv.Structure;
import org.epics.ioc.pv.Type;
import org.epics.ioc.util.MessageType;
import org.epics.ioc.util.RequestResult;

/**
 * Support for an array of fields that have support.
 * The links can be process, input, or output.
 * @author mrk
 *
 */
public class SupportArrayFactory {
    /**
     * Create support for an array of structures that have support.
     * @param dbField The array which must be an array of structures.
     * @return An interface to the support or null if the supportName was not "supportArray".
     */
    public static Support create(DBField dbField) {
        PVField pvField = dbField.getPVField();
        Field field = pvField.getField();
        Type type = field.getType();
        if(type!=Type.pvArray) {
            pvField.message("type is not an array",MessageType.error);
            return null;
        }
        Array array = (Array)field;
        Type elementType = array.getElementType();
        if(elementType!=Type.pvStructure) {
            pvField.message("element type is not a structure",MessageType.error);
            return null;
        }
        String supportName = pvField.getSupportName();
        if(supportName==null || !supportName.equals(supportName)) {
            pvField.message("does not have support " + supportName,MessageType.error);
            return null;
        }
        return new SupportArrayImpl(dbField);
    }
    
    private static String supportName = "supportArray";
    
    private static class SupportArrayImpl extends AbstractSupport implements SupportProcessRequester
    {
        private DBField dbField;
        private PVField pvField;
        private String processRequesterName = null;
        private PVBoolean[] pvWaits = null;
        private Support[] supports = null;
               
        private SupportProcessRequester supportProcessRequester;
        private int nextLink;
        private int numberWait;
        private RequestResult finalResult;
       
        private SupportArrayImpl(DBField dbField) {
            super(supportName,dbField);
            this.dbField = dbField;
            pvField = dbField.getPVField();
            processRequesterName = pvField.getFullName();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.SupportProcessRequester#getProcessRequesterName()
         */
        public String getRequesterName() {
            return processRequesterName;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#initialize()
         */
        public void initialize() {
            if(!super.checkSupportState(SupportState.readyForInitialize,supportName)) return;
            DBStructureArray dbStructureArray = (DBStructureArray)dbField;
            PVStructureArray pvStructureArray = dbStructureArray.getPVStructureArray();
            processRequesterName = pvStructureArray.getFullName();
            SupportState supportState = SupportState.readyForStart;
            DBStructure[] datas = dbStructureArray.getElementDBStructures();
            int n = datas.length;
            pvWaits = new PVBoolean[n];
            supports = new Support[n];
            for(int i=0; i< n; i++) {
                pvWaits[i] = null;
                supports[i] = null;
                DBStructure dbStructure = datas[i];
                if(dbStructure==null) continue;
                PVStructure pvStructure = dbStructure.getPVStructure();
                DBField[] dbFields = dbStructure.getDBFields();
                Structure structure = (Structure)pvStructure.getField();
                String structureName = structure.getStructureName();
                Support support = null;
                if(!structureName.equals("supportArrayElement")) {
                    support = dbStructure.getSupport();
                } else {
                    PVField[] pvdatas = pvStructure.getPVFields();
                    int index = structure.getFieldIndex("wait");
                    if(index<0) {
                        pvStructure.message("structure does not have field wait", MessageType.fatalError);
                        continue;
                    }
                    PVField pvField = pvdatas[index];
                    if(pvField.getField().getType()!=Type.pvBoolean) {
                        pvStructure.message("field wait is not boolean", MessageType.fatalError);
                        continue;
                    }
                    pvWaits[i] = (PVBoolean)pvField;
                    index = structure.getFieldIndex("link");
                    if(index<0) {
                        pvStructure.message("structure does not have field link", MessageType.fatalError);
                        continue;
                    }
                    pvField = pvdatas[index];
                    support = dbFields[index].getSupport();
                }
                if(support==null) {
                    pvStructure.message("field does not have suport", MessageType.fatalError);
                    continue;
                }
                support.initialize();
                if(support.getSupportState()!=SupportState.readyForStart) {
                    supportState = SupportState.readyForInitialize;
                    for(int j=0; j<i; j++) {
                        if(supports[j]!=null) supports[j].uninitialize();
                    }
                    break;
                }
                supports[i] = support;
            }
            setSupportState(supportState);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#start()
         */
        public void start() {
            if(!super.checkSupportState(SupportState.readyForStart,supportName)) return;
            SupportState supportState = SupportState.ready;
            int n = supports.length;
            for(int i=0; i< n; i++) {
                Support processLink = supports[i];
                if(processLink==null) continue;
                processLink.start();
            }
            setSupportState(supportState);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#stop()
         */
        public void stop() {
            if(super.getSupportState()!=SupportState.ready) return;
            int n = supports.length;
            for(int i=0; i< n; i++) {
                Support processLink = supports[i];
                if(processLink==null) continue;
                processLink.stop();
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
            if(supports!=null) for(Support processLink: supports) {
                if(processLink==null) continue;
                processLink.uninitialize();
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
            if(supports.length<=0) {
                supportProcessRequester.supportProcessDone(RequestResult.success);
                return;
            }
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
                supportProcessRequester.supportProcessDone(finalResult);
            }
        }
        
        private void callSupport() {
            if(!super.checkSupportState(SupportState.ready,supportName + ".processContinue")) {
                throw new IllegalStateException("processContinue but not ready");
            }
            while(nextLink<supports.length) {    
                Support processLink = supports[nextLink];
                if(processLink==null) {
                    nextLink++;
                    continue;
                }
                boolean wait = true; 
                if(pvWaits[nextLink]!=null) wait = pvWaits[nextLink].get();
                nextLink++;
                if(processLink.getSupportState()!=SupportState.ready) {
                    if(finalResult==RequestResult.success) {
                        finalResult = RequestResult.failure;
                    }
                } else {
                    numberWait++;
                    processLink.process(this);
                }
                if(wait) return;
            }
        }
    }
}
