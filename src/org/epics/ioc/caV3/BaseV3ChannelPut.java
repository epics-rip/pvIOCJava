/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.caV3;

import gov.aps.jca.CAException;
import gov.aps.jca.Channel;
import gov.aps.jca.dbr.DBRType;
import gov.aps.jca.event.ConnectionEvent;
import gov.aps.jca.event.ConnectionListener;
import gov.aps.jca.event.PutEvent;
import gov.aps.jca.event.PutListener;

import org.epics.ioc.ca.ChannelField;
import org.epics.ioc.ca.ChannelFieldGroup;
import org.epics.ioc.ca.ChannelProcess;
import org.epics.ioc.ca.ChannelProcessRequester;
import org.epics.ioc.ca.ChannelPut;
import org.epics.ioc.ca.ChannelPutRequester;
import org.epics.ioc.pv.ByteArrayData;
import org.epics.ioc.pv.DoubleArrayData;
import org.epics.ioc.pv.FloatArrayData;
import org.epics.ioc.pv.IntArrayData;
import org.epics.ioc.pv.PVByte;
import org.epics.ioc.pv.PVByteArray;
import org.epics.ioc.pv.PVDouble;
import org.epics.ioc.pv.PVDoubleArray;
import org.epics.ioc.pv.PVEnumerated;
import org.epics.ioc.pv.PVField;
import org.epics.ioc.pv.PVFloat;
import org.epics.ioc.pv.PVFloatArray;
import org.epics.ioc.pv.PVInt;
import org.epics.ioc.pv.PVIntArray;
import org.epics.ioc.pv.PVShort;
import org.epics.ioc.pv.PVShortArray;
import org.epics.ioc.pv.PVString;
import org.epics.ioc.pv.PVStringArray;
import org.epics.ioc.pv.ShortArrayData;
import org.epics.ioc.pv.StringArrayData;
import org.epics.ioc.util.MessageType;
import org.epics.ioc.util.RequestResult;

/**
 * Base class that implements ChannelPut for communicating with a V3 IOC.
 * @author mrk
 *
 */
public class BaseV3ChannelPut
implements ChannelPut,PutListener,ChannelProcessRequester,ConnectionListener
{
    private ChannelFieldGroup channelFieldGroup = null;
    private ChannelPutRequester channelPutRequester = null;
    private boolean process = false;
    
    private V3Channel v3Channel = null;
    private gov.aps.jca.Channel jcaChannel = null;
    private int elementCount = 0;
   
    private boolean isDestroyed = false;
    private ChannelField channelField = null;
    private PVField pvField = null;
    private PVInt pvIndex = null; // only if nativeDBRType.isENUM()
    private ChannelProcess channelProcess = null; // only if process is true
    private ByteArrayData byteArrayData = new ByteArrayData();
    private ShortArrayData shortArrayData = new ShortArrayData();
    private IntArrayData intArrayData = new IntArrayData();
    private FloatArrayData floatArrayData = new FloatArrayData();
    private DoubleArrayData doubleArrayData = new DoubleArrayData();
    private StringArrayData stringArrayData = new StringArrayData();
    
    private boolean isActive = false;

    /**
     * Constructor.
     * @param channelFieldGroup The channelFieldGroup.
     * @param channelPutRequester The channelPutRequester.
     * @param process Should the record be processed after the put.
     */
    public BaseV3ChannelPut(ChannelFieldGroup channelFieldGroup,
            ChannelPutRequester channelPutRequester, boolean process)
    {
        this.channelFieldGroup = channelFieldGroup;
        this.channelPutRequester = channelPutRequester;
        this.process = process;
    }
    /**
     * Initialize the channelPut.
     * @param v3Channel The V3Channel
     * @return (false,true) if the channelPut (did not, did) properly initialize.
     */
    public boolean init(V3Channel v3Channel)
    {
        this.v3Channel = v3Channel;
        DBRType nativeDBRType = v3Channel.getV3ChannelRecord().getNativeDBRType();
        jcaChannel = v3Channel.getJCAChannel();
        try {
            jcaChannel.addConnectionListener(this);
        } catch (CAException e) {
            message(
                    "addConnectionListener failed " + e.getMessage(),
                    MessageType.error);
            jcaChannel = null;
            return false;
        };
        elementCount = jcaChannel.getElementCount();
        ChannelField[] channelFields = channelFieldGroup.getArray();
        if(channelFields.length!=1) {
            message("only one channelField supported", MessageType.error);
            return false;
        }
        channelField = channelFields[0];
        pvField = channelField.getPVField();
        if(pvField==null) {
            message("value pvField not found",MessageType.error);
            return false;
        }
        if(nativeDBRType.isENUM()) {
            if(process) {
                message("process not supported for enumerated", MessageType.error);
                return false;
            }
            if(elementCount!=1) {
                message("array of ENUM not supported",MessageType.error);
                return false;
            }
            PVEnumerated pvEnumerated = pvField.getPVEnumerated();
            pvIndex = pvEnumerated.getIndexField();
        }
        if(process) {
            channelProcess = v3Channel.createChannelProcess(this);
            if(channelProcess==null) return false;
        }
        return true;
    } 
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.ChannelPut#destroy()
     */
    public void destroy() {
        isDestroyed = true;
        if(channelProcess!=null) channelProcess.destroy();
        v3Channel.remove(this);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.ChannelPut#put()
     */
    public void put() {
        if(isDestroyed) {
            message("isDestroyed",MessageType.error);
            putDone(RequestResult.failure);
            return;
        }
        if(jcaChannel.getConnectionState()!=Channel.ConnectionState.CONNECTED) {
            putDone(RequestResult.failure);
            return;
        }
        DBRType nativeDBRType = v3Channel.getV3ChannelRecord().getNativeDBRType();
        String message = null;
        isActive = true;
        try {
            boolean more = channelPutRequester.nextPutField(channelField,pvField);
            if(more) {
                message = "cant handle nextPutField returning more";
            } else if(pvIndex!=null) {
                short index = (short)pvIndex.get();
                jcaChannel.put(index, this);
            } else if(nativeDBRType==DBRType.BYTE) {
                if(elementCount==1) {
                    PVByte pvFrom = (PVByte)pvField;
                    byte from = pvFrom.get();
                    jcaChannel.put(from, this);
                } else {
                    PVByteArray fromArray =(PVByteArray)pvField;
                    int len = fromArray.get(0, elementCount, byteArrayData);
                    byte[] from = byteArrayData.data;
                    int capacity = fromArray.getCapacity();
                    for (int i=len; i<capacity; i++) from[i] = 0;
                    jcaChannel.put(from, this);
                }
            } else if(nativeDBRType==DBRType.SHORT) {
                if(elementCount==1) {
                    PVShort pvFrom = (PVShort)pvField;
                    short from = pvFrom.get();
                    jcaChannel.put(from, this);
                } else {
                    PVShortArray fromArray =(PVShortArray)pvField;
                    int len = fromArray.get(0, elementCount, shortArrayData);
                    short[] from = shortArrayData.data;
                    int capacity = fromArray.getCapacity();
                    for (int i=len; i<capacity; i++) from[i] = 0;
                    jcaChannel.put(from, this);
                }
            } else if(nativeDBRType==DBRType.INT) {
                if(elementCount==1) {
                    PVInt pvFrom = (PVInt)pvField;
                    int from = pvFrom.get();
                    jcaChannel.put(from, this);
                } else {
                    PVIntArray fromArray =(PVIntArray)pvField;
                    int len = fromArray.get(0, elementCount, intArrayData);
                    int[] from = intArrayData.data;
                    int capacity = fromArray.getCapacity();
                    for (int i=len; i<capacity; i++) from[i] = 0;
                    jcaChannel.put(from, this);
                }
            } else if(nativeDBRType==DBRType.FLOAT) {
                if(elementCount==1) {
                    PVFloat pvFrom = (PVFloat)pvField;
                    float from = pvFrom.get();
                    jcaChannel.put(from, this);
                } else {
                    PVFloatArray fromArray =(PVFloatArray)pvField;
                    int len = fromArray.get(0, elementCount, floatArrayData);
                    float[] from = floatArrayData.data;
                    int capacity = fromArray.getCapacity();
                    for (int i=len; i<capacity; i++) from[i] = 0;
                    jcaChannel.put(from, this);
                }
            } else if(nativeDBRType==DBRType.DOUBLE) {
                if(elementCount==1) {
                    PVDouble pvFrom = (PVDouble)pvField;
                    double from = pvFrom.get();
                    jcaChannel.put(from, this);
                } else {
                    PVDoubleArray fromArray =(PVDoubleArray)pvField;
                    int len = fromArray.get(0, elementCount, doubleArrayData);
                    double[] from = doubleArrayData.data;
                    int capacity = fromArray.getCapacity();
                    for (int i=len; i<capacity; i++) from[i] = 0;
                    jcaChannel.put(from, this);
                }
            } else if(nativeDBRType==DBRType.STRING) {
                if(elementCount==1) {
                    PVString pvFrom = (PVString)pvField;
                    String from = pvFrom.get();
                    jcaChannel.put(from, this);
                } else {
                    PVStringArray fromArray =(PVStringArray)pvField;
                    int len = fromArray.get(0, elementCount, stringArrayData);
                    String[] from = stringArrayData.data;
                    int capacity = fromArray.getCapacity();
                    for (int i=len; i<capacity; i++) from[i] = "";
                    jcaChannel.put(from, this);
                }
            } else {
                message = "unknown DBRType " + nativeDBRType.getName();
            }
        } catch (CAException e) {
            message = e.getMessage();
        } catch (IllegalStateException e) {
            message = e.getMessage();
        }
        if(message!=null) {
            message(message,MessageType.error);
            putDone(RequestResult.failure);
        } else {
            isActive = true;
        }
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.ChannelPut#putDelayed(org.epics.ioc.pv.PVField)
     */
    public void putDelayed(PVField pvField) {
        // nothing to do
    }
    /* (non-Javadoc)
     * @see gov.aps.jca.event.PutListener#putCompleted(gov.aps.jca.event.PutEvent)
     */
    public void putCompleted(PutEvent arg0) {
        if(!arg0.getStatus().isSuccessful()) {
            message(arg0.getStatus().getMessage(),MessageType.error);
            putDone(RequestResult.failure);
            return;
        }
        if(process) {
            channelProcess.process();
            return;
        }
        putDone(RequestResult.success);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.util.Requester#getRequesterName()
     */
    public String getRequesterName() {
        return channelPutRequester.getRequesterName();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.util.Requester#message(java.lang.String, org.epics.ioc.util.MessageType)
     */
    public void message(String message, MessageType messageType) {
        channelPutRequester.message(message, messageType);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.ChannelProcessRequester#processDone(org.epics.ioc.util.RequestResult)
     */
    public void processDone(RequestResult requestResult) {
        putDone(requestResult);
    }
    /* (non-Javadoc)
     * @see gov.aps.jca.event.ConnectionListener#connectionChanged(gov.aps.jca.event.ConnectionEvent)
     */
    public void connectionChanged(ConnectionEvent arg0) {
        if(!arg0.isConnected()) {
            if(isActive) {
                message("disconnected while active",MessageType.error);
                putDone(RequestResult.failure);
            }
        }
    }
    
    private void putDone(RequestResult requestResult) {
        isActive = false;
        channelPutRequester.putDone(requestResult);
    }
}
