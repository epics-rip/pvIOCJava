/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS pvData is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.support.rpc;

import org.epics.pvaccess.client.Channel;
import org.epics.pvaccess.client.ChannelRPCRequester;
import org.epics.pvdata.factory.PVDataFactory;
import org.epics.pvdata.factory.StatusFactory;
import org.epics.pvdata.pv.PVField;
import org.epics.pvdata.pv.PVStringArray;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.ScalarType;
import org.epics.pvdata.pv.Status;
import org.epics.pvdata.pv.StatusCreate;
import org.epics.pvdata.pv.StringArrayData;
import org.epics.pvioc.database.PVDatabase;
import org.epics.pvioc.database.PVDatabaseFactory;
import org.epics.pvioc.database.PVRecord;
import org.epics.pvioc.pvAccess.RPCServer;

/**
 * channelRPC support for returning the PVStructure for a PVRecord or a PVStructire from a database.
 * It accepts an NTNameValue structure and returns the PVStructure. The names supported are: "database", "record", and "structure".
 * Exactly one of "record" and "structure" must be specified.
 * @author mrk
 *
 */
public class RecordDumpFactory {
    /**
     * Create an example RPCServer
     * @return The interface.
     */
    public static RPCServer create() {
        return new RPCServerImpl();
    }
    
    private static final StatusCreate statusCreate = StatusFactory.getStatusCreate();
    private static final Status okStatus = statusCreate.getStatusOK();
    private static final String[] emptyStrings = new String[0];
    private static final PVField[] emptyPVFields = new PVField[0];
    private static final PVStructure emptyPVStructure = PVDataFactory.getPVDataCreate().createPVStructure(emptyStrings,emptyPVFields);
    
    private static class RPCServerImpl implements RPCServer
    {
    	private ChannelRPCRequester channelRPCRequester;
		@Override
		public Status initialize(Channel channel, PVRecord pvRecord,
				ChannelRPCRequester channelRPCRequester,PVStructure pvRequest)
		{
		    this.channelRPCRequester = channelRPCRequester;
			return okStatus;
		}
		@Override
		public void destroy() {}
		@Override
		public void request(PVStructure pvArgument) {
		    if(pvArgument==null || pvArgument.getNumberFields()==0) {
		        Status status = statusCreate.createStatus(Status.StatusType.ERROR,"pvArgument is empty",null);
                channelRPCRequester.requestDone(status, emptyPVStructure);
                return;
		    }
		    PVField pvField = pvArgument.getSubField("name");
		    if(pvField==null) {
                Status status = statusCreate.createStatus(Status.StatusType.ERROR,"pvArgument not a NTNameValue: name not found",null);
                channelRPCRequester.requestDone(status, emptyPVStructure);
                return;
            }
		    pvField = pvArgument.getScalarArrayField("name", ScalarType.pvString);
		    if(pvField==null) {
		        Status status = statusCreate.createStatus(Status.StatusType.ERROR,"pvArgument not a NTNameValue: name not found",null);
		        channelRPCRequester.requestDone(status, emptyPVStructure);
                return;
		    }
		    PVStringArray pvnames = (PVStringArray)pvField;
		    pvField = pvArgument.getScalarArrayField("value", ScalarType.pvString);
            if(pvField==null) {
                Status status = statusCreate.createStatus(Status.StatusType.ERROR,"pvArgument not a NTNameValue: value not found",null);
                channelRPCRequester.requestDone(status, emptyPVStructure);
                return;
            }
            PVStringArray pvvalues = (PVStringArray)pvField;
            int length = pvnames.getLength();
            if(length!=pvvalues.getLength()) {
                Status status = statusCreate.createStatus(Status.StatusType.ERROR,"pvArgument not a NTNameValue: name and value not same length",null);
                channelRPCRequester.requestDone(status, emptyPVStructure);
                return;
            }
            String databaseName = "master";
            String recordName = null;
            String structureName = null;
            StringArrayData data = new StringArrayData();
            pvnames.get(0, length, data);
            String[] names = data.data;
            pvvalues.get(0, length, data);
            String[] values = data.data;
            for(int index=0; index<length; index++) {
               if(names[index].equals("database")) {
                   databaseName = values[index];
               } else if(names[index].equals("record")) {
                   recordName = values[index];
               } else if(names[index].equals("structure")) {
                   structureName = values[index];
               }
            }
            PVDatabase pvDatabase = null;
            if(databaseName.equals("master")) {
                pvDatabase = PVDatabaseFactory.getMaster();
            } else if(databaseName.equals("beingInstalled")) {
                pvDatabase = PVDatabaseFactory.getBeingInstalled();
            }
            if(pvDatabase==null) {
                Status status = statusCreate.createStatus(Status.StatusType.ERROR,"database " + databaseName + " does not exist",null);
                channelRPCRequester.requestDone(status, emptyPVStructure);
                return;
            }
            if(recordName!=null) {
                PVRecord pvRecord = pvDatabase.findRecord(recordName);
                if(pvRecord==null) {
                    Status status = statusCreate.createStatus(Status.StatusType.ERROR,"record " + recordName + " does not exist",null);
                    channelRPCRequester.requestDone(status, emptyPVStructure);
                    return;
                }
                channelRPCRequester.requestDone(okStatus, pvRecord.getPVRecordStructure().getPVStructure());
                return;
            }
            if(structureName!=null) {
                PVStructure pvStructure = pvDatabase.findStructure(structureName);
                if(pvStructure==null) {
                    Status status = statusCreate.createStatus(Status.StatusType.ERROR,"structure " + structureName + " does not exist",null);
                    channelRPCRequester.requestDone(status, emptyPVStructure);
                    return;
                }
                channelRPCRequester.requestDone(okStatus, pvStructure);
                return;
            }
            Status status = statusCreate.createStatus(Status.StatusType.ERROR,"neither record or satructure were specfied ",null);
            channelRPCRequester.requestDone(status, emptyPVStructure);
		}
    }
}
