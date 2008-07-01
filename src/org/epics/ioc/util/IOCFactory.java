/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.util;

import java.util.concurrent.atomic.AtomicBoolean;

import org.epics.ioc.db.IOCDB;
import org.epics.ioc.db.XMLToIOCDBFactory;
import org.epics.ioc.support.SupportCreation;
import org.epics.ioc.support.SupportCreationFactory;

/**
 * A factory for installing and initializing record instances.
 * @author mrk
 *
 */
public class IOCFactory {
    private static MessageType maxError;
    private static AtomicBoolean isInUse = new AtomicBoolean(false);
    /**
     * Install and initialize record instances.
     * @param dbFile The file containing xml record instance definitions.
     * The file must define only new instances, i.e. if any record names are already
     * in the master IOC Database, the request will fails.
     * Each new record is then initialized. All record instances must initialize,
     * i.e. enter the readyForStart state or else the request fails.
     * If all records initialize the records are merged into the master IOCDB
     * and then started.
     * @param requester A listener for any messages generated while initDatabase is executing.
     * @return (false,true) if the request (failed,succeeded)
     */
    public static boolean initDatabase(String dbFile,Requester requester) {
        boolean gotIt = isInUse.compareAndSet(false,true);
        if(!gotIt) {
            requester.message("XMLToIOCDBFactory.convert is already active",
                MessageType.fatalError);
            return false;
        }
        try {
            maxError = MessageType.info;
            IOCDB iocdbAdd = XMLToIOCDBFactory.convert("add",dbFile,requester);
            if(maxError!=MessageType.info) {
                requester.message("iocInit failed because of xml errors.",
                        MessageType.fatalError);
                return false;
            }
            SupportCreation supportCreation = SupportCreationFactory.createSupportCreation(
                iocdbAdd,requester);
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
            iocdbAdd.mergeIntoMaster();
            boolean ready = supportCreation.startSupport();
            if(!ready) {
                requester.message("startSupport failed",MessageType.fatalError);
                return false;
            }
            supportCreation = null;
            iocdbAdd = null;
            return true;
        } finally {
            isInUse.set(false);
        }
    }
}
