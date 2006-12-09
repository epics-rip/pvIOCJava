/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbProcess;

import java.util.Iterator;
import java.util.LinkedList;

import org.epics.ioc.dbAccess.*;
import org.epics.ioc.pvAccess.*;
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
     * @see org.epics.ioc.dbProcess.Support#getName()
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
     * @see org.epics.ioc.dbProcess.Support#getSupportState()
     */
    public SupportState getSupportState() {
        return supportState;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.dbProcess.Support#getDBData()
     */
    public DBData getDBData() {
        return dbData;
    } 
    /* (non-Javadoc)
     * @see org.epics.ioc.dbProcess.Support#initialize()
     */
    public void initialize() {
        setSupportState(SupportState.readyForStart);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.dbProcess.Support#start()
     */
    public void start() {
        setSupportState(SupportState.ready);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.dbProcess.Support#stop()
     */
    public void stop() {
        setSupportState(SupportState.readyForStart);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.dbProcess.Support#uninitialize()
     */
    public void uninitialize() {
        setSupportState(SupportState.readyForInitialize);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.dbProcess.Support#process(org.epics.ioc.dbProcess.RecordProcessRequestor)
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
    protected DBStructure getConfigStructure(String structureName) {
        DBStructure configStructure = dbData.getConfigurationStructure();
        if(configStructure==null) {
            dbData.message("no configuration structure", MessageType.fatalError);
            return null;
        }
        Structure structure = (Structure)configStructure.getField();
        String configStructureName = structure.getStructureName();
        if(!configStructureName.equals(structureName)) {
            configStructure.message(
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
    protected static PVBoolean getBoolean(DBStructure configStructure,String fieldName)
    {
        DBData[] dbData = configStructure.getFieldDBDatas();
        int index = configStructure.getFieldDBDataIndex(fieldName);
        if(index<0) {
            configStructure.message(
                "configStructure does not have field" + fieldName,
                MessageType.error);
            return null;
        }
        if(dbData[index].getField().getType()!=Type.pvBoolean) {
            dbData[index].message(
                "configStructure field "
                + fieldName + " does not have type boolean ",
                MessageType.error);
            return null;
        }
        return (PVBoolean)dbData[index];
    }
    /**
     * Get a byte field from the configuration structure.
     * @param configStructure The configuration structure.
     * @param fieldName The field name.
     * @return The PVBoolean for accessing the field or null if it does not exist.
     */
    protected static PVByte getByte(DBStructure configStructure,String fieldName)
    {
        DBData[] dbData = configStructure.getFieldDBDatas();
        int index = configStructure.getFieldDBDataIndex(fieldName);
        if(index<0) {
            configStructure.message(
                "configStructure does not have field" + fieldName,
                MessageType.error);
            return null;
        }
        if(dbData[index].getField().getType()!=Type.pvByte) {
            dbData[index].message(
                "configStructure field "
                + fieldName + " does not have type byte ",
                MessageType.error);
            return null;
        }
        return (PVByte)dbData[index];
    }
    /**
     * Get an int field from the configuration structure.
     * @param configStructure The configuration structure.
     * @param fieldName The field name.
     * @return The PVInt for accessing the field or null if it does not exist.
     */
    protected static PVInt getInt(DBStructure configStructure,String fieldName)
    {
        DBData[] dbData = configStructure.getFieldDBDatas();
        int index = configStructure.getFieldDBDataIndex(fieldName);
        if(index<0) {
            configStructure.message(
                "configStructure does not have field" + fieldName,
                MessageType.error);
            return null;
        }
        if(dbData[index].getField().getType()!=Type.pvInt) {
            dbData[index].message(
                "configStructure field "
                + fieldName + " does not have type int ",
                MessageType.error);
            return null;
        }
        return (PVInt)dbData[index];
    }
    /**
     * Get a long field from the configuration structure.
     * @param configStructure The configuration structure.
     * @param fieldName The field name.
     * @return The PVLong for accessing the field or null if it does not exist.
     */
    protected static PVLong getLong(DBStructure configStructure,String fieldName)
    {
        DBData[] dbData = configStructure.getFieldDBDatas();
        int index = configStructure.getFieldDBDataIndex(fieldName);
        if(index<0) {
            configStructure.message(
                "configStructure does not have field" + fieldName,
                MessageType.error);
            return null;
        }
        if(dbData[index].getField().getType()!=Type.pvLong) {
            dbData[index].message(
                "configStructure field "
                + fieldName + " does not have type int ",
                MessageType.error);
            return null;
        }
        return (PVLong)dbData[index];
    }
    /**
     * Get a float field from the configuration structure.
     * @param configStructure The configuration structure.
     * @param fieldName The field name.
     * @return The PVFloat for accessing the field or null if it does not exist.
     */
    protected static PVFloat getFloat(DBStructure configStructure,String fieldName)
    {
        DBData[] dbData = configStructure.getFieldDBDatas();
        int index = configStructure.getFieldDBDataIndex(fieldName);
        if(index<0) {
            configStructure.message(
                "configStructure does not have field" + fieldName,
                MessageType.error);
            return null;
        }
        if(dbData[index].getField().getType()!=Type.pvFloat) {
            dbData[index].message(
                "configStructure field "
                + fieldName + " does not have type int ",
                MessageType.error);
            return null;
        }
        return (PVFloat)dbData[index];
    }
    /**
     * Get a double field from the configuration structure.
     * @param configStructure The configuration structure.
     * @param fieldName The field name.
     * @return The PVDouble for accessing the field or null if it does not exist.
     */
    protected static PVDouble getDouble(DBStructure configStructure,String fieldName)
    {
        DBData[] dbData = configStructure.getFieldDBDatas();
        int index = configStructure.getFieldDBDataIndex(fieldName);
        if(index<0) {
            configStructure.message(
                "configStructure does not have field" + fieldName,
                MessageType.error);
            return null;
        }
        if(dbData[index].getField().getType()!=Type.pvDouble) {
            dbData[index].message(
                "configStructure field "
                + fieldName + " does not have type int ",
                MessageType.error);
            return null;
        }
        return (PVDouble)dbData[index];
    }
    /**
     * Get a string field from the configuration structure.
     * @param configStructure The configuration structure.
     * @param fieldName The field name.
     * @return The PVString for accessing the field or null if it does not exist.
     */
    protected static PVString getString(DBStructure configStructure,String fieldName)
    {
        DBData[] dbData = configStructure.getFieldDBDatas();
        int index = configStructure.getFieldDBDataIndex(fieldName);
        if(index<0) {
            configStructure.message(
                "configStructure does not have field" + fieldName,
                MessageType.error);
            return null;
        }
        if(dbData[index].getField().getType()!=Type.pvString) {
            dbData[index].message(
                "configStructure field "
                + fieldName + " does not have type string ",
                MessageType.error);
            return null;
        }
        return (PVString)dbData[index];
    }
    /**
     * Get a string field from the configuration structure.
     * @param configStructure The configuration structure.
     * @param fieldName The field name.
     * @return The PVEnum for accessing the field or null if it does not exist.
     */
    protected static PVEnum getEnum(
            DBStructure configStructure,String fieldName)
    {
        DBData[] dbData = configStructure.getFieldDBDatas();
        int index = configStructure.getFieldDBDataIndex(fieldName);
        if(index<0) {
            configStructure.message(
                "configStructure does not have field" + fieldName,
                MessageType.error);
            return null;
        }
        if(dbData[index].getField().getType()!=Type.pvEnum) {
            dbData[index].message(
                "configStructure field "
                + fieldName + " does not have type string ",
                MessageType.error);
            return null;
        }
        return (PVEnum)dbData[index];
    }
}
