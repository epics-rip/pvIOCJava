/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.support.pdrv.serial;

import org.epics.pvdata.pv.MessageType;
import org.epics.pvdata.pv.PVScalar;
import org.epics.pvdata.pv.PVString;
import org.epics.pvdata.pv.Type;
import org.epics.pvioc.database.PVRecordStructure;
import org.epics.pvioc.pdrv.Trace;
import org.epics.pvioc.support.SupportState;
import org.epics.pvioc.support.pdrv.AbstractPortDriverSupport;

/**
 * @author mrk
 *
 */
public class BaseScalarQuery extends AbstractPortDriverSupport
{
    /**
     * Constructor.
     * @param pvRecordStructure The structure being supported.
     * @param supportName The name of the support.
     */
    public BaseScalarQuery(PVRecordStructure pvRecordStructure,String supportName) {
        super(supportName,pvRecordStructure);
    }
    
    private PVString pvPrefix = null;
    private PVScalar pvValueScalar = null;
    private PVString pvResponseString = null;
    /* (non-Javadoc)
     * @see org.epics.pvioc.support.pdrv.AbstractPortDriverSupport#initialize()
     */
    @Override
    public void initialize() {
        super.initialize();
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
     * @see org.epics.pvioc.support.pdrv.AbstractPortDriverSupport#endProcess()
     */
    @Override
    public void endProcess() {
        super.endProcess();
        if((deviceTrace.getMask()&Trace.FLOW)!=0) {
            deviceTrace.print(Trace.FLOW,"pv %s endProcess",fullName);
        }
        String prefix = pvPrefix.get();
        String value = pvResponseString.get();
        if(prefix!=null && prefix.length()>0) {
            if(!value.startsWith(prefix)) {
                if((deviceTrace.getMask()&Trace.ERROR)!=0) {
                    deviceTrace.print(Trace.ERROR,
                        "pv %s support %s prefix does not exist",fullName,supportName);
                }
                return;
            }
            value = value.substring(prefix.length());
        }
        try {
            convert.fromString(pvValueScalar, value);
        } catch (NumberFormatException e) {
            if((deviceTrace.getMask()&Trace.ERROR)!=0) {
                deviceTrace.print(Trace.ERROR,
                    "pv %s support %s conversion error %s",fullName,supportName,e.getMessage());
            }
            return;
        }     
    }
}
