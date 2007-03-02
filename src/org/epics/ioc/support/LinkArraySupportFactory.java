/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support;

import org.epics.ioc.db.*;
import org.epics.ioc.process.*;
import org.epics.ioc.pv.*;
import org.epics.ioc.util.*;

/**
 * Support for an array of links.
 * The links can be process, input, or output.
 * @author mrk
 *
 */
public class LinkArraySupportFactory {
    /**
     * Create support for an array of links.
     * @param pvArray The array which must be an array of links.
     * @return An interface to the support or null if the supportName was not "linkArray".
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
        return new StructureArray(dbField);
    }
    
    private static String supportName = "linkArray";
    
    private static class StructureArray extends AbstractSupport
    implements LinkSupport,SupportProcessRequestor
    {
        private PVStructureArray pvStructureArray;
        private String processRequestorName = null;
        private PVBoolean[] pvWaits = null;
        private LinkSupport[] linkSupports = null;
        
        private DBField valueField = null;        
        private SupportProcessRequestor supportProcessRequestor;
        private int nextLink;
        private int numberWait;
        private RequestResult finalResult;
       
        private StructureArray(DBField dbField) {
            super(supportName,dbField);
            PVField pvField = dbField.getPVField();
            processRequestorName = 
                pvField.getPVRecord().getRecordName() + pvField.getFullFieldName();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.SupportProcessRequestor#getProcessRequestorName()
         */
        public String getRequestorName() {
            return processRequestorName;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#initialize()
         */
        public void initialize() {
            if(!super.checkSupportState(SupportState.readyForInitialize,supportName)) return;
            DBNonScalarArray dbNonScalarArray = (DBNonScalarArray)super.getDBField();
            pvStructureArray = (PVStructureArray)dbNonScalarArray.getPVField();
            processRequestorName = 
                pvStructureArray.getPVRecord().getRecordName()
                + pvStructureArray.getFullFieldName();
            SupportState supportState = SupportState.readyForStart;
            int n = pvStructureArray.getLength();
            StructureArrayData structureArrayData = new StructureArrayData();
            structureArrayData.offset = 0;
            n = pvStructureArray.get(0,n,structureArrayData);
            DBField[] datas = dbNonScalarArray.getElementDBFields();
            pvWaits = new PVBoolean[n];
            linkSupports = new LinkSupport[n];
            for(int i=0; i< n; i++) {
                pvWaits[i] = null;
                linkSupports[i] = null;
                PVStructure pvStructure = structureArrayData.data[i];
                if(pvStructure==null) continue;
                DBStructure dbStructure = (DBStructure)datas[i];
                DBField[] dbFields = dbStructure.getFieldDBFields();
                Structure structure = (Structure)pvStructure.getField();
                String structureName = structure.getStructureName();
                if(!structureName.equals("linkArrayElement")) {
                    pvStructure.message(structureName + " not linkArrayElement", MessageType.fatalError);
                    continue;
                }
                PVField[] pvdatas = pvStructure.getFieldPVFields();
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
                if(pvField.getField().getType()!=Type.pvLink) {
                    pvStructure.message("field link is not a link", MessageType.fatalError);
                    continue;
                }
                LinkSupport linkSupport = (LinkSupport)dbFields[index].getSupport();
                if(linkSupport==null) {
                    pvStructure.message("field link is not a link", MessageType.fatalError);
                    continue;
                }
                linkSupport.initialize();
                linkSupport.setField(valueField);
                if(linkSupport.getSupportState()!=SupportState.readyForStart) {
                    supportState = SupportState.readyForInitialize;
                    for(int j=0; j<i; j++) {
                        if(linkSupports[j]!=null) linkSupports[j].uninitialize();
                    }
                    break;
                }
                linkSupports[i] = linkSupport;
            }
            setSupportState(supportState);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#start()
         */
        public void start() {
            if(!super.checkSupportState(SupportState.readyForStart,supportName)) return;
            SupportState supportState = SupportState.ready;
            int n = pvStructureArray.getLength();
            for(int i=0; i< n; i++) {
                LinkSupport processLink = linkSupports[i];
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
            int n = pvStructureArray.getLength();
            for(int i=0; i< n; i++) {
                LinkSupport processLink = linkSupports[i];
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
            if(linkSupports!=null) for(LinkSupport processLink: linkSupports) {
                if(processLink==null) continue;
                processLink.uninitialize();
            }
            setSupportState(SupportState.readyForInitialize);
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#process(org.epics.ioc.process.RecordProcessRequestor)
         */
        public void process(SupportProcessRequestor supportProcessRequestor) {
            if(supportProcessRequestor==null) {
                throw new IllegalStateException("no processRequestListener");
            }
            if(!super.checkSupportState(SupportState.ready,supportName + ".process")) {
                supportProcessRequestor.supportProcessDone(RequestResult.failure);
                return;
            }
            if(linkSupports.length<=0) {
                supportProcessRequestor.supportProcessDone(RequestResult.success);
                return;
            }
            this.supportProcessRequestor = supportProcessRequestor;
            nextLink = 0;
            finalResult = RequestResult.success;
            callLinkSupport();
        }                
        /* (non-Javadoc)
         * @see org.epics.ioc.process.LinkSupport#setField(org.epics.ioc.db.DBField)
         */
        public void setField(DBField dbField) {
            valueField = dbField;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.SupportProcessRequestor#supportProcessDone(org.epics.ioc.util.RequestResult)
         */
        public void supportProcessDone(RequestResult requestResult) {
            if(requestResult!=RequestResult.success) {
                if(finalResult!=RequestResult.zombie) {
                    finalResult = requestResult;
                }
            }
            numberWait--;
            if(numberWait>0) return;
            if(nextLink<linkSupports.length) {
                callLinkSupport();
            } else {
                supportProcessRequestor.supportProcessDone(finalResult);
            }
        }
        
        private void callLinkSupport() {
            if(!super.checkSupportState(SupportState.ready,supportName + ".processContinue")) {
                throw new IllegalStateException("processContinue but not ready");
            }
            while(nextLink<linkSupports.length) {    
                LinkSupport processLink = linkSupports[nextLink];
                if(processLink==null) {
                    nextLink++;
                    continue;
                }
                boolean wait = pvWaits[nextLink].get(); 
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
