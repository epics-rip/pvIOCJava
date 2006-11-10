/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbProcess.example;

import java.util.*;
import org.epics.ioc.dbProcess.*;
import org.epics.ioc.dbAccess.*;
import org.epics.ioc.pvAccess.*;
import org.epics.ioc.util.MessageType;
import org.epics.ioc.util.RequestResult;

/**
 * @author mrk
 *
 */
public class DelayLinkFactory {
    
    public static LinkSupport create(DBLink dbLink) {
        return new DelayLink(dbLink);
    }
    
    private static Timer timer = new Timer("DelayLinkTimer");
    private static String supportName = "delayLink";
    
    private static class DelayLink extends AbstractSupport
    implements LinkSupport,ProcessContinueRequestor
    {
        private TimerTask timerTask = new DelayTask(this);
        private DBLink dbLink = null;
        private DBRecord dbRecord = null;
        private RecordProcess recordProcess = null;
        private DBStructure configStructure = null;
        private PVLong minAccess = null;
        private PVLong maxAccess = null;
        private PVLong incAccess = null;
        private long min,max,inc;
        
        private long delay = 0;
        private SupportProcessRequestor supportProcessRequestor = null;

        private DelayLink(DBLink dbLink) {
            super(supportName,dbLink);
            this.dbLink = dbLink;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.AbstractSupport#initialize()
         */
        @Override
        public void initialize() {
            if(!super.checkSupportState(SupportState.readyForInitialize,supportName)) return;
            dbRecord = dbLink.getRecord();
            recordProcess = dbRecord.getRecordProcess();
            configStructure = super.getConfigStructure("delayLink");
            if(configStructure==null) return;
            minAccess = super.getLong(configStructure,"min");
            if(minAccess==null) return;
            maxAccess = super.getLong(configStructure,"max");
            if(maxAccess==null) return;
            incAccess = super.getLong(configStructure,"inc");
            if(incAccess==null) return;
            setSupportState(SupportState.readyForStart);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.AbstractSupport#uninitialize()
         */
        @Override
        public void uninitialize() {
            if(super.getSupportState()==SupportState.ready) {
                stop();
            }
            if(super.getSupportState()!=SupportState.readyForStart) return;
            setSupportState(SupportState.readyForInitialize);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.AbstractSupport#start()
         */
        public void start() {
            if(!super.checkSupportState(SupportState.readyForStart,supportName)) return;
            min = minAccess.get();
            max = maxAccess.get();
            inc = incAccess.get();
            if(min>max || inc<0) {
                configStructure.message(
                        "Illegal values for min,max,inc. (min must be <= max) and (inc must be >=0)",
                        MessageType.error);
            }
            delay = min;
            setSupportState(SupportState.ready);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.AbstractSupport#stop()
         */
        public void stop() {
            if(super.getSupportState()!=SupportState.ready) return;
            setSupportState(SupportState.readyForStart);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.AbstractSupport#process(org.epics.ioc.dbProcess.SupportProcessRequestor)
         */
        public void process(SupportProcessRequestor supportProcessRequestor) {
            this.supportProcessRequestor = supportProcessRequestor;
            timer.schedule(timerTask, delay);
            delay += inc;
            if(delay>max) delay = min;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.ProcessContinueRequestor#processContinue()
         */
        public void processContinue() {
            supportProcessRequestor.supportProcessDone(RequestResult.success);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.LinkSupport#setField(org.epics.ioc.pvAccess.PVData)
         */
        public void setField(PVData field) {
            // nothing to do
        }

        private void delayDone() {
            recordProcess.processContinue(this);
        }
        
    }
    
    private static class DelayTask extends TimerTask {
        DelayLink delayLink;
        
        private DelayTask(DelayLink delayLink) {
            this.delayLink = delayLink;
        }

        /* (non-Javadoc)
         * @see java.lang.Runnable#run()
         */
        public void run() {
            delayLink.delayDone();
        }
    }
}
