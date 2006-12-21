/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.process;

import org.epics.ioc.db.*;
import org.epics.ioc.pv.*;
import org.epics.ioc.util.*;

/**
 * Abstract base class for support code.
 * All support code should extend this class.
 * All methods must be called with the record locked.
 * @author mrk
 *
 */
public abstract class AbstractSupport implements Support {
        
    private String name;
    private DBData dbData;
    private SupportState supportState = SupportState.readyForInitialize;
    
    /**
     * Constructor.
     * This must be called by any class that extends AbstractSupport.
     * @param name The support name.
     * @param dbData The DBdata which is supported.
     * This can be a record or any field in a record.
     */
    protected AbstractSupport(String name,DBData dbData) {
        this.name = name;
        this.dbData = dbData;
    } 
    
    /* (non-Javadoc)
     * @see org.epics.ioc.process.Support#getName()
     */
    public String getRequestorName() {
        return name;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.util.Requestor#message(java.lang.String, org.epics.ioc.util.MessageType)
     */
    public void message(String message, MessageType messageType) {
        dbData.message(message, messageType);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.process.Support#getSupportState()
     */
    public SupportState getSupportState() {
        return supportState;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.process.Support#getDBData()
     */
    public DBData getDBData() {
        return dbData;
    } 
    /* (non-Javadoc)
     * @see org.epics.ioc.process.Support#initialize()
     */
    public void initialize() {
        setSupportState(SupportState.readyForStart);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.process.Support#start()
     */
    public void start() {
        setSupportState(SupportState.ready);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.process.Support#stop()
     */
    public void stop() {
        setSupportState(SupportState.readyForStart);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.process.Support#uninitialize()
     */
    public void uninitialize() {
        setSupportState(SupportState.readyForInitialize);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.process.Support#process(org.epics.ioc.process.RecordProcessRequestor)
     */
    public void process(SupportProcessRequestor supportProcessRequestor) {
        dbData.message("process default called", MessageType.error);
        supportProcessRequestor.supportProcessDone(RequestResult.failure);
    } 
    /**
     * This must be called whenever the supports changes state.
     * @param state The new state.
     */
    protected void setSupportState(SupportState state) {
        supportState = state;
    }
    /**
     * Check the support state.
     * The result should always be true.
     * If the result is false then some support code, normally the support than calls this support
     * is incorrectly implemented.
     * This it is safe to call this methods without the record lock being held.
     * @param expectedState Expected state.
     * @param message A message to display if the state is incorrect.
     * @return (false,true) if the state (is not, is) the expected state.
     */
    protected boolean checkSupportState(SupportState expectedState,String message) {
        if(expectedState==supportState) return true;
        dbData.message(
             message
             + " expected supportState " + expectedState.toString()
             + String.format("%n")
             + "but state is " +supportState.toString(),
             MessageType.fatalError);
        return false;
    }
    /**
     * Get the configuration structure for this support.
     * @param structureName The expected struvture name.
     * @return The PVStructure or null if the structure is not located.
     */
    protected PVStructure getConfigStructure(String structureName) {
        if(dbData.getField().getType()!=Type.pvLink) {
            dbData.message("field is not a link", MessageType.fatalError);
            return null;
        }
        PVLink pvLink = (PVLink)dbData;
        PVStructure configStructure = pvLink.getConfigurationStructure();
        if(configStructure==null) {
            dbData.message("no configuration structure", MessageType.fatalError);
            return null;
        }
        Structure structure = (Structure)configStructure.getField();
        String configStructureName = structure.getStructureName();
        if(!configStructureName.equals(structureName)) {
            dbData.message(
                    "configurationStructure name is " + configStructureName
                    + " but expecting " + structureName,
                MessageType.fatalError);
            return null;
        }
        return configStructure;
    }
    /**
     * Get a boolean field from the configuration structure.
     * @param configStructure The configuration structure.
     * @param fieldName The field name.
     * @return The PVBoolean for accessing the field or null if it does not exist.
     */
    protected static PVBoolean getBoolean(PVStructure configStructure,String fieldName)
    {
        DBData dbData = (DBData)configStructure;
        Structure structure = (Structure)configStructure.getField();
        PVData[] pvData = configStructure.getFieldPVDatas();
        int index = structure.getFieldIndex(fieldName);
        if(index<0) {
            dbData.message(
                "configStructure does not have field" + fieldName,
                MessageType.error);
            return null;
        }
        if(pvData[index].getField().getType()!=Type.pvBoolean) {
            dbData.message(
                "configStructure field "
                + fieldName + " does not have type boolean ",
                MessageType.error);
            return null;
        }
        return (PVBoolean)pvData[index];
    }
    /**
     * Get a byte field from the configuration structure.
     * @param configStructure The configuration structure.
     * @param fieldName The field name.
     * @return The PVBoolean for accessing the field or null if it does not exist.
     */
    protected static PVByte getByte(PVStructure configStructure,String fieldName)
    {
        DBData dbData = (DBData)configStructure;
        Structure structure = (Structure)configStructure.getField();
        PVData[] pvData = configStructure.getFieldPVDatas();
        int index = structure.getFieldIndex(fieldName);
        if(index<0) {
            dbData.message(
                "configStructure does not have field" + fieldName,
                MessageType.error);
            return null;
        }
        if(pvData[index].getField().getType()!=Type.pvByte) {
            dbData.message(
                "configStructure field "
                + fieldName + " does not have type byte ",
                MessageType.error);
            return null;
        }
        return (PVByte)pvData[index];
    }
    /**
     * Get an int field from the configuration structure.
     * @param configStructure The configuration structure.
     * @param fieldName The field name.
     * @return The PVInt for accessing the field or null if it does not exist.
     */
    protected static PVInt getInt(PVStructure configStructure,String fieldName)
    {
        DBData dbData = (DBData)configStructure;
        Structure structure = (Structure)configStructure.getField();
        PVData[] pvData = configStructure.getFieldPVDatas();
        int index = structure.getFieldIndex(fieldName);
        if(index<0) {
            dbData.message(
                "configStructure does not have field" + fieldName,
                MessageType.error);
            return null;
        }
        if(pvData[index].getField().getType()!=Type.pvInt) {
            dbData.message(
                "configStructure field "
                + fieldName + " does not have type int ",
                MessageType.error);
            return null;
        }
        return (PVInt)pvData[index];
    }
    /**
     * Get a long field from the configuration structure.
     * @param configStructure The configuration structure.
     * @param fieldName The field name.
     * @return The PVLong for accessing the field or null if it does not exist.
     */
    protected static PVLong getLong(PVStructure configStructure,String fieldName)
    {
        DBData dbData = (DBData)configStructure;
        Structure structure = (Structure)configStructure.getField();
        PVData[] pvData = configStructure.getFieldPVDatas();
        int index = structure.getFieldIndex(fieldName);
        if(index<0) {
            dbData.message(
                "configStructure does not have field" + fieldName,
                MessageType.error);
            return null;
        }
        if(pvData[index].getField().getType()!=Type.pvLong) {
            dbData.message(
                "configStructure field "
                + fieldName + " does not have type int ",
                MessageType.error);
            return null;
        }
        return (PVLong)pvData[index];
    }
    /**
     * Get a float field from the configuration structure.
     * @param configStructure The configuration structure.
     * @param fieldName The field name.
     * @return The PVFloat for accessing the field or null if it does not exist.
     */
    protected static PVFloat getFloat(PVStructure configStructure,String fieldName)
    {
        DBData dbData = (DBData)configStructure;
        Structure structure = (Structure)configStructure.getField();
        PVData[] pvData = configStructure.getFieldPVDatas();
        int index = structure.getFieldIndex(fieldName);
        if(index<0) {
            dbData.message(
                "configStructure does not have field" + fieldName,
                MessageType.error);
            return null;
        }
        if(pvData[index].getField().getType()!=Type.pvFloat) {
            dbData.message(
                "configStructure field "
                + fieldName + " does not have type int ",
                MessageType.error);
            return null;
        }
        return (PVFloat)pvData[index];
    }
    /**
     * Get a double field from the configuration structure.
     * @param configStructure The configuration structure.
     * @param fieldName The field name.
     * @return The PVDouble for accessing the field or null if it does not exist.
     */
    protected static PVDouble getDouble(PVStructure configStructure,String fieldName)
    { 
        DBData dbData = (DBData)configStructure;
        Structure structure = (Structure)configStructure.getField();
        PVData[] pvData = configStructure.getFieldPVDatas();
        int index = structure.getFieldIndex(fieldName);
        if(index<0) {
            dbData.message(
                "configStructure does not have field" + fieldName,
                MessageType.error);
            return null;
        }
        if(pvData[index].getField().getType()!=Type.pvDouble) {
            dbData.message(
                "configStructure field "
                + fieldName + " does not have type int ",
                MessageType.error);
            return null;
        }
        return (PVDouble)pvData[index];
    }
    /**
     * Get a string field from the configuration structure.
     * @param configStructure The configuration structure.
     * @param fieldName The field name.
     * @return The PVString for accessing the field or null if it does not exist.
     */
    protected static PVString getString(PVStructure configStructure,String fieldName)
    {
        DBData dbData = (DBData)configStructure;
        Structure structure = (Structure)configStructure.getField();
        PVData[] pvData = configStructure.getFieldPVDatas();
        int index = structure.getFieldIndex(fieldName);
        if(index<0) {
            dbData.message(
                "configStructure does not have field" + fieldName,
                MessageType.error);
            return null;
        }
        if(pvData[index].getField().getType()!=Type.pvString) {
            dbData.message(
                "configStructure field "
                + fieldName + " does not have type string ",
                MessageType.error);
            return null;
        }
        return (PVString)pvData[index];
    }
    /**
     * Get a string field from the configuration structure.
     * @param configStructure The configuration structure.
     * @param fieldName The field name.
     * @return The PVEnum for accessing the field or null if it does not exist.
     */
    protected static PVEnum getEnum(
            PVStructure configStructure,String fieldName)
    {
        DBData dbData = (DBData)configStructure;
        Structure structure = (Structure)configStructure.getField();
        PVData[] pvData = configStructure.getFieldPVDatas();
        int index = structure.getFieldIndex(fieldName);
        if(index<0) {
            dbData.message(
                "configStructure does not have field" + fieldName,
                MessageType.error);
            return null;
        }
        Type type = pvData[index].getField().getType();
        if(type!=Type.pvEnum && type!=Type.pvMenu) {
            dbData.message(
                "configStructure field "
                + fieldName + " does not have type enum ",
                MessageType.error);
            return null;
        }
        return (PVEnum)pvData[index];
    }
}
