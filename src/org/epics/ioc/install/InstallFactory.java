/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.install;

import java.util.concurrent.atomic.AtomicBoolean;

import org.epics.pvData.factory.PVDatabaseFactory;
import org.epics.pvData.factory.PVReplaceFactory;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVDatabase;
import org.epics.pvData.pv.PVRecord;
import org.epics.pvData.pv.PVStructure;
import org.epics.pvData.pv.Requester;
import org.epics.pvData.xml.XMLToPVDatabaseFactory;

/**
 * @author mrk
 *
 */
public class InstallFactory {
    private static MessageType maxError;
    private static AtomicBoolean isInUse = new AtomicBoolean(false);

    public static boolean installStructures(String file,Requester requester) {
        boolean gotIt = isInUse.compareAndSet(false,true);
        if(!gotIt) {
            requester.message("InstallFactory is already active",
                    MessageType.fatalError);
            return false;
        }
        try {
            maxError = MessageType.info;
            PVDatabase pvDatabaseAdd = PVDatabaseFactory.create("beingInstalled");
            XMLToPVDatabaseFactory.convert(pvDatabaseAdd,file,requester);
            if(maxError!=MessageType.info) {
                requester.message("installStructures failed because of xml errors.",
                        MessageType.fatalError);
                return false;
            }
            PVRecord[] pvRecords = pvDatabaseAdd.getRecords();
            if(pvRecords.length!=0) {
                requester.message("installStructures failed because file " + file + " contained record definitions",
                        MessageType.fatalError);
                return false;
            }
            pvDatabaseAdd.mergeIntoMaster();
            return true;
        } finally {
            isInUse.set(false);
        }
    }

    public static boolean installRecords(String file,Requester requester) {
        boolean gotIt = isInUse.compareAndSet(false,true);
        if(!gotIt) {
            requester.message("InstallFactory.convert is already active",
                MessageType.fatalError);
            return false;
        }
        try {
            maxError = MessageType.info;
            PVDatabase pvDatabaseAdd = PVDatabaseFactory.create("beingInstalled");
            XMLToPVDatabaseFactory.convert(pvDatabaseAdd,file,requester);
            if(maxError!=MessageType.info) {
                requester.message("installRecords failed because of xml errors.",
                        MessageType.fatalError);
                return false;
            }
            PVStructure[] pvStructures = pvDatabaseAdd.getStructures();
            if(pvStructures.length!=0) {
                requester.message("installRecords failed because file " + file + " contained structure definitions",
                        MessageType.fatalError);
                return false;
            }
            PVReplaceFactory.replace(pvDatabaseAdd);
            IOCDatabase iocDatabaseAdd = IOCDatabaseFactory.create(pvDatabaseAdd);
            SupportCreation supportCreation = SupportCreationFactory.create(iocDatabaseAdd, requester);
            boolean gotSupport = supportCreation.createSupport();
            if(!gotSupport) {
                requester.message("Did not find all support.",MessageType.fatalError);
                return false;
            }
            boolean readyForStart = supportCreation.initializeSupport();
            if(!readyForStart) {
                requester.message("initializeSupport failed",MessageType.fatalError);
                return false;
            }
            AfterStart afterStart = AfterStartFactory.create();
            boolean ready = supportCreation.startSupport(afterStart);
            if(!ready) {
                requester.message("startSupport failed",MessageType.fatalError);
                return false;
            }
            afterStart.callRequesters(false);
            iocDatabaseAdd.mergeIntoMaster();
            pvDatabaseAdd.mergeIntoMaster();
            afterStart.callRequesters(true);
            afterStart = null;
            supportCreation = null;
            pvDatabaseAdd = null;
            return true;
        } finally {
            isInUse.set(false);
        }
    }
}
