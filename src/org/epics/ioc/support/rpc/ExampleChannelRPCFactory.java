/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.rpc;

import org.epics.ca.client.Channel;
import org.epics.ca.client.ChannelRPCRequester;
import org.epics.ioc.database.PVRecord;
import org.epics.ioc.pvAccess.RPCServer;
import org.epics.pvData.factory.FieldFactory;
import org.epics.pvData.factory.PVDataFactory;
import org.epics.pvData.factory.StatusFactory;
import org.epics.pvData.misc.BitSet;
import org.epics.pvData.pv.FieldCreate;
import org.epics.pvData.pv.PVDataCreate;
import org.epics.pvData.pv.PVField;
import org.epics.pvData.pv.PVInt;
import org.epics.pvData.pv.PVStructure;
import org.epics.pvData.pv.Status;
import org.epics.pvData.pv.StatusCreate;
import org.epics.pvData.pv.Structure;
import org.epics.pvData.pv.Status.StatusType;

/**
 * Support for remotely get a list of records.
 * @author mrk
 *
 */
public class ExampleChannelRPCFactory {
    /**
     * Create an example RPCServer
     * @return The interface.
     */
    public static RPCServer create() {
        return new RPCServerImpl();
    }
    private static final PVDataCreate pvDataCreate = PVDataFactory.getPVDataCreate();
    private static final FieldCreate fieldCreate = FieldFactory.getFieldCreate();
    private static final StatusCreate statusCreate = StatusFactory.getStatusCreate();
    private static final Status okStatus = statusCreate.getStatusOK();
    private static final Status elementNotFoundStatus = statusCreate.createStatus(StatusType.ERROR, "pvrecord did not have element substructure", null);
    
    private static class RPCServerImpl implements RPCServer
    {
    	private ChannelRPCRequester channelRPCRequester;
    	private PVStructure pvElement;
		
		@Override
		public Status initialize(Channel channel, PVRecord pvRecord,
				ChannelRPCRequester channelRPCRequester,PVStructure pvRequest)
		{
		    this.channelRPCRequester = channelRPCRequester;
		    pvElement = pvRecord.getPVRecordStructure().getPVStructure().getStructureField("element");
		    if(pvElement==null) return elementNotFoundStatus;
			return okStatus;
		}
		@Override
		public void destroy() {}
		@Override
		public void request(PVStructure pvArgument) {
System.out.println("argument");
System.out.println(pvArgument.toString());
			long start =System.currentTimeMillis();
			int size = 2;
			Structure[] fields = new Structure[size];
			for(int index=0; index<size; index++) {
				fields[index] = fieldCreate.createStructure(Integer.toString(index), pvElement.getStructure().getFields());
			}
			PVStructure pvTop = pvDataCreate.createPVStructure(null, "",fields);
			PVField[] pvFields = pvTop.getPVFields();
			for(int index=0; index<size; index++) {
				pvFields[index] = pvDataCreate.createPVStructure(pvTop, Integer.toString(index), pvElement);
			}
			long end =System.currentTimeMillis();
			double diff = end-start;
			diff /= 1000.0;
            System.out.println("ExampleChannelRPVFactory " + diff + " seconds to create PVStructure"); 
			channelRPCRequester.requestDone(okStatus, pvTop);
		}
		
    }
}
