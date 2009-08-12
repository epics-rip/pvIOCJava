/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support;

import org.epics.ca.channelAccess.server.ChannelProcessor;
import org.epics.ca.channelAccess.server.ChannelProcessorProvider;
import org.epics.ca.channelAccess.server.ChannelProcessorRequester;
import org.epics.ca.channelAccess.server.ChannelServer;
import org.epics.ca.channelAccess.server.impl.ChannelServerFactory;
import org.epics.ioc.install.IOCDatabaseFactory;
import org.epics.ioc.install.LocateSupport;
import org.epics.ioc.util.RequestResult;
import org.epics.pvData.factory.PVDatabaseFactory;
import org.epics.pvData.property.TimeStamp;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVDatabase;
import org.epics.pvData.pv.PVRecord;

/**
 * @author mrk
 *
 */
public class ChannelProcessorProviderFactory {
    
    private static final ChannelProcessorProvider channelProcessProvider = new Provider();
    private static final ChannelServer channelServer = ChannelServerFactory.getChannelServer();
    private static final PVDatabase pvDatabase = PVDatabaseFactory.getMaster();
    /**
     * Register. This is called by InstallFactory.
     */
    static public void register() {
        channelServer.registerChannelProcessProvider(channelProcessProvider);
    }
    

    static private class Provider implements ChannelProcessorProvider {

        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelProcessProvider#requestChannelProcess(org.epics.pvData.pv.PVRecord, org.epics.ioc.channelAccess.ChannelProcessRequester)
         */
        @Override
        public ChannelProcessor requestChannelProcessor(PVRecord pvRecord,ChannelProcessorRequester channelProcessorRequester) {
          LocateSupport locateSupport = IOCDatabaseFactory.get(pvDatabase).getLocateSupport(pvRecord);
          if(locateSupport==null) {
              PVDatabase pvDatabase = PVDatabaseFactory.getBeingInstalled();
              locateSupport = IOCDatabaseFactory.get(pvDatabase).getLocateSupport(pvRecord);
          }
          if(locateSupport==null) {
              channelProcessorRequester.message("locateSupport not found", MessageType.error);
              return null;
          }
          RecordProcess recordProcess = locateSupport.getRecordProcess();
          Process process =  new Process(channelProcessorRequester,recordProcess);
          return (process.isProcessor() ? process : null);
        }
    }
    
    static private class Process implements ChannelProcessor, RecordProcessRequester,ProcessSelfRequester {
        private RecordProcess recordProcess = null;
        private ChannelProcessorRequester channelProcessRequester;
        private boolean isRecordProcessRequester = false;
        private ProcessSelf processSelf = null;
        
        
        private Process(ChannelProcessorRequester channelProcessRequester,RecordProcess recordProcess) {
            this.channelProcessRequester = channelProcessRequester;
            this.recordProcess = recordProcess;
        }
        
        boolean isProcessor() {
            isRecordProcessRequester = recordProcess.setRecordProcessRequester(this);
            if(!isRecordProcessRequester) {
                processSelf = recordProcess.canProcessSelf();
                if(processSelf==null) {
                    channelProcessRequester.message(
                            "already has process requester other than self", MessageType.error);
                    return false;
                }
            }
            return true;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelProcess#detach()
         */
        @Override
        public void detach() {
            recordProcess.releaseRecordProcessRequester(this);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelProcessor#requestProcess()
         */
        @Override
        public void requestProcess() {
            if(isRecordProcessRequester) {
                channelProcessRequester.becomeProcessor();
                return;
            }
            processSelf.request(this);
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelProcess#setActive()
         */
        @Override
        public boolean setActive() {
            return recordProcess.setActive(this);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelProcess#process(boolean, org.epics.pvData.property.TimeStamp)
         */
        @Override
        public boolean process(boolean leaveActive, TimeStamp timeStamp) {
                return recordProcess.process(this, leaveActive, timeStamp);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelProcess#setInactive()
         */
        @Override
        public void setInactive() {
            recordProcess.setInactive(this);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.support.RecordProcessRequester#recordProcessComplete()
         */
        @Override
        public void recordProcessComplete() {
            if(!isRecordProcessRequester) processSelf.endRequest(this);
            channelProcessRequester.recordProcessComplete();
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.support.RecordProcessRequester#recordProcessResult(org.epics.ioc.util.RequestResult)
         */
        @Override
        public void recordProcessResult(RequestResult requestResult) {
            if(requestResult!=RequestResult.success) {
                channelProcessRequester.message("requestResult " + requestResult.toString(), MessageType.error);
                channelProcessRequester.recordProcessResult(false);
                return;
            }
            channelProcessRequester.recordProcessResult(true);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.support.ProcessSelfRequester#becomeProcessor(org.epics.ioc.support.RecordProcess)
         */
        @Override
        public void becomeProcessor(RecordProcess recordProcess) {
            channelProcessRequester.becomeProcessor();
        }

        /* (non-Javadoc)
         * @see org.epics.pvData.pv.Requester#getRequesterName()
         */
        @Override
        public String getRequesterName() {
            return channelProcessRequester.getRequesterName();
        }
        /* (non-Javadoc)
         * @see org.epics.pvData.pv.Requester#message(java.lang.String, org.epics.pvData.pv.MessageType)
         */
        @Override
        public void message(String message, MessageType messageType) {
            channelProcessRequester.message(message, messageType);
        }
        
    }
}
