/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support;

import org.epics.ca.server.ChannelProcessor;
import org.epics.ca.server.ChannelProcessorProvider;
import org.epics.ca.server.ChannelProcessorRequester;
import org.epics.ca.server.ChannelServer;
import org.epics.ca.server.impl.local.ChannelServerFactory;
import org.epics.ioc.install.IOCDatabaseFactory;
import org.epics.ioc.install.LocateSupport;
import org.epics.ioc.util.RequestResult;
import org.epics.pvData.factory.PVDatabaseFactory;
import org.epics.pvData.factory.StatusFactory;
import org.epics.pvData.property.TimeStamp;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVDatabase;
import org.epics.pvData.pv.PVRecord;
import org.epics.pvData.pv.Status;
import org.epics.pvData.pv.StatusCreate;
import org.epics.pvData.pv.Status.StatusType;

/**
 * @author mrk
 *
 */
public class ChannelProcessorProviderFactory {
    
    private static final ChannelProcessorProvider channelProcessProvider = new Provider();
    private static final ChannelServer channelServer = ChannelServerFactory.getChannelServer();
    private static final PVDatabase pvDatabase = PVDatabaseFactory.getMaster();
    private static final StatusCreate statusCreate = StatusFactory.getStatusCreate();
    private static final Status okStatus = statusCreate.getStatusOK();
	
   /**
     * Register. This is called by InstallFactory.
     */
    static public void register() {
        channelServer.registerChannelProcessProvider(channelProcessProvider);
    }
    

    static private class Provider implements ChannelProcessorProvider {
        /* (non-Javadoc)
         * @see org.epics.ca.server.ChannelProcessorProvider#requestChannelProcessor(org.epics.pvData.pv.PVRecord, org.epics.ca.server.ChannelProcessorRequester)
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
    
    static private class Process implements ChannelProcessor, RecordProcessRequester {
        private RecordProcess recordProcess = null;
        private ChannelProcessorRequester channelProcessRequester;
        private ProcessToken processToken = null;
        
        
        private Process(ChannelProcessorRequester channelProcessRequester,RecordProcess recordProcess) {
            this.channelProcessRequester = channelProcessRequester;
            this.recordProcess = recordProcess;
        }
        
        boolean isProcessor() {
        	processToken = recordProcess.requestProcessToken(this);
        	return (processToken!=null) ? true : false;
        }
        /* (non-Javadoc)
         * @see org.epics.ca.server.ChannelProcessor#detach()
         */
        @Override
        public void detach() {
            recordProcess.releaseProcessToken(processToken);
            processToken = null;
        }
        /* (non-Javadoc)
         * @see org.epics.ca.server.ChannelProcessor#requestProcess()
         */
        @Override
        public void requestProcess() {
        	recordProcess.queueProcessRequest(processToken);
        }
        /* (non-Javadoc)
         * @see org.epics.ca.server.ChannelProcessor#process(boolean, org.epics.pvData.property.TimeStamp)
         */
        @Override
        public void process(boolean leaveActive, TimeStamp timeStamp) {
            recordProcess.process(processToken,leaveActive, timeStamp);
        }
        /* (non-Javadoc)
         * @see org.epics.ca.server.ChannelProcessor#setInactive()
         */
        @Override
        public void setInactive() {
            recordProcess.setInactive(processToken);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.support.RecordProcessRequester#becomeProcessor()
         */
        @Override
		public void becomeProcessor() {
        	channelProcessRequester.becomeProcessor();
		}
		/* (non-Javadoc)
		 * @see org.epics.ioc.support.RecordProcessRequester#canNotProcess(java.lang.String)
		 */
		@Override
		public void canNotProcess(String reason) {
			channelProcessRequester.canNotProcess(reason);
		}
		/* (non-Javadoc)
		 * @see org.epics.ioc.support.RecordProcessRequester#lostRightToProcess()
		 */
		@Override
		public void lostRightToProcess() {
			channelProcessRequester.lostRightToProcess();
		}
		/* (non-Javadoc)
         * @see org.epics.ioc.support.RecordProcessRequester#recordProcessComplete()
         */
        @Override
        public void recordProcessComplete() {
            channelProcessRequester.recordProcessComplete();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.support.RecordProcessRequester#recordProcessResult(org.epics.ioc.util.RequestResult)
         */
        @Override
        public void recordProcessResult(RequestResult requestResult) {
            if(requestResult!=RequestResult.success) {
                channelProcessRequester.recordProcessResult(statusCreate.createStatus(StatusType.ERROR, "requestResult " + requestResult.toString(), null));
                return;
            }
            channelProcessRequester.recordProcessResult(okStatus);
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
