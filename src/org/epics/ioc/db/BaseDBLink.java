/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.db;

import java.util.Iterator;

import org.epics.ioc.pv.*;
import org.epics.ioc.process.*;
import org.epics.ioc.support.Support;

/**
 * Base class for PVLink.
 * @author mrk
 *
 */
public class BaseDBLink extends BaseDBField implements DBLink {
    private static Convert convert = ConvertFactory.getConvert();
    private PVLink pvLink;
    
    /**
     * Constructor for BaseDBLink.
     * @param parent The parent.
     * @param record The DBRecord to which this field belongs.
     * @param pvField The reflection interface.
     */
    public BaseDBLink(DBField parent,DBRecord record, PVField pvField) {
        super(parent,record,pvField);
        this.pvLink = (PVLink)pvField;
    }   
    /* (non-Javadoc)
     * @see org.epics.ioc.db.DBLink#getPVLink()
     */
    public PVLink getPVLink() {
        return (PVLink)super.getPVField();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.db.DBLink#replacePVLink()
     */
    public void replacePVLink() {
        pvLink = (PVLink)super.getPVField();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVLink#getConfigurationStructure()
     */
    public PVStructure getConfigurationStructure() {
        return pvLink.getConfigurationStructure();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVLink#setConfigurationStructure(org.epics.ioc.pv.PVStructure)
     */
    public String setConfigurationStructure(PVStructure configurationStructure) {
        PVStructure configStructure = pvLink.getConfigurationStructure();
        if(configStructure==null) return "link does not have a configurationStructure";
        if(!convert.isCopyStructureCompatible(
        (Structure)configurationStructure.getField(),
        (Structure)configStructure.getField())) {
            return "configurationStructure is not compatible with pvLink.getConfigurationStructure()";
        }
        convert.copyStructure(configurationStructure, configStructure);
        pvLink.setConfigurationStructure(configurationStructure);
        Iterator<RecordListener> iter;
        iter = super.getRecordListenerList().iterator();
        while(iter.hasNext()) {
            RecordListener listener = iter.next();
            DBListener dbListener = listener.getDBListener();
            dbListener.configurationStructurePut(this);
        }
        DBField parent = super.getParent();
        while(parent!=null) {
            iter = parent.getRecordListenerList().iterator();
            while(iter.hasNext()) {
                RecordListener listener = iter.next();
                DBListener dbListener = listener.getDBListener();
                dbListener.configurationStructurePut(parent, this);
            }
            parent = parent.getParent();
        }
        Support support = super.getSupport();
        if(support==null) {
            // Wait until SupportCreation has been run
            return null;
        }
        SupportState supportState = support.getSupportState();
        if(supportState!=SupportState.readyForInitialize) {
            support.uninitialize();
        }
        DBStructure dbStructure = super.getDBRecord().getDBStructure();
        supportState = dbStructure.getSupport().getSupportState();
        switch(supportState) {
        case readyForInitialize:
            break;
        case readyForStart:
            support.initialize();
            break;
        case ready:
            support.initialize();
            if(support.getSupportState()!=SupportState.readyForStart) break;
            support.start();
            break;
        default:
        }
        return null;
    }
}
