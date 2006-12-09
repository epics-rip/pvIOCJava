/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.util;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.epics.ioc.channelAccess.ChannelAccessLocalFactory;
import org.epics.ioc.dbAccess.*;
import org.epics.ioc.dbDefinition.DBD;
import org.epics.ioc.dbDefinition.DBDFactory;
import org.epics.ioc.dbDefinition.DBDSupport;
import org.epics.ioc.dbProcess.*;

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
     * @param requestor A listener for any messages generated while initDatabase is executing.
     * @return (false,true) if the request (failed,succeeded)
     */
    public static boolean initDatabase(String dbFile,Requestor requestor) {
        boolean gotIt = isInUse.compareAndSet(false,true);
        if(!gotIt) {
            requestor.message("XMLToIOCDBFactory.convert is already active",
                MessageType.fatalError);
            return false;
        }
        try {
            maxError = MessageType.info;
            DBD dbd = DBDFactory.getMasterDBD(); 
            IOCDB iocdbAdd = XMLToIOCDBFactory.convert("add",dbFile,requestor);
            if(maxError!=MessageType.info) {
                requestor.message("iocInit failed because of xml errors.",
                        MessageType.fatalError);
                return false;
            }
            SupportCreation supportCreation = SupportCreationFactory.createSupportCreation(
                iocdbAdd,requestor);
            ChannelAccessLocalFactory.setIOCDB(iocdbAdd);
            boolean gotSupport = supportCreation.createSupport();
            if(!gotSupport) {
                requestor.message("Did not find all support.",MessageType.fatalError);
                requestor.message("nrecords",MessageType.info);
                Map<String,DBRecord> recordMap = iocdbAdd.getRecordMap();
                Set<String> keys = recordMap.keySet();
                for(String key: keys) {
                    DBRecord record = recordMap.get(key);
                    requestor.message(record.toString(),MessageType.info);
                }
                requestor.message("support",MessageType.info);
                Map<String,DBDSupport> supportMap = dbd.getSupportMap();
                keys = supportMap.keySet();
                for(String key: keys) {
                    DBDSupport dbdSupport = supportMap.get(key);
                    requestor.message(dbdSupport.toString(),MessageType.info);
                }
                return false;
            }
            boolean readyForStart = supportCreation.initializeSupport();
            if(!readyForStart) {
                requestor.message("initializeSupport failed",MessageType.fatalError);
                return false;
            }
            iocdbAdd.mergeIntoMaster();
            ChannelAccessLocalFactory.setIOCDB(IOCDBFactory.getMaster());
            boolean ready = supportCreation.startSupport();
            if(!ready) {
                requestor.message("startSupport failed",MessageType.fatalError);
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
