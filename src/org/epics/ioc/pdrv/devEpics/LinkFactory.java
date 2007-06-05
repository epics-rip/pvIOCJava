 /**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pdrv.devEpics;

import org.epics.ioc.pdrv.*;
import org.epics.ioc.db.*;
import org.epics.ioc.pdrv.interfaces.*;
import org.epics.ioc.process.*;
import org.epics.ioc.pv.*;
import org.epics.ioc.support.*;
import org.epics.ioc.util.*;

/**
 * Factory to create link support.
 * @author mrk
 *
 */
public class LinkFactory {
    /**
     * Create link support for Channel Access links.
     * @param dbLink The field for which to create support.
     * @return A LinkSupport interface or null failure.
     */
    public static LinkSupport create(DBLink dbLink) {
        String supportName = dbLink.getSupportName();
        PVStructure pvStructure = dbLink.getPVLink().getConfigurationStructure();
        Structure structure = (Structure)pvStructure.getField();
        if(!structure.getStructureName().equals("pdrvLink")) {
            throw new IllegalStateException("configurationStructure is not pdrvLink");
        }
        if(supportName.equals(pdrvOctetInputSupportName)) {
            return new OctetInput(dbLink);
        } else if(supportName.equals(pdrvOctetOutputSupportName)) {
            return new OctetOutput(dbLink);
        }
        dbLink.getPVLink().message("no support for " + supportName, MessageType.fatalError);
        return null;
    }
    private static final String pdrvOctetInputSupportName = "pdrvOctetInput";
    private static final String pdrvOctetOutputSupportName = "pdrvOctetOutput";
    
    private static enum ValueType {
        integer,
        floating,
        string
    }
    
    private static class OctetInput extends AbstractPdrvLinkSupport
    {
        private OctetInput(DBLink dbLink) {
            super(pdrvOctetInputSupportName,dbLink);
        }
        
        private ValueType valueType;
        private PVString valueStringPVField = null;
        
        private Octet octet = null;
        private byte[] octetArray = new byte[100];
        private char[] charArray = new char[100];
        private int ncharsRead = 0;
        
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.devEpics.AbstractPdrvLinkSupport#initialize()
         */
        public void initialize() {
            if(!super.initBase()) return;
            PVField pvField = valueDBField.getPVField();
            Field field = pvField.getField();
            switch(field.getType()) {
            case pvByte:
            case pvShort:
            case pvInt:
            case pvLong:
                valueType = ValueType.integer; break;
            case pvFloat:
            case pvDouble:
                valueType = ValueType.floating; break;
            case pvString:
                valueStringPVField = (PVString)valuePVField;
                valueType = ValueType.string; break;
            default:
                pvLink.message("value field is not a supported type", MessageType.fatalError);
                return;
            }
        }      
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.devEpics.AbstractPdrvLinkSupport#uninitialize()
         */
        public void uninitialize() {
            super.uninitBase();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.devEpics.AbstractPdrvLinkSupport#start()
         */
        public void start() {
            if(!super.startBase()) return;
            switch(valueType) {
            case integer:
            case floating:
                pvLink.message("value field is not yet supported type", MessageType.fatalError);
                return;
            case string:
                Interface iface = device.findInterface(user, "octet", true);
                if(iface==null) {
                    pvLink.message("interface octet not supported", MessageType.fatalError);
                    setSupportState(SupportState.readyForInitialize);
                    return;
                }
                octet = (Octet)iface;
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.devEpics.AbstractPdrvLinkSupport#stop()
         */
        public void stop() {
            super.stopBase();
        }            
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.devEpics.AbstractPdrvLinkSupport#processContinue()
         */
        public void processContinue() {
            for(int i=0; i<ncharsRead; i++) charArray[i] = (char)octetArray[i];
            String string = String.copyValueOf(charArray, 0, ncharsRead);
            valueStringPVField.put(string);
            supportProcessRequester.supportProcessDone(RequestResult.success);
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.devEpics.AbstractPdrvLinkSupport#queueCallback()
         */
        public void queueCallback() {
            Status status = octet.read(user, octetArray, octetArray.length);
            if(status!=Status.success) {
                alarmSupport.setAlarm(user.getMessage(), AlarmSeverity.invalid);
                return;
            }
            ncharsRead = user.getInt();
            deviceTrace.printIO(Trace.IO_SUPPORT, octetArray, user.getInt(), "%s", fullName);
        }
    }
    
    private static class OctetOutput extends AbstractPdrvLinkSupport
    {
        private OctetOutput(DBLink dbLink) {
            super(pdrvOctetOutputSupportName,dbLink);
        }
        
        private ValueType valueType;
        private PVString valueStringPVField = null;
        
        private Octet octet = null;
        private byte[] octetArray = new byte[100];
        
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.devEpics.AbstractPdrvLinkSupport#initialize()
         */
        public void initialize() {
            if(!super.initBase()) return;
            PVField pvField = valueDBField.getPVField();
            Field field = pvField.getField();
            switch(field.getType()) {
            case pvByte:
            case pvShort:
            case pvInt:
            case pvLong:
                valueType = ValueType.integer; break;
            case pvFloat:
            case pvDouble:
                valueType = ValueType.floating; break;
            case pvString:
                valueStringPVField = (PVString)valuePVField;
                valueType = ValueType.string; break;
            default:
                pvLink.message("value field is not a supported type", MessageType.fatalError);
                return;
            }
        }      
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.devEpics.AbstractPdrvLinkSupport#uninitialize()
         */
        public void uninitialize() {
            super.uninitBase();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.devEpics.AbstractPdrvLinkSupport#start()
         */
        public void start() {
            if(!super.startBase()) return;
            switch(valueType) {
            case integer:
            case floating:
                pvLink.message("value field is not yet supported type", MessageType.fatalError);
                return;
            case string:
                Interface iface = device.findInterface(user, "octet", true);
                if(iface==null) {
                    pvLink.message("interface octet not supported", MessageType.fatalError);
                    setSupportState(SupportState.readyForInitialize);
                    return;
                }
                octet = (Octet)iface;
            }           
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.devEpics.AbstractPdrvLinkSupport#stop()
         */
        public void stop() {
            super.stopBase();
        }            
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.devEpics.AbstractPdrvLinkSupport#processContinue()
         */
        public void processContinue() {
            supportProcessRequester.supportProcessDone(RequestResult.success);
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.devEpics.AbstractPdrvLinkSupport#queueCallback()
         */
        public void queueCallback() {
            String string = valueStringPVField.get();
            int nbytesWrite = string.length();
            if(nbytesWrite>octetArray.length) nbytesWrite = octetArray.length;
            string.getBytes(0, nbytesWrite, octetArray, 0);
            Status status = octet.write(user, octetArray, nbytesWrite);
            if(status!=Status.success) {
                alarmSupport.setAlarm(user.getMessage(), AlarmSeverity.invalid);
                return;
            }
            deviceTrace.printIO(Trace.IO_SUPPORT, octetArray, user.getInt(), "%s", fullName);
        }
    }
}
