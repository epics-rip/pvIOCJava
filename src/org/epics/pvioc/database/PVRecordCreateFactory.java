/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.database;

import org.epics.pvdata.pv.PVStructure;

/**
 * @author mrk
 *
 */
public class PVRecordCreateFactory{
	private PVRecordCreateFactory() {} // don't create

	public static synchronized PVRecordCreate getPVRecordCreate() {
		return PVRecordCreateImpl.getPVRecordCreate();
	}

	private static final class PVRecordCreateImpl implements PVRecordCreate {
		private static PVRecordCreateImpl singleImplementation = null;
		private static synchronized PVRecordCreateImpl getPVRecordCreate() {
			if (singleImplementation==null) {
				singleImplementation = new PVRecordCreateImpl();
			}
			return singleImplementation;
		}
		/* (non-Javadoc)
		 * @see org.epics.pvioc.database.PVRecordCreate#createPVRecord(java.lang.String, org.epics.pvdata.pv.PVStructure)
		 */
		@Override
		public PVRecord createPVRecord(String recordName,PVStructure pvStructure) {
			return new BasePVRecord(recordName,pvStructure);
		}
	}
}
