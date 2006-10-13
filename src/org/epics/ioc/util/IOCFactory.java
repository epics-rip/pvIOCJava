/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.util;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.epics.ioc.dbAccess.*;
import org.epics.ioc.dbDefinition.DBD;
import org.epics.ioc.dbDefinition.DBDFactory;
import org.epics.ioc.dbDefinition.DBDSupport;
import org.epics.ioc.dbProcess.*;

/**
 * A factory for insyalling and initializing record instances.
 * @author mrk
 *
 */
public class IOCFactory {
    private static IOCMessageType maxError;
    private static AtomicBoolean isInUse = new AtomicBoolean(false);
    /**
     * Install and initialize record instances.
     * @param dbFile The file containing xml record instance definitions.
     * The file must define only new instances, i.e. if any record names are already
     * in the master IOC Database, the request will fails.
     * Each new record is then initialized and started. All record instances must start,
     * i.e. enter the ready state or else the request fails.
     * @param iocMessageListener A listener for any messages generated while initDatabase is executing.
     * @return (false,true) if the request (failed,succeeded)
     */
    public static boolean initDatabase(String dbFile,IOCMessageListener iocMessageListener) {
        boolean gotIt = isInUse.compareAndSet(false,true);
        if(!gotIt) {
            iocMessageListener.message("XMLToIOCDBFactory.convert is already active",
                IOCMessageType.fatalError);
            return false;
        }
        try {
            maxError = IOCMessageType.info;
            DBD dbd = DBDFactory.getMasterDBD(); 
            IOCDB iocdbAdd = XMLToIOCDBFactory.convert("add",dbFile,iocMessageListener);
            if(maxError!=IOCMessageType.info) {
                iocMessageListener.message("iocInit failed because of xml errors.",
                        IOCMessageType.fatalError);
                return false;
            }
            SupportCreation supportCreation = SupportCreationFactory.createSupportCreation(
                iocdbAdd,iocMessageListener);
            ChannelAccessLocalFactory.setIOCDB(iocdbAdd);
            boolean gotSupport = supportCreation.createSupport();
            if(!gotSupport) {
                iocMessageListener.message("Did not find all support.",IOCMessageType.fatalError);
                iocMessageListener.message("nrecords",IOCMessageType.info);
                Map<String,DBRecord> recordMap = iocdbAdd.getRecordMap();
                Set<String> keys = recordMap.keySet();
                for(String key: keys) {
                    DBRecord record = recordMap.get(key);
                    iocMessageListener.message(record.toString(),IOCMessageType.info);
                }
                iocMessageListener.message("support",IOCMessageType.info);
                Map<String,DBDSupport> supportMap = dbd.getSupportMap();
                keys = supportMap.keySet();
                for(String key: keys) {
                    DBDSupport dbdSupport = supportMap.get(key);
                    iocMessageListener.message(dbdSupport.toString(),IOCMessageType.info);
                }
                return false;
            }
            boolean readyForStart = supportCreation.initializeSupport();
            if(!readyForStart) {
                iocMessageListener.message("initializeSupport failed",IOCMessageType.fatalError);
                return false;
            }
            boolean ready = supportCreation.startSupport();
            if(!ready) {
                iocMessageListener.message("startSupport failed",IOCMessageType.fatalError);
                return false;
            }
            supportCreation = null;
            iocdbAdd.mergeIntoMaster();
            iocdbAdd = null;
            return true;
        } finally {
            isInUse.set(false);
            ChannelAccessLocalFactory.setIOCDB(IOCDBFactory.getMaster());
        }
    }
}
