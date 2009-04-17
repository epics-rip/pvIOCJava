/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.basic;

import java.util.Timer;
import java.util.TimerTask;

import org.epics.ioc.install.AfterStart;
import org.epics.ioc.install.LocateSupport;
import org.epics.ioc.support.AbstractSupport;
import org.epics.ioc.support.ProcessContinueRequester;
import org.epics.ioc.support.RecordProcess;
import org.epics.ioc.support.Support;
import org.epics.ioc.support.SupportProcessRequester;
import org.epics.ioc.support.SupportState;
import org.epics.ioc.util.RequestResult;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVLong;
import org.epics.pvData.pv.PVStructure;



/**
 * @author mrk
 *
 */
public class DelayFactory {
    
    public static Support create(PVStructure pvStructure) {
        return new DelayImpl(pvStructure);
    }
    
    private static Timer timer = new Timer("delaySupportTimer");
    
    private static class DelayImpl extends AbstractSupport implements ProcessContinueRequester
    {       
        private PVStructure pvStructure = null;
        private RecordProcess recordProcess = null;
        private PVLong minAccess = null;
        private PVLong maxAccess = null;
        private PVLong incAccess = null;
        private long min,max,inc;
        
        private long delay = 0;
        private SupportProcessRequester supportProcessRequester = null;

        private DelayImpl(PVStructure pvStructure) {
            super("delay",pvStructure);
            this.pvStructure = pvStructure;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.support.Support#initialize(org.epics.ioc.support.RecordProcess)
         */
        
        /* (non-Javadoc)
         * @see org.epics.ioc.support.AbstractSupport#initialize(org.epics.ioc.support.RecordSupport)
         */
        @Override
        public void initialize(LocateSupport recordSupport) {
            if(!super.checkSupportState(SupportState.readyForInitialize,supportName)) return;
            this.recordProcess = recordSupport.getRecordProcess();
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
        public void start(AfterStart afterStart) {
            if(!super.checkSupportState(SupportState.readyForStart,supportName)) return;
            min = minAccess.get();
            max = maxAccess.get();
            inc = incAccess.get();
            if(min>max || inc<0) {
                super.message(
                        "Illegal values for min,max,inc. (min must be <= max) and (inc must be >=0)",
                        MessageType.error);
                return;
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
            TimerTask timerTask = new DelayTask(this);
            try {
                timer.schedule(timerTask, delay);
            } catch (IllegalStateException e) {
                pvStructure.message(
                        " timer.schedule failed " + e.getMessage(), MessageType.error);
            }
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
