/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.support.basic;


import org.epics.pvdata.misc.ThreadPriority;
import org.epics.pvdata.misc.Timer;
import org.epics.pvdata.misc.TimerFactory;
import org.epics.pvdata.pv.MessageType;
import org.epics.pvdata.pv.PVDouble;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvioc.database.PVRecordStructure;
import org.epics.pvioc.install.AfterStart;
import org.epics.pvioc.support.AbstractSupport;
import org.epics.pvioc.support.ProcessContinueRequester;
import org.epics.pvioc.support.RecordProcess;
import org.epics.pvioc.support.Support;
import org.epics.pvioc.support.SupportProcessRequester;
import org.epics.pvioc.support.SupportState;
import org.epics.pvioc.util.RequestResult;



/**
 * @author mrk
 *
 */
public class DelayFactory {
    
    public static Support create(PVRecordStructure pvRecordStructure) {
        return new DelayImpl(pvRecordStructure);
    }
    
    private static Timer timer = TimerFactory.create("delaySupportTimer",ThreadPriority.middle);
    
    private static class DelayImpl extends AbstractSupport implements ProcessContinueRequester,Timer.TimerCallback
    {
        private static final String supportName = "org.epics.pvioc.delay";
        private Timer.TimerNode timerNode = TimerFactory.createNode(this);
        private PVStructure pvStructure = null;
        private RecordProcess recordProcess = null;
        private PVDouble minAccess = null;
        private PVDouble maxAccess = null;
        private PVDouble incAccess = null;
        private double delay = 0;
        private SupportProcessRequester supportProcessRequester = null;

        private DelayImpl(PVRecordStructure pvRecordStructure) {
            super(supportName,pvRecordStructure);
            pvStructure = pvRecordStructure.getPVStructure();
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.support.Support#initialize(org.epics.pvioc.support.RecordProcess)
         */
        
        /* (non-Javadoc)
         * @see org.epics.pvioc.support.AbstractSupport#initialize(org.epics.pvioc.support.RecordSupport)
         */
        @Override
        public void initialize() {
            if(!super.checkSupportState(SupportState.readyForInitialize,supportName)) return;
            this.recordProcess = super.getPVRecordField().getPVRecord().getRecordProcess();
            minAccess = pvStructure.getDoubleField("min");
            if(minAccess==null) return;
            maxAccess = pvStructure.getDoubleField("max");
            if(maxAccess==null) return;
            incAccess = pvStructure.getDoubleField("inc");
            if(incAccess==null) return;
            setSupportState(SupportState.readyForStart);
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.process.AbstractSupport#uninitialize()
         */
        public void uninitialize() {
            if(super.getSupportState()==SupportState.ready) {
                stop();
            }
            if(super.getSupportState()!=SupportState.readyForStart) return;
            setSupportState(SupportState.readyForInitialize);
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.process.AbstractSupport#start()
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
         * @see org.epics.pvioc.process.AbstractSupport#stop()
         */
        public void stop() {
            if(super.getSupportState()!=SupportState.ready) return;
            timerNode.cancel();
            setSupportState(SupportState.readyForStart);
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.process.AbstractSupport#process(org.epics.pvioc.process.SupportProcessRequester)
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
         * @see org.epics.pvioc.process.ProcessContinueRequester#processContinue()
         */
        public void processContinue() {
            supportProcessRequester.supportProcessDone(RequestResult.success);
        }
        /* (non-Javadoc)
         * @see org.epics.pvdata.misc.Timer.TimerCallback#callback()
         */
        public void callback() {
            recordProcess.processContinue(this);
        }
        /* (non-Javadoc)
         * @see org.epics.pvdata.misc.Timer.TimerCallback#timerStopped()
         */
        public void timerStopped() {
            pvStructure.message("Why was timerStopped called", MessageType.error);
        }
        
    }
}
