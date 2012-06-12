/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.support.rpc;

import org.epics.pvaccess.client.Channel;
import org.epics.pvaccess.client.ChannelRPCRequester;
import org.epics.pvdata.factory.FieldFactory;
import org.epics.pvdata.factory.PVDataFactory;
import org.epics.pvdata.factory.StatusFactory;
import org.epics.pvdata.pv.FieldCreate;
import org.epics.pvdata.pv.PVDataCreate;
import org.epics.pvdata.pv.PVField;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.Status;
import org.epics.pvdata.pv.Status.StatusType;
import org.epics.pvdata.pv.StatusCreate;
import org.epics.pvdata.pv.Structure;
import org.epics.pvioc.database.PVRecord;
import org.epics.pvioc.pvAccess.RPCServer;

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
			long start =System.currentTimeMillis();
			int size = 2;
			Structure[] fields = new Structure[size];
			String[] fieldNames = new String[size];
			for(int index=0; index<size; index++) {
			    fieldNames[index] = Integer.toString(index);
				fields[index] = fieldCreate.createStructure(pvElement.getStructure().getFieldNames(), pvElement.getStructure().getFields());
			}
			Structure top = fieldCreate.createStructure(fieldNames, fields);
			PVStructure pvTop = pvDataCreate.createPVStructure(top);
			long end =System.currentTimeMillis();
			double diff = end-start;
			diff /= 1000.0;
            System.out.println("ExampleChannelRPVFactory " + diff + " seconds to create PVStructure"); 
			channelRPCRequester.requestDone(okStatus, pvTop);
		}
		
    }
}
