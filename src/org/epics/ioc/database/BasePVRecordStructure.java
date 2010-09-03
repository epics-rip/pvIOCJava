/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.database;

import org.epics.pvData.pv.PVField;
import org.epics.pvData.pv.PVStructure;
import org.epics.pvData.pv.Type;

/**
 * @author mrk
 *
 */

public class BasePVRecordStructure extends BasePVRecordField implements PVRecordStructure {
	private BasePVRecordField[] pvRecordFields;
	
	BasePVRecordStructure(PVStructure pvStructure,PVRecordStructure parent,BasePVRecord pvRecord) {
		super(pvStructure,parent,pvRecord);
		PVField[] pvFields = pvStructure.getPVFields();
	    pvRecordFields = new BasePVRecordField[pvFields.length];
	    for(int i=0; i<pvFields.length; i++) {
	    	PVField pvField = pvFields[i];
	    	if(pvField.getField().getType()==Type.structure) {
	    		pvRecordFields[i]  = new BasePVRecordStructure((PVStructure)pvField,this,pvRecord);
	    	} else {
	    		pvRecordFields[i] = new BasePVRecordField(pvFields[i],this,pvRecord);
	    	}
	    }
	}
	/* (non-Javadoc)
	 * @see org.epics.ioc.database.PVRecordStructure#getPVRecordFields()
	 */
	@Override
	public PVRecordField[] getPVRecordFields() {
		return pvRecordFields;
	}
	/* (non-Javadoc)
	 * @see org.epics.ioc.database.PVRecordStructure#getPVStructure()
	 */
	@Override
	public PVStructure getPVStructure() {
		return (PVStructure)super.getPVField();
	}
}
