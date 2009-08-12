/*
 * Copyright (c) 2004 by Cosylab
 *
 * The full license specifying the redistribution, modification, usage and other
 * rights and obligations is included with the distribution of this project in
 * the file "LICENSE-CAJ". If the license is not included visit Cosylab web site,
 * <http://www.cosylab.com>.
 *
 * THIS SOFTWARE IS PROVIDED AS-IS WITHOUT WARRANTY OF ANY KIND, NOT EVEN THE
 * IMPLIED WARRANTY OF MERCHANTABILITY. THE AUTHOR OF THIS SOFTWARE, ASSUMES
 * _NO_ RESPONSIBILITY FOR ANY CONSEQUENCE RESULTING FROM THE USE, MODIFICATION,
 * OR REDISTRIBUTION OF THIS SOFTWARE.
 */

package org.epics.ioc.channelAccess.test;

import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.TestCase;

import org.epics.ca.CAConstants;
import org.epics.ca.client.Channel;
import org.epics.ca.client.ClientContext;
import org.epics.ca.client.ConnectionEvent;
import org.epics.ca.client.EventListener;
import org.epics.ca.core.impl.client.ClientContextImpl;
import org.epics.pvData.channelAccess.ChannelMonitor;
import org.epics.pvData.channelAccess.ChannelMonitorRequester;
import org.epics.pvData.factory.ConvertFactory;
import org.epics.pvData.factory.FieldFactory;
import org.epics.pvData.factory.PVDataFactory;
import org.epics.pvData.misc.BitSet;
import org.epics.pvData.misc.Executor;
import org.epics.pvData.misc.ExecutorFactory;
import org.epics.pvData.misc.ThreadPriority;
import org.epics.pvData.pv.Convert;
import org.epics.pvData.pv.Field;
import org.epics.pvData.pv.FieldCreate;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVDataCreate;
import org.epics.pvData.pv.PVInt;
import org.epics.pvData.pv.PVString;
import org.epics.pvData.pv.PVStructure;
import org.epics.pvData.pv.ScalarType;

/**
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $Id$
 */
public class ChannelMonitorTest extends TestCase {

    /**
     * CA context.
     */
    protected ClientContext context = null;
    
    /* (non-Javadoc)
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        // Create a context with default configuration values.
        context = new ClientContextImpl();
        context.initialize();
    }

    /* (non-Javadoc)
     * @see junit.framework.TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        // Destroy the context, check if never initialized.
        if (context != null)
            context.destroy();
    }


    private static class ConnectionListener implements EventListener<ConnectionEvent>
    {
        private Boolean notification = null;
        
        public void onEvent(ConnectionEvent connectionEvent) {
            synchronized (this) {
                notification = new Boolean(connectionEvent.isConnected());
                this.notify();
            }
        }
        
        public void waitAndCheck() {
            synchronized (this) {
                if (notification == null)
                {
                    try {
                        this.wait(TIMEOUT_MS);
                    } catch (InterruptedException e) {
                        // noop
                    }
                }
                
                assertNotNull("channel connect timeout", notification);
                assertTrue("channel not connected", notification.booleanValue());
            }
        }
    };

    private Channel syncCreateChannel(String name) throws Throwable
    {
        ConnectionListener cl = new ConnectionListener();
        Channel ch = context.createChannel(name, cl, CAConstants.CA_DEFAULT_PRIORITY);
        cl.waitAndCheck();
        return ch;
    }

    private static FieldCreate fieldCreate = FieldFactory.getFieldCreate();
    private static PVDataCreate pvDataCreate = PVDataFactory.getPVDataCreate();
    
    

    private class ChannelMonitorRequesterImpl implements ChannelMonitorRequester {
        
        
        
        AtomicInteger monitorCounter = new AtomicInteger();
            
        ChannelMonitor channelMonitor;
        PVStructure pvStructure;
        BitSet changeBitSet;
        BitSet overrunBitSet;
        
        private Boolean connected = null;

        @Override
        public void channelMonitorConnect(ChannelMonitor channelMonitor) {
            synchronized (this)
            {
                this.channelMonitor = channelMonitor;

                connected = new Boolean(true);  // put here connection status
                this.notify();
            }
        }

        public void waitAndCheckConnect()
        {
            synchronized (this) {
                if (connected == null)
                {
                    try {
                        this.wait(TIMEOUT_MS);
                    } catch (InterruptedException e) {
                        // noop
                    }
                }
                
                assertNotNull("channel monitor connect timeout", connected);
                assertTrue("channel monitor failed to connect", connected.booleanValue());
            }
        }

        @Override
        public void unlisten() {
            // TODO Auto-generated method stub
            
        }
        
        @Override
        public void monitorEvent(PVStructure pvStructure, BitSet changeBitSet, BitSet overrunBitSet) {
            synchronized (this) {
                this.pvStructure = pvStructure;
                this.changeBitSet = changeBitSet;
                this.overrunBitSet = overrunBitSet;
                this.notify();
            }
        }
        
        @Override
        public String getRequesterName() {
            return this.getClass().getName();
        }

        @Override
        public void message(String message, MessageType messageType) {
            System.err.println("[" + messageType + "] " + message);
        }
    };

    private static final long TIMEOUT_MS = 3000;

   
    
    private static final Executor executor = ExecutorFactory.create("MonitorHandler", ThreadPriority.lowest);
    private static final Convert convert = ConvertFactory.getConvert();

    public void testChannelMonitors() throws Throwable
    {
        PVStructure pvRequest = pvDataCreate.createPVStructure(null, "psSimple", new Field[0]);
        Field newField = fieldCreate.createScalar("alarm", ScalarType.pvString);
        PVString pvString = (PVString)pvDataCreate.createPVField(pvRequest, newField);
        pvString.put("alarm");
        pvRequest.appendPVField(pvString);
        newField = fieldCreate.createScalar("timeStamp", ScalarType.pvString);
        pvString = (PVString)pvDataCreate.createPVField(pvRequest, newField);
        pvString.put("timeStamp");
        pvRequest.appendPVField(pvString);
        PVStructure pvVoltage = pvDataCreate.createPVStructure(pvRequest, "voltage", new Field[0]);
        newField = fieldCreate.createScalar("value", ScalarType.pvString);
        pvString = (PVString)pvDataCreate.createPVField(pvVoltage, newField);
        pvString.put("voltage.value");
        pvVoltage.appendPVField(pvString);
        newField = fieldCreate.createScalar("alarm", ScalarType.pvString);
        pvString = (PVString)pvDataCreate.createPVField(pvVoltage, newField);
        pvString.put("voltage.alarm");
        pvVoltage.appendPVField(pvString);
        pvRequest.appendPVField(pvVoltage);
        
        PVStructure pvCurrent = pvDataCreate.createPVStructure(pvRequest, "current", new Field[0]);
        newField = fieldCreate.createScalar("value", ScalarType.pvString);
        pvString = (PVString)pvDataCreate.createPVField(pvCurrent, newField);
        pvString.put("current.value");
        pvCurrent.appendPVField(pvString);
        newField = fieldCreate.createScalar("alarm", ScalarType.pvString);
        pvString = (PVString)pvDataCreate.createPVField(pvCurrent, newField);
        pvString.put("current.alarm");
        pvCurrent.appendPVField(pvString);
        pvRequest.appendPVField(pvCurrent);
        
        PVStructure pvPower = pvDataCreate.createPVStructure(pvRequest, "power", new Field[0]);
        newField = fieldCreate.createScalar("value", ScalarType.pvString);
        pvString = (PVString)pvDataCreate.createPVField(pvPower, newField);
        pvString.put("power.value");
        pvPower.appendPVField(pvString);
        pvRequest.appendPVField(pvPower);
        

        PVStructure pvOption = pvDataCreate.createPVStructure(null, "pvOption", new Field[0]);
        PVString pvAlgorithm = (PVString)pvDataCreate.createPVScalar(pvOption, "algorithm", ScalarType.pvString);
        pvAlgorithm.put("onPut");        // TODO constant!!!
        pvOption.appendPVField(pvAlgorithm);
        PVInt pvQueueSize = (PVInt)pvDataCreate.createPVScalar(pvOption, "queueSize", ScalarType.pvInt);
        pvQueueSize.put(10);
        pvOption.appendPVField(pvQueueSize);

        Channel ch = syncCreateChannel("psEmbeded");
        
        ChannelMonitorRequesterImpl channelMonitorRequester = new ChannelMonitorRequesterImpl();
        ch.createChannelMonitor(channelMonitorRequester, pvRequest, "monitor-test", pvOption, executor);
        channelMonitorRequester.waitAndCheckConnect();
        
        synchronized (channelMonitorRequester) {
            channelMonitorRequester.channelMonitor.start();
            int ntimes=0;
            while(ntimes++ < 3) {
                channelMonitorRequester.wait();
                PVStructure pvStructure = channelMonitorRequester.pvStructure;
                if(pvStructure==null) {
                    System.out.println("pvStructure null");
                    continue;
                }
                BitSet changeBitSet = channelMonitorRequester.changeBitSet;
                int next = changeBitSet.nextSetBit(0);
                while(next>=0) {
                    System.out.println(pvStructure.getSubField(next).toString());
                    changeBitSet.clear(next);
                    next = changeBitSet.nextSetBit(next);
                }
            }
            channelMonitorRequester.channelMonitor.stop();
        }
    }   
	
	
}
