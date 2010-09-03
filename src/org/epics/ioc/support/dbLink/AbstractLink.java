/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.dbLink;

import org.epics.ioc.database.PVDatabase;
import org.epics.ioc.database.PVDatabaseFactory;
import org.epics.ioc.database.PVRecord;
import org.epics.ioc.database.PVRecordField;
import org.epics.ioc.install.AfterStart;
import org.epics.ioc.support.AbstractSupport;
import org.epics.ioc.support.RecordProcess;
import org.epics.ioc.support.SupportState;
import org.epics.ioc.support.alarm.AlarmSupport;
import org.epics.ioc.support.alarm.AlarmSupportFactory;
import org.epics.pvData.factory.ConvertFactory;
import org.epics.pvData.property.TimeStamp;
import org.epics.pvData.property.TimeStampFactory;
import org.epics.pvData.pv.Convert;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVField;
import org.epics.pvData.pv.PVString;
import org.epics.pvData.pv.PVStructure;
import org.epics.pvData.pv.Type;

/**
 * Abstract Support for database Links.
 * This is nopt public since it is for use by this package.
 * @author mrk
 *
 */
abstract class AbstractLink extends AbstractSupport {
    /**
     * The convert implementation.
     */
    protected static final Convert convert = ConvertFactory.getConvert();
    /**
     * The pvRecordField supported.
     */
    protected final PVRecordField pvRecordField;
    /**
     * The interface for getting the pvName.
     */
    protected PVString pvnamePV;
    /**
     * The pvStructure that this link supports.
     */
    protected PVStructure pvDatabaseLink;
    /**
     * The pvRecord for pvStructure.
     */
    protected PVRecord pvRecord;
    /**
     * The timeStamp for pvRecord.
     */
    protected TimeStamp timeStamp = null;
    /**
     * The recordProcess for this record.
     */
    protected RecordProcess recordProcess = null;
    /**
     * The alarmSupport.
     */
    protected AlarmSupport alarmSupport = null;
    /**
     * The record for pvname
     */
    protected PVRecord linkPVRecord = null;
    /**
     * The record process for linkPVRecord.
     */
    protected RecordProcess linkRecordProcess = null;
    /**
     * The locateSupport for the linjkPVRecord.
     */
    
    /**
     * Constructor.
     * @param supportName The support name.
     * @param pvRecordField The field which is supported.
     */
    public AbstractLink(String supportName,PVRecordField pvRecordField) {
        super(supportName,pvRecordField);
        this.pvRecordField = pvRecordField;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.support.AbstractSupport#initialize()
     */
    @Override
    public void initialize() {
        if(!super.checkSupportState(SupportState.readyForInitialize,null)) return;
        
        pvnamePV = (PVString)pvRecordField.getPVField();
        pvDatabaseLink = pvnamePV.getParent();
        pvRecord = pvRecordField.getPVRecord();
        PVField pvField = pvRecord.getPVRecordStructure().getPVStructure().getSubField("timeStamp");
        if(pvField!=null && pvField.getField().getType()==Type.structure) {
            timeStamp = TimeStampFactory.getTimeStamp((PVStructure)pvField);
        }
        super.initialize();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.support.AbstractSupport#start()
     */
    @Override
    public void start(AfterStart afterStart) {
        if(!super.checkSupportState(SupportState.readyForStart,null)) return;
        recordProcess = pvRecord.getRecordProcess();
        if(recordProcess==null) {
            super.message("no recordProcess", MessageType.error);
            return;
        }
        alarmSupport = AlarmSupportFactory.findAlarmSupport(pvRecord.findPVRecordField(pvDatabaseLink));
        if(alarmSupport==null) {
            super.message("no alarmSupport", MessageType.error);
            return;
        }
        String name = pvnamePV.get();
        int ind = name.indexOf(".");
        if(ind>=0) name = name.substring(0,ind);
        PVDatabase pvDatabase = PVDatabaseFactory.getMaster();
        linkPVRecord = pvDatabase.findRecord(name);
        if(linkPVRecord==null) {
        	pvDatabase = PVDatabaseFactory.getBeingInstalled();
            linkPVRecord = pvDatabase.findRecord(name);
        }
        if(linkPVRecord==null) {
            super.message("pvname not found", MessageType.error);
            return;
        }
        linkRecordProcess = linkPVRecord.getRecordProcess();
        super.start(afterStart);
    }
}
