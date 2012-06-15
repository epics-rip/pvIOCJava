/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.support.dbLink;

import org.epics.pvdata.factory.ConvertFactory;
import org.epics.pvdata.property.PVTimeStamp;
import org.epics.pvdata.property.PVTimeStampFactory;
import org.epics.pvdata.pv.Convert;
import org.epics.pvdata.pv.MessageType;
import org.epics.pvdata.pv.PVField;
import org.epics.pvdata.pv.PVString;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.Type;
import org.epics.pvioc.database.PVDatabase;
import org.epics.pvioc.database.PVDatabaseFactory;
import org.epics.pvioc.database.PVRecord;
import org.epics.pvioc.database.PVRecordField;
import org.epics.pvioc.install.AfterStart;
import org.epics.pvioc.support.AbstractSupport;
import org.epics.pvioc.support.RecordProcess;
import org.epics.pvioc.support.SupportState;
import org.epics.pvioc.support.alarm.AlarmSupport;
import org.epics.pvioc.support.alarm.AlarmSupportFactory;

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
     * A PVTimeStamp.
     */
    protected PVTimeStamp pvTimeStamp = PVTimeStampFactory.create();
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
     * @see org.epics.pvioc.support.AbstractSupport#initialize()
     */
    @Override
    public void initialize() {
        if(!super.checkSupportState(SupportState.readyForInitialize,null)) return;
        
        pvnamePV = (PVString)pvRecordField.getPVField();
        pvDatabaseLink = pvnamePV.getParent();
        pvRecord = pvRecordField.getPVRecord();
        PVField pvField = pvRecord.getPVRecordStructure().getPVStructure().getSubField("timeStamp");
        if(pvField!=null && pvField.getField().getType()==Type.structure) {
            pvTimeStamp.attach(pvField);
        }
        super.initialize();
    }
    /* (non-Javadoc)
     * @see org.epics.pvioc.support.AbstractSupport#start()
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
    /* (non-Javadoc)
     * @see org.epics.pvioc.support.AbstractSupport#message(java.lang.String, org.epics.pvdata.pv.MessageType)
     */
    @Override
    public void message(String message,MessageType messageType) {
        pvRecord.lock();
        try {
            pvDatabaseLink.message(pvRecordField.getFullName() + " " + message, messageType);
        } finally {
            pvRecord.unlock();
        }
    }

}
