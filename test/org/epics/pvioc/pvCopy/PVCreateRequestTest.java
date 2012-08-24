/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS pvData is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.pvCopy;

import junit.framework.TestCase;

import org.epics.pvaccess.client.CreateRequestFactory;
import org.epics.pvdata.pv.MessageType;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.Requester;



/**
 * JUnit test for pvAccess.
 * It also provides examples of how to use the pvAccess interfaces.
 * @author mrk
 *
 */
public class PVCreateRequestTest extends TestCase {
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
    
    /**
     * 
     */
    public static void testCreateRequest() {
    	String request = "";
        PVStructure pvRequest = CreateRequestFactory.createRequest(request,requester);
        System.out.printf("request %s%n%s%n",request,pvRequest.toString());
        request = "alarm,timeStamp,power.value";
        pvRequest = CreateRequestFactory.createRequest(request,requester);
        assertFalse(pvRequest==null);
        System.out.printf("request %s%n%s%n",request,pvRequest.toString());
        request = "record[process=true]field(alarm,timeStamp,power.value)";
        pvRequest = CreateRequestFactory.createRequest(request,requester);
        assertFalse(pvRequest==null);
        System.out.printf("request %s%n%s%n",request,pvRequest.toString());
        request = "record[process=true]field(alarm,timeStamp[algorithm=onChange,causeMonitor=false],power{value,alarm})";
        pvRequest = CreateRequestFactory.createRequest(request,requester);
        assertFalse(pvRequest==null);
        System.out.printf("request %s%n%s%n",request,pvRequest.toString());
        request = "record[process=true,xxx=yyy]field(alarm,timeStamp[shareData=true],power.value)";
        pvRequest = CreateRequestFactory.createRequest(request,requester);
        assertFalse(pvRequest==null);
        System.out.printf("request %s%n%s%n",request,pvRequest.toString());
        request = "record[process=true,xxx=yyy]"
        	+ "putField(power.value)"
        	+ "getField(alarm,timeStamp,power{value,alarm},"
        	+ "current{value,alarm},voltage{value,alarm})";
        pvRequest = CreateRequestFactory.createRequest(request,requester);
        assertFalse(pvRequest==null);
        System.out.printf("request %s%n%s%n",request,pvRequest.toString());
        request = "field(alarm,timeStamp,supply{" 
                + "0{voltage.value,current.value,power.value},"
                + "1{voltage.value,current.value,power.value}"
                + "})";
        System.out.println("request " + request);
        pvRequest = CreateRequestFactory.createRequest(request,requester);
        assertFalse(pvRequest==null);
        System.out.printf("request %s%n%s%n",request,pvRequest.toString());
        request = "record[process=true,xxx=yyy]"
        	+ "putField(power.value)"
        	+ "getField(alarm,timeStamp,power{value,alarm},"
        	+ "current{value,alarm},voltage{value,alarm},"
        	+ "ps0{alarm,timeStamp,power{value,alarm},current{value,alarm},voltage{value,alarm}},"
        	+ "ps1{alarm,timeStamp,power{value,alarm},current{value,alarm},voltage{value,alarm}}"
        	+ ")";
        pvRequest = CreateRequestFactory.createRequest(request,requester);
        assertFalse(pvRequest==null);
        System.out.printf("request %s%n%s%n",request,pvRequest.toString());
        request = "a{b{c{d}}}";
        pvRequest = CreateRequestFactory.createRequest(request,requester);
        assertFalse(pvRequest==null);
        System.out.printf("request %s%n%s%n",request,pvRequest.toString());
        request = "record[process=true,xxx=yyy]field(alarm,timeStamp[shareData=true],power.value";
        System.out.printf("%nError Expected for next call!!%n");
        pvRequest = CreateRequestFactory.createRequest(request,requester);
        assertTrue(pvRequest==null);
        request = "record[process=true,xxx=yyy]"
                + "putField(power.value)"
                + "getField(alarm,timeStamp,power{value,alarm},"
                + "current{value,alarm},voltage{value,alarm},"
                + "ps0{alarm,timeStamp,power{value,alarm},current{value,alarm},voltage{value,alarm}},"
                + "ps1{alarm,timeStamp,power{value,alarm},current{value,alarm},voltage{value,alarm}"
                + ")";
        System.out.printf("%nError Expected for next call!!%n");
        pvRequest = CreateRequestFactory.createRequest(request,requester);
        assertTrue(pvRequest==null);
    }
}

