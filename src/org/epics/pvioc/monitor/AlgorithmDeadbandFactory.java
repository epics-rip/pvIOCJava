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
import org.epics.pvdata.pv.PVDouble;
import org.epics.pvdata.pv.PVField;
import org.epics.pvdata.pv.PVScalar;
import org.epics.pvdata.pv.PVString;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.Scalar;
import org.epics.pvdata.pv.Type;
import org.epics.pvioc.database.PVRecord;
import org.epics.pvioc.database.PVRecordField;

/**
 * Factory that implements alarm deadband.
 * @author mrk
 *
 */
public class AlgorithmDeadbandFactory {
    private static final String name = "deadband";
    private static final Deadband deadband = new Deadband();
    protected static final PVDataCreate pvDataCreate = PVDataFactory.getPVDataCreate();
    protected static final Convert convert = ConvertFactory.getConvert();

    /**
     * Register the create factory.
     */
    public static void register() {
    	MonitorFactory.registerMonitorAlgorithmCreater(deadband);
    }
    
    private static class Deadband implements MonitorAlgorithmCreate {
        /* (non-Javadoc)
         * @see org.epics.pvioc.channelAccess.MonitorCreate#getName()
         */
        @Override
        public String getAlgorithmName() {
            return name;
        }
		/* (non-Javadoc)
		 * @see org.epics.pvdata.monitor.MonitorAlgorithmCreate#create(org.epics.pvdata.pv.PVRecord, org.epics.pvdata.monitor.MonitorRequester, org.epics.pvdata.pv.PVField, org.epics.pvdata.pv.PVStructure)
		 */
		@Override
		public MonitorAlgorithm create(PVRecord pvRecord,
				MonitorRequester monitorRequester, PVRecordField fromPVRecord,
				PVStructure pvOptions)
		{
			PVField pvf = fromPVRecord.getPVField();
			if(pvf.getField().getType()!=Type.scalar) return null;
			Scalar scalar = (Scalar)pvf.getField();
			if(!scalar.getScalarType().isNumeric()) return null;
			PVStructure pvRecordDeadband = null;
			PVStructure pvParent = pvf.getParent();
			PVField pvField = pvParent.getSubField("deadband");
			if(pvField!=null && (pvField instanceof PVStructure)) pvRecordDeadband = (PVStructure)pvField;
			double optionsDeadband = 0.0;
			boolean optionsIncludesPercent = false;
			boolean optionsIsPercent = false;
			boolean optionsTypeIsDisplay = true;
			try {
				if(pvOptions!=null) {
					pvField = pvOptions.getSubField("deadband");
					if(pvField!=null) {
						PVString pvString = pvOptions.getStringField("deadband");
						optionsDeadband = Double.valueOf(pvString.get());
					}
					pvField = pvOptions.getSubField("isPercent");
					if(pvField!=null) {
						optionsIncludesPercent = true;
						PVString pvString = pvOptions.getStringField("isPercent");
						optionsIsPercent = Boolean.valueOf(pvString.get());
					}
					pvField = pvOptions.getSubField("type");
					if(pvField!=null) {
						PVString pvString = pvOptions.getStringField("type");
						if(pvString.get().equals("archive")) optionsTypeIsDisplay = false;
					}
				}
			} catch (Exception e) {
				monitorRequester.message("illegal options " + e.getMessage(), MessageType.error);
				return null;
			}
			double deadbandRecord = 0.0;
			boolean isPercentRecord = false;
			if(pvRecordDeadband!=null) {
				PVStructure pvStruct = null;
				if(optionsTypeIsDisplay) {
					pvField = pvRecordDeadband.getSubField("display");
				} else {
					pvField = pvRecordDeadband.getSubField("archive");
				}
				if(pvField!=null && (pvField instanceof PVStructure)) pvStruct = (PVStructure)pvField;
				if(pvStruct!=null) {
					pvField = pvStruct.getSubField("isPercent");
					if(pvField!=null && (pvField instanceof PVBoolean)) {
						PVBoolean pvBoolean = (PVBoolean)pvField;
						isPercentRecord = pvBoolean.get();
						
					}
					
					pvField = pvStruct.getSubField("value");
					if(pvField!=null && (pvField instanceof PVDouble)) {
						PVDouble pvDouble = (PVDouble)pvField;
						deadbandRecord = pvDouble.get();
					}
				}
			}
			double deadband = 0.0;
			boolean isPercent = false;
			if(optionsIncludesPercent && (optionsIsPercent!=isPercentRecord)) {
				deadband = optionsDeadband;
				isPercent = optionsIsPercent;
			} else {
				deadband = (optionsDeadband<deadbandRecord) ? deadbandRecord : optionsDeadband;
				isPercent = isPercentRecord;
			}
			if(deadband<=0.0) return null;
			return new MonitorAlgorithmImpl((PVScalar)fromPVRecord.getPVField(),deadband,isPercent);
		}
    }
    
    
    private static class MonitorAlgorithmImpl implements MonitorAlgorithm {
    	
    	
        private MonitorAlgorithmImpl(PVScalar pvFromRecord,double deadband,boolean isPercent)
        {
            this.pvFromRecord = pvFromRecord;
            this.deadband = deadband;
            this.isPercent = isPercent;
            prevValue = convert.toDouble(pvFromRecord);
        }
        
        private final PVScalar pvFromRecord;
        private final double deadband; 
        private final boolean isPercent;
        private double prevValue;
        private double currentValue;
		/* (non-Javadoc)
		 * @see org.epics.pvdata.monitor.MonitorAlgorithm#causeMonitor()
		 */
		@Override
		public boolean causeMonitor() {
			currentValue = convert.toDouble(pvFromRecord);
			double diff = Math.abs(currentValue-prevValue);
			if(isPercent) {
                if(currentValue!=0.0) {
                	return ((100.0*diff/Math.abs(currentValue))<deadband) ? false : true;
                } else {
                	return (prevValue==0.0) ? false : true;
                }
			} else {
				if(diff<=deadband) return false;
				return true;
			}
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
		public void monitorIssued() {
			prevValue = currentValue;
		}
    }
}
