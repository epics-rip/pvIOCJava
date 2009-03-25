/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.pdrv.serial;

import org.epics.ioc.pdrv.Trace;
import org.epics.ioc.support.RecordSupport;
import org.epics.ioc.support.SupportState;
import org.epics.ioc.support.pdrv.AbstractPortDriverSupport;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVScalar;
import org.epics.pvData.pv.PVString;
import org.epics.pvData.pv.PVStructure;
import org.epics.pvData.pv.Type;

/**
 * @author mrk
 *
 */
public class BaseScalarQuery extends AbstractPortDriverSupport
{
    /**
     * Constructor.
     * @param pvStructure The structure being supported.
     * @param supportName The name of the support.
     */
    public BaseScalarQuery(PVStructure pvStructure,String supportName) {
        super(supportName,pvStructure);
    }
    
    private PVString pvPrefix = null;
    private PVScalar pvValueScalar = null;
    private PVString pvResponseString = null;
    /* (non-Javadoc)
     * @see org.epics.ioc.support.pdrv.AbstractPortDriverSupport#initialize(org.epics.ioc.support.RecordSupport)
     */
    public void initialize(RecordSupport recordSupport) {
        super.initialize(recordSupport);
        if(!super.checkSupportState(SupportState.readyForStart,supportName)) return;
        if(valuePVField.getField().getType()!=Type.scalar) {
            pvStructure.message("value field is not a supported type", MessageType.fatalError);
            super.uninitialize();
            return;
        }
        
        pvValueScalar = (PVScalar)valuePVField;
        pvPrefix = pvStructure.getStringField("prefix");
        if(pvPrefix==null) {
            pvStructure.message("prefix field not found", MessageType.error);
            super.uninitialize();
            return;
        }
        pvResponseString = pvStructure.getStringField("response");
        if(pvResponseString==null) {
            pvStructure.message("response field not found", MessageType.error);
            super.uninitialize();
            return;
        }
    }      
    
    /* (non-Javadoc)
     * @see org.epics.ioc.support.pdrv.AbstractPortDriverSupport#endProcess()
     */
    public void endProcess() {
        super.endProcess();
        deviceTrace.print(Trace.FLOW,
                "%s:%s endProcess",fullName,supportName);
        String prefix = pvPrefix.get();
        String value = pvResponseString.get();
        if(prefix!=null && prefix.length()>0) {
            if(!value.startsWith(prefix)) {
                deviceTrace.print(Trace.ERROR,
                        "%s:%s prefix does not exist",fullName,supportName);
                return;
            }
            value = value.substring(prefix.length());
        }
        try {
            convert.fromString(pvValueScalar, value);
        } catch (NumberFormatException e) {
            deviceTrace.print(Trace.ERROR,
                    "%s:%s conversion error %s",fullName,supportName,e.getMessage());
            return;
        }     
    }
}
