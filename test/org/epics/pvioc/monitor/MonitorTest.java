/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS pvData is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.monitor;

import junit.framework.TestCase;

import org.epics.pvaccess.client.CreateRequestFactory;
import org.epics.pvdata.misc.BitSet;
import org.epics.pvdata.monitor.Monitor;
import org.epics.pvdata.monitor.MonitorElement;
import org.epics.pvdata.monitor.MonitorRequester;
import org.epics.pvdata.pv.MessageType;
import org.epics.pvdata.pv.PVByte;
import org.epics.pvdata.pv.PVDouble;
import org.epics.pvdata.pv.PVInt;
import org.epics.pvdata.pv.PVLong;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.Requester;
import org.epics.pvdata.pv.Status;
import org.epics.pvdata.pv.Structure;
import org.epics.pvioc.database.PVDatabase;
import org.epics.pvioc.database.PVDatabaseFactory;
import org.epics.pvioc.database.PVRecord;
import org.epics.pvioc.database.PVReplaceFactory;
import org.epics.pvioc.xml.XMLToPVDatabaseFactory;



/**
 * JUnit test for pvAccess.
 * It also provides examples of how to use the pvAccess interfaces.
 * @author mrk
 *
 */
public class MonitorTest extends TestCase {
    private static PVDatabase master = PVDatabaseFactory.getMaster();
    private static final Requester requester = new RequesterImpl();
    
    private static class RequesterImpl implements Requester {
		@Override
		public String getRequesterName() {
			return "pvCopyTest";
		}
		@Override
		public void message(String message, MessageType messageType) {
		    System.out.printf("message %s messageType %s%n",message,messageType.name());
			
		}
    }
    
    public static void testPVCopy() {
     // get database for testing
        Requester iocRequester = new RequesterForTesting("accessTest");
        XMLToPVDatabaseFactory.convert(master,"${JAVAIOC}/xml/structures.xml", iocRequester);
        XMLToPVDatabaseFactory.convert(master,"${JAVAIOC}/test/org/epics/pvioc/monitor/power.xml", iocRequester);
        PVReplaceFactory.replace(master);
        simpleTest();
        timeStampTest();
        queueTest();
        deadbandTest();
        deadbandPercentTest();
        periodicTest();
    }
    
    public static void simpleTest() {
         System.out.printf("%n**** simpleTest ****%n");
         PVRecord pvRecord = master.findRecord("simple");
//System.out.println(pvRecord);
         PVStructure pvStructure = pvRecord.getPVRecordStructure().getPVStructure();
         PVByte pvValue = pvStructure.getByteField("value");
         assertTrue(pvValue!=null);
         PVLong pvRecordSeconds = (PVLong)pvStructure.getSubField("timeStamp.secondsPastEpoch");
         PVInt pvRecordNanoSeconds = (PVInt)pvStructure.getSubField("timeStamp.nanoSeconds");
         PVInt pvRecordUserTag = (PVInt)pvStructure.getSubField("timeStamp.userTag");
         String request = "record[queueSize=1]field(value)";
 //System.out.println("pvRecord " + pvRecord.getPVRecordStructure().getPVField());
         PVStructure pvRequest = CreateRequestFactory.createRequest(request,requester);
 System.out.println("request:" + request);
 System.out.println("pvRequest:" + pvRequest);
         MonitorRequesterImpl monitorRequester = new  MonitorRequesterImpl(pvRecord,pvRequest);
         MonitorElement monitorElement = monitorRequester.poll();
         assertTrue(monitorElement!=null);
         PVStructure pvCopy = monitorElement.getPVStructure();
//System.out.println("pvCopy " + pvCopy);
         BitSet change = monitorElement.getChangedBitSet();
         BitSet overrun = monitorElement.getOverrunBitSet();
 //System.out.println("pvCopy " + pvCopy);
 //System.out.println("change " + change);
 //System.out.println("overrun " + overrun);
         assertTrue(change.get(0));
         assertTrue(overrun.isEmpty());
         change.clear(0);
         assertTrue(change.isEmpty());
         monitorRequester.release();
         pvRecord.beginGroupPut();
         pvValue.put((byte)5);
         pvRecord.endGroupPut();
         monitorElement = monitorRequester.poll();
         assertTrue(monitorElement!=null);
         pvCopy = monitorElement.getPVStructure();
//System.out.println("pvCopy " + pvCopy);
         
         request = "record[queueSize=1]field(value,timeStamp)";
         //System.out.println("pvRecord " + pvRecord.getPVRecordStructure().getPVField());
         pvRequest = CreateRequestFactory.createRequest(request,requester);
//System.out.println("request:" + request);
//System.out.println("pvRequest:" + pvRequest);
         monitorRequester = new  MonitorRequesterImpl(pvRecord,pvRequest);
         monitorElement = monitorRequester.poll();
         assertTrue(monitorElement!=null);
         pvCopy = monitorElement.getPVStructure();
//System.out.println("pvCopy " + pvCopy);
         change = monitorElement.getChangedBitSet();
         overrun = monitorElement.getOverrunBitSet();
//System.out.println("pvCopy " + pvCopy);
//System.out.println("change " + change);
//System.out.println("overrun " + overrun);
         assertTrue(change.get(0));
         assertTrue(overrun.isEmpty());
         change.clear(0);
         assertTrue(change.isEmpty());
         monitorRequester.release();
         pvRecord.beginGroupPut();
         pvValue.put((byte)10);
         pvRecordSeconds.put(100000L);
         pvRecordNanoSeconds.put(1000000000);
         pvRecordUserTag.put(1);
         pvRecord.endGroupPut();
         monitorElement = monitorRequester.poll();
         assertTrue(monitorElement!=null);
         change = monitorElement.getChangedBitSet();
         overrun = monitorElement.getOverrunBitSet();
 //System.out.println("change " + change);
 //System.out.println("overrun " + overrun);
         pvCopy = monitorElement.getPVStructure();
         System.out.println("pvCopy " + pvCopy);
         change.clear(0);
         pvRecord.beginGroupPut();
         pvValue.put((byte)10);
         pvRecord.endGroupPut();
         monitorElement = monitorRequester.poll();
         assertTrue(monitorElement!=null);
         change = monitorElement.getChangedBitSet();
         overrun = monitorElement.getOverrunBitSet();
 //System.out.println("change " + change);
 //System.out.println("overrun " + overrun);
         pvCopy = monitorElement.getPVStructure();
         System.out.println("pvCopy " + pvCopy);
     }
    
    public static void timeStampTest() {
        System.out.printf("%n****timeStamp Test****%n");
        PVRecord pvRecord = master.findRecord("powerWithoutDeadband");
//System.out.println(pvRecord);
        PVStructure pvStructure = pvRecord.getPVRecordStructure().getPVStructure();
        PVLong pvRecordSeconds = (PVLong)pvStructure.getSubField("timeStamp.secondsPastEpoch");
        PVInt pvRecordNanoSeconds = (PVInt)pvStructure.getSubField("timeStamp.nanoSeconds");
        PVInt pvRecordUserTag = (PVInt)pvStructure.getSubField("timeStamp.userTag");
        PVDouble pvRecordPowerValue = (PVDouble)pvStructure.getSubField("power.value");
        String request = "record[queueSize=1]field(alarm,timeStamp[algorithm=onChange,causeMonitor=true],power.value)";
        PVStructure pvRequest = CreateRequestFactory.createRequest(request,requester);
//System.out.println("request:" + request);
//System.out.println("pvRequest:" + pvRequest);
        MonitorRequesterImpl monitorRequester = new  MonitorRequesterImpl(pvRecord,pvRequest);
        MonitorElement monitorElement = monitorRequester.poll();
        assertTrue(monitorElement!=null);
        PVStructure pvCopy = monitorElement.getPVStructure();
        PVStructure pvTimeStamp = pvCopy.getStructureField("timeStamp");
        int timeStampOffset = pvTimeStamp.getFieldOffset();
        PVDouble pvValue = pvCopy.getDoubleField("value");
        BitSet change = monitorElement.getChangedBitSet();
        BitSet overrun = monitorElement.getOverrunBitSet();
//System.out.println("pvCopy " + pvCopy);
//System.out.println("change " + change);
//System.out.println("overrun " + overrun);
        assertTrue(change.get(0));
        assertTrue(overrun.isEmpty());
        change.clear(0);
        assertTrue(change.isEmpty());
        monitorRequester.release();
        pvRecord.beginGroupPut();
        pvRecordSeconds.put(5);
        pvRecordNanoSeconds.put(0);
        pvRecordUserTag.put(1);
        pvRecord.endGroupPut();
        monitorElement = monitorRequester.poll();
        assertTrue(monitorElement!=null);
        pvCopy = monitorElement.getPVStructure();
        change = monitorElement.getChangedBitSet();
        overrun = monitorElement.getOverrunBitSet();
//System.out.println("change " + change);
//System.out.println("overrun " + overrun);
        assertTrue(change.get(timeStampOffset));
        change.clear(timeStampOffset);
        assertTrue(change.isEmpty());
        assertTrue(overrun.isEmpty());
//System.out.println(pvRecordPowerValue);
//System.out.println(pvValue);
        assertTrue(pvRecordPowerValue.get()==pvValue.get());
        monitorRequester.release();
        monitorElement = monitorRequester.poll();
        assertTrue(monitorElement==null);
        monitorRequester.destroy();
    }
    
    public static void queueTest() {
        System.out.printf("%n****Queue Test****%n");
        // definitions for request structure to pass to PVCopyFactory
        PVRecord pvRecord = master.findRecord("powerWithoutDeadband");
        PVStructure pvStructure = pvRecord.getPVRecordStructure().getPVStructure();
        PVLong pvRecordSeconds = (PVLong)pvStructure.getSubField("timeStamp.secondsPastEpoch");
        PVInt pvRecordNanoSeconds = (PVInt)pvStructure.getSubField("timeStamp.nanoSeconds");
        PVInt pvRecordUserTag = (PVInt)pvStructure.getSubField("timeStamp.userTag");
        PVDouble pvRecordPowerValue = (PVDouble)pvStructure.getSubField("power.value");
        String request = "alarm,timeStamp,power.value";
//System.out.println("pvRecord " + pvRecord);
        PVStructure pvRequest = CreateRequestFactory.createRequest(request,requester);
//System.out.println("request:" + request);
//System.out.println("pvRequest:" + pvRequest);
        MonitorRequesterImpl monitorRequester = new  MonitorRequesterImpl(pvRecord,pvRequest);
        MonitorElement monitorElement = monitorRequester.poll();
        assertTrue(monitorElement!=null);
        PVStructure pvCopy = monitorElement.getPVStructure();
        BitSet change = monitorElement.getChangedBitSet();
        BitSet overrun = monitorElement.getOverrunBitSet();
        PVStructure pvTimeStamp = pvCopy.getStructureField("timeStamp");
        int timeStampOffset = pvTimeStamp.getFieldOffset();
        PVDouble pvValue = pvCopy.getDoubleField("value");
        int valueOffset = pvValue.getFieldOffset();
//System.out.println("pvCopy " + pvCopy);
//System.out.println("change " + change);
//System.out.println("overrun " + overrun);
        assertTrue(change.get(0));
        assertTrue(overrun.isEmpty());
        change.clear(0);
        assertTrue(change.isEmpty());
        monitorRequester.release();
        pvRecord.beginGroupPut();
        pvRecordSeconds.put(5);
        pvRecordNanoSeconds.put(0);
        pvRecordUserTag.put(1);
        pvRecordPowerValue.put(5.0);
        pvRecord.endGroupPut();
        monitorElement = monitorRequester.poll();
        assertTrue(monitorElement!=null);
        pvCopy = monitorElement.getPVStructure();
        pvValue = pvCopy.getDoubleField("value");
        change = monitorElement.getChangedBitSet();
        overrun = monitorElement.getOverrunBitSet();
//System.out.println("change " + change);
//System.out.println("overrun " + overrun);
        assertTrue(change.get(timeStampOffset));
        change.clear(timeStampOffset);
        assertTrue(change.get(valueOffset));
        change.clear(valueOffset);
        assertTrue(change.isEmpty());
        assertTrue(overrun.isEmpty());
        assertTrue(pvRecordPowerValue.get()==pvValue.get());
        monitorRequester.release();
        pvRecordPowerValue.put(6.0);
        pvRecordPowerValue.put(7.0);
        pvRecordPowerValue.put(8.0);
        monitorElement = monitorRequester.poll();
        assertTrue(monitorElement!=null);
        pvCopy = monitorElement.getPVStructure();
        change = monitorElement.getChangedBitSet();
        overrun = monitorElement.getOverrunBitSet();
        pvValue = pvCopy.getDoubleField("value");
//System.out.println("change " + change);
//System.out.println("overrun " + overrun);
        assertTrue(change.get(valueOffset));
        change.clear(valueOffset);
        assertTrue(change.isEmpty());
        assertTrue(pvValue.get()==6.0);
        monitorRequester.release();
        monitorElement = monitorRequester.poll();
        assertTrue(monitorElement!=null);
        pvCopy = monitorElement.getPVStructure();
        pvValue = pvCopy.getDoubleField("value");
        change = monitorElement.getChangedBitSet();
        overrun = monitorElement.getOverrunBitSet();
//System.out.println("change " + change);
//System.out.println("overrun " + overrun);
        assertTrue(change.get(valueOffset));
        change.clear(valueOffset);
        assertTrue(change.isEmpty());
//System.out.println("pvValue " + pvValue + " pvRecordPowerValue " + pvRecordPowerValue );
        assertTrue(pvValue.get()==pvRecordPowerValue.get());
        assertTrue(overrun.get(valueOffset));
        overrun.clear(valueOffset);
        assertTrue(overrun.isEmpty());
        monitorRequester.release();
        monitorElement = monitorRequester.poll();
        assertTrue(monitorElement==null);
        monitorRequester.destroy();
    }
    
    public static void deadbandTest() {
        System.out.printf("%n****deadband Test****%n");
        // definitions for request structure to pass to PVCopyFactory
        PVRecord pvRecord = master.findRecord("powerWithDeadband");
        PVStructure pvStructure = pvRecord.getPVRecordStructure().getPVStructure();
        PVLong pvRecordSeconds = (PVLong)pvStructure.getSubField("timeStamp.secondsPastEpoch");
        PVInt pvRecordNanoSeconds = (PVInt)pvStructure.getSubField("timeStamp.nanoSeconds");
        PVInt pvRecordUserTag = (PVInt)pvStructure.getSubField("timeStamp.userTag");
        PVDouble pvRecordPowerValue = (PVDouble)pvStructure.getSubField("power.value");
        String request = "alarm,timeStamp,power.value";
//System.out.println("pvRecord " + pvRecord);
        PVStructure pvRequest = CreateRequestFactory.createRequest(request,requester);
//System.out.println("request:" + request);
//System.out.println("pvRequest:" + pvRequest);
        MonitorRequesterImpl monitorRequester = new  MonitorRequesterImpl(pvRecord,pvRequest);
        MonitorElement monitorElement = monitorRequester.poll();
        assertTrue(monitorElement!=null);
        PVStructure pvCopy = monitorElement.getPVStructure();
        BitSet change = monitorElement.getChangedBitSet();
        BitSet overrun = monitorElement.getOverrunBitSet();
        PVStructure pvTimeStamp = pvCopy.getStructureField("timeStamp");
        int timeStampOffset = pvTimeStamp.getFieldOffset();
        PVDouble pvValue = pvCopy.getDoubleField("value");
        int valueOffset = pvValue.getFieldOffset();
//System.out.println("pvCopy " + pvCopy);
//System.out.println("change " + change);
//System.out.println("overrun " + overrun);
        assertTrue(change.get(0));
        assertTrue(overrun.isEmpty());
        change.clear(0);
        assertTrue(change.isEmpty());
        monitorRequester.release();
        pvRecord.beginGroupPut();
        pvRecordSeconds.put(5);
        pvRecordNanoSeconds.put(0);
        pvRecordUserTag.put(1);
        pvRecordPowerValue.put(5.0);
        pvRecord.endGroupPut();
        monitorElement = monitorRequester.poll();
        assertTrue(monitorElement!=null);
        pvCopy = monitorElement.getPVStructure();
        pvValue = pvCopy.getDoubleField("value");
        change = monitorElement.getChangedBitSet();
        overrun = monitorElement.getOverrunBitSet();
//System.out.println("change " + change);
//System.out.println("overrun " + overrun);
        assertTrue(change.get(timeStampOffset));
        change.clear(timeStampOffset);
        assertTrue(change.get(valueOffset));
        change.clear(valueOffset);
        assertTrue(change.isEmpty());
        assertTrue(overrun.isEmpty());
        assertTrue(pvRecordPowerValue.get()==pvValue.get());
        monitorRequester.release();
        pvRecordPowerValue.put(5.01);
        monitorElement = monitorRequester.poll();
        assertTrue(monitorElement==null);
        pvRecordPowerValue.put(5.11);
        monitorElement = monitorRequester.poll();
        pvRecordPowerValue.put(6.0);
        assertTrue(monitorElement!=null);
        pvCopy = monitorElement.getPVStructure();
        pvValue = pvCopy.getDoubleField("value");
        change = monitorElement.getChangedBitSet();
        overrun = monitorElement.getOverrunBitSet();
//System.out.println("change " + change);
//System.out.println("overrun " + overrun);
        assertTrue(change.get(valueOffset));
        change.clear(valueOffset);
        assertTrue(change.isEmpty());
        assertTrue(pvValue.get()==5.11);
        monitorRequester.release();
        monitorElement = monitorRequester.poll();
        assertTrue(monitorElement!=null);
        pvCopy = monitorElement.getPVStructure();
        pvValue = pvCopy.getDoubleField("value");
        change = monitorElement.getChangedBitSet();
        overrun = monitorElement.getOverrunBitSet();
//System.out.println("change " + change);
//System.out.println("overrun " + overrun);
        assertTrue(change.get(valueOffset));
        change.clear(valueOffset);
        assertTrue(change.isEmpty());
        assertTrue(pvValue.get()==pvRecordPowerValue.get());
        assertTrue(overrun.isEmpty());
        monitorRequester.release();
        monitorElement = monitorRequester.poll();
        assertTrue(monitorElement==null);
        monitorRequester.destroy();
    }
    
    public static void deadbandPercentTest() {
        System.out.printf("%n****deadband percent Test****%n");
        // definitions for request structure to pass to PVCopyFactory
        PVRecord pvRecord = master.findRecord("powerWithDeadbandPercent");
        PVStructure pvStructure = pvRecord.getPVRecordStructure().getPVStructure();
        PVLong pvRecordSeconds = (PVLong)pvStructure.getSubField("timeStamp.secondsPastEpoch");
        PVInt pvRecordNanoSeconds = (PVInt)pvStructure.getSubField("timeStamp.nanoSeconds");
        PVInt pvRecordUserTag = (PVInt)pvStructure.getSubField("timeStamp.userTag");
        PVDouble pvRecordPowerValue = (PVDouble)pvStructure.getSubField("power.value");
        String request = "alarm,timeStamp,power.value";
//System.out.println("pvRecord " + pvRecord);
        PVStructure pvRequest = CreateRequestFactory.createRequest(request,requester);
//System.out.println("request:" + request);
//System.out.println("pvRequest:" + pvRequest);
        MonitorRequesterImpl monitorRequester = new  MonitorRequesterImpl(pvRecord,pvRequest);
        MonitorElement monitorElement = monitorRequester.poll();
        assertTrue(monitorElement!=null);
        PVStructure pvCopy = monitorElement.getPVStructure();
        BitSet change = monitorElement.getChangedBitSet();
        BitSet overrun = monitorElement.getOverrunBitSet();
        PVStructure pvTimeStamp = pvCopy.getStructureField("timeStamp");
        int timeStampOffset = pvTimeStamp.getFieldOffset();
        PVDouble pvValue = pvCopy.getDoubleField("value");
        int valueOffset = pvValue.getFieldOffset();
//System.out.println("pvCopy " + pvCopy);
//System.out.println("change " + change);
//System.out.println("overrun " + overrun);
        assertTrue(change.get(0));
        assertTrue(overrun.isEmpty());
        change.clear(0);
        assertTrue(change.isEmpty());
        monitorRequester.release();
        pvRecord.beginGroupPut();
        pvRecordSeconds.put(5);
        pvRecordNanoSeconds.put(0);
        pvRecordUserTag.put(1);
        pvRecordPowerValue.put(5.0);
        pvRecord.endGroupPut();
        monitorElement = monitorRequester.poll();
        assertTrue(monitorElement!=null);
        pvCopy = monitorElement.getPVStructure();
        pvValue = pvCopy.getDoubleField("value");
        change = monitorElement.getChangedBitSet();
        overrun = monitorElement.getOverrunBitSet();
//System.out.println("change " + change);
//System.out.println("overrun " + overrun);
        assertTrue(change.get(timeStampOffset));
        change.clear(timeStampOffset);
        assertTrue(change.get(valueOffset));
        change.clear(valueOffset);
        assertTrue(change.isEmpty());
        assertTrue(overrun.isEmpty());
        assertTrue(pvRecordPowerValue.get()==pvValue.get());
        monitorRequester.release();
        pvRecordPowerValue.put(5.01);
        monitorElement = monitorRequester.poll();
        assertTrue(monitorElement==null);
        pvRecordPowerValue.put(5.11);
        monitorElement = monitorRequester.poll();
        pvRecordPowerValue.put(6.0);
        assertTrue(monitorElement!=null);
        pvCopy = monitorElement.getPVStructure();
        pvValue = pvCopy.getDoubleField("value");
        change = monitorElement.getChangedBitSet();
        overrun = monitorElement.getOverrunBitSet();
//System.out.println("change " + change);
//System.out.println("overrun " + overrun);
        assertTrue(change.get(valueOffset));
        change.clear(valueOffset);
        assertTrue(change.isEmpty());
        assertTrue(pvValue.get()==5.11);
        monitorRequester.release();
        monitorElement = monitorRequester.poll();
        assertTrue(monitorElement!=null);
        pvCopy = monitorElement.getPVStructure();
        pvValue = pvCopy.getDoubleField("value");
        change = monitorElement.getChangedBitSet();
        overrun = monitorElement.getOverrunBitSet();
//System.out.println("change " + change);
//System.out.println("overrun " + overrun);
        assertTrue(change.get(valueOffset));
        change.clear(valueOffset);
        assertTrue(change.isEmpty());
        assertTrue(pvValue.get()==pvRecordPowerValue.get());
        assertTrue(overrun.isEmpty());
        monitorRequester.release();
        monitorElement = monitorRequester.poll();
        assertTrue(monitorElement==null);
        monitorRequester.destroy();
    }
    
    public static void periodicTest() {
        System.out.printf("%n****periodic Test****%n");
        // definitions for request structure to pass to PVCopyFactory
        PVRecord pvRecord = master.findRecord("powerWithoutDeadband");
        PVStructure pvStructure = pvRecord.getPVRecordStructure().getPVStructure();
        PVLong pvRecordSeconds = (PVLong)pvStructure.getSubField("timeStamp.secondsPastEpoch");
        PVInt pvRecordNanoSeconds = (PVInt)pvStructure.getSubField("timeStamp.nanoSeconds");
        PVInt pvRecordUserTag = (PVInt)pvStructure.getSubField("timeStamp.userTag");
        PVDouble pvRecordPowerValue = (PVDouble)pvStructure.getSubField("power.value");
        String request = "record[periodicRate=.2]field(alarm,timeStamp,power.value)";
//System.out.println("pvRecord " + pvRecord);
        PVStructure pvRequest = CreateRequestFactory.createRequest(request,requester);
//System.out.println("request:" + request);
//System.out.println("pvRequest:" + pvRequest);
        MonitorRequesterImpl monitorRequester = new  MonitorRequesterImpl(pvRecord,pvRequest);
        MonitorElement monitorElement = monitorRequester.poll();
        assertTrue(monitorElement!=null);
        PVStructure pvCopy = monitorElement.getPVStructure();
        PVStructure pvTimeStamp = pvCopy.getStructureField("timeStamp");
        int timeStampOffset = pvTimeStamp.getFieldOffset();
        PVDouble pvValue = pvCopy.getDoubleField("value");
        int valueOffset = pvValue.getFieldOffset();
        BitSet change = monitorElement.getChangedBitSet();
        BitSet overrun = monitorElement.getOverrunBitSet();
//System.out.println("pvCopy " + pvCopy);
//System.out.println("change " + change);
//System.out.println("overrun " + overrun);
        assertTrue(change.get(0));
        assertTrue(overrun.isEmpty());
        change.clear(0);
        assertTrue(change.isEmpty());
        monitorRequester.release();
        pvRecord.beginGroupPut();
        pvRecordSeconds.put(5);
        pvRecordNanoSeconds.put(0);
        pvRecordUserTag.put(1);
        pvRecordPowerValue.put(5.0);
        pvRecord.endGroupPut();
        monitorElement = monitorRequester.poll();
        assertTrue(monitorElement==null);
        try {
        	Thread.sleep(300);
        } catch (InterruptedException e) {}
        monitorElement = monitorRequester.poll();
        assertTrue(monitorElement!=null);
        pvCopy = monitorElement.getPVStructure();
        change = monitorElement.getChangedBitSet();
        overrun = monitorElement.getOverrunBitSet();
//System.out.println("change " + change);
//System.out.println("overrun " + overrun);
        assertTrue(change.get(timeStampOffset));
        change.clear(timeStampOffset);
        assertTrue(change.get(valueOffset));
        change.clear(valueOffset);
        assertTrue(change.isEmpty());
        assertTrue(overrun.isEmpty());
        assertTrue(pvRecordPowerValue.get()==pvValue.get());
        monitorRequester.release();
        pvRecord.beginGroupPut();
        pvRecordPowerValue.put(6.0);
        pvRecordPowerValue.put(6.0);
        pvRecord.endGroupPut();
        monitorElement = monitorRequester.poll();
        assertTrue(monitorElement==null);
        try {
        	Thread.sleep(300);
        } catch (InterruptedException e) {}
        monitorElement = monitorRequester.poll();
        assertTrue(monitorElement!=null);
        pvCopy = monitorElement.getPVStructure();
        change = monitorElement.getChangedBitSet();
        overrun = monitorElement.getOverrunBitSet();
//System.out.println("change " + change);
//System.out.println("overrun " + overrun);
        assertTrue(change.get(valueOffset));
        change.clear(valueOffset);
        assertTrue(change.isEmpty());
        assertTrue(overrun.get(valueOffset));
        assertTrue(pvRecordPowerValue.get()==pvValue.get());
        monitorRequester.release();
        monitorElement = monitorRequester.poll();
        assertTrue(monitorElement==null);
        monitorRequester.destroy();
    }
    
    private static class MonitorRequesterImpl implements MonitorRequester {
    	private Monitor monitor = null;
    	private boolean isDestroyed = false;
    	private MonitorElement monitorElement = null;
    	
    	MonitorRequesterImpl(PVRecord pvRecord,PVStructure pvRequest) {
    		monitor = MonitorFactory.create(pvRecord, this, pvRequest);
    		monitor.start();
    	}
    	
    	void destroy() {
    		isDestroyed = true;
    		monitor.destroy();
    		monitor = null;
    	}
    	
    	MonitorElement poll() {
    		monitorElement =  monitor.poll();
    		return monitorElement;
    	}
    	
    	void release() {
    		if(monitorElement==null) {
    			throw new IllegalStateException("Logic error. Should never get here");
    		}
    		monitor.release(monitorElement);
    		monitorElement = null;
    	}

		@Override
		public void monitorConnect(Status status, Monitor monitor,Structure structure) {
			if(isDestroyed) return;
			if(status.isSuccess()) {
				this.monitor = monitor;
				return;
			}
			System.out.printf("monitorConnect %s %s%n",status.getMessage(),structure.toString());
		}

		@Override
		public void monitorEvent(Monitor monitor) {
			if(isDestroyed) return;
		}

		@Override
		public void unlisten(Monitor monitor) {}

		@Override
		public String getRequesterName() {
			return "monitorTest";
		}

		@Override
		public void message(String message, MessageType messageType) {
			System.out.printf("%s %s%n",messageType.toString(),message);
		}  
    }
    /* (non-Javadoc)
     * @see junit.framework.TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        master.cleanMaster();
    }
}

