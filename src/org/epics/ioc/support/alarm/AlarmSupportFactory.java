/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.alarm;

import org.epics.ioc.database.PVRecordField;
import org.epics.ioc.database.PVRecordStructure;
import org.epics.ioc.support.RecordProcess;
import org.epics.ioc.support.Support;
import org.epics.ioc.support.basic.GenericBase;
import org.epics.pvData.property.Alarm;
import org.epics.pvData.property.AlarmSeverity;
import org.epics.pvData.property.AlarmStatus;
import org.epics.pvData.property.PVAlarm;
import org.epics.pvData.property.PVAlarmFactory;
import org.epics.pvData.pv.Field;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVField;
import org.epics.pvData.pv.PVInt;
import org.epics.pvData.pv.PVString;
import org.epics.pvData.pv.PVStructure;
import org.epics.pvData.pv.Type;

/**
 * Support for alarm field.
 * 
 * @author mrk
 * 
 */
public class AlarmSupportFactory {

	private static final String alarmSupportName = "org.epics.ioc.alarm";

	/**
	 * Create support for an alarm field.
	 * 
	 * @param pvRecordStructure The interface to the alarm field.
	 * @return The support or null if the alarm field is improperly defined.
	 */
	public static Support create(PVRecordStructure pvRecordStructure) {
		AlarmSupportImpl impl = new AlarmSupportImpl(pvRecordStructure);
		if (impl.isAlarmSupport())
			return impl;
		return null;
	}

	/**
	 * If pvField has AlarmSupport return it.
	 * 
	 * @param pvRecordField The field.
	 * @return The AlarmSupport or null if not found.
	 */
	public static AlarmSupport getAlarmSupport(PVRecordField pvRecordField) {
		Support support = pvRecordField.getSupport();
		if (support != null && (support instanceof AlarmSupportImpl)) {
			return (AlarmSupport) support;
		}
		return null;
	}
	/**
	 * Find alarm support. Look first in startPVField if it is a structure. If
	 * not found look up the parent tree.
	 * 
	 * @param startPVRecordField
	 *            The starting field.
	 * @return The AlarmSupport or null if not found.
	 */
	public static AlarmSupport findAlarmSupport(PVRecordField startPVRecordField) {
		if (startPVRecordField == null)
			return null;
		PVRecordStructure parentPVRecordStructure;
		if (startPVRecordField instanceof PVRecordStructure) {
			parentPVRecordStructure = (PVRecordStructure) startPVRecordField;
		} else {
			parentPVRecordStructure = startPVRecordField.getParent();
		}
		while (parentPVRecordStructure != null) {
			PVRecordField[] pvRecordFields = parentPVRecordStructure
					.getPVRecordFields();
			PVField[] pvFields = parentPVRecordStructure.getPVStructure()
					.getPVFields();
			for (int i = 0; i < pvFields.length; i++) {
				PVField pvField = pvFields[i];
				Field field = pvField.getField();
				Type type = field.getType();
				if (type == Type.structure) {
					if (field.getFieldName().equals("alarm")) {
						Support support = pvRecordFields[i].getSupport();
						if (support != null
								&& (support instanceof AlarmSupportImpl)) {
							return (AlarmSupport) support;
						}
					}
				}
			}
			parentPVRecordStructure = parentPVRecordStructure.getParent();
		}
		return null;
	}

	private static class AlarmSupportImpl extends GenericBase implements AlarmSupport {
		private PVRecordStructure pvRecordStructureAlarm = null;
		private RecordProcess recordProcess = null;

		private PVInt pvSeverity = null;
		private PVInt pvStatus = null;
		private PVString pvMessage = null;

		private boolean gotAlarm;
		private boolean active = false;
		private int beginSeverity = 0;
		private int currentSeverity = 0;
		private int beginStatus = 0;
		private int currentStatus = 0;
		private String beginMessage = null;
		private String currentMessage = null;
		private Alarm alarm = new Alarm();
		private PVAlarm pvAlarm = PVAlarmFactory.create();

		private AlarmSupportImpl parentAlarmSupport = null;

		private AlarmSupportImpl(PVRecordStructure pvAlarm) {
			super(alarmSupportName,pvAlarm);
			this.pvRecordStructureAlarm = pvAlarm;
			pvRecordStructureAlarm = (PVRecordStructure)pvAlarm;
		}

		private boolean isAlarmSupport() {
			if(!pvAlarm.attach(pvRecordStructureAlarm.getPVStructure())) {
			    pvRecordStructureAlarm.message("field is not alarm",MessageType.error);
                return false;
			}
			PVStructure pvStruct = pvRecordStructureAlarm.getPVStructure();
			pvSeverity = pvStruct.getIntField("severity");
			pvStatus = pvStruct.getIntField("status");
			pvMessage = pvStruct.getStringField("message");
			return true;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.epics.ioc.support.AbstractSupport#initialize(org.epics.ioc.support
		 * .RecordSupport)
		 */
		@Override
		public void initialize() {
			if(!isAlarmSupport()) return;
			// look for parent starting with parent of parent
			AlarmSupport parent = AlarmSupportFactory.findAlarmSupport(pvRecordStructureAlarm.getParent().getParent());
			if (parent != null) {
				parentAlarmSupport = (AlarmSupportImpl) parent;
			}
			recordProcess = pvRecordStructureAlarm.getPVRecord().getRecordProcess();
			super.initialize();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.epics.ioc.process.AbstractSupport#uninitialize()
		 */
		public void uninitialize() {
			parentAlarmSupport = null;
			super.uninitialize();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.epics.ioc.support.AlarmSupport#beginProcess()
		 */
		public void beginProcess() {
			if (active)
				return;
			gotAlarm = false;
			active = true;
			beginSeverity = pvSeverity.get();
			beginStatus = pvStatus.get();
			beginMessage = pvMessage.get();
			currentMessage = beginMessage;
			currentSeverity = 0;
			currentStatus = 0;
			currentMessage = null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.epics.ioc.support.AlarmSupport#endProcess()
		 */
		public void endProcess() {
			active = false;
			boolean messageChange = false;
			if (beginMessage == null) {
				if (currentMessage != null)
					messageChange = true;
			} else {
				if (currentMessage == null) {
					messageChange = true;
				} else if (!beginMessage.equals(currentMessage)) {
					messageChange = true;
				}
			}
			if (currentSeverity != beginSeverity || currentStatus != beginStatus || messageChange) {
				pvSeverity.put(currentSeverity);
				pvStatus.put(currentStatus);
				pvMessage.put(currentMessage);
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.epics.ioc.support.AlarmSupport#setStatusSeverity(java.lang.String
		 * , org.epics.ioc.util.AlarmSeverity)
		 */
		public boolean setAlarm(String message, AlarmSeverity severity,AlarmStatus status) {
			int newSeverity = severity.ordinal();
			int newStatus = status.ordinal();
			if (!active) {
				if (recordProcess.isActive()) {
					beginProcess();
				} else { // record is not being processed
					if (newSeverity > 0) { // raise alarm
						pvSeverity.put(newSeverity);
						pvMessage.put(message);
						return true;
					} else { // no alarm just return false
						return false;
					}
				}
			}
			if (!gotAlarm || newSeverity > currentSeverity) {
				currentSeverity = newSeverity;
				currentMessage = message;
				currentStatus = newStatus;
				gotAlarm = true;
				if (parentAlarmSupport != null) {
					parentAlarmSupport.setAlarm(message, severity,status);
				}
				return true;
			}
			return false;
		}
		/* (non-Javadoc)
		 * @see org.epics.ioc.support.alarm.AlarmSupport#getAlarm(org.epics.pvData.property.Alarm)
		 */
		@Override
		public void getAlarm(Alarm alarm) {
			alarm.setMessage(this.alarm.getMessage());
			alarm.setSeverity(this.alarm.getSeverity());
			alarm.setStatus(this.alarm.getStatus());
		}
	}
}
