/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;

import java.util.Iterator;
import java.util.LinkedList;

import org.epics.ioc.pv.PVDataCreate;
import org.epics.ioc.pv.PVField;


/**
 * Base class for CD.
 * @author mrk
 *
 */
public class BaseCD implements CD
{
    private Channel channel;
    private ChannelFieldGroup channelFieldGroup;
    private CDRecord cdRecord;
    private boolean isDestroyed = false;
    
    private LinkedList<CDGet> cdGetList = new LinkedList<CDGet>();
    private LinkedList<CDPut> cdPutList = new LinkedList<CDPut>();
    
    /**
     * Constructor.
     * @param channel The channel for which to create a CD.
     * @param channelFieldGroup The channelFieldGroup for whicg to cobstruct a CDRecord.
     * @param pvDataCreate Factory to create PVField objects.
     */
    public BaseCD(Channel channel,ChannelFieldGroup channelFieldGroup,PVDataCreate pvDataCreate)
    {
        this.channel = channel;
        this.channelFieldGroup = channelFieldGroup;
        cdRecord = new BaseCDRecord(pvDataCreate,channel.getChannelName(),channelFieldGroup);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CD#destroy()
     */
    public synchronized void destroy() {
        if(isDestroyed) return;
        isDestroyed = true;
        Iterator<CDGet> getIter = cdGetList.iterator();
        while(getIter.hasNext()) {
            CDGet cdGet = getIter.next();
            cdGet.destroy();
            getIter.remove();
        }
        Iterator<CDPut> putIter = cdPutList.iterator();
        while(putIter.hasNext()) {
            CDPut cdPut = putIter.next();
            cdPut.destroy();
            putIter.remove();
        }
    } 
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CD#getChannel()
     */
    public Channel getChannel() {
        return channel;
    }    
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CD#getChannelFieldGroup()
     */
    public ChannelFieldGroup getChannelFieldGroup() {
        return channelFieldGroup;   
    }    
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CD#getCDRecord()
     */
    public CDRecord getCDRecord() {
        return cdRecord;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CD#clearNumPuts()
     */
    public void clearNumPuts() {
        cdRecord.getCDStructure().clearNumPuts();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CD#get(org.epics.ioc.pv.PVField)
     */
    public void get(PVField pvField) {
        CDField[] cdFields = cdRecord.getCDStructure().getCDFields();
        int length = cdFields.length;
        for(int i=0; i<length; i++) {
            CDField cdField = cdFields[i];
            if(cdField.getChannelField().getPVField()==pvField) {
                cdField.get(pvField,true);
                return;
            }
        }
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CD#put(org.epics.ioc.pv.PVField, org.epics.ioc.pv.PVField)
     */
    public void put(PVField pvField, PVField pvSubField) {
        CDField cdField = cdRecord.findCDField(pvField);
        if(cdField==null) return;
        cdField.put(pvField, pvSubField);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CD#put(org.epics.ioc.pv.PVField)
     */
    public void put(PVField pvField) {
        CDField[] cdFields = cdRecord.getCDStructure().getCDFields();
        int length = cdFields.length;
        for(int i=0; i<length; i++) {
            CDField cdField = cdFields[i];
            if(cdField.getChannelField().getPVField()==pvField) {
                cdField.put(pvField);
                return;
            }
        }
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CD#createCDGet(org.epics.ioc.ca.CDGetRequester, boolean)
     */
    public synchronized CDGet createCDGet(CDGetRequester cdGetRequester,boolean process)
    {
        CDGet cdGet = new BaseCDGet(channel,channelFieldGroup,cdGetRequester,process);
        cdGetList.add(cdGet);
        return cdGet;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CD#destroy(org.epics.ioc.ca.CDGet)
     */
    public synchronized void destroy(CDGet toDelete) {           
        Iterator<CDGet> iter = cdGetList.iterator();
        while(iter.hasNext()) {
            CDGet cdGet = iter.next();
            if(cdGet==toDelete) {
                cdGet.destroy();
                iter.remove();
                return;
            }
        }
    }    
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CD#createCDPut(org.epics.ioc.ca.CDPutRequester, boolean)
     */
    public synchronized CDPut createCDPut(CDPutRequester cdPutRequester,boolean process)
    {
        CDPut cdPut = new BaseCDPut(channel,channelFieldGroup,cdPutRequester,process);
        cdPutList.add(cdPut);
        return cdPut;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CD#destroy(org.epics.ioc.ca.CDPut)
     */
    public synchronized void destroy(CDPut temp) {
        CDPut toDelete = (CDPut)temp;
        Iterator<CDPut> putIter = cdPutList.iterator();
        while(putIter.hasNext()) {
            CDPut cdPut = putIter.next();
            if(cdPut==toDelete) {
                cdPut.destroy();
                putIter.remove();
                return;
            }
        }
    }
}
