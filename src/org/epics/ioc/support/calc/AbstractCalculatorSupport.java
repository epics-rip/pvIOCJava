/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.calc;

import java.util.List;

import org.epics.pvData.pv.*;
import org.epics.pvData.misc.*;
import org.epics.pvData.factory.*;
import org.epics.pvData.property.*;
import org.epics.ioc.support.*;
import org.epics.ioc.support.alarm.*;

import org.epics.ioc.util.*;


import org.epics.ioc.ca.*;

/**
 * @author mrk
 *
 */
public abstract class AbstractCalculatorSupport extends AbstractSupport {
    private static PVProperty pvProperty = PVPropertyFactory.getPVProperty(); 
    private String supportName = null;
    private PVStructure pvStructure = null;
    
    

    /**
     * Constructor.
     * @param supportName The supportName.
     * @param pvStructure The structure being supported.
     */
    protected AbstractCalculatorSupport(String supportName,PVStructure pvStructure) {
        super(supportName,pvStructure);
        this.supportName = supportName;
        this.pvStructure = pvStructure;
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
     * @see org.epics.ioc.support.AbstractSupport#initialize(org.epics.ioc.support.RecordSupport)
     */
    public void initialize(RecordSupport recordSupport) {
        if(!super.checkSupportState(SupportState.readyForInitialize,supportName)) return;
        PVStructure pvParent = pvStructure.getParent();
        PVField valuePVField = pvProperty.findProperty(pvParent,"value");
        // if not found try partent of parent
        if(valuePVField==null) {
            PVStructure pvTemp = pvParent.getParent();
            if(pvTemp!=null) valuePVField = pvProperty.findProperty(pvTemp,"value");
        }
        if(valuePVField==null) {
            pvStructure.message("value field not found", MessageType.error);
            return;
        }
        if(getValueType()!=valuePVField.getField().getType()) {
            pvStructure.message("value field has illegal type", MessageType.error);
            return;
        }
        setValuePVField(valuePVField);
        PVField pvField = pvProperty.findProperty(pvParent,"calcArgArray");
        if(pvField==null) {
            pvStructure.message("calcArgArray field not found", MessageType.error);
            return;
        }
        Support support = recordSupport.getSupport(pvField);
        if(!(support instanceof CalcArgs)) {
            pvStructure.message("calcArgArraySupport not found", MessageType.error);
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
                pvStructure.message("field " + argType.name + " not found", MessageType.error);
                return;
            }
            if(pvField.getField().getType()!=argType.type) {
                pvStructure.message("field " + argType.name + " has illegal type", MessageType.error);
                return;
            }
            if(argType.type==Type.scalarArray) {
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
