/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support;

import java.util.*;
import org.epics.ioc.db.*;
import org.epics.ioc.process.*;
import org.epics.ioc.pv.*;
import org.epics.ioc.util.*;

/**
 * @author mrk
 *
 */
public class DelayLinkFactory {
    
    public static LinkSupport create(DBLink dbLink) {
        PVLink pvLink = dbLink.getPVLink();
        String supportName = pvLink.getSupportName();
        if(supportName==null || !supportName.equals(supportName)) {
            pvLink.message("does not have support " + supportName,MessageType.error);
            return null;
        }
        return new DelayLink(dbLink);
    }
    
    private static Timer timer = new Timer("delayLinkTimer");
    private static String supportName = "delayLink";
    
    private static class DelayLink extends AbstractLinkSupport implements ProcessContinueRequestor
    {       
        private TimerTask timerTask = null;
        private DBLink dbLink = null;
        private DBRecord dbRecord = null;
        private RecordProcess recordProcess = null;
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
         * @see org.epics.ioc.process.AbstractSupport#initialize()
         */
        public void initialize() {
            if(!super.checkSupportState(SupportState.readyForInitialize,supportName)) return;
            dbRecord = dbLink.getDBRecord();
            recordProcess = dbRecord.getRecordProcess();
            PVStructure configStructure = super.getConfigStructure("delayLink", true);
            if(configStructure==null) return;
            minAccess = configStructure.getLongField("min");
            if(minAccess==null) return;
            maxAccess = configStructure.getLongField("max");
            if(maxAccess==null) return;
            incAccess = configStructure.getLongField("inc");
            if(incAccess==null) return;
            setSupportState(SupportState.readyForStart);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.AbstractSupport#uninitialize()
         */
        public void uninitialize() {
            if(super.getSupportState()==SupportState.ready) {
                stop();
            }
            if(super.getSupportState()!=SupportState.readyForStart) return;
            setSupportState(SupportState.readyForInitialize);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.AbstractSupport#start()
         */
        public void start() {
            if(!super.checkSupportState(SupportState.readyForStart,supportName)) return;
            min = minAccess.get();
            max = maxAccess.get();
            inc = incAccess.get();
            if(min>max || inc<0) {
                super.message(
                        "Illegal values for min,max,inc. (min must be <= max) and (inc must be >=0)",
                        MessageType.error);
            }
            delay = min;
            setSupportState(SupportState.ready);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.AbstractSupport#stop()
         */
        public void stop() {
            if(super.getSupportState()!=SupportState.ready) return;
            setSupportState(SupportState.readyForStart);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.AbstractSupport#process(org.epics.ioc.process.SupportProcessRequestor)
         */
        public void process(SupportProcessRequestor supportProcessRequestor) {
            this.supportProcessRequestor = supportProcessRequestor;
            timerTask = new DelayTask(this);
            timer.schedule(timerTask, delay);
            delay += inc;
            if(delay>max) delay = min;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.ProcessContinueRequestor#processContinue()
         */
        public void processContinue() {
            supportProcessRequestor.supportProcessDone(RequestResult.success);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.LinkSupport#setField(org.epics.ioc.pvAccess.PVData)
         */
        public void setField(DBField dbField) {
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
