/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbProcess;

import java.util.Iterator;
import java.util.LinkedList;

import org.epics.ioc.dbAccess.*;
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
    private LinkedList<SupportStateListener> listenerList
        = new LinkedList<SupportStateListener>();
    
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
             + " but state is " +supportState.toString(),
             IOCMessageType.fatalError);
        return false;
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
     * @see org.epics.ioc.dbProcess.Support#process(org.epics.ioc.dbProcess.ProcessRequestListener)
     */
    public ProcessReturn process(ProcessRequestListener listener) {
        dbData.message("process default called", IOCMessageType.error);
        return ProcessReturn.noop;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.dbProcess.Support#processContinue()
     */
    public ProcessContinueReturn processContinue(){
        dbData.message("processContinue default called", IOCMessageType.error);
        return ProcessContinueReturn.failure;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.dbProcess.Support#update()
     */
    public void update() {}
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
}
