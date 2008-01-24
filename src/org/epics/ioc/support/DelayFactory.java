/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support;

import java.util.Timer;
import java.util.TimerTask;

import org.epics.ioc.db.DBRecord;
import org.epics.ioc.db.DBStructure;
import org.epics.ioc.process.ProcessContinueRequester;
import org.epics.ioc.process.RecordProcess;
import org.epics.ioc.process.SupportProcessRequester;
import org.epics.ioc.process.SupportState;
import org.epics.ioc.pv.PVLong;
import org.epics.ioc.pv.PVStructure;
import org.epics.ioc.util.MessageType;
import org.epics.ioc.util.RequestResult;

/**
 * @author mrk
 *
 */
public class DelayFactory {
    
    public static Support create(DBStructure dbStructure) {
        PVStructure pvStructure = dbStructure.getPVStructure();
        String supportName = pvStructure.getSupportName();
        if(supportName==null || !supportName.equals(supportName)) {
            pvStructure.message("does not have support " + supportName,MessageType.error);
            return null;
        }
        return new DelayImpl(dbStructure);
    }
    
    private static Timer timer = new Timer("delaySupportTimer");
    private static String supportName = "delay";
    
    private static class DelayImpl extends AbstractSupport implements ProcessContinueRequester
    {       
        private TimerTask timerTask = null;
        private DBStructure dbStructure = null;
        private DBRecord dbRecord = null;
        private RecordProcess recordProcess = null;
        private PVLong minAccess = null;
        private PVLong maxAccess = null;
        private PVLong incAccess = null;
        private long min,max,inc;
        
        private long delay = 0;
        private SupportProcessRequester supportProcessRequester = null;

        private DelayImpl(DBStructure dbStructure) {
            super(supportName,dbStructure);
            this.dbStructure = dbStructure;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.AbstractSupport#initialize()
         */
        public void initialize() {
            if(!super.checkSupportState(SupportState.readyForInitialize,supportName)) return;
            PVStructure pvStructure = dbStructure.getPVStructure();
            dbRecord = dbStructure.getDBRecord();
            recordProcess = dbRecord.getRecordProcess();
            minAccess = pvStructure.getLongField("min");
            if(minAccess==null) return;
            maxAccess = pvStructure.getLongField("max");
            if(maxAccess==null) return;
            incAccess = pvStructure.getLongField("inc");
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
         * @see org.epics.ioc.process.AbstractSupport#process(org.epics.ioc.process.SupportProcessRequester)
         */
        public void process(SupportProcessRequester supportProcessRequester) {
            this.supportProcessRequester = supportProcessRequester;
            timerTask = new DelayTask(this);
            timer.schedule(timerTask, delay);
            delay += inc;
            if(delay>max) delay = min;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.ProcessContinueRequester#processContinue()
         */
        public void processContinue() {
            supportProcessRequester.supportProcessDone(RequestResult.success);
        }
        
        private void delayDone() {
            recordProcess.processContinue(this);
        }
        
    }
    
    private static class DelayTask extends TimerTask {
        DelayImpl delayImpl;
        
        private DelayTask(DelayImpl delayImpl) {
            this.delayImpl = delayImpl;
        }

        /* (non-Javadoc)
         * @see java.lang.Runnable#run()
         */
        public void run() {
            delayImpl.delayDone();
        }
    }
}
