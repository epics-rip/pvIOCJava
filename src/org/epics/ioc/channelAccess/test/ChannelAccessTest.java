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

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.TestCase;

import org.epics.ca.channelAccess.client.Channel;
import org.epics.ca.channelAccess.client.ChannelArray;
import org.epics.ca.channelAccess.client.ChannelArrayRequester;
import org.epics.ca.channelAccess.client.ChannelGet;
import org.epics.ca.channelAccess.client.ChannelGetRequester;
import org.epics.ca.channelAccess.client.ChannelProcess;
import org.epics.ca.channelAccess.client.ChannelProcessRequester;
import org.epics.ca.channelAccess.client.ChannelProvider;
import org.epics.ca.channelAccess.client.ChannelPut;
import org.epics.ca.channelAccess.client.ChannelPutGet;
import org.epics.ca.channelAccess.client.ChannelPutGetRequester;
import org.epics.ca.channelAccess.client.ChannelPutRequester;
import org.epics.ca.channelAccess.client.ChannelRequester;
import org.epics.ca.channelAccess.client.GetFieldRequester;
import org.epics.ca.channelAccess.server.impl.ChannelAccessFactory;
import org.epics.ioc.install.Install;
import org.epics.ioc.install.InstallFactory;
import org.epics.pvData.factory.ConvertFactory;
import org.epics.pvData.factory.FieldFactory;
import org.epics.pvData.factory.PVDataFactory;
import org.epics.pvData.misc.BitSet;
import org.epics.pvData.monitor.Monitor;
import org.epics.pvData.monitor.MonitorElement;
import org.epics.pvData.monitor.MonitorRequester;
import org.epics.pvData.property.TimeStamp;
import org.epics.pvData.property.TimeStampFactory;
import org.epics.pvData.pv.Convert;
import org.epics.pvData.pv.DoubleArrayData;
import org.epics.pvData.pv.Field;
import org.epics.pvData.pv.FieldCreate;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVArray;
import org.epics.pvData.pv.PVDataCreate;
import org.epics.pvData.pv.PVDouble;
import org.epics.pvData.pv.PVDoubleArray;
import org.epics.pvData.pv.PVField;
import org.epics.pvData.pv.PVInt;
import org.epics.pvData.pv.PVString;
import org.epics.pvData.pv.PVStringArray;
import org.epics.pvData.pv.PVStructure;
import org.epics.pvData.pv.Requester;
import org.epics.pvData.pv.ScalarType;
import org.epics.pvData.pv.StringArrayData;
import org.epics.pvData.pv.Structure;
import org.epics.pvData.pv.Type;

/**
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $Id$
 */
public class ChannelAccessTest extends TestCase {

	private class ChannelRequesterImpl implements ChannelRequester {
		
		Channel channel;
		
		@Override
		public void message(String message, MessageType messageType) {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public String getRequesterName() {
			// TODO Auto-generated method stub
			return null;
		}
		
		@Override
		public void destroy(org.epics.ca.channelAccess.client.Channel c) {
			// TODO Auto-generated method stub
		}
		
		@Override
		public void channelStateChange(org.epics.ca.channelAccess.client.Channel c,
				boolean isConnected) {
			// TODO Auto-generated method stub
		}
		
		@Override
		public synchronized void channelNotCreated() {
			this.notifyAll();
		}
		
		@Override
		public synchronized void channelCreated(org.epics.ca.channelAccess.client.Channel channel) {
			this.channel = channel;
			this.notifyAll();
		}
	};
	

	private Channel syncCreateChannel(String name)
	{
		ChannelRequesterImpl cr = new ChannelRequesterImpl();
		synchronized (cr) {
			provider.createChannel(name, cr, ChannelProvider.PRIORITY_DEFAULT);
			if (cr.channel == null)
			{
				try {
					cr.wait(TIMEOUT_MS);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			assertNotNull("failed to create channel", cr.channel);
			return cr.channel;
		}
	}

	private ChannelProvider provider;

    private static class Listener implements Requester {
        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requester#getRequesterName()
         */
        public String getRequesterName() {
            return ChannelAccessTest.class.getName();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requester#message(java.lang.String, org.epics.ioc.util.MessageType)
         */
        public void message(String message, MessageType messageType) {
            System.out.println(message);
            
        }
    }

    private static final Install install = InstallFactory.get();

    static
	{
		// start javaIOC
        Requester iocRequester = new Listener();
        try {
            install.installStructures("xml/structures.xml",iocRequester);
            install.installRecords("example/exampleDB.xml",iocRequester);
        }  catch (IllegalStateException e) {
            System.out.println("IllegalStateException: " + e);
        }
		
	}
	
	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	@Override
	protected void setUp() throws Exception {
		provider = ChannelAccessFactory.getChannelAccess().getProvider("local");
	}

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#tearDown()
	 */
	@Override
	protected void tearDown() throws Exception {
	}

    private static FieldCreate fieldCreate = FieldFactory.getFieldCreate();
    private static PVDataCreate pvDataCreate = PVDataFactory.getPVDataCreate();

    private static class ChannelGetRequesterImpl implements ChannelGetRequester
	{
		ChannelGet channelGet;
		BitSet bitSet;
		PVStructure pvStructure;

		private Boolean connected = null;
		private Boolean success = null;
		
		@Override
		public void channelGetConnect(ChannelGet channelGet, PVStructure pvStructure, BitSet bitSet) {
			synchronized (this) {
				this.channelGet = channelGet;
				this.pvStructure = pvStructure;
				this.bitSet = bitSet;

				connected = new Boolean(true);	// put here connection status
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
				
				assertNotNull("channel get connect timeout", connected);
				assertTrue("channel get failed to connect", connected.booleanValue());
				assertNotNull(pvStructure);
				assertNotNull(bitSet);
				
			}
		}

		public void syncGet(boolean lastRequest)
		{
			synchronized (this) {
				if (connected == null)
					assertNotNull("channel get not connected", connected);
					
				success = null;
				channelGet.get(lastRequest);
				
				try {
					if (success == null)
						this.wait(TIMEOUT_MS);
				} catch (InterruptedException e) {
					// noop
				}
				
				assertNotNull("channel get timeout", success);
				assertTrue("channel get failed", success.booleanValue());
			}
		}
		
		@Override
		public void getDone(boolean success) {
			synchronized (this) {
				this.success = new Boolean(success);
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

	private class ChannelPutRequesterImpl implements ChannelPutRequester
	{
		ChannelPut channelPut;
		PVStructure pvStructure;
		BitSet bitSet;

		private Boolean connected = null;
		private Boolean success = null;

		@Override
		public void channelPutConnect(ChannelPut channelPut, PVStructure pvStructure, BitSet bitSet) {
			synchronized (this) {
				this.channelPut = channelPut;
				this.pvStructure = pvStructure;
				this.bitSet = bitSet;

				connected = new Boolean(true);	// put here connection status
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
				
				assertNotNull("channel put connect timeout", connected);
				assertTrue("channel put failed to connect", connected.booleanValue());
				assertNotNull(pvStructure);
				assertNotNull(bitSet);
				
			}
		}

		public void syncGet()
		{
			synchronized (this) {
				if (connected == null)
					assertNotNull("channel put not connected", connected);
					
				success = null;
				channelPut.get();
				
				try {
					if (success == null)
						this.wait(TIMEOUT_MS);
				} catch (InterruptedException e) {
					// noop
				}
				
				assertNotNull("channel get timeout", success);
				assertTrue("channel get failed", success.booleanValue());
			}
		}
		
		@Override
		public void getDone(boolean success) {
			synchronized (this) {
				this.success = new Boolean(success);
				this.notify();
			}
		}
		
		public void syncPut(boolean lastRequest)
		{
			synchronized (this) {
				if (connected == null)
					assertNotNull("channel put not connected", connected);
					
				success = null;
				channelPut.put(lastRequest);
				
				try {
					if (success == null)
						this.wait(TIMEOUT_MS);
				} catch (InterruptedException e) {
					// noop
				}
				
				assertNotNull("channel put timeout", success);
				assertTrue("channel put failed", success.booleanValue());
			}
		}
		
		@Override
		public void putDone(boolean success) {
			synchronized (this) {
				this.success = new Boolean(success);
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
	
	
	private class ChannelPutGetRequesterImpl implements ChannelPutGetRequester
	{
		ChannelPutGet channelPutGet;
		PVStructure pvPutStructure;
		PVStructure pvGetStructure;

		private Boolean connected = null;
		private Boolean success = null;

		/* (non-Javadoc)
		 * @see org.epics.ca.channelAccess.client.ChannelPutGetRequester#channelPutGetConnect(org.epics.ca.channelAccess.client.ChannelPutGet, org.epics.pvData.pv.PVStructure, org.epics.pvData.pv.PVStructure)
		 */
		@Override
		public void channelPutGetConnect(
				ChannelPutGet channelPutGet,
				PVStructure pvPutStructure, PVStructure pvGetStructure) {
			synchronized (this)
			{
				this.channelPutGet = channelPutGet;
				this.pvPutStructure = pvPutStructure;
				this.pvGetStructure = pvGetStructure;

				connected = new Boolean(true);	// put here connection status
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
				
				assertNotNull("channel put-get connect timeout", connected);
				assertTrue("channel put-get failed to connect", connected.booleanValue());
				assertNotNull(pvPutStructure);
				assertNotNull(pvGetStructure);
			}
		}

		public void syncPutGet(boolean lastRequest)
		{
			synchronized (this) {
				if (connected == null)
					assertNotNull("channel put-get not connected", connected);
					
				success = null;
				channelPutGet.putGet(lastRequest);
				
				try {
					if (success == null)
						this.wait(TIMEOUT_MS);
				} catch (InterruptedException e) {
					// noop
				}
				
				assertNotNull("channel put-get timeout", success);
				assertTrue("channel put-get failed", success.booleanValue());
			}
		}
	
		@Override
		public void putGetDone(boolean success) {
			synchronized (this) {
				this.success = new Boolean(success);
				this.notify();
			}
		}

		public void syncGetGet()
		{
			synchronized (this) {
				if (connected == null)
					assertNotNull("channel put-get not connected", connected);
					
				success = null;
				channelPutGet.getGet();
				
				try {
					if (success == null)
						this.wait(TIMEOUT_MS);
				} catch (InterruptedException e) {
					// noop
				}
				
				assertNotNull("channel get-get timeout", success);
				assertTrue("channel get-get failed", success.booleanValue());
			}
		}
	
		@Override
		public void getGetDone(boolean success) {
			synchronized (this) {
				this.success = new Boolean(success);
				this.notify();
			}
		}

		public void syncGetPut()
		{
			synchronized (this) {
				if (connected == null)
					assertNotNull("channel put-get not connected", connected);
					
				success = null;
				channelPutGet.getPut();
				
				try {
					if (success == null)
						this.wait(TIMEOUT_MS);
				} catch (InterruptedException e) {
					// noop
				}
				
				assertNotNull("channel get-put timeout", success);
				assertTrue("channel get-put failed", success.booleanValue());
			}
		}

		@Override
		public void getPutDone(boolean success) {
			synchronized (this) {
				this.success = new Boolean(success);
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
	
	ChannelProcessRequester channelProcessRequester = new ChannelProcessRequester() {
		
		volatile ChannelProcess channelProcess;
		
		@Override
		public void processDone(boolean success) {
			System.out.println("processDone: " + success);
			channelProcess.process(true);
		}
		
		@Override
		public void channelProcessConnect(ChannelProcess channelProcess) {
			System.out.println("channelProcessConnect done");
			this.channelProcess = channelProcess;
			channelProcess.process(false);
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

	private class GetFieldRequesterImpl implements GetFieldRequester {

		Field field;
		
		private Boolean success;
		
		@Override
		public void getDone(Field field) {
			synchronized (this) {
				this.field = field;
				this.success = new Boolean(true);
				this.notify();
			}
		}

		public void syncGetField(Channel ch, String subField)
		{
			synchronized (this) {
					
				success = null;
				ch.getField(this, subField);
				
				try {
					if (success == null)
						this.wait(TIMEOUT_MS);
				} catch (InterruptedException e) {
					// noop
				}
				
				assertNotNull("channel getField timeout", success);
				assertTrue("channel getField failed", success.booleanValue());
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

	private class ChannelProcessRequesterImpl implements ChannelProcessRequester {
		
		ChannelProcess channelProcess;

		private Boolean success;
		private Boolean connected;
		
		@Override
		public void processDone(boolean success) {
			synchronized (this) {
				this.success = new Boolean(true);
				this.notify();
			}
		}
		
		@Override
		public void channelProcessConnect(ChannelProcess channelProcess) {
			synchronized (this) {
				this.channelProcess = channelProcess;
	
				connected = new Boolean(channelProcess != null);	// put here connection status
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
				
				assertNotNull("channel process connect timeout", connected);
				assertTrue("channel process failed to connect", connected.booleanValue());
			}
		}
		
		public void syncProcess(boolean lastRequest)
		{
			synchronized (this) {
				if (connected == null)
					assertNotNull("channel process not connected", connected);
					
				success = null;
				channelProcess.process(lastRequest);
				
				try {
					if (success == null)
						this.wait(TIMEOUT_MS);
				} catch (InterruptedException e) {
					// noop
				}
				
				assertNotNull("channel process timeout", success);
				assertTrue("channel process failed", success.booleanValue());
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
	
	private class ChannelArrayRequesterImpl implements ChannelArrayRequester {

		ChannelArray channelArray;
		PVArray pvArray;
		
		private Boolean connected = null;
		private Boolean success = null;

		@Override
		public void channelArrayConnect(ChannelArray channelArray, PVArray pvArray) {
			synchronized (this)
			{
				this.channelArray = channelArray;
				this.pvArray = pvArray;

				connected = new Boolean(true);	// put here connection status
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
				
				assertNotNull("channel array connect timeout", connected);
				assertTrue("channel array failed to connect", connected.booleanValue());
				assertNotNull(pvArray);
			}
		}

		@Override
		public void getArrayDone(boolean success) {
			synchronized (this) {
				this.success = new Boolean(success);
				this.notify();
			}
		}

		public void syncGet(boolean lastRequest, int offset, int count)
		{
			synchronized (this) {
				if (connected == null)
					assertNotNull("channel array not connected", connected);
					
				success = null;
				channelArray.getArray(lastRequest, offset, count);
				
				try {
					if (success == null)
						this.wait(TIMEOUT_MS);
				} catch (InterruptedException e) {
					// noop
				}
				
				assertNotNull("channel array get timeout", success);
				assertTrue("channel array get failed", success.booleanValue());
			}
		}

		@Override
		public void putArrayDone(boolean success) {
			synchronized (this) {
				this.success = new Boolean(success);
				this.notify();
			}
		}

		public void syncPut(boolean lastRequest, int offset, int count)
		{
			synchronized (this) {
				if (connected == null)
					assertNotNull("channel array not connected", connected);
					
				success = null;
				channelArray.putArray(lastRequest, offset, count);
				
				try {
					if (success == null)
						this.wait(TIMEOUT_MS);
				} catch (InterruptedException e) {
					// noop
				}
				
				assertNotNull("channel array put timeout", success);
				assertTrue("channel array put failed", success.booleanValue());
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

	private class ChannelMonitorRequesterImpl implements MonitorRequester {
		
		PVStructure pvStructure;
		BitSet changeBitSet;
		BitSet overrunBitSet;
		
		AtomicInteger monitorCounter = new AtomicInteger();
			
		Monitor channelMonitor;
		
		private Boolean connected = null;

		@Override
		public void monitorConnect(Monitor channelMonitor, Structure structure) {
			synchronized (this)
			{
				this.channelMonitor = channelMonitor;

				connected = new Boolean(true);	// put here connection status
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
		public void monitorEvent(Monitor monitor) {
			synchronized (this) {
				MonitorElement element = monitor.poll();
				// TODO copy (but monitors are slow)
				this.pvStructure = element.getPVStructure();
				this.changeBitSet = element.getChangedBitSet();
				this.overrunBitSet = element.getOverrunBitSet();
				monitor.release(element);
				
				assertNotNull(this.pvStructure);
				assertNotNull(this.changeBitSet);
				assertNotNull(this.overrunBitSet);
			
				monitorCounter.incrementAndGet();
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

	public void testChannelGet() throws Throwable
	{
	    Channel ch = syncCreateChannel("valueOnly");
		channelGetTestNoProcess(ch, false);
		//channelGetTestNoProcess(ch, true);
		
		ch.destroy();
		

	    ch = syncCreateChannel("simpleCounter");
		channelGetTestIntProcess(ch, false);
		//channelGetTestIntProcess(ch, true);
	}
	
	private void channelGetTestNoProcess(Channel ch, boolean share) throws Throwable
	{
    	PVStructure pvRequest = pvDataCreate.createPVStructure(null, "", new Field[0]);
        Field newField = fieldCreate.createScalar("timeStamp", ScalarType.pvString);
        PVString pvString = (PVString)pvDataCreate.createPVField(pvRequest, newField);
        pvString.put("timeStamp");
        pvRequest.appendPVField(pvString);
        newField = fieldCreate.createScalar("value", ScalarType.pvString);
        pvString = (PVString)pvDataCreate.createPVField(pvRequest, newField);
        pvString.put("value");
        pvRequest.appendPVField(pvString);

		ChannelGetRequesterImpl channelGetRequester = new ChannelGetRequesterImpl();
		ch.createChannelGet(channelGetRequester, pvRequest, "get-test", share, false, null);
		channelGetRequester.waitAndCheckConnect();
		
		assertEquals("get-test", channelGetRequester.pvStructure.getFullName());
		
		channelGetRequester.syncGet(false);
		// only first bit must be set
		assertEquals(1, channelGetRequester.bitSet.cardinality());
		assertTrue(channelGetRequester.bitSet.get(0));
		
		channelGetRequester.syncGet(false);
		// no changes
		assertEquals(0, channelGetRequester.bitSet.cardinality());

		channelGetRequester.syncGet(true);
		// no changes, again
		assertEquals(0, channelGetRequester.bitSet.cardinality());
		
		/*
		// TODO check when error handling is implemented
		channelGetRequester.channelGet.destroy();
		channelGetRequester.syncGet(true);
		*/
	}
	
	private void channelGetTestIntProcess(Channel ch, boolean share) throws Throwable
	{
    	PVStructure pvRequest = pvDataCreate.createPVStructure(null, "", new Field[0]);
        Field newField = fieldCreate.createScalar("timeStamp", ScalarType.pvString);
        PVString pvString = (PVString)pvDataCreate.createPVField(pvRequest, newField);
        pvString.put("timeStamp");
        pvRequest.appendPVField(pvString);
        newField = fieldCreate.createScalar("value", ScalarType.pvString);
        pvString = (PVString)pvDataCreate.createPVField(pvRequest, newField);
        pvString.put("value");
        pvRequest.appendPVField(pvString);

		ChannelGetRequesterImpl channelGetRequester = new ChannelGetRequesterImpl();
		ch.createChannelGet(channelGetRequester, pvRequest, "get-test", share, true, null);
		channelGetRequester.waitAndCheckConnect();
		
		assertEquals("get-test", channelGetRequester.pvStructure.getFullName());
		PVInt value = channelGetRequester.pvStructure.getIntField("value");
		TimeStamp timestamp = TimeStampFactory.getTimeStamp(channelGetRequester.pvStructure.getStructureField("timeStamp"));
		
		channelGetRequester.syncGet(false);
		// only first bit must be set
		assertEquals(1, channelGetRequester.bitSet.cardinality());
		assertTrue(channelGetRequester.bitSet.get(0));
		
		// multiple tests 
		final int TIMES = 3;
		for (int i = 1; i <= TIMES; i++)
		{
			int previousValue = value.get();
			long previousTimestampSec = timestamp.getSecondsPastEpoch();
			
			// 2 seconds to have different timestamps
			Thread.sleep(1000);
			
			channelGetRequester.syncGet(i == TIMES);
			// changes of value and timeStamp
			assertEquals((previousValue + 1)%11, value.get());
			assertTrue(timestamp.getSecondsPastEpoch() > previousTimestampSec);
		}

		
		/*
		// TODO check when error handling is implemented
		channelGetRequester.channelGet.destroy();
		channelGetRequester.syncGet(true);
		*/
	}
	
	public void testChannelPut() throws Throwable
	{
	    Channel ch = syncCreateChannel("valueOnly");
		channelPutTestNoProcess(ch, false);
		//channelPutTestNoProcess(ch, true);
		
		ch.destroy();
		
	    ch = syncCreateChannel("simpleCounter");
		channelPutTestIntProcess(ch, false);
		//channelPutTestIntProcess(ch, true);
	}
	
	private void channelPutTestNoProcess(Channel ch, boolean share) throws Throwable
	{
    	PVStructure pvRequest = pvDataCreate.createPVStructure(null, "", new Field[0]);
        Field newField = fieldCreate.createScalar("value", ScalarType.pvString);
        PVString pvString = (PVString)pvDataCreate.createPVField(pvRequest, newField);
        pvString.put("value");
        pvRequest.appendPVField(pvString);

		ChannelPutRequesterImpl channelPutRequester = new ChannelPutRequesterImpl();
		ch.createChannelPut(channelPutRequester, pvRequest, "put-test", share, false, null);
		channelPutRequester.waitAndCheckConnect();
		
		assertEquals("put-test", channelPutRequester.pvStructure.getFullName());

		// set and get test
		PVDouble value = channelPutRequester.pvStructure.getDoubleField("value");
		assertNotNull(value);
		final double INIT_VAL = 123.0;
		value.put(INIT_VAL);
		channelPutRequester.bitSet.set(value.getFieldOffset());
		
		channelPutRequester.syncPut(false);
		// TODO should put bitSet be reset here
		//assertEquals(0, channelPutRequester.bitSet.cardinality());
		channelPutRequester.syncGet();
		assertEquals(INIT_VAL, value.get());


		// value should not change since bitSet is not set
		value.put(INIT_VAL+1);
		channelPutRequester.bitSet.clear();
		channelPutRequester.syncPut(false);
		channelPutRequester.syncGet();
		assertEquals(INIT_VAL, value.get());
		
		// now should change
		value.put(INIT_VAL+1);
		channelPutRequester.bitSet.set(value.getFieldOffset());
		channelPutRequester.syncPut(false);
		channelPutRequester.syncGet();
		assertEquals(INIT_VAL+1, value.get());

		// destroy
		channelPutRequester.syncPut(true);

		/*
		// TODO check when error handling is implemented
		channelPutRequester.channelPut.destroy();
		channelPutRequester.syncPut(true);
		*/
	}

	private void channelPutTestIntProcess(Channel ch, boolean share) throws Throwable
	{
    	PVStructure pvRequest = pvDataCreate.createPVStructure(null, "", new Field[0]);
        Field newField = fieldCreate.createScalar("value", ScalarType.pvString);
        PVString pvString = (PVString)pvDataCreate.createPVField(pvRequest, newField);
        pvString.put("value");
        pvRequest.appendPVField(pvString);

		ChannelPutRequesterImpl channelPutRequester = new ChannelPutRequesterImpl();
		ch.createChannelPut(channelPutRequester, pvRequest, "put-test", share, true, null);
		channelPutRequester.waitAndCheckConnect();
		
		assertEquals("put-test", channelPutRequester.pvStructure.getFullName());

		// set and get test
		PVInt value = channelPutRequester.pvStructure.getIntField("value");
		assertNotNull(value);
		final int INIT_VAL = 3;
		value.put(INIT_VAL);
		channelPutRequester.bitSet.set(value.getFieldOffset());
		
		channelPutRequester.syncPut(false);
		// TODO should put bitSet be reset here
		//assertEquals(0, channelPutRequester.bitSet.cardinality());
		channelPutRequester.syncGet();
		assertEquals(INIT_VAL+1, value.get());	// +1 due to process


		// value should change only due to process
		value.put(INIT_VAL+3);
		channelPutRequester.bitSet.clear();
		channelPutRequester.syncPut(false);
		channelPutRequester.syncGet();
		assertEquals(INIT_VAL+2, value.get());
		
		// destroy
		channelPutRequester.syncPut(true);

		/*
		// TODO check when error handling is implemented
		channelPutRequester.channelPut.destroy();
		channelPutRequester.syncPut(true);
		*/
	}

	public void testChannelGetField() throws Throwable
	{
	    Channel ch = syncCreateChannel("simpleCounter");
	
		GetFieldRequesterImpl channelGetField = new GetFieldRequesterImpl();
		
		// get all
		channelGetField.syncGetField(ch, null);
		assertNotNull(channelGetField.field);
		assertEquals(Type.structure, channelGetField.field.getType());
		// TODO there is no name
		// assertEquals(ch.getChannelName(), channelGetField.field.getFieldName());

		// value only
		channelGetField.syncGetField(ch, "value");
		assertNotNull(channelGetField.field);
		assertEquals(Type.scalar, channelGetField.field.getType());
		assertEquals("value", channelGetField.field.getFieldName());

		// non-existant
		channelGetField.syncGetField(ch, "invalid");
		assertNull(channelGetField.field);
	}
	
	public void testChannelProcess() throws Throwable
	{
	    Channel ch = syncCreateChannel("simpleCounter");

		// create get to check processing
    	PVStructure pvRequest = pvDataCreate.createPVStructure(null, "", new Field[0]);
        Field newField = fieldCreate.createScalar("value", ScalarType.pvString);
        PVString pvString = (PVString)pvDataCreate.createPVField(pvRequest, newField);
        pvString.put("value");
        pvRequest.appendPVField(pvString);

		ChannelGetRequesterImpl channelGetRequester = new ChannelGetRequesterImpl();
		ch.createChannelGet(channelGetRequester, pvRequest, "get-process-test", true, false, null);
		channelGetRequester.waitAndCheckConnect();
		
		// get initial state
		channelGetRequester.syncGet(false);

		// create process
		ChannelProcessRequesterImpl channelProcessRequester = new ChannelProcessRequesterImpl();
		ch.createChannelProcess(channelProcessRequester, null);
		channelProcessRequester.waitAndCheckConnect();
		
		// there should be no changes
		channelGetRequester.syncGet(false);
		assertEquals(0, channelGetRequester.bitSet.cardinality());
		
		channelProcessRequester.syncProcess(false);

		// there should be a change
		channelGetRequester.syncGet(false);
		assertEquals(1, channelGetRequester.bitSet.cardinality());
		
		// now let's try to create another processor :)
		ChannelProcessRequesterImpl channelProcessRequester2 = new ChannelProcessRequesterImpl();
		ch.createChannelProcess(channelProcessRequester2, null);
		channelProcessRequester2.waitAndCheckConnect();
		
		// and process
		channelProcessRequester2.syncProcess(false);

		// there should be a change
		channelGetRequester.syncGet(false);
		assertEquals(1, channelGetRequester.bitSet.cardinality());
		
		// TODO since there is no good error handling I do not know that creating of second process failed !!!
		// however it shoudn't, right!!!
		
		// check if process works with destroy option
		channelProcessRequester.syncProcess(true);

		// there should be a change
		channelGetRequester.syncGet(false);
		assertEquals(1, channelGetRequester.bitSet.cardinality());

		/*
		// TODO check when error handling is implemented
		channelProcessRequester.channelProcess.destroy();
		channelProcessRequester.syncProcess(true);
		*/
	}
	
	public void testChannelTwoGetProcess() throws Throwable
	{
	    Channel ch = syncCreateChannel("simpleCounter");

		// create gets to check processing
    	PVStructure pvRequest = pvDataCreate.createPVStructure(null, "", new Field[0]);
        Field newField = fieldCreate.createScalar("value", ScalarType.pvString);
        PVString pvString = (PVString)pvDataCreate.createPVField(pvRequest, newField);
        pvString.put("value");
        pvRequest.appendPVField(pvString);

		ChannelGetRequesterImpl channelGetRequester = new ChannelGetRequesterImpl();
		ch.createChannelGet(channelGetRequester, pvRequest, "get-process-test", true, true, null);
		channelGetRequester.waitAndCheckConnect();
		
		// get initial state
		channelGetRequester.syncGet(false);
		
		// there should be a change
		channelGetRequester.syncGet(false);
		assertEquals(1, channelGetRequester.bitSet.cardinality());

		// another get
		ChannelGetRequesterImpl channelGetRequester2 = new ChannelGetRequesterImpl();
		ch.createChannelGet(channelGetRequester2, pvRequest, "get-process-test-2", true, true, null);
		channelGetRequester2.waitAndCheckConnect();
		
		// get initial state
		channelGetRequester2.syncGet(false);

		// there should be a change too
		channelGetRequester2.syncGet(false);
		assertEquals(1, channelGetRequester2.bitSet.cardinality());

		// TODO since there is no good error handling I do not know that creating of second process failed !!!
		// however it shoudn't, right!!!
	}

	public void testChannelPutGet() throws Throwable
	{
	    Channel ch = syncCreateChannel("valueOnly");
	
		channelPutGetTestNoProcess(ch, false);
		//channelPutGetTestNoProcess(ch, true);
		
		ch.destroy();
		
	    ch = syncCreateChannel("simpleCounter");
		channelPutGetTestIntProcess(ch, false);
		//channelPutGetTestIntProcess(ch, true);
	}
	
	private void channelPutGetTestNoProcess(Channel ch, boolean share) throws Throwable
	{
    	PVStructure pvRequest = pvDataCreate.createPVStructure(null, "", new Field[0]);
        Field newField = fieldCreate.createScalar("value", ScalarType.pvString);
        PVString pvString = (PVString)pvDataCreate.createPVField(pvRequest, newField);
        pvString.put("value");
        pvRequest.appendPVField(pvString);

    	PVStructure pvGetRequest = pvDataCreate.createPVStructure(null, "", new Field[0]);
        newField = fieldCreate.createScalar("timeStamp", ScalarType.pvString);
        pvString = (PVString)pvDataCreate.createPVField(pvRequest, newField);
        pvString.put("timeStamp");
        pvGetRequest.appendPVField(pvString);
        newField = fieldCreate.createScalar("value", ScalarType.pvString);
        pvString = (PVString)pvDataCreate.createPVField(pvRequest, newField);
        pvString.put("value");
        pvGetRequest.appendPVField(pvString);

        ChannelPutGetRequesterImpl channelPutGetRequester = new ChannelPutGetRequesterImpl();
		ch.createChannelPutGet(channelPutGetRequester, pvRequest, "put-test", share, pvGetRequest, "get-test", share, false, null);
		channelPutGetRequester.waitAndCheckConnect();
		
		assertEquals("put-test", channelPutGetRequester.pvPutStructure.getFullName());
		assertEquals("get-test", channelPutGetRequester.pvGetStructure.getFullName());

		// set and get test
		PVDouble putValue = channelPutGetRequester.pvPutStructure.getDoubleField("value");
		assertNotNull(putValue);
		final double INIT_VAL = 321.0;
		putValue.put(INIT_VAL);
		
		PVDouble getValue = channelPutGetRequester.pvGetStructure.getDoubleField("value");
		assertNotNull(getValue);

		
		channelPutGetRequester.syncPutGet(false);
		assertEquals(INIT_VAL, getValue.get());

		// again...
		putValue.put(INIT_VAL+1);
		channelPutGetRequester.syncPutGet(false);
		assertEquals(INIT_VAL+1, getValue.get());

		// test get-put
		channelPutGetRequester.syncGetPut();
		// TODO
		
		// test get-get
		channelPutGetRequester.syncGetGet();
		// TODO
		
		// destroy
		channelPutGetRequester.syncPutGet(true);

		/*
		// TODO check when error handling is implemented
		channelPutGetRequester.channelPutGet.destroy();
		channelPutGetRequester.syncPutGet(true);
		*/
	}

	private void channelPutGetTestIntProcess(Channel ch, boolean share) throws Throwable
	{
    	PVStructure pvRequest = pvDataCreate.createPVStructure(null, "", new Field[0]);
        Field newField = fieldCreate.createScalar("value", ScalarType.pvString);
        PVString pvString = (PVString)pvDataCreate.createPVField(pvRequest, newField);
        pvString.put("value");
        pvRequest.appendPVField(pvString);

    	PVStructure pvGetRequest = pvDataCreate.createPVStructure(null, "", new Field[0]);
        newField = fieldCreate.createScalar("timeStamp", ScalarType.pvString);
        pvString = (PVString)pvDataCreate.createPVField(pvRequest, newField);
        pvString.put("timeStamp");
        pvGetRequest.appendPVField(pvString);
        newField = fieldCreate.createScalar("value", ScalarType.pvString);
        pvString = (PVString)pvDataCreate.createPVField(pvRequest, newField);
        pvString.put("value");
        pvGetRequest.appendPVField(pvString);

        ChannelPutGetRequesterImpl channelPutGetRequester = new ChannelPutGetRequesterImpl();
		ch.createChannelPutGet(channelPutGetRequester, pvRequest, "put-test", share, pvGetRequest, "get-test", share, true, null);
		channelPutGetRequester.waitAndCheckConnect();
		
		assertEquals("put-test", channelPutGetRequester.pvPutStructure.getFullName());
		assertEquals("get-test", channelPutGetRequester.pvGetStructure.getFullName());

		// set and get test
		PVInt putValue = channelPutGetRequester.pvPutStructure.getIntField("value");
		assertNotNull(putValue);
		final int INIT_VAL = 3;
		putValue.put(INIT_VAL);
		
		PVInt getValue = channelPutGetRequester.pvGetStructure.getIntField("value");
		assertNotNull(getValue);
		TimeStamp timestamp = TimeStampFactory.getTimeStamp(channelPutGetRequester.pvGetStructure.getStructureField("timeStamp"));
		assertNotNull(timestamp);

		// get all
		channelPutGetRequester.syncGetGet();
		// TODO what should happen with getBitSet?!!!
		
		// multiple tests 
		final int TIMES = 3;
		for (int i = 1; i <= TIMES; i++)
		{
			int previousValue = getValue.get();
			long previousTimestampSec = timestamp.getSecondsPastEpoch();
			
			putValue.put(previousValue + 1);
			
			// 2 seconds to have different timestamps
			Thread.sleep(1000);
			
			channelPutGetRequester.syncPutGet(i == TIMES);
			// changes of value and timeStamp
			assertEquals((previousValue + 1 + 1)%11, getValue.get());	// +1 (new value) +1 (process)
			assertTrue(timestamp.getSecondsPastEpoch() > previousTimestampSec);
		}

		/*
		// TODO check when error handling is implemented
		channelPutGetRequester.channelPutGet.destroy();
		channelPutGetRequester.syncPutGet(true);
		*/
	}
	
	public void testChannelArray() throws Throwable
	{
	    Channel ch = syncCreateChannel("simpleCounter");
	    
	    ChannelArrayRequesterImpl channelArrayRequester = new ChannelArrayRequesterImpl();
	    ch.createChannelArray(channelArrayRequester, "alarm.severity.choices", null);
	    channelArrayRequester.waitAndCheckConnect();
	    
	    // test get
	    PVStringArray array = (PVStringArray)channelArrayRequester.pvArray;
	    StringArrayData data = new StringArrayData();
	    channelArrayRequester.syncGet(true, 1, 2);
	    int count = array.get(0, 100, data);
	    assertEquals(2, count);
	    assertTrue(Arrays.equals(new String[] { "minor", "major" }, data.data));
	 
	    ch.destroy();

	    /*
		// TODO check when error handling is implemented
		channelArrayRequester.channelArray.destroy();
		channelArrayRequester.syncGet(true);
		*/
	    
	    ch = syncCreateChannel("arrayValueOnly");
	    channelArrayRequester = new ChannelArrayRequesterImpl();
	    ch.createChannelArray(channelArrayRequester, "value", null);
	    channelArrayRequester.waitAndCheckConnect();
	    
	    // test put
	    PVDoubleArray doubleArray = (PVDoubleArray)channelArrayRequester.pvArray;
	    final double[] ARRAY_VALUE = new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 }; 
	    doubleArray.put(0, ARRAY_VALUE.length, ARRAY_VALUE, 0);
	    channelArrayRequester.syncPut(false, 0, -1);
	    channelArrayRequester.syncGet(false, 0, -1);
	    DoubleArrayData doubleData = new DoubleArrayData();
	    count = doubleArray.get(0, 100, doubleData);
	    assertEquals(ARRAY_VALUE.length, count);
	    for (int i = 0; i < count; i++)
	    	assertEquals(ARRAY_VALUE[i], doubleData.data[i]);
	    
	    channelArrayRequester.syncPut(false, 4, -1);
	    channelArrayRequester.syncGet(false, 3, 3);
	    count = doubleArray.get(0, 3, doubleData);
	    assertEquals(3, count);
	    final double[] EXPECTED_VAL = new double[] { 4.4, 1.1, 2.2 };
	    for (int i = 0; i < count; i++)
	    	assertEquals(EXPECTED_VAL[i], doubleData.data[i]);
	}
	
    private static final Convert convert = ConvertFactory.getConvert();

	public void testChannelMonitors() throws Throwable
	{
    	PVStructure pvRequest = pvDataCreate.createPVStructure(null, "", new Field[0]);
        Field newField = fieldCreate.createScalar("timeStamp", ScalarType.pvString);
        PVString pvString = (PVString)pvDataCreate.createPVField(pvRequest, newField);
        pvString.put("timeStamp");
        pvRequest.appendPVField(pvString);
        newField = fieldCreate.createScalar("value", ScalarType.pvString);
        pvString = (PVString)pvDataCreate.createPVField(pvRequest, newField);
        pvString.put("value");
        pvRequest.appendPVField(pvString);
        // add something static
        newField = fieldCreate.createScalar("alarm.severity.choices", ScalarType.pvString);
        pvString = (PVString)pvDataCreate.createPVField(pvRequest, newField);
        pvString.put("alarm.severity.choices");
        pvRequest.appendPVField(pvString);

        PVStructure pvOption = pvDataCreate.createPVStructure(null, "pvOption", new Field[0]);
        PVString pvAlgorithm = (PVString)pvDataCreate.createPVScalar(pvOption, "algorithm", ScalarType.pvString);
        pvAlgorithm.put("onChange");		// TODO constant!!!
        pvOption.appendPVField(pvAlgorithm);
        PVInt pvQueueSize = (PVInt)pvDataCreate.createPVScalar(pvOption, "queueSize", ScalarType.pvInt);
        pvQueueSize.put(10);
        pvOption.appendPVField(pvQueueSize);

        Channel ch = syncCreateChannel("counter");
		
	    ChannelMonitorRequesterImpl channelMonitorRequester = new ChannelMonitorRequesterImpl();
	    ch.createMonitor(channelMonitorRequester, pvRequest, "monitor-test", pvOption);
	    channelMonitorRequester.waitAndCheckConnect();

	    // TODO currently we get no pvStructure until first monitor
	    
	    // not start, no monitors
	    assertEquals(0, channelMonitorRequester.monitorCounter.get());
	    
	    synchronized (channelMonitorRequester) {
		    channelMonitorRequester.channelMonitor.start();
		    
		    channelMonitorRequester.wait(TIMEOUT_MS);
		    // first monitor, entire structure
		    assertEquals("monitor-test", channelMonitorRequester.pvStructure.getFullName());
		    assertEquals(1, channelMonitorRequester.monitorCounter.get());
		    assertEquals(1, channelMonitorRequester.changeBitSet.cardinality());
		    assertTrue(channelMonitorRequester.changeBitSet.get(0));

		    PVField valueField = channelMonitorRequester.pvStructure.getSubField("value");
		    PVField previousValue = pvDataCreate.createPVField(null, valueField.getField());
		    convert.copy(valueField, previousValue);
		    assertTrue(valueField.equals(previousValue));
		    
		    // all subsequent only timestamp and value
		    for (int i = 2; i < 5; i++) {
			    channelMonitorRequester.wait(TIMEOUT_MS);
			    assertEquals("monitor-test", channelMonitorRequester.pvStructure.getFullName());
			    assertEquals(i, channelMonitorRequester.monitorCounter.get());
			    assertEquals(2, channelMonitorRequester.changeBitSet.cardinality());
			    assertTrue(channelMonitorRequester.changeBitSet.get(1));
			    assertTrue(channelMonitorRequester.changeBitSet.get(4));
			    
			    valueField = channelMonitorRequester.pvStructure.getSubField("value");
			    assertFalse(valueField.equals(previousValue));
			    convert.copy(valueField, previousValue);
		    }

		    channelMonitorRequester.channelMonitor.stop();
		    channelMonitorRequester.wait(500);
		    int mc = channelMonitorRequester.monitorCounter.get();
		    Thread.sleep(2000);
		    // no more monitors
		    assertEquals(mc, channelMonitorRequester.monitorCounter.get());
	    }
	    
	    
	}
	
}
