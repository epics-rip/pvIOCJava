/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS pvData is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.monitor;


import org.epics.pvdata.factory.ConvertFactory;
import org.epics.pvdata.factory.PVDataFactory;
import org.epics.pvdata.monitor.MonitorAlgorithm;
import org.epics.pvdata.monitor.MonitorRequester;
import org.epics.pvdata.pv.Convert;
import org.epics.pvdata.pv.MessageType;
import org.epics.pvdata.pv.PVBoolean;
import org.epics.pvdata.pv.PVDataCreate;
import org.epics.pvdata.pv.PVField;
import org.epics.pvdata.pv.PVString;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvioc.database.PVRecord;
import org.epics.pvioc.database.PVRecordField;

/**
 * Factory that implements alarm onChange
 * @author mrk
 *
 */
public class AlgorithmOnChangeFactory {
    private static final String name = "onChange";
    private static final OnChange onChange = new OnChange();
    protected static final PVDataCreate pvDataCreate = PVDataFactory.getPVDataCreate();
    protected static final Convert convert = ConvertFactory.getConvert();

    /**
     * Register the create factory.
     */
    public static void register() {
    	MonitorFactory.registerMonitorAlgorithmCreater(onChange);
    }
    
    private static class OnChange implements MonitorAlgorithmCreate {
        /* (non-Javadoc)
         * @see org.epics.pvioc.channelAccess.MonitorCreate#getName()
         */
        @Override
        public String getAlgorithmName() {
            return name;
        }
		@Override
		public MonitorAlgorithm create(PVRecord pvRecord,
				MonitorRequester monitorRequester, PVRecordField fromPVRecord,
				PVStructure pvOptions)
		{
			if(pvOptions==null) {
				monitorRequester.message("no monitor options", MessageType.error);
				return null;
			}
			boolean causeMonitor = true;
			PVField pvField = pvOptions.getSubField("causeMonitor");
			if(pvField!=null) {
				if(pvField instanceof PVString) {
					PVString pvString = (PVString)pvField;
					if(pvString.get().equals("false")) causeMonitor = false;
				} else if(pvField instanceof PVBoolean) {
					PVBoolean pvBoolean = (PVBoolean)pvField;
					causeMonitor = pvBoolean.get();
				}
			}
			return new MonitorAlgorithmImpl(fromPVRecord.getPVField(),causeMonitor);
		}
    }
    
    
    private static class MonitorAlgorithmImpl implements MonitorAlgorithm {
        private MonitorAlgorithmImpl(PVField pvFromRecord,boolean causeMonitor)
        {
            this.pvFromRecord = pvFromRecord;
            this.causeMonitor = causeMonitor;
            if(causeMonitor) {
            	pvCopy = pvDataCreate.createPVField(pvFromRecord);
            } else {
            	pvCopy = null;
            }
        }
        
        private final PVField pvFromRecord;
        private final boolean causeMonitor;
        private final PVField pvCopy;
		/* (non-Javadoc)
		 * @see org.epics.pvdata.monitor.MonitorAlgorithm#causeMonitor()
		 */
		@Override
		public boolean causeMonitor() {
			if(!causeMonitor) return false;
			if(pvFromRecord.equals(pvCopy))return false;
			convert.copy(pvFromRecord, pvCopy);
			return true;
		}
		/* (non-Javadoc)
		 * @see org.epics.pvdata.monitor.MonitorAlgorithm#getAlgorithmName()
		 */
		@Override
		public String getAlgorithmName() {
			return name;
		}
		/* (non-Javadoc)
		 * @see org.epics.pvdata.monitor.MonitorAlgorithm#monitorIssued()
		 */
		@Override
		public void monitorIssued() {}
        
    }
}
