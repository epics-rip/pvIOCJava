/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support;

import org.epics.ioc.dbAccess.*;
import org.epics.ioc.dbProcess.*;
import org.epics.ioc.dbProcess.*;

/**
 * @author mrk
 *
 */
public class ProcessLinkArrayFactory {
    public static Support create(DBArray dbArray) {
        Support support = null;
        String supportName = dbArray.getSupportName();
        if(supportName!=null && supportName.equals("processLinkArray")) {
            support = new ProcessLinkArray(dbArray);
        }
        return support;
    }
    
    private static class ProcessLinkArray extends AbstractSupport
    implements ProcessCallbackListener,ProcessCompleteListener,SupportStateListener
    {
        private RecordProcess recordProcess;
        private RecordProcessSupport recordProcessSupport;
        private SupportState supportState = SupportState.readyForInitialize;
        private static String supportName = "ProcessLinkArray";
        private DBLinkArray dbLinkArray;
        private LinkArrayData linkArrayData = new LinkArrayData();
        private DBLink[] dbLinks = null;
        private LinkSupport[] processLinks = null;
        
        private ProcessCompleteListener listener;
        private int nextLink;
        private int numberWait;
        private ProcessResult finalResult = ProcessResult.success;
       
        ProcessLinkArray(DBArray array) {
            super(supportName,array);
            dbLinkArray = (DBLinkArray)array; 
        }

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

        public void stop() {
            int n = dbLinkArray.getLength();
            for(int i=0; i< n; i++) {
                LinkSupport processLink = processLinks[i];
                if(processLink==null) continue;
                processLink.stop();
            }
            setSupportState(SupportState.readyForInitialize);
        }
        public void uninitialize() {
            supportState = SupportState.readyForInitialize;
            if(processLinks!=null) for(LinkSupport processLink: processLinks) {
                if(processLink==null) continue;
                processLink.uninitialize();
            }
            setSupportState(supportState);
        }
        
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

        public void callback() {
            recordProcessSupport.processContinue(this);
        }
        
        public void processComplete(Support support,ProcessResult result) {
            if(result==ProcessResult.failure) finalResult = result;
            numberWait--;
            if(numberWait>0) return;
            listener.processComplete(this,finalResult);
        }
        public void newState(Support support,SupportState state) {
            if(state.compareTo(getSupportState())>0) {
                setSupportState(state);
            }
        }
    }
}
