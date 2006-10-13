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
        if(elementDBType!=DBType.dbLink) {
            dbArray.message("element type is not a link",IOCMessageType.error);
            return null;
        }
        String supportName = dbArray.getSupportName();
        if(supportName==null || !supportName.equals(supportName)) {
            dbArray.message("does not have support " + supportName,IOCMessageType.error);
            return null;
        }
        return new LinkArray((DBLinkArray)dbArray);
    }
    private static String supportName = "linkArray";
    
    private static class LinkArray extends AbstractSupport
    implements LinkSupport,ProcessCallbackListener,ProcessRequestListener
    {
        
        private DBLinkArray dbLinkArray;
        private RecordProcess recordProcess = null;
        private RecordProcessSupport recordProcessSupport = null;
        private LinkArrayData linkArrayData = new LinkArrayData();
        private DBLink[] dbLinks = null;
        private LinkSupport[] processLinks = null;
        
        private PVData valueData = null;
        
        private ProcessRequestListener listener;
        private int nextLink;
        private int numberWait;
       
        private LinkArray(DBLinkArray array) {
            super(supportName,array);
            dbLinkArray = array; 
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.Support#initialize()
         */
        public void initialize() {
            if(!super.checkSupportState(SupportState.readyForInitialize,supportName)) return;
            recordProcess = dbLinkArray.getRecord().getRecordProcess();
            recordProcessSupport = recordProcess.getRecordProcessSupport();
            SupportState supportState = SupportState.readyForStart;
            int n = dbLinkArray.getLength();
            linkArrayData.offset = 0;
            n = dbLinkArray.get(0,n,linkArrayData);
            processLinks = new LinkSupport[n];
            dbLinks = linkArrayData.data;;
            for(int i=0; i< n; i++) {
                processLinks[i] = null;
                DBLink dbLink = dbLinks[i];
                if(dbLink==null) continue;
                LinkSupport linkSupport = (LinkSupport)dbLink.getSupport();
                linkSupport.initialize();
                linkSupport.setField(valueData);
                if(linkSupport.getSupportState()!=SupportState.readyForStart) {
                    supportState = SupportState.readyForInitialize;
                    for(int j=0; j<i; j++) {
                        if(processLinks[j]!=null) processLinks[j].uninitialize();
                    }
                    break;
                }
                processLinks[i] = linkSupport;
            }
            setSupportState(supportState);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.Support#start()
         */
        public void start() {
            if(!super.checkSupportState(SupportState.readyForStart,supportName)) return;
            SupportState supportState = SupportState.ready;
            int n = dbLinkArray.getLength();
            for(int i=0; i< n; i++) {
                LinkSupport processLink = processLinks[i];
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
            int n = dbLinkArray.getLength();
            for(int i=0; i< n; i++) {
                LinkSupport processLink = processLinks[i];
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
            if(processLinks!=null) for(LinkSupport processLink: processLinks) {
                if(processLink==null) continue;
                processLink.uninitialize();
            }
            setSupportState(SupportState.readyForInitialize);
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.Support#process(org.epics.ioc.dbProcess.ProcessRequestListener)
         */
        public ProcessReturn process(ProcessRequestListener listener) {
            if(!super.checkSupportState(SupportState.ready,supportName + ".process")) return ProcessReturn.failure;
            if(processLinks.length<=0) {
                return ProcessReturn.noop;
            }
            nextLink = 0;
            this.listener = listener;
            recordProcessSupport.requestProcessCallback(this);
            return ProcessReturn.active;
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.Support#processContinue()
         */
        public ProcessContinueReturn processContinue() {
            if(!super.checkSupportState(SupportState.ready,supportName + ".processContinue")) {
                return ProcessContinueReturn.failure;
            }
            boolean allDone = true;
            while(nextLink<processLinks.length) {
                LinkSupport processLink = processLinks[nextLink++];
                if(processLink==null) continue;
                ProcessReturn processReturn;
                if(processLink.getSupportState()!=SupportState.ready) {
                    processReturn = ProcessReturn.failure;
                } else {
                    processReturn = processLink.process(this);
                }
                switch(processReturn) {
                case zombie:
                case noop:
                case success: break;
                case failure:
                    break;
                case active:
                    numberWait++;
                    allDone = false;
                    break;
                default:
                    dbLinkArray.message(
                        "Unknown ProcessReturn state in ChannelAccessLocal",IOCMessageType.fatalError);
                }
            }
            if(allDone) {
                listener.processComplete();
                return ProcessContinueReturn.success;
            }
            return ProcessContinueReturn.active;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.LinkSupport#setField(org.epics.ioc.pvAccess.PVData)
         */
        public void setField(PVData field) {
            valueData = field;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.ProcessCallbackListener#callback()
         */
        public void processCallback() {
            recordProcessSupport.processContinue(this);
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.ProcessRequestListener#processComplete(org.epics.ioc.dbProcess.Support, org.epics.ioc.dbProcess.ProcessResult)
         */
        public void processComplete() {
            ProcessRequestListener listener = null;
            dbLinkArray.getRecord().lock();
            try {
                if(!super.checkSupportState(SupportState.ready,supportName + ".processComplete")) return;
                numberWait--;
                if(numberWait>0) return;
                listener = this.listener;
            } finally {
                dbLinkArray.getRecord().unlock();
            }
            if(listener!=null) listener.processComplete();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.ProcessRequestListener#requestResult(org.epics.ioc.util.AlarmSeverity, java.lang.String, org.epics.ioc.util.TimeStamp)
         */
        public void requestResult(AlarmSeverity alarmSeverity, String status, TimeStamp timeStamp) {
            // nothing to do   
        }
    }
}
