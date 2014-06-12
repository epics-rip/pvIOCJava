/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS pvData is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.example;

import org.epics.pvdata.factory.FieldFactory;
import org.epics.pvdata.factory.PVDataFactory;
import org.epics.pvdata.pv.Field;
import org.epics.pvdata.pv.FieldCreate;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.ScalarType;
import org.epics.pvioc.database.BasePVRecord;
import org.epics.pvioc.database.PVDatabase;
import org.epics.pvioc.database.PVDatabaseFactory;
import org.epics.pvioc.database.PVRecord;
import org.epics.pvioc.support.RecordProcess;
import org.epics.pvioc.support.RecordProcessFactory;
import org.epics.pvioc.support.Support;
import org.epics.pvioc.support.basic.GenericFactory;

/**
 * channelRPC support for returning the PVStructure for a PVRecord or a PVStructure from a database.
 * It accepts an NTNameValue structure and returns the PVStructure. The names supported are: "database", "record", and "structure".
 * Exactly one of "record" and "structure" must be specified.
 * @author mrk
 *
 */
public class StructureArrayExampleRecord {
    private static final FieldCreate fieldCreate = FieldFactory.getFieldCreate();
  
    public static void start(String recordName) {
        Field[] fields = new Field[2];
        String[] fieldNames = new String[2];
        fields[0] = fieldCreate.createScalar(ScalarType.pvString);
        fieldNames[0] = "name";
        fields[1] = fieldCreate.createScalar(ScalarType.pvString);
        fieldNames[1] = "value";
      
        Field[] topFields = new Field[1];
        String[] topNames = new String[1];
        topFields[0] = fieldCreate.createStructureArray(fieldCreate.createStructure(fieldNames, fields));
        topNames[0] = "value";
        PVStructure pvStructure = PVDataFactory.getPVDataCreate().createPVStructure(
                fieldCreate.createStructure(topNames, topFields));
        PVRecord pvRecord = new BasePVRecord(recordName,pvStructure);
        Support support = GenericFactory.create(pvRecord.getPVRecordStructure());
        pvRecord.getPVRecordStructure().setSupport(support);
        RecordProcess recordProcess = RecordProcessFactory.createRecordProcess(pvRecord);
        recordProcess.initialize();
        recordProcess.start(null);
        PVDatabase pvDatabase = PVDatabaseFactory.getMaster();
        pvDatabase.addRecord(pvRecord);
    }
    
}
