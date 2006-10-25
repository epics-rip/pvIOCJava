/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support;

import org.epics.ioc.dbAccess.*;
import org.epics.ioc.dbProcess.*;
import org.epics.ioc.pvAccess.*;
import org.epics.ioc.dbDefinition.*;
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
     * @param dbArray The array which must be an array of links.
     * @return An interface to the support or null if the supportName was not "linkArray".
     */
    public static Support create(DBArray dbArray) {
        DBDArrayField dbdArrayField = (DBDArrayField)dbArray.getDBDField();
        DBType elementDBType = dbdArrayField.getElementDBType();
        if(elementDBType!=DBType.dbStructure) {
            dbArray.message("element type is not a structure",IOCMessageType.error);
            return null;
        }
        String supportName = dbArray.getSupportName();
        if(supportName==null || !supportName.equals(supportName)) {
            dbArray.message("does not have support " + supportName,IOCMessageType.error);
            return null;
        }
        return new StructureArray((DBStructureArray)dbArray);
    }
    private static String supportName = "linkArray";
    
    private static class StructureArray extends AbstractSupport
    implements LinkSupport,ProcessCallbackListener,SupportProcessRequestor
    {
        private DBStructureArray dbStructureArray;
        private String processRequestorName = null;
        private RecordProcess recordProcess = null;
        private RecordProcessSupport recordProcessSupport = null;
        private DBStructureArrayData structureArrayData = new DBStructureArrayData();
        private PVBoolean[] pvWaits = null;
        private DBLink[] dbLinks = null;
        private LinkSupport[] linkSupports = null;
        
        private PVData valueData = null;        
        private SupportProcessRequestor supportProcessRequestor;
        private int nextLink;
        private int numberWait;
        private RequestResult finalResult;
       
        private StructureArray(DBStructureArray array) {
            super(supportName,array);
            dbStructureArray = array; 
            processRequestorName = 
                dbStructureArray.getRecord().getRecordName()
                + dbStructureArray.getFullFieldName();
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.SupportProcessRequestor#getProcessRequestorName()
         */
        public String getSupportProcessRequestorName() {
            return processRequestorName;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.Support#initialize()
         */
        public void initialize() {
            if(!super.checkSupportState(SupportState.readyForInitialize,supportName)) return;
            recordProcess = dbStructureArray.getRecord().getRecordProcess();
            recordProcessSupport = recordProcess.getRecordProcessSupport();
            SupportState supportState = SupportState.readyForStart;
            int n = dbStructureArray.getLength();
            structureArrayData.offset = 0;
            n = dbStructureArray.get(0,n,structureArrayData);
            pvWaits = new PVBoolean[n];
            dbLinks = new DBLink[n];
            linkSupports = new LinkSupport[n];
            for(int i=0; i< n; i++) {
                pvWaits[i] = null;
                dbLinks[i] = null;
                linkSupports[i] = null;
                
                DBStructure dbStructure = structureArrayData.data[i];
                if(dbStructure==null) continue;
                if(!dbStructure.getDBDStructure().getStructureName().equals("linkArrayElement")) {
                    dbStructure.message("structureName not linkArrayElement", IOCMessageType.fatalError);
                    continue;
                }
                DBData[] dbDatas = dbStructure.getFieldDBDatas();
                int index = dbStructure.getFieldDBDataIndex("wait");
                if(index<0) {
                    dbStructure.message("structure does not have field wait", IOCMessageType.fatalError);
                    continue;
                }
                DBData dbData = dbDatas[index];
                if(dbData.getField().getType()!=Type.pvBoolean) {
                    dbStructure.message("field wait is not boolean", IOCMessageType.fatalError);
                    continue;
                }
                pvWaits[i] = (PVBoolean)dbData;
                index = dbStructure.getFieldDBDataIndex("link");
                if(index<0) {
                    dbStructure.message("structure does not have field link", IOCMessageType.fatalError);
                    continue;
                }
                dbData = dbDatas[index];
                if(dbData.getDBDField().getDBType()!=DBType.dbLink) {
                    dbStructure.message("field link is not a link", IOCMessageType.fatalError);
                    continue;
                }
                dbLinks[i] = (DBLink)dbData;
                DBLink dbLink = dbLinks[i];
                LinkSupport linkSupport = (LinkSupport)dbLink.getSupport();
                if(linkSupport==null) {
                    dbStructure.message("field link is not a link", IOCMessageType.fatalError);
                    continue;
                }
                linkSupport.initialize();
                linkSupport.setField(valueData);
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
         * @see org.epics.ioc.dbProcess.Support#start()
         */
        public void start() {
            if(!super.checkSupportState(SupportState.readyForStart,supportName)) return;
            SupportState supportState = SupportState.ready;
            int n = dbStructureArray.getLength();
            for(int i=0; i< n; i++) {
                LinkSupport processLink = linkSupports[i];
                if(processLink==null) continue;
                processLink.start();
            }
            setSupportState(supportState);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.Support#stop()
         */
        public void stop() {
            if(super.getSupportState()!=SupportState.ready) return;
            int n = dbStructureArray.getLength();
            for(int i=0; i< n; i++) {
                LinkSupport processLink = linkSupports[i];
                if(processLink==null) continue;
                processLink.stop();
            }
            setSupportState(SupportState.readyForStart);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.Support#uninitialize()
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
         * @see org.epics.ioc.dbProcess.Support#process(org.epics.ioc.dbProcess.RecordProcessRequestor)
         */
        public RequestResult process(SupportProcessRequestor supportProcessRequestor) {
            if(supportProcessRequestor==null) {
                throw new IllegalStateException("no processRequestListener");
            }
            if(!super.checkSupportState(SupportState.ready,supportName + ".process")) return RequestResult.failure;
            if(linkSupports.length<=0) {
                return RequestResult.success;
            }
            nextLink = 0;
            finalResult = RequestResult.success;
            this.supportProcessRequestor = supportProcessRequestor;
            recordProcessSupport.requestProcessCallback(this);
            return RequestResult.active;
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.Support#processContinue()
         */
        public void processContinue() {
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
                RequestResult requestResult;
                if(processLink.getSupportState()!=SupportState.ready) {
                    requestResult = RequestResult.failure;
                } else {
                    requestResult = processLink.process(this);
                }
                switch(requestResult) {
                case zombie:
                     finalResult = RequestResult.failure;
                     break;
                case failure:
                    finalResult = RequestResult.failure;
                    break;
                case success:
                    wait = false;
                    break;
                case active:
                    numberWait++;
                    break;
                default:
                    dbStructureArray.message(
                        "Unknown RequestResult state in ChannelAccessLocal",IOCMessageType.fatalError);
                }
                if(wait) return;
            }
            if(numberWait<=0) {
                supportProcessRequestor.processComplete(null);
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.LinkSupport#setField(org.epics.ioc.pvAccess.PVData)
         */
        public void setField(PVData field) {
            valueData = field;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.ProcessCallbackListener#processCallback()
         */
        public void processCallback() {
            recordProcessSupport.processContinue(this);
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcessRequestor#processComplete(org.epics.ioc.dbProcess.Support, org.epics.ioc.dbProcess.ProcessResult)
         */
        public void processComplete(RequestResult requestResult) {
            numberWait--;
            if(numberWait>0) return;
            recordProcessSupport.requestProcessCallback(this);
        }
    }
}
