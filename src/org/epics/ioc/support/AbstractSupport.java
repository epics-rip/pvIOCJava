/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support;

import org.epics.ioc.util.RequestResult;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVField;

/**
 * Abstract base class for support code.
 * All support code should extend this class.
 * All methods must be called with the record locked.
 * @author mrk
 *
 */
public abstract class AbstractSupport implements Support {
    protected String supportName;
    private PVField pvField;
    private SupportState supportState = SupportState.readyForInitialize;
    protected RecordSupport recordSupport = null;
    
    /**
     * Constructor.
     * This must be called by any class that extends AbstractSupport.
     * @param name The support name.
     * @param dbField The DBdata which is supported.
     * This can be a record or any field in a record.
     */
    protected AbstractSupport(String name,PVField pvField) {
        this.supportName = name;
        this.pvField = pvField;
    } 
    /* (non-Javadoc)
     * @see org.epics.ioc.support.Support#getSupportName()
     */
    public String getSupportName() {
        return supportName;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.util.Requester#getRequesterName()
     */
    public String getRequesterName() {
        return supportName;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.util.Requester#message(java.lang.String, org.epics.ioc.util.MessageType)
     */
    public void message(String message, MessageType messageType) {
        pvField.message(message, messageType);
    }    
    /* (non-Javadoc)
     * @see org.epics.ioc.support.Support#getSupportState()
     */
    public SupportState getSupportState() {
        return supportState;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.support.Support#getPVField()
     */
    public PVField getPVField() {
        return pvField;
    } 
    /* (non-Javadoc)
     * @see org.epics.ioc.support.Support#initialize(org.epics.ioc.support.RecordProcess)
     */
    public void initialize(RecordSupport recordSupport) {
        this.recordSupport = recordSupport;
        setSupportState(SupportState.readyForStart);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.support.Support#start()
     */
    public void start() {
        setSupportState(SupportState.ready);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.support.Support#stop()
     */
    public void stop() {
        setSupportState(SupportState.readyForStart);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.support.Support#uninitialize()
     */
    public void uninitialize() {
        if(supportState==SupportState.ready) stop();
        setSupportState(SupportState.readyForInitialize);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.support.Support#allSupportStarted()
     */
    public void allSupportStarted() {
        // nothing to do
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.support.Support#process(org.epics.ioc.process.SupportProcessRequester)
     */
    public void process(SupportProcessRequester supportProcessRequester) {
        supportProcessRequester.supportProcessDone(RequestResult.success);
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
        if(message==null) message = "";
        pvField.message(
             message
             + " expected supportState " + expectedState.toString()
             + String.format("%n")
             + "but state is " +supportState.toString(),
             MessageType.fatalError);
        return false;
    }
}
