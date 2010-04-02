/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.rpc;

import org.epics.ca.client.Channel;
import org.epics.ca.client.ChannelRPCRequester;
import org.epics.ca.client.RPCServer;
import org.epics.pvData.factory.PVDataFactory;
import org.epics.pvData.factory.*;
import org.epics.pvData.misc.BitSet;
import org.epics.pvData.pv.*;
import org.epics.pvData.pv.PVDataCreate;
import org.epics.pvData.pv.PVInt;
import org.epics.pvData.pv.PVRecord;
import org.epics.pvData.pv.PVStructure;
import org.epics.pvData.pv.Status;
import org.epics.pvData.pv.StatusCreate;
import org.epics.pvData.pv.Status.StatusType;

/**
 * Support for remotely get a list of records.
 * @author mrk
 *
 */
public class ExampleChannelRPCFactory {
    /**
     * Create support for an array of calcArg structures.
     * @param pvStructure The processControlStructure
     * @return An interface to the support or null if the supportName was not "linkArray".
     */
    public static RPCServer create() {
        return new RPCServerImpl();
    }
    private static final PVDataCreate pvDataCreate = PVDataFactory.getPVDataCreate();
    private static final FieldCreate fieldCreate = FieldFactory.getFieldCreate();
    private static final StatusCreate statusCreate = StatusFactory.getStatusCreate();
    private static final Status okStatus = statusCreate.getStatusOK();
    private static final Status sizeNotFoundStatus = statusCreate.createStatus(StatusType.ERROR, "pvArguments did not have size", null);
    private static final Status elementNotFoundStatus = statusCreate.createStatus(StatusType.ERROR, "pvrecord did not have element substructure", null);
    
    private static class RPCServerImpl implements RPCServer
    {
    	private ChannelRPCRequester channelRPCRequester;
    	private PVInt pvSize;
    	private PVStructure pvElement;
		
		@Override
		public Status initialize(Channel channel, PVRecord pvRecord,
				ChannelRPCRequester channelRPCRequester,
				PVStructure pvArgument,BitSet bitSet, PVStructure pvRequest)
		{
		    this.channelRPCRequester = channelRPCRequester;
		    pvSize = pvArgument.getIntField("size");
		    if(pvSize==null) return sizeNotFoundStatus;
		    pvElement = pvRecord.getPVStructure().getStructureField("element");
		    if(pvElement==null) return elementNotFoundStatus;
			return okStatus;
		}
		@Override
		public void destroy() {}
		@Override
		public void request() {
			long start =System.currentTimeMillis();
			int size = pvSize.get();
			Field[] fields = new Field[size];
			for(int index=0; index<size; index++) {
				fields[index] = fieldCreate.createStructure(Integer.toString(index), new Field[0]);
			}
			PVStructure pvTop = pvDataCreate.createPVStructure(null, "",fields);
			PVField[] pvFields = pvTop.getPVFields();
			for(int index=0; index<size; index++) {
				pvFields[index] = pvDataCreate.createPVStructure(pvTop, Integer.toString(index), pvElement);
			}
			pvTop.updateInternal();
			long end =System.currentTimeMillis();
			double diff = end-start;
			diff /= 1000.0;
            System.out.println("ExampleChannelRPVFactory " + diff + " seconds to create PVStructure"); 
			channelRPCRequester.requestDone(okStatus, pvTop);
		}
    }
}
