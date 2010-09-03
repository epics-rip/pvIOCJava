/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.calc;

import org.epics.ioc.database.PVRecordStructure;
import org.epics.ioc.support.AbstractSupport;
import org.epics.ioc.support.Support;
import org.epics.ioc.support.SupportState;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVField;
import org.epics.pvData.pv.PVStructure;
import org.epics.pvData.pv.ScalarArray;
import org.epics.pvData.pv.Type;

/**
 * @author mrk
 *
 */
public abstract class AbstractCalculatorSupport extends AbstractSupport {
    private final String supportName;
    private final PVRecordStructure pvRecordStructure;
    
    

    /**
     * Constructor.
     * @param supportName The supportName.
     * @param pvRecordStructure The structure being supported.
     */
    protected AbstractCalculatorSupport(String supportName,PVRecordStructure pvRecordStructure) {
        super(supportName,pvRecordStructure);
        this.supportName = supportName;
        this.pvRecordStructure = pvRecordStructure;
    }
    
    /**
     * Get The ArgType[] required by the derived class.
     * @return The argType[].
     */
    abstract protected ArgType[] getArgTypes();
    /**
     * Get the value type required by the derived class.
     * @return The type.
     */
    abstract protected Type getValueType();
    /**
     * Called by AbstractCalculatorSupport to give the derived class the PVField[] for the arguments.
     * @param pvArgs The PVField[] for the arguments.
     */
    abstract protected void setArgPVFields(PVField[] pvArgs);
    /**
     * Called by AbstractCalculatorSupport to give the derived class the PVField for the value.
     * @param pvValue The PVField for the value.
     */
    abstract protected void setValuePVField(PVField pvValue);
    /* (non-Javadoc)
     * @see org.epics.ioc.support.AbstractSupport#initialize()
     */
    public void initialize() {
        if(!super.checkSupportState(SupportState.readyForInitialize,supportName)) return;
        PVStructure pvParent = pvRecordStructure.getPVStructure().getParent();
        PVField valuePVField = pvParent.getSubField("value");
        // if not found try parent of parent
        if(valuePVField==null) {
            PVStructure pvTemp = pvParent.getParent();
            if(pvTemp!=null) valuePVField = pvTemp.getSubField("value");
        }
        if(valuePVField==null) {
            pvRecordStructure.message("value field not found", MessageType.error);
            return;
        }
        if(getValueType()!=valuePVField.getField().getType()) {
            pvRecordStructure.message("value field has illegal type", MessageType.error);
            return;
        }
        setValuePVField(valuePVField);
        PVField pvField = pvParent.getSubField("calcArgs");
        if(pvField==null) {
            setArgPVFields(null);;
        } else {
        	Support support = pvRecordStructure.getPVRecord().findPVRecordField(pvField).getSupport();
            if(!(support instanceof CalcArgs)) {
                pvRecordStructure.message("calcArgArraySupport not found", MessageType.error);
                return;
            }
            CalcArgs calcArgArraySupport = (CalcArgs)support;
            ArgType[] argTypes = getArgTypes();
            int num = argTypes.length;
            PVField[] pvFields = new PVField[num];
            for(int i=0; i<num; i++) {
                ArgType argType = argTypes[i];
                pvField = calcArgArraySupport.getPVField(argType.name);
                if(pvField==null) {
                    pvRecordStructure.message("field " + argType.name + " not found", MessageType.error);
                    return;
                }
                if(pvField.getField().getType()!=argType.type) {
                    pvRecordStructure.message("field " + argType.name + " has illegal type", MessageType.error);
                    return;
                }
                if(argType.type==Type.scalarArray) {
                    ScalarArray array = (ScalarArray)pvField.getField();
                    if(array.getElementType()!=argType.elementType) {
                        pvRecordStructure.message("field " + argType.name + " has illegal element type", MessageType.error);
                        return;
                    }
                }
                pvFields[i] = pvField;
            }
            setArgPVFields(pvFields);
        }
        setSupportState(SupportState.readyForStart);
    }
}
