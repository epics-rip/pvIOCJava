/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.basic;


import org.epics.ioc.install.AfterStart;
import org.epics.ioc.install.LocateSupport;
import org.epics.ioc.support.AbstractSupport;
import org.epics.ioc.support.ProcessContinueRequester;
import org.epics.ioc.support.RecordProcess;
import org.epics.ioc.support.Support;
import org.epics.ioc.support.SupportProcessRequester;
import org.epics.ioc.support.SupportState;
import org.epics.ioc.util.RequestResult;
import org.epics.pvData.misc.ThreadPriority;
import org.epics.pvData.misc.Timer;
import org.epics.pvData.misc.TimerFactory;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVDouble;
import org.epics.pvData.pv.PVStructure;



/**
 * @author mrk
 *
 */
public class DelayFactory {
    
    public static Support create(PVStructure pvStructure) {
        return new DelayImpl(pvStructure);
    }
    
    private static Timer timer = TimerFactory.create("delaySupportTimer",ThreadPriority.middle);
    
    private static class DelayImpl extends AbstractSupport implements ProcessContinueRequester,Timer.TimerCallback
    {
        private static final String supportName = "org.epics.ioc.delay";
        private Timer.TimerNode timerNode = TimerFactory.createNode(this);
        private PVStructure pvStructure = null;
        private RecordProcess recordProcess = null;
        private PVDouble minAccess = null;
        private PVDouble maxAccess = null;
        private PVDouble incAccess = null;
        private double delay = 0;
        private SupportProcessRequester supportProcessRequester = null;

        private DelayImpl(PVStructure pvStructure) {
            super(supportName,pvStructure);
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
            minAccess = pvStructure.getDoubleField("min");
            if(minAccess==null) return;
            maxAccess = pvStructure.getDoubleField("max");
            if(maxAccess==null) return;
            incAccess = pvStructure.getDoubleField("inc");
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
            double min = minAccess.get();
            double max = maxAccess.get();
            double inc = incAccess.get();
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
            timerNode.cancel();
            setSupportState(SupportState.readyForStart);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.AbstractSupport#process(org.epics.ioc.process.SupportProcessRequester)
         */
        public void process(SupportProcessRequester supportProcessRequester) {
            this.supportProcessRequester = supportProcessRequester;
            double min = minAccess.get();
            double max = maxAccess.get();
            double inc = incAccess.get();
            if(min>max || inc<0) {
                super.message(
                        "Illegal values for min,max,inc. (min must be <= max) and (inc must be >=0)",
                        MessageType.error);
                supportProcessRequester.supportProcessDone(RequestResult.failure);
            }
            timer.scheduleAfterDelay(timerNode, delay);
            delay += inc;
            if(delay>max) delay = min;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.ProcessContinueRequester#processContinue()
         */
        public void processContinue() {
            supportProcessRequester.supportProcessDone(RequestResult.success);
        }
        /* (non-Javadoc)
         * @see org.epics.pvData.misc.Timer.TimerCallback#callback()
         */
        public void callback() {
            recordProcess.processContinue(this);
        }
        /* (non-Javadoc)
         * @see org.epics.pvData.misc.Timer.TimerCallback#timerStopped()
         */
        public void timerStopped() {
            pvStructure.message("Why was timerStopped called", MessageType.error);
        }
        
    }
}
