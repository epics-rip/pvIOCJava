/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.rpc;

import org.epics.ioc.database.PVDatabase;
import org.epics.ioc.database.PVDatabaseFactory;
import org.epics.ioc.database.PVRecordStructure;
import org.epics.ioc.support.AbstractSupport;
import org.epics.ioc.support.Support;
import org.epics.ioc.support.SupportProcessRequester;
import org.epics.ioc.util.RequestResult;
import org.epics.pvData.pv.PVArray;
import org.epics.pvData.pv.PVString;
import org.epics.pvData.pv.PVStringArray;
import org.epics.pvData.pv.PVStructure;
import org.epics.pvData.pv.ScalarType;

/**
 * Support for remotely get a list of records.
 * @author mrk
 *
 */
public class StructureListFactory {
    /**
     * Create support for getting the list of structures..
     * @param pvRecordStructure The field supported.
     * @return An interface to the support or null if the supportName was not "linkArray".
     */
    public static Support create(PVRecordStructure pvRecordStructure) {
        return new StructureListImpl(pvRecordStructure);
    }
    
    private static final String supportName = "org.epics.ioc.rpc.structureList";
    
    
    private static class StructureListImpl extends AbstractSupport
    {
       
        private final PVRecordStructure pvRecordStructure;
        private PVString pvDatabaseName = null;
        private PVString pvRegularExpression = null;
        private PVString pvStatus = null;
        private PVStringArray pvNames = null;
        
        private StructureListImpl(PVRecordStructure pvRecordStructure) {
            super(StructureListFactory.supportName,pvRecordStructure); 
            this.pvRecordStructure = pvRecordStructure;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.support.AbstractSupport#initialize()
         */
        @Override
        public void initialize() {
            PVStructure pvStructure = pvRecordStructure.getPVStructure();
            pvDatabaseName = pvStructure.getStringField("arguments.database");
            if(pvDatabaseName ==null) return;
            pvRegularExpression = pvStructure.getStringField("arguments.regularExpression");
            if(pvRegularExpression ==null) return;
            pvStatus = pvStructure.getStringField("result.status");
            if(pvStatus==null) return;
            PVArray pvArray = pvStructure.getScalarArrayField("result.names",ScalarType.pvString);
            if(pvArray==null) return;
            pvNames = (PVStringArray)pvArray;
            super.initialize();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.support.AbstractSupport#process(org.epics.ioc.process.SupportProcessRequester)
         */
        @Override
        public void process(SupportProcessRequester supportProcessRequester) {
            String databaseName = pvDatabaseName.get();
            if(databaseName==null) databaseName = "master";
            PVDatabase pvDatabase = null;
            if(databaseName.equals("master")) {
                pvDatabase = PVDatabaseFactory.getMaster();
            } else if(databaseName.equals("beingInstalled")) {
                pvDatabase = PVDatabaseFactory.getBeingInstalled();
            }
            if(pvDatabase==null) {
                pvStatus.put("database not found");
                pvNames.setLength(0);
            } else {
                String[] names = pvDatabase.structureList(pvRegularExpression.get());
                pvNames.setLength(names.length);
                pvNames.put(0, names.length, names, 0);
                pvStatus.put("success");
            }
            supportProcessRequester.supportProcessDone(RequestResult.success);
        }
    }
}
