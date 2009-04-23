/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.pdrv.serial;

import org.epics.ioc.install.LocateSupport;
import org.epics.ioc.pdrv.Trace;
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
public class BaseScalarCommand extends AbstractPortDriverSupport
{
    /**
     * Constructor.
     * @param pvStructure The structure being supported.
     * @param supportName The name of the support.
     */
    public BaseScalarCommand(PVStructure pvStructure,String supportName) {
        super(supportName,pvStructure);
    }
    
    private PVString pvPrefix = null;
    private PVScalar pvValueScalar = null;
    private PVString pvCommandString = null;
    private StringBuilder builder = new StringBuilder();
    /* (non-Javadoc)
     * @see org.epics.ioc.support.pdrv.AbstractPortDriverSupport#initialize(org.epics.ioc.support.RecordSupport)
     */
    public void initialize(LocateSupport recordSupport) {
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
            super.uninitialize();
            return;
        }
        pvCommandString = pvStructure.getStringField("command");
        if(pvCommandString==null) {
            super.uninitialize();
            return;
        }
    }      
    
    /* (non-Javadoc)
     * @see org.epics.ioc.support.pdrv.AbstractPortDriverSupport#beginProcess()
     */
    public void beginProcess() {
        if((deviceTrace.getMask()&Trace.FLOW)!=0) {
            deviceTrace.print(Trace.FLOW,"pv %s beginProcess",fullName);
        }
        String prefix = pvPrefix.get();
        builder.setLength(0);
        if(prefix!=null) builder.append(prefix + " ");
        builder.append(convert.getString(pvValueScalar));
        pvCommandString.put(builder.toString());
        pvCommandString.postPut();
        super.beginProcess();
    }
}
