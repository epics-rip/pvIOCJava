/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbProcess;

import java.util.Iterator;
import java.util.LinkedList;

import org.epics.ioc.dbAccess.*;

/**
 * Abstract base class for support code.
 * All support code should extend this class.
 * @author mrk
 *
 */
public abstract class AbstractSupport implements Support {
    
    /* (non-Javadoc)
     * @see org.epics.ioc.dbProcess.Support#initialize()
     */
    abstract public void initialize();
    /* (non-Javadoc)
     * @see org.epics.ioc.dbProcess.Support#start()
     */
    abstract public void start();
    /* (non-Javadoc)
     * @see org.epics.ioc.dbProcess.Support#stop()
     */
    abstract public void stop();
    /* (non-Javadoc)
     * @see org.epics.ioc.dbProcess.Support#uninitialize()
     */
    abstract public void uninitialize() ;
    /* (non-Javadoc)
     * @see org.epics.ioc.dbProcess.Support#process(org.epics.ioc.dbProcess.ProcessCompleteListener)
     */
    abstract public ProcessReturn process(ProcessCompleteListener listener);
    /* (non-Javadoc)
     * @see org.epics.ioc.dbProcess.Support#processContinue()
     */
    abstract public void processContinue();
    
    private String name;
    private DBData dbData;
    private DBRecord dbRecord = null;
    private SupportState supportState = SupportState.readyForInitialize;
    private LinkedList<SupportStateListener> listenerList
        = new LinkedList<SupportStateListener>();
    
    /**
     * Constructor.
     * This muts be called by any class that extends AbstractSupport.
     * @param name The support name.
     * @param dbData The DBdata which is supported.
     * This can be a record or any field in a record.
     */
    protected AbstractSupport(String name,DBData dbData) {
        this.name = name;
        this.dbData = dbData;
        dbRecord = dbData.getRecord();
    }
    
    /**
     * This must be called whenever the supports changes state.
     * @param state The new state.
     */
    protected void setSupportState(SupportState state) {
        SupportState oldState = supportState;
        supportState = state;
        if(oldState!=state) {
            Iterator<SupportStateListener> iter = listenerList.iterator();
            while(iter.hasNext()) {
                SupportStateListener listener = iter.next();
                listener.newState(this,state);
            }
        }
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.dbProcess.Support#getName()
     */
    public String getName() {
        return name;
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
     * @see org.epics.ioc.dbProcess.Support#addSupportStateListener(org.epics.ioc.dbProcess.SupportStateListener)
     */
    public boolean addSupportStateListener(SupportStateListener listener) {
        return listenerList.add(listener);
    }
    
    /* (non-Javadoc)
     * @see org.epics.ioc.dbProcess.Support#removeSupportStateListener(org.epics.ioc.dbProcess.SupportStateListener)
     */
    public boolean removeSupportStateListener(SupportStateListener listener) {
        return listenerList.remove(listener);
    }

    /* (non-Javadoc)
     * @see org.epics.ioc.dbProcess.Support#errorMessage(java.lang.String)
     * This is meant for use by support. It prepends the field name to the message
     * and calls recordProgress.errorMessage, which will prepend the record instance name.
     */
    public void errorMessage(String message) {
        String fieldName = getFullFieldName();
        
        RecordProcessSupport recordProcessSupport = dbRecord.getRecordProcess().getRecordProcessSupport();
        recordProcessSupport.errorMessage("." + fieldName + " " + message);
    }
    
    /* (non-Javadoc)
     * @see org.epics.ioc.dbProcess.Support#getFullFieldName()
     */
    public String getFullFieldName() {
        StringBuilder fieldName = new StringBuilder();
        fieldName.append(dbData.getField().getName());
        DBData parent = dbData.getParent();
        while(parent!=null && parent!=dbRecord) {
            fieldName.insert(0,parent.getField().getName());
            parent = parent.getParent();
        }
        return fieldName.toString();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.dbProcess.Support#update()
     */
    public void update() {
        // Do nothing.
    }
    
}
