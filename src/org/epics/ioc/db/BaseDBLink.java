/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.db;

import java.util.Iterator;

import org.epics.ioc.dbd.*;
import org.epics.ioc.pv.*;
import org.epics.ioc.process.*;

/**
 * Base class for PVLink.
 * @author mrk
 *
 */
public class BaseDBLink extends BaseDBData implements DBLink {
    private static Convert convert = ConvertFactory.getConvert();
    private static PVDataCreate pvDataCreate = PVDataFactory.getPVDataCreate();
    private PVLink pvLink;
    
    public BaseDBLink(DBData parent,DBRecord record, PVData pvData) {
        super(parent,record,pvData);
        this.pvLink = (PVLink)pvData;
    }   
    /* (non-Javadoc)
     * @see org.epics.ioc.db.DBLink#newSupport(org.epics.ioc.dbd.DBDLinkSupport)
     */
    public String newSupport(DBDLinkSupport linkSupport,DBD dbd) {
        String configurationStructureName = linkSupport.getConfigurationStructureName();
        DBDStructure dbdStructure = dbd.getStructure(configurationStructureName);
        if(dbdStructure==null) {
            return "configurationStructure " + configurationStructureName
                + " for support " + pvLink.getSupportName()
                + " does not exist";
        }
        FieldCreate fieldCreate = FieldFactory.getFieldCreate();
        Field[] fields = dbdStructure.getFields();
        Structure structure = fieldCreate.createStructure(
            "configuration",
            dbdStructure.getStructureName(),
            fields);
        PVStructure configStructure = (PVStructure)pvDataCreate.createData(pvLink, structure);
        pvLink.setConfigurationStructure(configStructure);
        return null;
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
    public boolean setConfigurationStructure(PVStructure pvStructure) {
        PVStructure configStructure = pvLink.getConfigurationStructure();
        if(configStructure==null) return false;
        if(!convert.isCopyStructureCompatible(
        (Structure)pvStructure.getField(),
        (Structure)configStructure.getField())) {
            return false;
        }
        convert.copyStructure(pvStructure, configStructure);
        pvLink.setConfigurationStructure(pvStructure);
        Support support = super.getSupport();
        if(support!=null) {
            SupportState supportState = support.getSupportState();
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
        }
        Iterator<RecordListener> iter;
        iter = super.getRecordListenerList().iterator();
        while(iter.hasNext()) {
            RecordListener listener = iter.next();
            DBListener dbListener = listener.getDBListener();
            dbListener.configurationStructurePut(this);
        }
        DBData parent = super.getParent();
        while(parent!=null) {
            iter = parent.getRecordListenerList().iterator();
            while(iter.hasNext()) {
                RecordListener listener = iter.next();
                DBListener dbListener = listener.getDBListener();
                dbListener.configurationStructurePut(parent, this);
            }
            parent = parent.getParent();
        }
        return true;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.db.DBLink#getPVLink()
     */
    public PVLink getPVLink() {
        return (PVLink)super.getPVData();
    }

}
