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
        if(supportName.equals(pdrvOctetInputSupportName)) return new OctetInput(dbLink);
        if(supportName.equals(pdrvOctetOutputSupportName)) return new OctetOutput(dbLink);
        if(supportName.equals(pdrvInt32InputSupportName)) return new Int32Input(dbLink);
        if(supportName.equals(pdrvInt32OutputSupportName)) return new Int32Output(dbLink);
        if(supportName.equals(pdrvInt32ArrayInputSupportName)) return new Int32ArrayInput(dbLink);
        if(supportName.equals(pdrvInt32ArrayOutputSupportName)) return new Int32ArrayOutput(dbLink);
        if(supportName.equals(pdrvFloat64InputSupportName)) return new Float64Input(dbLink);
        if(supportName.equals(pdrvFloat64OutputSupportName)) return new Float64Output(dbLink);
        if(supportName.equals(pdrvFloat64ArrayInputSupportName)) return new Float64ArrayInput(dbLink);
        if(supportName.equals(pdrvFloat64ArrayOutputSupportName)) return new Float64ArrayOutput(dbLink);
        if(supportName.equals(pdrvUInt32DigitalInputSupportName)) return new UInt32DigitalInput(dbLink);
        if(supportName.equals(pdrvUInt32DigitalOutputSupportName)) return new UInt32DigitalOutput(dbLink);
        dbLink.getPVLink().message("no support for " + supportName, MessageType.fatalError);
        return null;
    }
    private static final String pdrvOctetInputSupportName = "pdrvOctetInput";
    private static final String pdrvOctetOutputSupportName = "pdrvOctetOutput";
    private static final String pdrvInt32InputSupportName = "pdrvInt32Input";
    private static final String pdrvInt32OutputSupportName = "pdrvInt32Output";
    private static final String pdrvInt32ArrayInputSupportName = "pdrvInt32ArrayInput";
    private static final String pdrvInt32ArrayOutputSupportName = "pdrvInt32ArrayOutput";
    private static final String pdrvFloat64InputSupportName = "pdrvFloat64Input";
    private static final String pdrvFloat64OutputSupportName = "pdrvFloat64Output";
    private static final String pdrvFloat64ArrayInputSupportName = "pdrvFloat64ArrayInput";
    private static final String pdrvFloat64ArrayOutputSupportName = "pdrvFloat64ArrayOutput";
    private static final String pdrvUInt32DigitalInputSupportName = "pdrvUInt32DigitalInput";
    private static final String pdrvUInt32DigitalOutputSupportName = "pdrvUInt32DigitalOutput";
    
    private static Convert convert = ConvertFactory.getConvert();
    
    private static enum OctetValueType {
        string,
        bool,
        numeric,
        array
    }
    
    private static class OctetInput extends AbstractPdrvLinkSupport
    {
        private OctetInput(DBLink dbLink) {
            super(pdrvOctetInputSupportName,dbLink);
        }
        
        private OctetValueType octetValueType;
        private int size = 0;
        
        private Octet octet = null;
        private byte[] octetArray = null;
        private char[] charArray = null;
        private int nbytes = 0;
        private Status status = Status.success;
        
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.devEpics.AbstractPdrvLinkSupport#initialize()
         */
        public void initialize() {
            if(!super.initBase()) return;
            PVField pvField = valueDBField.getPVField();
            Field field = pvField.getField();
            Type type = field.getType();
            if(type==Type.pvString) {
                octetValueType = OctetValueType.string;
            } else if(type==Type.pvBoolean) {
                octetValueType = OctetValueType.bool;
            } else if(type.isNumeric()) {
                octetValueType = OctetValueType.numeric;
            } else if(type==Type.pvArray) {
                Array array = (Array)field;
                Type elementType = array.getElementType();
                if(!elementType.isNumeric()) {
                    pvLink.message("value field is not a supported type", MessageType.fatalError);
                    super.uninitBase();
                    return;
                }
                octetValueType = OctetValueType.array;
            } else {
                pvLink.message("value field is not a supported type", MessageType.fatalError);
                super.uninitBase();
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
            size = pvSize.get();
            octetArray = new byte[size];
            if(octetValueType!=OctetValueType.array) charArray = new char[size];
            Interface iface = device.findInterface(user, "octet", true);
            if(iface==null) {
                pvLink.message("interface octet not supported", MessageType.fatalError);
                setSupportState(SupportState.readyForInitialize);
                return;
            }
            octet = (Octet)iface;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.devEpics.AbstractPdrvLinkSupport#stop()
         */
        public void stop() {
            super.stopBase();
            octetArray = null;
            charArray = null;
        }            
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.devEpics.AbstractPdrvLinkSupport#processContinue()
         */
        public void processContinue() {
            if(status==Status.success) {
                if(octetValueType==OctetValueType.array) {
                    convert.fromByteArray(valuePVField, 0, nbytes, octetArray, 0);
                } else {
                    for(int i=0; i<nbytes; i++) charArray[i] = (char)octetArray[i];
                    String string = String.copyValueOf(charArray, 0, nbytes);
                    convert.fromString(valuePVField, string);
                }
                valueDBField.postPut();
            } else {
                alarmSupport.setAlarm(user.getMessage(), AlarmSeverity.invalid);
            }
            supportProcessRequester.supportProcessDone(RequestResult.success);
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.devEpics.AbstractPdrvLinkSupport#queueCallback()
         */
        public void queueCallback() {
            status = octet.read(user, octetArray, size);
            if(status!=Status.success) {
                alarmSupport.setAlarm(user.getMessage(), AlarmSeverity.invalid);
                return;
            }
            nbytes = user.getInt();
            deviceTrace.printIO(Trace.IO_SUPPORT, octetArray, user.getInt(), "%s", fullName);
        }
    }
    
    private static class OctetOutput extends AbstractPdrvLinkSupport
    {
        private OctetOutput(DBLink dbLink) {
            super(pdrvOctetOutputSupportName,dbLink);
        }
        
        private OctetValueType octetValueType;
        private int size = 0;
        
        private Octet octet = null;
        private byte[] octetArray = null;
        private int nbytes = 0;
        private Status status = Status.success;
        
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.devEpics.AbstractPdrvLinkSupport#initialize()
         */
        public void initialize() {
            if(!super.initBase()) return;
            PVField pvField = valueDBField.getPVField();
            Field field = pvField.getField();
            Type type = field.getType();
            if(type==Type.pvString) {
                octetValueType = OctetValueType.string;
            } else if(type==Type.pvBoolean) {
                octetValueType = OctetValueType.bool;
            } else if(type.isNumeric()) {
                octetValueType = OctetValueType.numeric;
            } else if(type==Type.pvArray) {
                Array array = (Array)field;
                Type elementType = array.getElementType();
                if(!elementType.isNumeric()) {
                    pvLink.message("value field is not a supported type", MessageType.fatalError);
                    super.uninitBase();
                    return;
                }
                octetValueType = OctetValueType.array;
            } else {
                pvLink.message("value field is not a supported type", MessageType.fatalError);
                super.uninitBase();
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
            size = pvSize.get();
            octetArray = new byte[size];
            Interface iface = device.findInterface(user, "octet", true);
            if(iface==null) {
                pvLink.message("interface octet not supported", MessageType.fatalError);
                setSupportState(SupportState.readyForInitialize);
                return;
            }
            octet = (Octet)iface;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.devEpics.AbstractPdrvLinkSupport#stop()
         */
        public void stop() {
            super.stopBase();
            octetArray = null;
        } 
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.devEpics.AbstractPdrvLinkSupport#process(org.epics.ioc.process.SupportProcessRequester)
         */
        public void process(SupportProcessRequester supportProcessRequester) {
            if(octetValueType==OctetValueType.array) {
                nbytes = convert.toByteArray(valuePVField, 0, size, octetArray, 0);
            } else {
                String string = convert.getString(valuePVField);
                nbytes = string.length();
                if(size<nbytes) {
                    size = nbytes;
                    octetArray = new byte[size];
                }
                for(int i=0; i<nbytes; i++) {
                    char nextChar = string.charAt(i);
                    octetArray[i] = (byte)nextChar;
                }
            }
            super.process(supportProcessRequester);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.devEpics.AbstractPdrvLinkSupport#processContinue()
         */
        public void processContinue() {
            if(status!=Status.success) {
                alarmSupport.setAlarm(user.getMessage(), AlarmSeverity.invalid);
            }
            supportProcessRequester.supportProcessDone(RequestResult.success);
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.devEpics.AbstractPdrvLinkSupport#queueCallback()
         */
        public void queueCallback() {
            status = octet.write(user, octetArray, size);
            if(status!=Status.success) {
                alarmSupport.setAlarm(user.getMessage(), AlarmSeverity.invalid);
                return;
            }
            deviceTrace.printIO(Trace.IO_SUPPORT, octetArray, user.getInt(), "%s", fullName);
        }
    }
    
    private static class Int32Input extends AbstractPdrvLinkSupport
    {
        private Int32Input(DBLink dbLink) {
            super(pdrvOctetInputSupportName,dbLink);
        }

        private PVField valuePVField = null;
        private Int32 int32 = null;
        private int value;
        private Status status = Status.success;
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.devEpics.AbstractPdrvLinkSupport#initialize()
         */
        public void initialize() {
            if(!super.initBase()) return;
            PVField pvField = valueDBField.getPVField();
            Field field = pvField.getField();
            if(field.getType().isScalar()) {
                valuePVField = pvField;
                return;
            }
            super.uninitBase();
            pvLink.message("value field is not a scalar type", MessageType.fatalError);
            return;
        }      
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.devEpics.AbstractPdrvLinkSupport#uninitialize()
         */
        public void uninitialize() {
            super.uninitBase();
            valuePVField = null;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.devEpics.AbstractPdrvLinkSupport#start()
         */
        public void start() {
            if(!super.startBase()) return;
            Interface iface = device.findInterface(user, "int32", true);
            if(iface==null) {
                pvLink.message("interface int32 not supported", MessageType.fatalError);
                setSupportState(SupportState.readyForInitialize);
                super.stopBase();
                return;
            }
            int32 = (Int32)iface;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.devEpics.AbstractPdrvLinkSupport#stop()
         */
        public void stop() {
            super.stopBase();
            int32 = null;
        }            
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.devEpics.AbstractPdrvLinkSupport#processContinue()
         */
        public void processContinue() {
            if(status==Status.success) {
                convert.fromInt(valuePVField, value);
                valueDBField.postPut();
            } else {
                alarmSupport.setAlarm(user.getMessage(), AlarmSeverity.invalid);
            }
            supportProcessRequester.supportProcessDone(RequestResult.success);
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.devEpics.AbstractPdrvLinkSupport#queueCallback()
         */
        public void queueCallback() {
            Status status = int32.read(user);
            if(status==Status.success) value = user.getInt();
            deviceTrace.print(Trace.IO_SUPPORT, "%s value = %d", fullName,value);
        }
    }
    
    private static class Int32Output extends AbstractPdrvLinkSupport
    {
        private Int32Output(DBLink dbLink) {
            super(pdrvOctetInputSupportName,dbLink);
        }

        private PVField valuePVField = null;
        private Int32 int32 = null;
        private int value;
        private Status status = Status.success;
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.devEpics.AbstractPdrvLinkSupport#initialize()
         */
        public void initialize() {
            if(!super.initBase()) return;
            PVField pvField = valueDBField.getPVField();
            Field field = pvField.getField();
            if(field.getType().isScalar()) {
                valuePVField = pvField;
                return;
            }
            super.uninitBase();
            pvLink.message("value field is not a scalar type", MessageType.fatalError);
            return;
        }      
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.devEpics.AbstractPdrvLinkSupport#uninitialize()
         */
        public void uninitialize() {
            super.uninitBase();
            valuePVField = null;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.devEpics.AbstractPdrvLinkSupport#start()
         */
        public void start() {
            if(!super.startBase()) return;
            Interface iface = device.findInterface(user, "int32", true);
            if(iface==null) {
                pvLink.message("interface int32 not supported", MessageType.fatalError);
                setSupportState(SupportState.readyForInitialize);
                super.stopBase();
                return;
            }
            int32 = (Int32)iface;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.devEpics.AbstractPdrvLinkSupport#stop()
         */
        public void stop() {
            super.stopBase();
            int32 = null;
        }            
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.devEpics.AbstractPdrvLinkSupport#process(org.epics.ioc.process.SupportProcessRequester)
         */
        public void process(SupportProcessRequester supportProcessRequester) {
            value = convert.toInt(valuePVField);
            super.process(supportProcessRequester);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.devEpics.AbstractPdrvLinkSupport#processContinue()
         */
        public void processContinue() {
            if(status!=Status.success) {
                alarmSupport.setAlarm(user.getMessage(), AlarmSeverity.invalid);
            }
            supportProcessRequester.supportProcessDone(RequestResult.success);
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.devEpics.AbstractPdrvLinkSupport#queueCallback()
         */
        public void queueCallback() {
            deviceTrace.print(Trace.IO_SUPPORT, "%s value = %d", fullName,value);
            status = int32.write(user, value);
        }
    }
    
    private static class Int32ArrayInput extends AbstractPdrvLinkSupport
    implements AsynAccessListener
    {
        private Int32ArrayInput(DBLink dbLink) {
            super(pdrvOctetInputSupportName,dbLink);
        }

        private PVArray valuePVArray = null;
        private Int32Array int32Array = null;
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.devEpics.AbstractPdrvLinkSupport#initialize()
         */
        public void initialize() {
            if(!super.initBase()) return;
            PVField pvField = valueDBField.getPVField();
            Field field = pvField.getField();
            Type type = field.getType();
            if(type!=Type.pvArray) {
                super.uninitBase();
                pvLink.message("value field is not an array", MessageType.fatalError);
                return;
            }
            Array array = (Array)field;
            Type elementType = array.getElementType();
            if(elementType.isNumeric()) {
                valuePVArray = (PVArray)pvField;
                return;
            }
            super.uninitBase();
            pvLink.message("value field is not a scalar type", MessageType.fatalError);
            return;
        }      
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.devEpics.AbstractPdrvLinkSupport#uninitialize()
         */
        public void uninitialize() {
            super.uninitBase();
            valuePVArray = null;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.devEpics.AbstractPdrvLinkSupport#start()
         */
        public void start() {
            if(!super.startBase()) return;
            Interface iface = device.findInterface(user, "int32Array", true);
            if(iface==null) {
                pvLink.message("interface int32Array not supported", MessageType.fatalError);
                setSupportState(SupportState.readyForInitialize);
                super.stopBase();
                return;
            }
            int32Array = (Int32Array)iface;
            int32Array.asynAccessListenerAdd(this);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.devEpics.AbstractPdrvLinkSupport#stop()
         */
        public void stop() {
            super.stopBase();
            int32Array.asynAccessListenerRemove(this);
            int32Array = null;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.devEpics.AbstractPdrvLinkSupport#process(org.epics.ioc.process.SupportProcessRequester)
         */
        public void process(SupportProcessRequester supportProcessRequester) {
            valuePVArray.asynModifyStart(this);
            super.process(supportProcessRequester);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.devEpics.AbstractPdrvLinkSupport#processContinue()
         */
        public void processContinue() {
            valuePVArray.asynModifyEnd(this);
            valueDBField.postPut();
            supportProcessRequester.supportProcessDone(RequestResult.success);
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.devEpics.AbstractPdrvLinkSupport#queueCallback()
         */
        public void queueCallback() {
            convert.copyArray(int32Array, 0, valuePVArray, 0, int32Array.getLength());
            deviceTrace.print(Trace.IO_SUPPORT, "%s", fullName);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.AsynAccessListener#beginSyncAccess()
         */
        public void beginSyncAccess() {
            dbRecord.lock();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.AsynAccessListener#endSyncAccess()
         */
        public void endSyncAccess() {
            dbRecord.unlock();
        }
    }
    
    private static class Int32ArrayOutput extends AbstractPdrvLinkSupport
    implements AsynAccessListener
    {
        private Int32ArrayOutput(DBLink dbLink) {
            super(pdrvOctetOutputSupportName,dbLink);
        }

        private PVArray valuePVArray = null;
        private Int32Array int32Array = null;
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.devEpics.AbstractPdrvLinkSupport#initialize()
         */
        public void initialize() {
            if(!super.initBase()) return;
            PVField pvField = valueDBField.getPVField();
            Field field = pvField.getField();
            Type type = field.getType();
            if(type!=Type.pvArray) {
                super.uninitBase();
                pvLink.message("value field is not an array", MessageType.fatalError);
                return;
            }
            Array array = (Array)field;
            Type elementType = array.getElementType();
            if(elementType.isNumeric()) {
                valuePVArray = (PVArray)pvField;
                return;
            }
            super.uninitBase();
            pvLink.message("value field is not a scalar type", MessageType.fatalError);
            return;
        }      
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.devEpics.AbstractPdrvLinkSupport#uninitialize()
         */
        public void uninitialize() {
            super.uninitBase();
            valuePVArray = null;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.devEpics.AbstractPdrvLinkSupport#start()
         */
        public void start() {
            if(!super.startBase()) return;
            Interface iface = device.findInterface(user, "int32Array", true);
            if(iface==null) {
                pvLink.message("interface int32Array not supported", MessageType.fatalError);
                setSupportState(SupportState.readyForInitialize);
                super.stopBase();
                return;
            }
            int32Array = (Int32Array)iface;
            int32Array.asynAccessListenerAdd(this);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.devEpics.AbstractPdrvLinkSupport#stop()
         */
        public void stop() {
            super.stopBase();
            int32Array.asynAccessListenerRemove(this);
            int32Array = null;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.devEpics.AbstractPdrvLinkSupport#process(org.epics.ioc.process.SupportProcessRequester)
         */
        public void process(SupportProcessRequester supportProcessRequester) {
            super.process(supportProcessRequester);
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
            convert.copyArray(valuePVArray, 0, int32Array, 0, valuePVArray.getLength());
            deviceTrace.print(Trace.IO_SUPPORT, "%s", fullName);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.AsynAccessListener#beginSyncAccess()
         */
        public void beginSyncAccess() {
            dbRecord.lock();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.AsynAccessListener#endSyncAccess()
         */
        public void endSyncAccess() {
            dbRecord.unlock();
        }
    }
    
    private static class UInt32DigitalInput extends AbstractPdrvLinkSupport
    {
        private UInt32DigitalInput(DBLink dbLink) {
            super(pdrvOctetInputSupportName,dbLink);
        }

        private PVField valuePVField = null;
        private UInt32Digital uint32Digital = null;
        private int value;
        private int mask;
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.devEpics.AbstractPdrvLinkSupport#initialize()
         */
        public void initialize() {
            if(!super.initBase()) return;
            PVField pvField = valueDBField.getPVField();
            Field field = pvField.getField();
            if(field.getType().isScalar()) {
                valuePVField = pvField;
                return;
            }
            super.uninitBase();
            pvLink.message("value field is not a scalar type", MessageType.fatalError);
            return;
        }      
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.devEpics.AbstractPdrvLinkSupport#uninitialize()
         */
        public void uninitialize() {
            super.uninitBase();
            valuePVField = null;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.devEpics.AbstractPdrvLinkSupport#start()
         */
        public void start() {
            if(!super.startBase()) return;
            Interface iface = device.findInterface(user, "uint32Digital", true);
            if(iface==null) {
                pvLink.message("interface uint32Digital not supported", MessageType.fatalError);
                setSupportState(SupportState.readyForInitialize);
                super.stopBase();
                return;
            }
            uint32Digital = (UInt32Digital)iface;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.devEpics.AbstractPdrvLinkSupport#stop()
         */
        public void stop() {
            super.stopBase();
            uint32Digital = null;
        } 
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.devEpics.AbstractPdrvLinkSupport#process(org.epics.ioc.process.SupportProcessRequester)
         */
        public void process(SupportProcessRequester supportProcessRequester) {
            mask = pvMask.get();
            super.process(supportProcessRequester);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.devEpics.AbstractPdrvLinkSupport#processContinue()
         */
        public void processContinue() {
            convert.fromInt(valuePVField, value);
            valueDBField.postPut();
            supportProcessRequester.supportProcessDone(RequestResult.success);
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.devEpics.AbstractPdrvLinkSupport#queueCallback()
         */
        public void queueCallback() {
            Status status = uint32Digital.read(user,mask);
            if(status!=Status.success) {
                alarmSupport.setAlarm(user.getMessage(), AlarmSeverity.invalid);
                return;
            }
            value = user.getInt();
            deviceTrace.print(Trace.IO_SUPPORT, "%s value = %d", fullName,value);
        }
    }
    
    private static class UInt32DigitalOutput extends AbstractPdrvLinkSupport
    {
        private UInt32DigitalOutput(DBLink dbLink) {
            super(pdrvOctetInputSupportName,dbLink);
        }

        private PVField valuePVField = null;
        private UInt32Digital uint32Digital = null;
        private int value;
        private int mask;
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.devEpics.AbstractPdrvLinkSupport#initialize()
         */
        public void initialize() {
            if(!super.initBase()) return;
            PVField pvField = valueDBField.getPVField();
            Field field = pvField.getField();
            if(field.getType().isScalar()) {
                valuePVField = pvField;
                return;
            }
            super.uninitBase();
            pvLink.message("value field is not a scalar type", MessageType.fatalError);
            return;
        }      
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.devEpics.AbstractPdrvLinkSupport#uninitialize()
         */
        public void uninitialize() {
            super.uninitBase();
            valuePVField = null;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.devEpics.AbstractPdrvLinkSupport#start()
         */
        public void start() {
            if(!super.startBase()) return;
            Interface iface = device.findInterface(user, "uint32Digital", true);
            if(iface==null) {
                pvLink.message("interface uint32Digital not supported", MessageType.fatalError);
                setSupportState(SupportState.readyForInitialize);
                super.stopBase();
                return;
            }
            uint32Digital = (UInt32Digital)iface;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.devEpics.AbstractPdrvLinkSupport#stop()
         */
        public void stop() {
            super.stopBase();
            uint32Digital = null;
        }            
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.devEpics.AbstractPdrvLinkSupport#process(org.epics.ioc.process.SupportProcessRequester)
         */
        public void process(SupportProcessRequester supportProcessRequester) {
            value = convert.toInt(valuePVField);
            mask = pvMask.get();
            super.process(supportProcessRequester);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.devEpics.AbstractPdrvLinkSupport#queueCallback()
         */
        public void queueCallback() {
            Status status = uint32Digital.write(user, value,mask);
            if(status!=Status.success) {
                alarmSupport.setAlarm(user.getMessage(), AlarmSeverity.invalid);
                return;
            }
            deviceTrace.print(Trace.IO_SUPPORT, "%s value = %d", fullName,value);
        }
    }
    
    private static class Float64Input extends AbstractPdrvLinkSupport
    {
        private Float64Input(DBLink dbLink) {
            super(pdrvOctetInputSupportName,dbLink);
        }

        private PVField valuePVField = null;
        private Float64 float64 = null;
        private int value;
        private Status status = Status.success;
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.devEpics.AbstractPdrvLinkSupport#initialize()
         */
        public void initialize() {
            if(!super.initBase()) return;
            PVField pvField = valueDBField.getPVField();
            Field field = pvField.getField();
            if(field.getType().isScalar()) {
                valuePVField = pvField;
                return;
            }
            super.uninitBase();
            pvLink.message("value field is not a scalar type", MessageType.fatalError);
            return;
        }      
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.devEpics.AbstractPdrvLinkSupport#uninitialize()
         */
        public void uninitialize() {
            super.uninitBase();
            valuePVField = null;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.devEpics.AbstractPdrvLinkSupport#start()
         */
        public void start() {
            if(!super.startBase()) return;
            Interface iface = device.findInterface(user, "float64", true);
            if(iface==null) {
                pvLink.message("interface float64 not supported", MessageType.fatalError);
                setSupportState(SupportState.readyForInitialize);
                super.stopBase();
                return;
            }
            float64 = (Float64)iface;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.devEpics.AbstractPdrvLinkSupport#stop()
         */
        public void stop() {
            super.stopBase();
            float64 = null;
        }            
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.devEpics.AbstractPdrvLinkSupport#processContinue()
         */
        public void processContinue() {
            if(status==Status.success) {
                convert.fromInt(valuePVField, value);
                valueDBField.postPut();
            } else {
                alarmSupport.setAlarm(user.getMessage(), AlarmSeverity.invalid);
            }
            supportProcessRequester.supportProcessDone(RequestResult.success);
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.devEpics.AbstractPdrvLinkSupport#queueCallback()
         */
        public void queueCallback() {
            Status status = float64.read(user);
            if(status==Status.success) value = user.getInt();
            deviceTrace.print(Trace.IO_SUPPORT, "%s value = %d", fullName,value);
        }
    }
    
    private static class Float64Output extends AbstractPdrvLinkSupport
    {
        private Float64Output(DBLink dbLink) {
            super(pdrvOctetInputSupportName,dbLink);
        }

        private PVField valuePVField = null;
        private Float64 float64 = null;
        private int value;
        private Status status = Status.success;
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.devEpics.AbstractPdrvLinkSupport#initialize()
         */
        public void initialize() {
            if(!super.initBase()) return;
            PVField pvField = valueDBField.getPVField();
            Field field = pvField.getField();
            if(field.getType().isScalar()) {
                valuePVField = pvField;
                return;
            }
            super.uninitBase();
            pvLink.message("value field is not a scalar type", MessageType.fatalError);
            return;
        }      
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.devEpics.AbstractPdrvLinkSupport#uninitialize()
         */
        public void uninitialize() {
            super.uninitBase();
            valuePVField = null;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.devEpics.AbstractPdrvLinkSupport#start()
         */
        public void start() {
            if(!super.startBase()) return;
            Interface iface = device.findInterface(user, "float64", true);
            if(iface==null) {
                pvLink.message("interface float64 not supported", MessageType.fatalError);
                setSupportState(SupportState.readyForInitialize);
                super.stopBase();
                return;
            }
            float64 = (Float64)iface;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.devEpics.AbstractPdrvLinkSupport#stop()
         */
        public void stop() {
            super.stopBase();
            float64 = null;
        }            
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.devEpics.AbstractPdrvLinkSupport#process(org.epics.ioc.process.SupportProcessRequester)
         */
        public void process(SupportProcessRequester supportProcessRequester) {
            value = convert.toInt(valuePVField);
            super.process(supportProcessRequester);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.devEpics.AbstractPdrvLinkSupport#processContinue()
         */
        public void processContinue() {
            if(status!=Status.success) {
                alarmSupport.setAlarm(user.getMessage(), AlarmSeverity.invalid);
            }
            supportProcessRequester.supportProcessDone(RequestResult.success);
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.devEpics.AbstractPdrvLinkSupport#queueCallback()
         */
        public void queueCallback() {
            deviceTrace.print(Trace.IO_SUPPORT, "%s value = %d", fullName,value);
            status = float64.write(user, value);
        }
    }
    
    private static class Float64ArrayInput extends AbstractPdrvLinkSupport
    implements AsynAccessListener
    {
        private Float64ArrayInput(DBLink dbLink) {
            super(pdrvOctetInputSupportName,dbLink);
        }

        private PVArray valuePVArray = null;
        private Float64Array float64Array = null;
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.devEpics.AbstractPdrvLinkSupport#initialize()
         */
        public void initialize() {
            if(!super.initBase()) return;
            PVField pvField = valueDBField.getPVField();
            Field field = pvField.getField();
            Type type = field.getType();
            if(type!=Type.pvArray) {
                super.uninitBase();
                pvLink.message("value field is not an array", MessageType.fatalError);
                return;
            }
            Array array = (Array)field;
            Type elementType = array.getElementType();
            if(elementType.isNumeric()) {
                valuePVArray = (PVArray)pvField;
                return;
            }
            super.uninitBase();
            pvLink.message("value field is not a scalar type", MessageType.fatalError);
            return;
        }      
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.devEpics.AbstractPdrvLinkSupport#uninitialize()
         */
        public void uninitialize() {
            super.uninitBase();
            valuePVArray = null;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.devEpics.AbstractPdrvLinkSupport#start()
         */
        public void start() {
            if(!super.startBase()) return;
            Interface iface = device.findInterface(user, "float64Array", true);
            if(iface==null) {
                pvLink.message("interface float64Array not supported", MessageType.fatalError);
                setSupportState(SupportState.readyForInitialize);
                super.stopBase();
                return;
            }
            float64Array = (Float64Array)iface;
            float64Array.asynAccessListenerAdd(this);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.devEpics.AbstractPdrvLinkSupport#stop()
         */
        public void stop() {
            super.stopBase();
            float64Array.asynAccessListenerRemove(this);
            float64Array = null;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.devEpics.AbstractPdrvLinkSupport#process(org.epics.ioc.process.SupportProcessRequester)
         */
        public void process(SupportProcessRequester supportProcessRequester) {
            valuePVArray.asynModifyStart(this);
            super.process(supportProcessRequester);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.devEpics.AbstractPdrvLinkSupport#processContinue()
         */
        public void processContinue() {
            valuePVArray.asynModifyEnd(this);
            valueDBField.postPut();
            supportProcessRequester.supportProcessDone(RequestResult.success);
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.devEpics.AbstractPdrvLinkSupport#queueCallback()
         */
        public void queueCallback() {
            convert.copyArray(float64Array, 0, valuePVArray, 0, float64Array.getLength());
            deviceTrace.print(Trace.IO_SUPPORT, "%s", fullName);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.AsynAccessListener#beginSyncAccess()
         */
        public void beginSyncAccess() {
            dbRecord.lock();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.AsynAccessListener#endSyncAccess()
         */
        public void endSyncAccess() {
            dbRecord.unlock();
        }
    }
    
    private static class Float64ArrayOutput extends AbstractPdrvLinkSupport
    implements AsynAccessListener
    {
        private Float64ArrayOutput(DBLink dbLink) {
            super(pdrvOctetOutputSupportName,dbLink);
        }

        private PVArray valuePVArray = null;
        private Float64Array float64Array = null;
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.devEpics.AbstractPdrvLinkSupport#initialize()
         */
        public void initialize() {
            if(!super.initBase()) return;
            PVField pvField = valueDBField.getPVField();
            Field field = pvField.getField();
            Type type = field.getType();
            if(type!=Type.pvArray) {
                super.uninitBase();
                pvLink.message("value field is not an array", MessageType.fatalError);
                return;
            }
            Array array = (Array)field;
            Type elementType = array.getElementType();
            if(elementType.isNumeric()) {
                valuePVArray = (PVArray)pvField;
                return;
            }
            super.uninitBase();
            pvLink.message("value field is not a scalar type", MessageType.fatalError);
            return;
        }      
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.devEpics.AbstractPdrvLinkSupport#uninitialize()
         */
        public void uninitialize() {
            super.uninitBase();
            valuePVArray = null;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.devEpics.AbstractPdrvLinkSupport#start()
         */
        public void start() {
            if(!super.startBase()) return;
            Interface iface = device.findInterface(user, "float64Array", true);
            if(iface==null) {
                pvLink.message("interface float64Array not supported", MessageType.fatalError);
                setSupportState(SupportState.readyForInitialize);
                super.stopBase();
                return;
            }
            float64Array = (Float64Array)iface;
            float64Array.asynAccessListenerAdd(this);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.devEpics.AbstractPdrvLinkSupport#stop()
         */
        public void stop() {
            super.stopBase();
            float64Array.asynAccessListenerRemove(this);
            float64Array = null;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.devEpics.AbstractPdrvLinkSupport#process(org.epics.ioc.process.SupportProcessRequester)
         */
        public void process(SupportProcessRequester supportProcessRequester) {
            super.process(supportProcessRequester);
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
            convert.copyArray(valuePVArray, 0, float64Array, 0, valuePVArray.getLength());
            deviceTrace.print(Trace.IO_SUPPORT, "%s", fullName);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.AsynAccessListener#beginSyncAccess()
         */
        public void beginSyncAccess() {
            dbRecord.lock();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.AsynAccessListener#endSyncAccess()
         */
        public void endSyncAccess() {
            dbRecord.unlock();
        }
    }
}
