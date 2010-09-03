/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support;

import org.epics.ioc.database.PVRecordField;
import org.epics.ioc.install.AfterStart;
import org.epics.ioc.util.RequestResult;
import org.epics.pvData.pv.MessageType;

/**
 * Abstract base class for support code.
 * All support code should extend this class.
 * All methods must be called with the record locked.
 * @author mrk
 *
 */
public abstract class AbstractSupport implements Support {
    private String supportName;
    private PVRecordField pvRecordField;
    private SupportState supportState = SupportState.readyForInitialize;
    
    /**
     * Constructor.
     * This must be called by any class that extends AbstractSupport.
     * @param name The support name.
     * @param dbField The DBdata which is supported.
     * This can be a record or any field in a record.
     */
    protected AbstractSupport(String name,PVRecordField pvRecordField) {
        this.supportName = name;
        this.pvRecordField = pvRecordField;
    } 
    /* (non-Javadoc)
     * @see org.epics.ioc.support.Support#getSupportName()
     */
    @Override
    public String getSupportName() {
        return supportName;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.util.Requester#getRequesterName()
     */
    @Override
    public String getRequesterName() {
        return supportName;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.util.Requester#message(java.lang.String, org.epics.ioc.util.MessageType)
     */
    @Override
    public void message(String message, MessageType messageType) {
        pvRecordField.message(message, messageType);
    }    
    /* (non-Javadoc)
     * @see org.epics.ioc.support.Support#getSupportState()
     */
    @Override
    public SupportState getSupportState() {
        return supportState;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.support.Support#getPVRecordField()
     */
    @Override
     public PVRecordField getPVRecordField() {
        return pvRecordField;
    } 
    /* (non-Javadoc)
     * @see org.epics.ioc.support.Support#initialize(org.epics.ioc.support.RecordProcess)
     */
    @Override
    public void initialize() {
        setSupportState(SupportState.readyForStart);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.support.Support#start()
     */
    @Override
    public void start(AfterStart afterStart) {
        setSupportState(SupportState.ready);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.support.Support#stop()
     */
    @Override
    public void stop() {
        setSupportState(SupportState.readyForStart);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.support.Support#uninitialize()
     */
    @Override
    public void uninitialize() {
        if(supportState==SupportState.ready) stop();
        setSupportState(SupportState.readyForInitialize);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.support.Support#process(org.epics.ioc.process.SupportProcessRequester)
     */
    @Override
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
        pvRecordField.message(
             message
             + " expected supportState " + expectedState.toString()
             + String.format("%n")
             + "but state is " +supportState.toString(),
             MessageType.fatalError);
        return false;
    }
}
