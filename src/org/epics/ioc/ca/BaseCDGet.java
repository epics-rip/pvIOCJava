/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;

import org.epics.ioc.util.RequestResult;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVField;


/**
 * Base class for CD.
 * @author mrk
 *
 */
public class BaseCDGet implements CDGet, ChannelGetRequester {

    public BaseCDGet(Channel channel,ChannelFieldGroup channelFieldGroup,
        CDGetRequester cdGetRequester,boolean process)
    {
        this.cdGetRequester = cdGetRequester;
        channelGet = channel.createChannelGet(channelFieldGroup, this,process);
    }

    private CDGetRequester cdGetRequester;           
    private ChannelGet channelGet = null;
    private CDField[] cdFields = null;

    public void destroy() {
        channelGet.destroy();
    }

    /* (non-Javadoc)
     * @see org.epics.ioc.util.Requester#getRequesterName()
     */
    public String getRequesterName() {
        return cdGetRequester.getRequesterName();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.util.Requester#message(java.lang.String, org.epics.ioc.util.MessageType)
     */
    public void message(String message, MessageType messageType) {
        cdGetRequester.message(message, messageType);
    }       
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDGet#get()
     */
    public void get(CD cd) {
        cdFields = cd.getCDRecord().getCDStructure().getCDFields();
        channelGet.get();
    }

    /* (non-Javadoc)
     * @see org.epics.ioc.ca.ChannelGetRequester#getDone(org.epics.ioc.util.RequestResult)
     */
    public void getDone(RequestResult requestResult) {
        cdGetRequester.getDone(requestResult);
    }

    /* (non-Javadoc)
     * @see org.epics.ioc.ca.ChannelGetRequester#nextDelayedGetField(org.epics.ioc.pv.PVField)
     */
    public boolean nextDelayedGetField(PVField pvField) {
        throw new IllegalStateException("Logic error");
    }

    /* (non-Javadoc)
     * @see org.epics.ioc.ca.ChannelGetRequester#nextGetField(org.epics.ioc.ca.ChannelField, org.epics.ioc.pv.PVField)
     */
    public boolean nextGetField(ChannelField channelField, PVField pvField) {
        int length = cdFields.length;
        for(int i=0; i<length; i++) {
            if(cdFields[i].getChannelField()==channelField) {
                CDField cdField = cdFields[i];
                cdField.put(pvField);
                return false;
            }
        }
        throw new IllegalStateException("Logic error");
    }
}