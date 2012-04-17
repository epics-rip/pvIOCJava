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
public class BaseScalarCommand extends AbstractPortDriverSupport
{
    /**
     * Constructor.
     * @param pvRecordStructure The structure being supported.
     * @param supportName The name of the support.
     */
    public BaseScalarCommand(PVRecordStructure pvRecordStructure,String supportName) {
        super(supportName,pvRecordStructure);
    }
    
    private PVString pvPrefix = null;
    private PVScalar pvValueScalar = null;
    private PVString pvCommandString = null;
    private StringBuilder builder = new StringBuilder();
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
     * @see org.epics.pvioc.support.pdrv.AbstractPortDriverSupport#beginProcess()
     */
    @Override
    public void beginProcess() {
        if((deviceTrace.getMask()&Trace.FLOW)!=0) {
            deviceTrace.print(Trace.FLOW,"pv %s beginProcess",fullName);
        }
        String prefix = pvPrefix.get();
        builder.setLength(0);
        if(prefix!=null) builder.append(prefix + " ");
        builder.append(convert.toString(pvValueScalar));
        pvCommandString.put(builder.toString());
        super.beginProcess();
    }
}
