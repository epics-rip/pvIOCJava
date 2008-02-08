/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pv;

import java.util.ArrayList;
import java.util.List;

import org.epics.ioc.util.MessageType;
import org.epics.ioc.util.Requester;

/**
 * Base class for a record instance.
 * @author mrk
 *
 */
public class BasePVRecord extends BasePVStructure implements PVRecord {
    private String recordName;
    private List<Requester> requesterList = new ArrayList<Requester>();
    
    /**
     * Constructor.
     * @param recordName The name of the record.
     * @param structure The introspection interface for the record.
     */
    public BasePVRecord(String recordName,Structure structure)
    {
        super(null,structure);
        this.recordName = recordName;
        super.setRecord(this);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVRecord#getRecordName()
     */
    public String getRecordName() {
        return recordName;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.AbstractPVField#message(java.lang.String, org.epics.ioc.util.MessageType)
     */
    public void message(String message, MessageType messageType) {
        if(message!=null && message.charAt(0)!='.') message = " " + message;
        message = recordName + message;
        for (Requester requester : requesterList) requester.message(message, messageType);
        if(requesterList.size()==0) {
            System.out.println(messageType.toString() + " " + message);
        }
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVRecord#addRequester(org.epics.ioc.util.Requester)
     */
    public void addRequester(Requester requester) {
        requesterList.add(requester);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVRecord#removeRequester(org.epics.ioc.util.Requester)
     */
    public void removeRequester(Requester requester) {
        requesterList.remove(requester);
    }
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() { return toString(0);}
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.BasePVStructure#toString(int)
     */
    public String toString(int indentLevel) {
        return super.toString(recordName + " recordType",indentLevel);
    }
}
