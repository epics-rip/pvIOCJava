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
        if(dbArray.getElementDBType()!=DBType.dbLink) {
            System.out.println(
                dbArray.getRecord().getRecordName()
                + "." + dbArray.getFullFieldName()
                + " element type is not a link");
            return null;
        }
        Support support = null;
        String supportName = dbArray.getSupportName();
        if(supportName!=null && supportName.equals("linkArray")) {
            support = new LinkArray(dbArray);
        }
        return support;
    }
    
    private static class LinkArray extends AbstractSupport
    implements LinkSupport,ProcessCallbackListener,ProcessCompleteListener,SupportStateListener
    {
        private RecordProcess recordProcess;
        private RecordProcessSupport recordProcessSupport;
        private SupportState supportState = SupportState.readyForInitialize;
        private static String supportName = "ProcessOutputLinkArray";
        private DBLinkArray dbLinkArray;
        private LinkArrayData linkArrayData = new LinkArrayData();
        private DBLink[] dbLinks = null;
        private LinkSupport[] processLinks = null;
        
        private PVData valueData = null;
        
        private ProcessCompleteListener listener;
        private int nextLink;
        private int numberWait;
        private ProcessResult finalResult = ProcessResult.success;
       
        private LinkArray(DBArray array) {
            super(supportName,array);
            dbLinkArray = (DBLinkArray)array; 
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.Support#initialize()
         */
        public void initialize() {
            recordProcess = dbLinkArray.getRecord().getRecordProcess();
            recordProcessSupport = recordProcess.getRecordProcessSupport();
            supportState = SupportState.readyForStart;
            int n = dbLinkArray.getLength();
            linkArrayData.offset = 0;
            n = dbLinkArray.get(0,n,linkArrayData);
            dbLinks = new DBLink[n];
            processLinks = new LinkSupport[n];
            dbLinks = linkArrayData.data;;
            for(int i=0; i< n; i++) {
                processLinks[i] = null;
                DBLink dbLink = dbLinks[i];
                if(dbLink==null) continue;
                LinkSupport linkSupport = null;
                try {
                    linkSupport = (LinkSupport)dbLink.getSupport();
                } catch (Exception e) {
                    errorMessage("getSupport " + e.getMessage());
                    supportState = SupportState.readyForInitialize;
                    continue;
                }
                linkSupport.initialize();
                linkSupport.setField(valueData);
                if(linkSupport.getSupportState()!=SupportState.readyForStart) {
                    supportState = SupportState.readyForInitialize;
                    linkSupport.uninitialize();
                }
                processLinks[i] = linkSupport;
            }
            if(supportState!=SupportState.readyForStart) {
                for(int i=0; i< n; i++) {
                    if(processLinks[i]!=null) processLinks[i].uninitialize();
                    processLinks[i] = null;
                }
            }
            setSupportState(supportState);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.Support#start()
         */
        public void start() {
            supportState = SupportState.ready;
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
            int n = dbLinkArray.getLength();
            for(int i=0; i< n; i++) {
                LinkSupport processLink = processLinks[i];
                if(processLink==null) continue;
                processLink.stop();
            }
            setSupportState(SupportState.readyForInitialize);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.Support#uninitialize()
         */
        public void uninitialize() {
            supportState = SupportState.readyForInitialize;
            if(processLinks!=null) for(LinkSupport processLink: processLinks) {
                if(processLink==null) continue;
                processLink.uninitialize();
            }
            setSupportState(supportState);
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.Support#process(org.epics.ioc.dbProcess.ProcessCompleteListener)
         */
        public ProcessReturn process(ProcessCompleteListener listener) {
            if(supportState!=SupportState.ready) {
                errorMessage(
                    "process called but supportState is "
                    + supportState.toString());
                return ProcessReturn.failure;
            }
            if(processLinks.length<=0) {
                return ProcessReturn.noop;
            }
            nextLink = 0;
            finalResult = ProcessResult.success;
            this.listener = listener;
            recordProcessSupport.requestProcessCallback(this);
            return ProcessReturn.active;
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.Support#processContinue()
         */
        public void processContinue() {
            boolean allDone = true;
            while(nextLink<processLinks.length) {
                LinkSupport processLink = processLinks[nextLink++];
                if(processLink==null) continue;
                ProcessReturn processReturn = processLink.process(this);
                switch(processReturn) {
                case zombie:
                case noop:
                case success: break;
                case failure:
                    finalResult = ProcessResult.failure;
                    break;
                case active:
                case alreadyActive:
                    numberWait++;
                    allDone = false;
                    break;
                default:
                    throw new IllegalStateException("Unknown ProcessReturn state in ChannelAccessLocal");
                }
            }
            if(allDone) listener.processComplete(this,finalResult);
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
        public void callback() {
            recordProcessSupport.processContinue(this);
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.ProcessCompleteListener#processComplete(org.epics.ioc.dbProcess.Support, org.epics.ioc.dbProcess.ProcessResult)
         */
        public void processComplete(Support support,ProcessResult result) {
            if(result==ProcessResult.failure) finalResult = result;
            numberWait--;
            if(numberWait>0) return;
            listener.processComplete(this,finalResult);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.SupportStateListener#newState(org.epics.ioc.dbProcess.Support, org.epics.ioc.dbProcess.SupportState)
         */
        public void newState(Support support,SupportState state) {
            if(state.compareTo(getSupportState())>0) {
                setSupportState(state);
            }
        }
    }
}
