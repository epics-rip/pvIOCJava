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

import junit.framework.TestCase;

import org.epics.ioc.channelAccess.ChannelAccessFactory;
import org.epics.ioc.install.Install;
import org.epics.ioc.install.InstallFactory;
import org.epics.pvData.channelAccess.Channel;
import org.epics.pvData.channelAccess.ChannelGet;
import org.epics.pvData.channelAccess.ChannelGetRequester;
import org.epics.pvData.channelAccess.ChannelProcess;
import org.epics.pvData.channelAccess.ChannelProcessRequester;
import org.epics.pvData.channelAccess.ChannelProvider;
import org.epics.pvData.channelAccess.ChannelPut;
import org.epics.pvData.channelAccess.ChannelPutGet;
import org.epics.pvData.channelAccess.ChannelPutGetRequester;
import org.epics.pvData.channelAccess.ChannelPutRequester;
import org.epics.pvData.channelAccess.ChannelRequester;
import org.epics.pvData.channelAccess.GetFieldRequester;
import org.epics.pvData.factory.FieldFactory;
import org.epics.pvData.factory.PVDataFactory;
import org.epics.pvData.misc.BitSet;
import org.epics.pvData.property.TimeStamp;
import org.epics.pvData.property.TimeStampFactory;
import org.epics.pvData.pv.Field;
import org.epics.pvData.pv.FieldCreate;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVDataCreate;
import org.epics.pvData.pv.PVDouble;
import org.epics.pvData.pv.PVInt;
import org.epics.pvData.pv.PVString;
import org.epics.pvData.pv.PVStructure;
import org.epics.pvData.pv.Requester;
import org.epics.pvData.pv.ScalarType;
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
		public void destroy(org.epics.pvData.channelAccess.Channel c) {
			// TODO Auto-generated method stub
		}
		
		@Override
		public void channelStateChange(org.epics.pvData.channelAccess.Channel c,
				boolean isConnected) {
			// TODO Auto-generated method stub
		}
		
		@Override
		public synchronized void channelNotCreated() {
			this.notifyAll();
		}
		
		@Override
		public synchronized void channelCreated(org.epics.pvData.channelAccess.Channel channel) {
			this.channel = channel;
			this.notifyAll();
		}
	};
	

	private Channel syncCreateChannel(String name)
	{
		ChannelRequesterImpl cr = new ChannelRequesterImpl();
		synchronized (cr) {
			provider.createChannel(name, cr);
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
		BitSet putBitSet;
		PVStructure pvGetStructure;
		BitSet getBitSet;

		private Boolean connected = null;
		private Boolean success = null;

		/* (non-Javadoc)
		 * @see org.epics.pvData.channelAccess.ChannelPutGetRequester#channelPutGetConnect(org.epics.pvData.channelAccess.ChannelPutGet, org.epics.pvData.pv.PVStructure, org.epics.pvData.pv.PVStructure)
		 */
		@Override
		public void channelPutGetConnect(
				ChannelPutGet channelPutGet,
				PVStructure pvPutStructure, BitSet putBitSet,
				PVStructure pvGetStructure, BitSet getBitSet) {
			synchronized (this)
			{
				this.channelPutGet = channelPutGet;
				this.pvPutStructure = pvPutStructure;
				this.putBitSet = putBitSet;
				this.pvGetStructure = pvGetStructure;
				this.getBitSet = getBitSet;

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
				assertNotNull(putBitSet);
				assertNotNull(pvGetStructure);
				assertNotNull(getBitSet);
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
				System.out.println("=================> " + field);
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
		ch.createChannelGet(ch, channelGetRequester, pvRequest, "get-test", share, false);
		channelGetRequester.waitAndCheckConnect();
		
		// TODO not implemented
		//assertEquals("get-test", channelGetRequester.pvStructure.getFullName());
		
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
		ch.createChannelGet(ch, channelGetRequester, pvRequest, "get-test", share, true);
		channelGetRequester.waitAndCheckConnect();
		
		// TODO not implemented
		//assertEquals("get-test", channelGetRequester.pvStructure.getFullName());
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
		ch.createChannelPut(ch, channelPutRequester, pvRequest, "put-test", share, false);
		channelPutRequester.waitAndCheckConnect();
		
		// TODO not implemented
		//assertEquals("put-test", channelPutRequester.pvStructure.getFullName());

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
		ch.createChannelPut(ch, channelPutRequester, pvRequest, "put-test", share, true);
		channelPutRequester.waitAndCheckConnect();
		
		// TODO not implemented
		//assertEquals("put-test", channelPutRequester.pvStructure.getFullName());

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
		ch.createChannelGet(ch, channelGetRequester, pvRequest, "get-process-test", true, false);
		channelGetRequester.waitAndCheckConnect();
		
		// get initial state
		channelGetRequester.syncGet(false);

		// create process
		ChannelProcessRequesterImpl channelProcessRequester = new ChannelProcessRequesterImpl();
		ch.createChannelProcess(ch, channelProcessRequester);
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
		ch.createChannelProcess(ch, channelProcessRequester2);
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
		ch.createChannelGet(ch, channelGetRequester, pvRequest, "get-process-test", true, true);
		channelGetRequester.waitAndCheckConnect();
		
		// get initial state
		channelGetRequester.syncGet(false);
		
		// there should be a change
		channelGetRequester.syncGet(false);
		assertEquals(1, channelGetRequester.bitSet.cardinality());

		// another get
		ChannelGetRequesterImpl channelGetRequester2 = new ChannelGetRequesterImpl();
		ch.createChannelGet(ch, channelGetRequester2, pvRequest, "get-process-test-2", true, true);
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
		ch.createChannelPutGet(ch, channelPutGetRequester, pvRequest, "put-test", share, pvGetRequest, "get-test", share, false);
		channelPutGetRequester.waitAndCheckConnect();
		
		// TODO not implemented
		//assertEquals("put-test", channelPutGetRequester.pvPutStructure.getFullName());
		//assertEquals("get-test", channelPutGetRequester.pvGetStructure.getFullName());

		// set and get test
		PVDouble putValue = channelPutGetRequester.pvPutStructure.getDoubleField("value");
		assertNotNull(putValue);
		final double INIT_VAL = 321.0;
		putValue.put(INIT_VAL);
		channelPutGetRequester.putBitSet.set(putValue.getFieldOffset());
		
		PVDouble getValue = channelPutGetRequester.pvGetStructure.getDoubleField("value");
		assertNotNull(getValue);

		
		channelPutGetRequester.syncPutGet(false);
		// TODO should put bitSet be reset here
		//assertEquals(0, channelPutGetRequester.putBitSet.cardinality());
		assertEquals(INIT_VAL, getValue.get());
		assertEquals(1, channelPutGetRequester.getBitSet.cardinality());


		// value should not change since bitSet is not set
		putValue.put(INIT_VAL+1);
		channelPutGetRequester.putBitSet.clear();
		channelPutGetRequester.syncPutGet(false);
		assertEquals(INIT_VAL, getValue.get());
		assertEquals(0, channelPutGetRequester.getBitSet.cardinality());
		
		// now should change
		channelPutGetRequester.putBitSet.set(putValue.getFieldOffset());
		channelPutGetRequester.syncPutGet(false);
		assertEquals(INIT_VAL+1, getValue.get());
		assertEquals(1, channelPutGetRequester.getBitSet.cardinality());

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
		ch.createChannelPutGet(ch, channelPutGetRequester, pvRequest, "put-test", share, pvGetRequest, "get-test", share, true);
		channelPutGetRequester.waitAndCheckConnect();
		
		// TODO not implemented
		//assertEquals("put-test", channelPutGetRequester.pvPutStructure.getFullName());
		//assertEquals("get-test", channelPutGetRequester.pvGetStructure.getFullName());

		// set and get test
		PVInt putValue = channelPutGetRequester.pvPutStructure.getIntField("value");
		assertNotNull(putValue);
		final int INIT_VAL = 3;
		putValue.put(INIT_VAL);
		channelPutGetRequester.putBitSet.set(putValue.getFieldOffset());
		
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
			channelPutGetRequester.putBitSet.set(putValue.getFieldOffset());
			
			// 2 seconds to have different timestamps
			Thread.sleep(1000);
			
			channelPutGetRequester.syncPutGet(i == TIMES);
			// changes of value and timeStamp
			assertEquals((previousValue + 1 + 1)%11, getValue.get());	// +1 (new value) +1 (process)
			assertTrue(timestamp.getSecondsPastEpoch() > previousTimestampSec);
		}

		// destroy
		channelPutGetRequester.syncPutGet(true);

		/*
		// TODO check when error handling is implemented
		channelPutGetRequester.channelPutGet.destroy();
		channelPutGetRequester.syncPutGet(true);
		*/
	}
	
	
}
