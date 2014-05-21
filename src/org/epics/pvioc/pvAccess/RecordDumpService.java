/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS pvData is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.pvAccess;

import org.epics.pvaccess.server.rpc.RPCRequestException;
import org.epics.pvaccess.server.rpc.RPCServer;
import org.epics.pvaccess.server.rpc.RPCService;
import org.epics.pvdata.pv.PVField;
import org.epics.pvdata.pv.PVStringArray;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.ScalarType;
import org.epics.pvdata.pv.Status;
import org.epics.pvdata.pv.StringArrayData;
import org.epics.pvioc.database.PVDatabase;
import org.epics.pvioc.database.PVDatabaseFactory;
import org.epics.pvioc.database.PVRecord;

/**
 * channelRPC support for returning the PVStructure for a PVRecord or a PVStructure from a database.
 * It accepts an NTNameValue structure and returns the PVStructure. The names supported are: "database", "record", and "structure".
 * Exactly one of "record" and "structure" must be specified.
 * @author mrk
 *
 */
public class RecordDumpService {
  
    public static void start(String serviceName,RPCServer rpcServer) {
        RPCService service = new RecordDump();
        rpcServer.registerService(serviceName, service);
    }
    
    private static class RecordDump implements RPCService
    {
		/* (non-Javadoc)
		 * @see org.epics.pvaccess.server.rpc.RPCService#request(org.epics.pvdata.pv.PVStructure)
		 */
		@Override
		public PVStructure request(PVStructure pvArgument) throws RPCRequestException
		{
		    if(pvArgument==null || pvArgument.getNumberFields()==0) {
		        throw new RPCRequestException(Status.StatusType.ERROR,"illegal argument");
		        
		    }
		    PVField pvField = pvArgument.getSubField("name");
		    if(pvField==null) {
                throw new RPCRequestException(Status.StatusType.ERROR,"pvArgument not a NTNameValue: name not found");
            }
		    pvField = pvArgument.getScalarArrayField("name", ScalarType.pvString);
		    if(pvField==null) {
		        throw new RPCRequestException(Status.StatusType.ERROR,"pvArgument not a NTNameValue: name not found");
		    }
		    PVStringArray pvnames = (PVStringArray)pvField;
		    pvField = pvArgument.getScalarArrayField("value", ScalarType.pvString);
            if(pvField==null) {
                throw new RPCRequestException(Status.StatusType.ERROR,"pvArgument not a NTNameValue: value not found");
                
            }
            PVStringArray pvvalues = (PVStringArray)pvField;
            int length = pvnames.getLength();
            if(length!=pvvalues.getLength()) {
                throw new RPCRequestException(Status.StatusType.ERROR,"pvArgument not a NTNameValue: name and value not same length");
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
                throw new RPCRequestException(Status.StatusType.ERROR,"database " + databaseName + " does not exist");
            }
            if(recordName!=null) {
                PVRecord pvRecord = pvDatabase.findRecord(recordName);
                if(pvRecord==null) {
                    throw new RPCRequestException(Status.StatusType.ERROR,"record " + recordName + " does not exist");
                }
                return pvRecord.getPVRecordStructure().getPVStructure();
            }
            if(structureName!=null) {
                PVStructure pvStructure = pvDatabase.findStructure(structureName);
                if(pvStructure==null) {
                    throw new RPCRequestException(Status.StatusType.ERROR,"structure " + structureName + " does not exist");
                }
                return pvStructure;
            }
            throw new RPCRequestException(Status.StatusType.ERROR,"neither record or satructure were specfied ");
		}
    }
}
