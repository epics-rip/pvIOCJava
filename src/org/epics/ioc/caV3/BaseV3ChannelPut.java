/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.caV3;

import gov.aps.jca.dbr.DBRType;
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
public class BaseV3ChannelPut implements ChannelPut,PutListener,ChannelProcessRequester
{
    private ChannelFieldGroup channelFieldGroup = null;
    private ChannelPutRequester channelPutRequester = null;
    private boolean process = false;
    
    private gov.aps.jca.Channel jcaChannel = null;
    private DBRType valueDBRType = null;
    private V3Channel channel = null;
    private int elementCount = 0;
   
    
    private boolean isDestroyed = false;
    private PVInt pvIndex = null;
    private PVField pvField;
    private ChannelProcess channelProcess = null;
    private ChannelField channelField;
    private ByteArrayData byteArrayData = new ByteArrayData();
    private ShortArrayData shortArrayData = new ShortArrayData();
    private IntArrayData intArrayData = new IntArrayData();
    private FloatArrayData floatArrayData = new FloatArrayData();
    private DoubleArrayData doubleArrayData = new DoubleArrayData();
    private StringArrayData stringArrayData = new StringArrayData();
    private RequestResult requestResult = null;


    /**
     * Constructer.
     * @param channelPutRequester The channelPutRequester.
     * @param process Should the record be processed after the put.
     */
    public BaseV3ChannelPut(ChannelFieldGroup channelFieldGroup,
            ChannelPutRequester channelPutRequester, boolean process)
    {
        if(channelFieldGroup==null) {
            throw new IllegalStateException("no field group");
        }
        this.channelFieldGroup = channelFieldGroup;
        this.channelPutRequester = channelPutRequester;
        this.process = process;
    }
    /**
     * Initialize the channelPut.
     * @param channel The V3Channel
     * @return (false,true) if the channelPut (did not, did) properly initialize.
     */
    public boolean init(V3Channel channel)
    {
        this.channel = channel;
        jcaChannel = channel.getJcaChannel();
        valueDBRType = channel.getValueDBRType();
        elementCount = jcaChannel.getElementCount();
        ChannelField[] channelFields = channelFieldGroup.getArray();
        if(channelFields.length!=1) {
            channelPutRequester.message("only one channelField supported", MessageType.error);
            return false;
        }
        channelField = channelFields[0];
        pvField = channelField.getPVField();
        if(pvField==null) {
            channelPutRequester.message("value pvField not found",MessageType.error);
            return false;
        }
        if(valueDBRType.isENUM()) {
            if(process) {
                channelPutRequester.message(
                    "process not supported for enumerated", MessageType.error);
                return false;
            }
            if(elementCount!=1) {
                channelPutRequester.message("array of ENUM not supported",MessageType.error);
                return false;
            }
            PVEnumerated pvEnumerated = pvField.getPVEnumerated();
            pvIndex = pvEnumerated.getIndexField();
        }
        if(process) {
            channelProcess = channel.createChannelProcess(this);
            if(channelProcess==null) return false;
        }
        return true;
    } 

    public void destroy() {
        isDestroyed = true;
        channel.remove(this);
    }

    /* (non-Javadoc)
     * @see org.epics.ioc.ca.ChannelPut#put()
     */
    public void put() {
        if(isDestroyed) {
            channelPutRequester.message("isDestroyed",MessageType.error);
            channelPutRequester.putDone(RequestResult.failure);
        }
        boolean more = channelPutRequester.nextPutField(channelField,pvField);
        if(more) {
            channelPutRequester.message("cant handle nextPutField returning more",MessageType.error);
        }
        requestResult = RequestResult.success;
        if(pvIndex!=null) {
            short index = (short)pvIndex.get();
            try {
                jcaChannel.put(index, this);
            } catch (Exception e) {
                channelPutRequester.message(e.getMessage(),MessageType.error);
                requestResult = RequestResult.failure;
                channelPutRequester.putDone(requestResult);
            }
        } else if(valueDBRType==DBRType.BYTE) {
            if(elementCount==1) {
                PVByte pvFrom = (PVByte)pvField;
                byte from = pvFrom.get();
                try {
                    jcaChannel.put(from, this);
                } catch (Exception e) {
                    channelPutRequester.message(e.getMessage(),MessageType.error);
                    requestResult = RequestResult.failure;
                    channelPutRequester.putDone(requestResult);
                }
            } else {
                PVByteArray fromArray =(PVByteArray)pvField;
                int len = fromArray.get(0, elementCount, byteArrayData);
                byte[] from = byteArrayData.data;
                int capacity = fromArray.getCapacity();
                for (int i=len; i<capacity; i++) from[i] = 0;
                try {
                    jcaChannel.put(from, this);
                } catch (Exception e) {
                    channelPutRequester.message(e.getMessage(),MessageType.error);
                    requestResult = RequestResult.failure;
                }

            }
        } else if(valueDBRType==DBRType.SHORT) {
            if(elementCount==1) {
                PVShort pvFrom = (PVShort)pvField;
                short from = pvFrom.get();
                try {
                    jcaChannel.put(from, this);
                } catch (Exception e) {
                    channelPutRequester.message(e.getMessage(),MessageType.error);
                    requestResult = RequestResult.failure;
                }
            } else {
                PVShortArray fromArray =(PVShortArray)pvField;
                int len = fromArray.get(0, elementCount, shortArrayData);
                short[] from = shortArrayData.data;
                int capacity = fromArray.getCapacity();
                for (int i=len; i<capacity; i++) from[i] = 0;
                try {
                    jcaChannel.put(from, this);
                } catch (Exception e) {
                    channelPutRequester.message(e.getMessage(),MessageType.error);
                    requestResult = RequestResult.failure;
                }

            }
        } else if(valueDBRType==DBRType.INT) {
            if(elementCount==1) {
                PVInt pvFrom = (PVInt)pvField;
                int from = pvFrom.get();
                try {
                    jcaChannel.put(from, this);
                } catch (Exception e) {
                    channelPutRequester.message(e.getMessage(),MessageType.error);
                    requestResult = RequestResult.failure;
                }
            } else {
                PVIntArray fromArray =(PVIntArray)pvField;
                int len = fromArray.get(0, elementCount, intArrayData);
                int[] from = intArrayData.data;
                int capacity = fromArray.getCapacity();
                for (int i=len; i<capacity; i++) from[i] = 0;
                try {
                    jcaChannel.put(from, this);
                } catch (Exception e) {
                    channelPutRequester.message(e.getMessage(),MessageType.error);
                    requestResult = RequestResult.failure;
                }

            }
        } else if(valueDBRType==DBRType.FLOAT) {
            if(elementCount==1) {
                PVFloat pvFrom = (PVFloat)pvField;
                float from = pvFrom.get();
                try {
                    jcaChannel.put(from, this);
                } catch (Exception e) {
                    channelPutRequester.message(e.getMessage(),MessageType.error);
                }
            } else {
                PVFloatArray fromArray =(PVFloatArray)pvField;
                int len = fromArray.get(0, elementCount, floatArrayData);
                float[] from = floatArrayData.data;
                int capacity = fromArray.getCapacity();
                for (int i=len; i<capacity; i++) from[i] = 0;
                try {
                    jcaChannel.put(from, this);
                } catch (Exception e) {
                    channelPutRequester.message(e.getMessage(),MessageType.error);
                    requestResult = RequestResult.failure;
                }

            }
        } else if(valueDBRType==DBRType.DOUBLE) {
            if(elementCount==1) {
                PVDouble pvFrom = (PVDouble)pvField;
                double from = pvFrom.get();
                try {
                    jcaChannel.put(from, this);
                } catch (Exception e) {
                    channelPutRequester.message(e.getMessage(),MessageType.error);
                    requestResult = RequestResult.failure;
                }
            } else {
                PVDoubleArray fromArray =(PVDoubleArray)pvField;
                int len = fromArray.get(0, elementCount, doubleArrayData);
                double[] from = doubleArrayData.data;
                int capacity = fromArray.getCapacity();
                for (int i=len; i<capacity; i++) from[i] = 0;
                try {
                    jcaChannel.put(from, this);
                } catch (Exception e) {
                    channelPutRequester.message(e.getMessage(),MessageType.error);
                    requestResult = RequestResult.failure;
                }

            }
        } else if(valueDBRType==DBRType.STRING) {
            if(elementCount==1) {
                PVString pvFrom = (PVString)pvField;
                String from = pvFrom.get();
                try {
                    jcaChannel.put(from, this);
                } catch (Exception e) {
                    channelPutRequester.message(e.getMessage(),MessageType.error);
                }
            } else {
                PVStringArray fromArray =(PVStringArray)pvField;
                int len = fromArray.get(0, elementCount, stringArrayData);
                String[] from = stringArrayData.data;
                int capacity = fromArray.getCapacity();
                for (int i=len; i<capacity; i++) from[i] = "";
                try {
                    jcaChannel.put(from, this);
                } catch (Exception e) {
                    channelPutRequester.message(e.getMessage(),MessageType.error);
                    requestResult = RequestResult.failure;
                }
            }
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
        if(process) {
            channelProcess.process();
            return;
        }
        channelPutRequester.putDone(requestResult);
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
        if(requestResult.compareTo(this.requestResult)>0) this.requestResult = requestResult;
        channelPutRequester.putDone(this.requestResult);
    }
}
