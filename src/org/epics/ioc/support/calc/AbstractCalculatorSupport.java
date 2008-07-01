/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.calc;

import org.epics.ioc.db.DBField;
import org.epics.ioc.db.DBStructure;
import org.epics.ioc.pv.Array;
import org.epics.ioc.pv.PVField;
import org.epics.ioc.pv.PVStructure;
import org.epics.ioc.pv.Type;
import org.epics.ioc.support.AbstractSupport;
import org.epics.ioc.support.Support;
import org.epics.ioc.support.SupportState;
import org.epics.ioc.util.MessageType;

/**
 * @author mrk
 *
 */
public abstract class AbstractCalculatorSupport extends AbstractSupport implements CalculatorSupport {
    private String supportName = null;
    private DBStructure dbStructure = null;
    private PVStructure pvStructure;
    private CalcArgArraySupport calcArgArraySupport = null;
    private DBField valueDBField;
    
    

    protected AbstractCalculatorSupport(String supportName,DBStructure dbStructure) {
        super(supportName,dbStructure);
        this.supportName = supportName;
        this.dbStructure = dbStructure;
        pvStructure = dbStructure.getPVStructure();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.process.AbstractSupport#initialize()
     */
    public void initialize() {
        if(!super.checkSupportState(SupportState.readyForInitialize,supportName)) return;
        DBField dbParent = dbStructure.getParent();
        PVField pvParent = dbParent.getPVField();
        PVField pvField = pvParent.findProperty("value");
        if(pvField==null) {
            pvStructure.message("value field not found", MessageType.error);
            return;
        }
        valueDBField = dbStructure.getDBRecord().findDBField(pvField);
        if(getValueType()!=valueDBField.getPVField().getField().getType()) {
            pvStructure.message("value field has illegal type", MessageType.error);
            return;
        }
        setValueDBField(valueDBField);
        pvField = pvParent.findProperty("calcArgArray");
        if(pvField==null) {
            pvStructure.message("calcArgArray field not found", MessageType.error);
            return;
        }
        valueDBField = dbStructure.getDBRecord().findDBField(pvField);
        Support support = valueDBField.getSupport();
        if(!(support instanceof CalcArgArraySupport)) {
            pvStructure.message("calcArgArraySupport not found", MessageType.error);
            return;
        }
        calcArgArraySupport = (CalcArgArraySupport)support;
        ArgType[] argTypes = getArgTypes();
        int num = argTypes.length;
        PVField[] pvFields = new PVField[num];
        for(int i=0; i<num; i++) {
            ArgType argType = argTypes[i];
            pvField = calcArgArraySupport.getPVField(argType.name);
            if(pvField==null) {
                pvStructure.message("field " + argType.name + " not found", MessageType.error);
                return;
            }
            if(pvField.getField().getType()!=argType.type) {
                pvStructure.message("field " + argType.name + " has illegal type", MessageType.error);
                return;
            }
            if(argType.type==Type.pvArray) {
                Array array = (Array)pvField.getField();
                if(array.getElementType()!=argType.elementType) {
                    pvStructure.message("field " + argType.name + " has illegal element type", MessageType.error);
                    return;
                }
            }
            pvFields[i] = pvField;
        }
        setArgPVFields(pvFields);
        setSupportState(SupportState.readyForStart);
    }
}
