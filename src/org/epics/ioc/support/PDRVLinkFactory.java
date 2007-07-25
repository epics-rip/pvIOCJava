 /**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support;

import org.epics.ioc.pdrv.*;
import org.epics.ioc.db.*;
import org.epics.ioc.pdrv.interfaces.*;
import org.epics.ioc.process.*;
import org.epics.ioc.pv.*;
import org.epics.ioc.util.*;

/**
 * Factory to create portDriver link support.
 * @author mrk
 *
 */
public class PDRVLinkFactory {
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
        if(supportName.equals(pdrvOctetInputSupportName))
            return new OctetInput(dbLink,pdrvOctetInputSupportName);
        if(supportName.equals(pdrvOctetInterruptInputSupportName))
            return new OctetInterruptInput(dbLink,pdrvOctetInterruptInputSupportName);
        if(supportName.equals(pdrvOctetOutputSupportName))
            return new OctetOutput(dbLink,pdrvOctetOutputSupportName);
        if(supportName.equals(pdrvInt32InputSupportName))
            return new Int32Input(dbLink,pdrvInt32InputSupportName);
        if(supportName.equals(pdrvInt32InterruptInputSupportName))
            return new Int32InterruptInput(dbLink,pdrvInt32InterruptInputSupportName);
        if(supportName.equals(pdrvInt32AverageInputSupportName))
            return new Int32AverageInput(dbLink,pdrvInt32AverageInputSupportName);
        if(supportName.equals(pdrvInt32OutputSupportName))
            return new Int32Output(dbLink,pdrvInt32OutputSupportName);
        if(supportName.equals(pdrvInt32ArrayInputSupportName))
            return new Int32ArrayInput(dbLink,pdrvInt32ArrayInputSupportName);
        if(supportName.equals(pdrvInt32ArrayInterruptInputSupportName))
            return new Int32ArrayInterruptInput(dbLink,pdrvInt32ArrayInterruptInputSupportName);
        if(supportName.equals(pdrvInt32ArrayOutputSupportName))
            return new Int32ArrayOutput(dbLink,pdrvInt32ArrayOutputSupportName);
        if(supportName.equals(pdrvFloat64InputSupportName))
            return new Float64Input(dbLink,pdrvFloat64InputSupportName);
        if(supportName.equals(pdrvFloat64InterruptInputSupportName))
            return new Float64InterruptInput(dbLink,pdrvFloat64InterruptInputSupportName);
        if(supportName.equals(pdrvFloat64AverageInputSupportName))
            return new Float64AverageInput(dbLink,pdrvFloat64AverageInputSupportName);
        if(supportName.equals(pdrvFloat64OutputSupportName))
            return new Float64Output(dbLink,pdrvFloat64OutputSupportName);
        if(supportName.equals(pdrvFloat64ArrayInputSupportName))
            return new Float64ArrayInput(dbLink,pdrvFloat64ArrayInputSupportName);
        if(supportName.equals(pdrvFloat64ArrayInterruptInputSupportName))
            return new Float64ArrayInterruptInput(dbLink,pdrvFloat64ArrayInterruptInputSupportName);
        if(supportName.equals(pdrvFloat64ArrayOutputSupportName))
            return new Float64ArrayOutput(dbLink,pdrvFloat64ArrayOutputSupportName);
        if(supportName.equals(pdrvUInt32DigitalInputSupportName))
            return new UInt32DigitalInput(dbLink,pdrvUInt32DigitalInputSupportName);
        if(supportName.equals(pdrvUInt32DigitalInterruptInputSupportName))
            return new UInt32DigitalInterruptInput(dbLink,pdrvUInt32DigitalInterruptInputSupportName);
        if(supportName.equals(pdrvUInt32DigitalOutputSupportName))
            return new UInt32DigitalOutput(dbLink,pdrvUInt32DigitalOutputSupportName);
        dbLink.getPVLink().message("no support for " + supportName, MessageType.fatalError);
        return null;
    }
    private static final String pdrvOctetInputSupportName = "pdrvOctetInput";
    private static final String pdrvOctetInterruptInputSupportName = "pdrvOctetInterruptInput";
    private static final String pdrvOctetOutputSupportName = "pdrvOctetOutput";
    private static final String pdrvInt32InputSupportName = "pdrvInt32Input";
    private static final String pdrvInt32InterruptInputSupportName = "pdrvInt32InterruptInput";
    private static final String pdrvInt32AverageInputSupportName = "pdrvInt32AverageInput";
    private static final String pdrvInt32OutputSupportName = "pdrvInt32Output";
    private static final String pdrvInt32ArrayInputSupportName = "pdrvInt32ArrayInput";
    private static final String pdrvInt32ArrayInterruptInputSupportName = "pdrvInt32ArrayInterruptInput";
    private static final String pdrvInt32ArrayOutputSupportName = "pdrvInt32ArrayOutput";
    private static final String pdrvFloat64InputSupportName = "pdrvFloat64Input";
    private static final String pdrvFloat64InterruptInputSupportName = "pdrvFloat64InterruptInput";
    private static final String pdrvFloat64AverageInputSupportName = "pdrvFloat64AverageInput";
    private static final String pdrvFloat64OutputSupportName = "pdrvFloat64Output";
    private static final String pdrvFloat64ArrayInputSupportName = "pdrvFloat64ArrayInput";
    private static final String pdrvFloat64ArrayInterruptInputSupportName = "pdrvFloat64ArrayInterruptInput";
    private static final String pdrvFloat64ArrayOutputSupportName = "pdrvFloat64ArrayOutput";
    private static final String pdrvUInt32DigitalInputSupportName = "pdrvUInt32DigitalInput";
    private static final String pdrvUInt32DigitalInterruptInputSupportName = "pdrvUInt32DigitalInterruptInput";
    private static final String pdrvUInt32DigitalOutputSupportName = "pdrvUInt32DigitalOutput";
    
    private static Convert convert = ConvertFactory.getConvert();
    
    private static enum OctetValueType {
        string,
        bool,
        numeric,
        array
    }
    
    private static class OctetInput extends AbstractPDRVLinkSupport
    {
        private OctetInput(DBLink dbLink,String supportName) {
            super(supportName,dbLink);
        }
        
        private OctetValueType octetValueType;
        private int size = 0;
        
        private Octet octet = null;
        private byte[] octetArray = null;
        private char[] charArray = null;
        private int nbytes = 0;
        private Status status = Status.success;
        
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#initialize()
         */
        public void initialize() {
            super.initialize();
            if(!super.checkSupportState(SupportState.readyForStart,supportName)) return;
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
                    super.uninitialize();
                    return;
                }
                octetValueType = OctetValueType.array;
            } else {
                pvLink.message("value field is not a supported type", MessageType.fatalError);
                super.uninitialize();
                return;
            }
        }      
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#uninitialize()
         */
        public void uninitialize() {
            super.uninitialize();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#start()
         */
        public void start() {
            super.start();
            if(!super.checkSupportState(SupportState.ready,supportName)) return;
            size = pvSize.get();
            octetArray = new byte[size];
            if(octetValueType!=OctetValueType.array) charArray = new char[size];
            Interface iface = device.findInterface(user, "octet", true);
            if(iface==null) {
                pvLink.message("interface octet not supported", MessageType.fatalError);
                super.stop();
                return;
            }
            octet = (Octet)iface;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#stop()
         */
        public void stop() {
            super.stop();
            octetArray = null;
            charArray = null;
        }            
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#processContinue()
         */
        public void processContinue() {
            deviceTrace.print(Trace.FLOW,
                "%s:%s processContinue ",fullName,supportName);
            if(status==Status.success) {
                if(octetValueType==OctetValueType.array) {
                    convert.fromByteArray(valuePVField, 0, nbytes, octetArray, 0);
                } else {
                    for(int i=0; i<nbytes; i++) charArray[i] = (char)octetArray[i];
                    String string = String.copyValueOf(charArray, 0, nbytes);
                    convert.fromString(valuePVField, string);
                }
                deviceTrace.print(Trace.FLOW,
                    "%s:%s processContinue calling postPut",fullName,supportName);
                valueDBField.postPut();
            } else {
                alarmSupport.setAlarm(user.getMessage(), AlarmSeverity.invalid);
            }
            deviceTrace.print(Trace.FLOW,
                "%s:%s processContinue calling supportProcessDone",
                fullName,supportName);
            supportProcessRequester.supportProcessDone(RequestResult.success);
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#queueCallback()
         */
        public void queueCallback() {
            deviceTrace.print(Trace.FLOW,
                "%s:%s queueCallback calling read ",fullName,supportName);
            status = octet.read(user, octetArray, size);
            if(status!=Status.success) {
                alarmSupport.setAlarm(user.getMessage(), AlarmSeverity.invalid);
                return;
            }
            nbytes = user.getInt();
            deviceTrace.printIO(Trace.SUPPORT, octetArray, user.getInt(), "%s", fullName);
        }
    }
    
    private static class OctetInterruptInput extends AbstractPDRVLinkSupport
    implements OctetInterruptListener
    {
        private OctetInterruptInput(DBLink dbLink,String supportName) {
            super(supportName,dbLink);
        }
        
        private OctetValueType octetValueType;
        private int size = 0;
        
        private Octet octet = null;
        private byte[] octetArray = null;
        private char[] charArray = null;
        private int nbytes = 0;
        private Status status = Status.success;
        
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#initialize()
         */
        public void initialize() {
            super.initialize();
            if(!super.checkSupportState(SupportState.readyForStart,supportName)) return;
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
                    super.uninitialize();
                    return;
                }
                octetValueType = OctetValueType.array;
            } else {
                pvLink.message("value field is not a supported type", MessageType.fatalError);
                super.uninitialize();
                return;
            }
        }      
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#uninitialize()
         */
        public void uninitialize() {
            super.uninitialize();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#start()
         */
        public void start() {
            super.start();
            if(!super.checkSupportState(SupportState.ready,supportName)) return;
            size = pvSize.get();
            octetArray = new byte[size];
            if(octetValueType!=OctetValueType.array) charArray = new char[size];
            Interface iface = device.findInterface(user, "octet", true);
            if(iface==null) {
                pvLink.message("interface octet not supported", MessageType.fatalError);
                super.stop();
                return;
            }
            octet = (Octet)iface;
            octet.addInterruptUser(user, this);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#stop()
         */
        public void stop() {
            super.stop();
            octet.removeInterruptUser(user, this);
            octetArray = null;
            charArray = null;
        }            
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#processContinue()
         */
        public void processContinue() {
            deviceTrace.print(Trace.FLOW,
                "%s:%s processContinue ",fullName,supportName);
            if(status==Status.success) {
                putData();
            } else {
                alarmSupport.setAlarm(user.getMessage(), AlarmSeverity.invalid);
            }
            deviceTrace.print(Trace.FLOW,
                "%s:%s processContinue calling supportProcessDone",
                fullName,supportName);
            supportProcessRequester.supportProcessDone(RequestResult.success);
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.interfaces.OctetInterruptListener#interrupt(byte[], int)
         */
        public void interrupt(byte[] data, int nbytes) {
            octetArray = data;
            this.nbytes = nbytes;
            if(super.isProcess()) {
                recordProcess.process(this, false, null);
            } else {
                dbRecord.lock();
                try {
                    putData();
                    deviceTrace.print(Trace.FLOW,
                        "%s:%s interrupt and record not processed",
                        fullName,supportName);
                } finally {
                    dbRecord.unlock();
                }
            }
        }
        
        private void putData() {
            if(octetValueType==OctetValueType.array) {
                convert.fromByteArray(valuePVField, 0, nbytes, octetArray, 0);
            } else {
                for(int i=0; i<nbytes; i++) charArray[i] = (char)octetArray[i];
                String string = String.copyValueOf(charArray, 0, nbytes);
                convert.fromString(valuePVField, string);
            }
            deviceTrace.print(Trace.FLOW,
                "%s:%s putData and  calling postPut",fullName,supportName);
            valueDBField.postPut();
        }
    }
    
    private static class OctetOutput extends AbstractPDRVLinkSupport
    {
        private OctetOutput(DBLink dbLink,String supportName) {
            super(supportName,dbLink);
        }
        
        private OctetValueType octetValueType;
        private int size = 0;
        
        private Octet octet = null;
        private byte[] octetArray = null;
        private int nbytes = 0;
        private Status status = Status.success;
        
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#initialize()
         */
        public void initialize() {
            super.initialize();
            if(!super.checkSupportState(SupportState.readyForStart,supportName)) return;
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
                    super.uninitialize();
                    return;
                }
                octetValueType = OctetValueType.array;
            } else {
                pvLink.message("value field is not a supported type", MessageType.fatalError);
                super.uninitialize();
                return;
            }
        }      
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#uninitialize()
         */
        public void uninitialize() {
            super.uninitialize();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#start()
         */
        public void start() {
            super.start();
            if(!super.checkSupportState(SupportState.ready,supportName)) return;
            size = pvSize.get();
            octetArray = new byte[size];
            Interface iface = device.findInterface(user, "octet", true);
            if(iface==null) {
                pvLink.message("interface octet not supported", MessageType.fatalError);
                super.stop();
                return;
            }
            octet = (Octet)iface;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#stop()
         */
        public void stop() {
            super.stop();
            octetArray = null;
        } 
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#process(org.epics.ioc.process.SupportProcessRequester)
         */
        public void process(SupportProcessRequester supportProcessRequester) {
            deviceTrace.print(Trace.FLOW,
                "%s:%s process",fullName,supportName);
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
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#processContinue()
         */
        public void processContinue() {
            if(status!=Status.success) {
                alarmSupport.setAlarm(user.getMessage(), AlarmSeverity.invalid);
            }
            deviceTrace.print(Trace.FLOW,
                "%s:%s processContinue calling supportProcessDone",
                fullName,supportName);
            supportProcessRequester.supportProcessDone(RequestResult.success);
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#queueCallback()
         */
        public void queueCallback() {
            deviceTrace.print(Trace.FLOW,
                    "%s:%s queueCallback calling write",
                    fullName,supportName);
            status = octet.write(user, octetArray, size);
            if(status!=Status.success) {
                alarmSupport.setAlarm(user.getMessage(), AlarmSeverity.invalid);
                return;
            }
            deviceTrace.printIO(Trace.SUPPORT, octetArray, user.getInt(), "%s", fullName);
        }
    }
    
    private static class Int32Input extends AbstractPDRVLinkSupport
    {
        private Int32Input(DBLink dbLink,String supportName) {
            super(supportName,dbLink);
        }

        private PVField valuePVField = null;
        private Int32 int32 = null;
        private int value;
        private Status status = Status.success;
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#initialize()
         */
        public void initialize() {
            super.initialize();
            if(!super.checkSupportState(SupportState.readyForStart,supportName)) return;
            PVField pvField = valueDBField.getPVField();
            Field field = pvField.getField();
            if(field.getType().isScalar()) {
                valuePVField = pvField;
                return;
            }
            super.uninitialize();
            pvLink.message("value field is not a scalar type", MessageType.fatalError);
            return;
        }      
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#uninitialize()
         */
        public void uninitialize() {
            super.uninitialize();
            valuePVField = null;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#start()
         */
        public void start() {
            super.start();
            if(!super.checkSupportState(SupportState.ready,supportName)) return;
            Interface iface = device.findInterface(user, "int32", true);
            if(iface==null) {
                pvLink.message("interface int32 not supported", MessageType.fatalError);
                super.stop();
                return;
            }
            int32 = (Int32)iface;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#stop()
         */
        public void stop() {
            super.stop();
            int32 = null;
        }            
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#processContinue()
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
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#queueCallback()
         */
        public void queueCallback() {
            Status status = int32.read(user);
            if(status==Status.success) value = user.getInt();
            deviceTrace.print(Trace.SUPPORT, "%s value = %d", fullName,value);
        }
    }
    
    private static class Int32InterruptInput extends AbstractPDRVLinkSupport
    implements Int32InterruptListener
    {
        private Int32InterruptInput(DBLink dbLink,String supportName) {
            super(supportName,dbLink);
        }

        private PVField valuePVField = null;
        private Int32 int32 = null;
        private int value;
        private Status status = Status.success;
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#initialize()
         */
        public void initialize() {
            super.initialize();
            if(!super.checkSupportState(SupportState.readyForStart,supportName)) return;
            PVField pvField = valueDBField.getPVField();
            Field field = pvField.getField();
            if(field.getType().isScalar()) {
                valuePVField = pvField;
                return;
            }
            super.uninitialize();
            pvLink.message("value field is not a scalar type", MessageType.fatalError);
            return;
        }      
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#uninitialize()
         */
        public void uninitialize() {
            super.uninitialize();
            valuePVField = null;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#start()
         */
        public void start() {
            super.start();
            if(!super.checkSupportState(SupportState.ready,supportName)) return;
            Interface iface = device.findInterface(user, "int32", true);
            if(iface==null) {
                pvLink.message("interface int32 not supported", MessageType.fatalError);
                super.stop();
                return;
            }
            int32 = (Int32)iface;
            int32.addInterruptUser(user, this);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#stop()
         */
        public void stop() {
            super.stop();
            int32.removeInterruptUser(user, this);
            int32 = null;
        }            
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#processContinue()
         */
        public void processContinue() {
            if(status==Status.success) {
                putData();
            } else {
                alarmSupport.setAlarm(user.getMessage(), AlarmSeverity.invalid);
            }
            supportProcessRequester.supportProcessDone(RequestResult.success);
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#queueCallback()
         */
        public void queueCallback() {
            Status status = int32.read(user);
            if(status==Status.success) value = user.getInt();
            deviceTrace.print(Trace.SUPPORT, "%s value = %d", fullName,value);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.interfaces.Int32InterruptListener#interrupt(int)
         */
        public void interrupt(int value) {
            this.value = value;
            if(super.isProcess()) {
                recordProcess.process(this, false, null);
            } else {
                dbRecord.lock();
                try {
                    putData();
                    deviceTrace.print(Trace.FLOW,
                        "%s:%s interrupt and record not processed",
                        fullName,supportName);
                } finally {
                    dbRecord.unlock();
                }
            }
        }
        
        private void putData() {
            convert.fromInt(valuePVField, value);
            deviceTrace.print(Trace.FLOW,
                "%s:%s putData and  calling postPut",fullName,supportName);
            valueDBField.postPut();
        }
    }
    
    private static class Int32AverageInput extends AbstractPDRVLinkSupport
    implements Int32InterruptListener
    {
        private Int32AverageInput(DBLink dbLink,String supportName) {
            super(supportName,dbLink);
        }

        private PVField valuePVField = null;
        private Int32 int32 = null;
        private int numValues = 0;
        private long sum = 0;
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#initialize()
         */
        public void initialize() {
            super.initialize();
            if(!super.checkSupportState(SupportState.readyForStart,supportName)) return;
            PVField pvField = valueDBField.getPVField();
            Field field = pvField.getField();
            if(field.getType().isScalar()) {
                valuePVField = pvField;
                return;
            }
            super.uninitialize();
            pvLink.message("value field is not a scalar type", MessageType.fatalError);
            return;
        }      
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#uninitialize()
         */
        public void uninitialize() {
            super.uninitialize();
            valuePVField = null;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#start()
         */
        public void start() {
            super.start();
            if(!super.checkSupportState(SupportState.ready,supportName)) return;
            Interface iface = device.findInterface(user, "int32", true);
            if(iface==null) {
                pvLink.message("interface int32 not supported", MessageType.fatalError);
                super.stop();
                return;
            }
            int32 = (Int32)iface;
            int32.addInterruptUser(user, this);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#stop()
         */
        public void stop() {
            super.stop();
            int32.removeInterruptUser(user, this);
            int32 = null;
        }            
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#process(org.epics.ioc.process.SupportProcessRequester)
         */
        @Override
        public void process(SupportProcessRequester supportProcessRequester) {
            if(!super.checkSupportState(SupportState.ready,supportName)) {
                if(alarmSupport!=null) alarmSupport.setAlarm(
                        fullName + " not ready",
                        AlarmSeverity.major);
                supportProcessRequester.supportProcessDone(RequestResult.failure);
                return;
            }
            if(numValues==0) {
                if(alarmSupport!=null) alarmSupport.setAlarm(
                        fullName + " no new values",
                        AlarmSeverity.major);
            } else {
                double average = ((double)sum)/numValues;
                convert.fromDouble(valuePVField, average);
                numValues = 0;
                sum = 0;
                valueDBField.postPut();
            }
            supportProcessRequester.supportProcessDone(RequestResult.success);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.interfaces.Int32InterruptListener#interrupt(int)
         */
        public void interrupt(int value) {
            dbRecord.lock();
            try {
                sum += (long)value;
                ++numValues;
            } finally {
                dbRecord.unlock();
            }
        }
    }
    
    private static class Int32Output extends AbstractPDRVLinkSupport
    {
        private Int32Output(DBLink dbLink,String supportName) {
            super(supportName,dbLink);
        }

        private PVField valuePVField = null;
        private Int32 int32 = null;
        private int value;
        private Status status = Status.success;
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#initialize()
         */
        public void initialize() {
            super.initialize();
            if(!super.checkSupportState(SupportState.readyForStart,supportName)) return;
            PVField pvField = valueDBField.getPVField();
            Field field = pvField.getField();
            if(field.getType().isScalar()) {
                valuePVField = pvField;
                return;
            }
            super.uninitialize();
            pvLink.message("value field is not a scalar type", MessageType.fatalError);
            return;
        }      
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#uninitialize()
         */
        public void uninitialize() {
            super.uninitialize();
            valuePVField = null;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#start()
         */
        public void start() {
            super.start();
            if(!super.checkSupportState(SupportState.ready,supportName)) return;
            Interface iface = device.findInterface(user, "int32", true);
            if(iface==null) {
                pvLink.message("interface int32 not supported", MessageType.fatalError);
                super.stop();
                return;
            }
            int32 = (Int32)iface;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#stop()
         */
        public void stop() {
            super.stop();
            int32 = null;
        }            
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#process(org.epics.ioc.process.SupportProcessRequester)
         */
        public void process(SupportProcessRequester supportProcessRequester) {
            value = convert.toInt(valuePVField);
            super.process(supportProcessRequester);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#processContinue()
         */
        public void processContinue() {
            if(status!=Status.success) {
                alarmSupport.setAlarm(user.getMessage(), AlarmSeverity.invalid);
            }
            supportProcessRequester.supportProcessDone(RequestResult.success);
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#queueCallback()
         */
        public void queueCallback() {
            deviceTrace.print(Trace.SUPPORT, "%s value = %d", fullName,value);
            status = int32.write(user, value);
        }
    }
    
    private static class Int32ArrayInput extends AbstractPDRVLinkSupport
    implements AsynAccessListener
    {
        private Int32ArrayInput(DBLink dbLink,String supportName) {
            super(supportName,dbLink);
        }

        private PVArray valuePVArray = null;
        private Int32Array int32Array = null;
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#initialize()
         */
        public void initialize() {
            super.initialize();
            if(!super.checkSupportState(SupportState.readyForStart,supportName)) return;
            PVField pvField = valueDBField.getPVField();
            Field field = pvField.getField();
            Type type = field.getType();
            if(type!=Type.pvArray) {
                super.uninitialize();
                pvLink.message("value field is not an array", MessageType.fatalError);
                return;
            }
            Array array = (Array)field;
            Type elementType = array.getElementType();
            if(elementType.isNumeric()) {
                valuePVArray = (PVArray)pvField;
                return;
            }
            super.uninitialize();
            pvLink.message("value field is not an array with numeric elements", MessageType.fatalError);
            return;
        }      
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#uninitialize()
         */
        public void uninitialize() {
            super.uninitialize();
            valuePVArray = null;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#start()
         */
        public void start() {
            super.start();
            if(!super.checkSupportState(SupportState.ready,supportName)) return;
            Interface iface = device.findInterface(user, "int32Array", true);
            if(iface==null) {
                pvLink.message("interface int32Array not supported", MessageType.fatalError);
                super.stop();
                return;
            }
            int32Array = (Int32Array)iface;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#stop()
         */
        public void stop() {
            if(super.getSupportState()!=SupportState.ready) return;
            super.stop();
            int32Array = null;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#process(org.epics.ioc.process.SupportProcessRequester)
         */
        public void process(SupportProcessRequester supportProcessRequester) {
            valuePVArray.asynAccessStart(this);
            super.process(supportProcessRequester);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#processContinue()
         */
        public void processContinue() {
            valuePVArray.asynAccessEnd(this);
            valueDBField.postPut();
            supportProcessRequester.supportProcessDone(RequestResult.success);
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#queueCallback()
         */
        public void queueCallback() {
            deviceTrace.print(Trace.SUPPORT, "%s queueCallback", fullName);           ;
            Status status = int32Array.startRead(user);
            if(status==Status.success) {
                convert.copyArray(int32Array, 0, valuePVArray, 0, int32Array.getLength());
                int32Array.endRead(user);
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.AsynAccessListener#beginSyncAccess()
         */
        public void beginSyncAccess() {
            deviceTrace.print(Trace.FLOW, "%s beginSyncAccess", fullName);
            dbRecord.lock();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.AsynAccessListener#endSyncAccess()
         */
        public void endSyncAccess() {
            deviceTrace.print(Trace.FLOW, "%s endSyncAccess", fullName);
            dbRecord.unlock();
        }
    }
    
    private static class Int32ArrayInterruptInput extends AbstractPDRVLinkSupport
    implements Int32ArrayInterruptListener,AsynAccessListener
    {
        private Int32ArrayInterruptInput(DBLink dbLink,String supportName) {
            super(supportName,dbLink);
        }

        private PVArray valuePVArray = null;
        private Int32Array int32Array = null;
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#initialize()
         */
        public void initialize() {
            super.initialize();
            if(!super.checkSupportState(SupportState.readyForStart,supportName)) return;
            PVField pvField = valueDBField.getPVField();
            Field field = pvField.getField();
            Type type = field.getType();
            if(type!=Type.pvArray) {
                super.uninitialize();
                pvLink.message("value field is not an array", MessageType.fatalError);
                return;
            }
            Array array = (Array)field;
            Type elementType = array.getElementType();
            if(elementType.isNumeric()) {
                valuePVArray = (PVArray)pvField;
                return;
            }
            super.uninitialize();
            pvLink.message("value field is not an array with numeric elements", MessageType.fatalError);
            return;
        }      
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#uninitialize()
         */
        public void uninitialize() {
            super.uninitialize();
            valuePVArray = null;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#start()
         */
        public void start() {
            super.start();
            if(!super.checkSupportState(SupportState.ready,supportName)) return;
            Interface iface = device.findInterface(user, "int32Array", true);
            if(iface==null) {
                pvLink.message("interface int32Array not supported", MessageType.fatalError);
                super.stop();
                return;
            }
            int32Array = (Int32Array)iface;
            int32Array.addInterruptUser(user, this);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#stop()
         */
        public void stop() {
            if(super.getSupportState()!=SupportState.ready) return;
            super.stop();
            int32Array.removeInterruptUser(user, this);
            int32Array = null;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#process(org.epics.ioc.process.SupportProcessRequester)
         */
        public void process(SupportProcessRequester supportProcessRequester) {
            valuePVArray.asynAccessStart(this);
            super.process(supportProcessRequester);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#processContinue()
         */
        public void processContinue() {
            valuePVArray.asynAccessEnd(this);
            valueDBField.postPut();
            supportProcessRequester.supportProcessDone(RequestResult.success);
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#queueCallback()
         */
        public void queueCallback() {           
            Status status = int32Array.startRead(user);
            if(status==Status.success) {
                convert.copyArray(int32Array, 0, valuePVArray, 0, int32Array.getLength());
                int32Array.endRead(user);
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.interfaces.Int32ArrayInterruptListener#interrupt(org.epics.ioc.pv.PVIntArray)
         */
        public void interrupt(Int32Array int32Array) {
            if(super.isProcess()) {
                recordProcess.setActive(this);
                valuePVArray.asynAccessStart(this);
                Status status = int32Array.startRead(user);
                if(status==Status.success) {
                    convert.copyArray(int32Array, 0, valuePVArray, 0, int32Array.getLength());
                    int32Array.endRead(user);
                }
                dbRecord.lock();
                try {
                    valuePVArray.asynAccessEnd(this);
                } finally {
                    dbRecord.unlock();
                }
                recordProcess.process(this, false, null);
            } else {
                boolean isModifier = false;
                dbRecord.lock();
                try {
                    isModifier = valuePVArray.asynAccessStart(this);
                } finally {
                    dbRecord.unlock();
                }
                if(!isModifier) {
                    deviceTrace.print(Trace.ERROR,
                            "%s:%s interrupt but asynNodifyActive",
                            fullName,supportName);
                    return;
                }
                Status status = int32Array.startRead(user);
                if(status==Status.success) {
                    convert.copyArray(int32Array, 0, valuePVArray, 0, int32Array.getLength());
                    int32Array.endRead(user);
                }
                dbRecord.lock();
                try {
                    valuePVArray.asynAccessEnd(this);
                    valueDBField.postPut();
                } finally {
                    dbRecord.unlock();
                }
                deviceTrace.print(Trace.SUPPORT,
                        "%s:%s interrupt and record not processed",
                        fullName,supportName);
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.AsynAccessListener#beginSyncAccess()
         */
        public void beginSyncAccess() {
            deviceTrace.print(Trace.FLOW,
                    "%s:%s beginSyncAccess", fullName,supportName);
            dbRecord.lock();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.AsynAccessListener#endSyncAccess()
         */
        public void endSyncAccess() {           
            dbRecord.unlock();
            deviceTrace.print(Trace.FLOW,
                    "%s:%s endSyncAccess", fullName,supportName);
        }
    }
    
    private static class Int32ArrayOutput extends AbstractPDRVLinkSupport
    {
        private Int32ArrayOutput(DBLink dbLink,String supportName) {
            super(supportName,dbLink);
        }

        private PVArray valuePVArray = null;
        private Int32Array int32Array = null;
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#initialize()
         */
        public void initialize() {
            super.initialize();
            if(!super.checkSupportState(SupportState.readyForStart,supportName)) return;
            PVField pvField = valueDBField.getPVField();
            Field field = pvField.getField();
            Type type = field.getType();
            if(type!=Type.pvArray) {
                super.uninitialize();
                pvLink.message("value field is not an array", MessageType.fatalError);
                return;
            }
            Array array = (Array)field;
            Type elementType = array.getElementType();
            if(elementType.isNumeric()) {
                valuePVArray = (PVArray)pvField;
                return;
            }
            super.uninitialize();
            pvLink.message("value field is not an array with numeric elements", MessageType.fatalError);
            return;
        }      
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#uninitialize()
         */
        public void uninitialize() {
            super.uninitialize();
            valuePVArray = null;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#start()
         */
        public void start() {
            super.start();
            if(!super.checkSupportState(SupportState.ready,supportName)) return;
            Interface iface = device.findInterface(user, "int32Array", true);
            if(iface==null) {
                pvLink.message("interface int32Array not supported", MessageType.fatalError);
                super.stop();
                return;
            }
            int32Array = (Int32Array)iface;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#stop()
         */
        public void stop() {
            if(super.getSupportState()!=SupportState.ready) return;
            super.stop();
            int32Array = null;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#process(org.epics.ioc.process.SupportProcessRequester)
         */
        public void process(SupportProcessRequester supportProcessRequester) {
            super.process(supportProcessRequester);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#processContinue()
         */
        public void processContinue() {
            supportProcessRequester.supportProcessDone(RequestResult.success);
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#queueCallback()
         */
        public void queueCallback() {
            Status status = int32Array.startWrite(user);
            if(status==Status.success) {
                convert.copyArray(valuePVArray, 0, int32Array, 0, valuePVArray.getLength());
                int32Array.endWrite(user);
            }
        }
    }
    
    private static class UInt32DigitalInput extends AbstractPDRVLinkSupport
    {
        private UInt32DigitalInput(DBLink dbLink,String supportName) {
            super(supportName,dbLink);
        }

        private PVBoolean valuePVBoolean = null;
        private PVEnum valuePVEnum = null;
        private UInt32Digital uint32Digital = null;
        private int value;
        private int mask;
        private int shift;
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#initialize()
         */
        public void initialize() {
            super.initialize();
            if(!super.checkSupportState(SupportState.readyForStart,supportName)) return;
            PVField pvField = valueDBField.getPVField();
            Field field = pvField.getField();
            Type type = field.getType();
            if(type==Type.pvBoolean) {
                valuePVBoolean = (PVBoolean)valuePVField;
                return;
            }
            if(type==Type.pvEnum) {
                valuePVEnum = (PVEnum)pvField;
                return;
            }
            if(type.isScalar()) {
                valuePVField = pvField;
                return;
            }
            super.uninitialize();
            pvLink.message("value field is not a scalar type", MessageType.fatalError);
            return;
        }      
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#uninitialize()
         */
        public void uninitialize() {
            super.uninitialize();
            valuePVField = null;
            valuePVBoolean = null;
            valuePVEnum = null;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#start()
         */
        public void start() {
            super.start();
            if(!super.checkSupportState(SupportState.ready,supportName)) return;
            Interface iface = device.findInterface(user, "uint32Digital", true);
            if(iface==null) {
                pvLink.message("interface uint32Digital not supported", MessageType.fatalError);
                super.stop();
                return;
            }
            mask = pvMask.get();
            if(mask==0) {
                pvLink.message("mask is 0", MessageType.fatalError);
                super.stop();
                return;
            }
            int i = 1;
            shift = 0;
            while(true) {
                if((mask&i)!=0) break;
                ++shift; i <<= 1;
            }
            uint32Digital = (UInt32Digital)iface;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#stop()
         */
        public void stop() {
            super.stop();
            uint32Digital = null;
        } 
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#processContinue()
         */
        public void processContinue() {
            value = value&mask;
            value >>>= shift;
            if(valuePVBoolean!=null) {
                valuePVBoolean.put((value==0) ? false : true);
            } else if(valuePVEnum!=null)  {
                valuePVEnum.setIndex(value);
            } else {
                convert.fromInt(valuePVField, value);
            }
            valueDBField.postPut();
            supportProcessRequester.supportProcessDone(RequestResult.success);
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#queueCallback()
         */
        public void queueCallback() {
            Status status = uint32Digital.read(user,mask);
            if(status!=Status.success) {
                alarmSupport.setAlarm(user.getMessage(), AlarmSeverity.invalid);
                return;
            }
            value = user.getInt();
            deviceTrace.print(Trace.SUPPORT, "%s value = %d", fullName,value);
        }
    }
    
    private static class UInt32DigitalInterruptInput
    extends AbstractPDRVLinkSupport implements UInt32DigitalInterruptListener
    {
        private UInt32DigitalInterruptInput(DBLink dbLink,String supportName) {
            super(supportName,dbLink);
        }

        private PVField valuePVField = null;
        private PVBoolean valuePVBoolean = null;
        private PVEnum valuePVEnum = null;
        private UInt32Digital uint32Digital = null;
        private int value;
        private int mask;
        private int shift = 0;
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#initialize()
         */
        public void initialize() {
            super.initialize();
            if(!super.checkSupportState(SupportState.readyForStart,supportName)) return;
            PVField pvField = valueDBField.getPVField();
            Field field = pvField.getField();
            Type type = field.getType();
            if(type==Type.pvBoolean) {
                valuePVBoolean = (PVBoolean)valuePVField;
                return;
            }
            if(type==Type.pvEnum) {
                valuePVEnum = (PVEnum)pvField;
                return;
            }
            if(type.isScalar()) {
                valuePVField = pvField;
                return;
            }
            super.uninitialize();
            pvLink.message("value field is not a scalar type", MessageType.fatalError);
            return;
        }      
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#uninitialize()
         */
        public void uninitialize() {
            super.uninitialize();
            valuePVField = null;
            valuePVBoolean = null;
            valuePVEnum = null;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#start()
         */
        public void start() {
            super.start();
            if(!super.checkSupportState(SupportState.ready,supportName)) return;
            Interface iface = device.findInterface(user, "uint32Digital", true);
            if(iface==null) {
                pvLink.message("interface uint32Digital not supported", MessageType.fatalError);
                super.stop();
                return;
            }
            mask = pvMask.get();
            if(mask==0) {
                pvLink.message("mask is 0", MessageType.fatalError);
                super.stop();
                return;
            }
            int i = 1;
            shift = 0;
            while(true) {
                if((mask&i)!=0) break;
                ++shift; i <<= 1;
            }
            uint32Digital = (UInt32Digital)iface;
            uint32Digital.addInterruptUser(user, this, mask);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#stop()
         */
        public void stop() {
            super.stop();
            uint32Digital.removeInterruptUser(user, this);
            uint32Digital = null;
        } 
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#process(org.epics.ioc.process.SupportProcessRequester)
         */
        public void process(SupportProcessRequester supportProcessRequester) {
            super.process(supportProcessRequester);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#processContinue()
         */
        public void processContinue() {
            putData();
            supportProcessRequester.supportProcessDone(RequestResult.success);
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#queueCallback()
         */
        public void queueCallback() {
            Status status = uint32Digital.read(user,mask);
            if(status!=Status.success) {
                alarmSupport.setAlarm(user.getMessage(), AlarmSeverity.invalid);
                return;
            }
            value = user.getInt();
            deviceTrace.print(Trace.SUPPORT, "%s value = %d", fullName,value);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.interfaces.UInt32DigitalInterruptListener#interrupt(int)
         */
        public void interrupt(int value) {
            this.value = value;
            if(super.isProcess()) {
                recordProcess.process(this, false, null);
            } else {
                dbRecord.lock();
                try {
                    putData();
                    deviceTrace.print(Trace.FLOW,
                        "%s:%s interrupt and record not processed",
                        fullName,supportName);
                } finally {
                    dbRecord.unlock();
                }
            }
        }
        
        private void putData() {
            value = value&mask;
            value >>>= shift;
            if(valuePVBoolean!=null) {
                valuePVBoolean.put((value==0) ? false : true);
            } else if(valuePVEnum!=null)  {
                valuePVEnum.setIndex(value);
            } else {
                convert.fromInt(valuePVField, value);
            }
            deviceTrace.print(Trace.FLOW,
                    "%s:%s putData and  calling postPut",fullName,supportName);
            valueDBField.postPut();
        }
    }
    
    private static class UInt32DigitalOutput extends AbstractPDRVLinkSupport
    {
        private UInt32DigitalOutput(DBLink dbLink,String supportName) {
            super(supportName,dbLink);
        }

        private PVField valuePVField = null;
        private PVBoolean valuePVBoolean = null;
        private PVEnum valuePVEnum = null;
        private UInt32Digital uint32Digital = null;
        private int value;
        private int mask;
        private int shift;
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#initialize()
         */
        public void initialize() {
            super.initialize();
            if(!super.checkSupportState(SupportState.readyForStart,supportName)) return;
            PVField pvField = valueDBField.getPVField();
            Field field = pvField.getField();
            Type type = field.getType();
            if(type==Type.pvBoolean) {
                valuePVBoolean = (PVBoolean)valuePVField;
                return;
            }
            if(type==Type.pvEnum) {
                valuePVEnum = (PVEnum)pvField;
                return;
            }
            if(type.isScalar()) {
                valuePVField = pvField;
                return;
            }
            super.uninitialize();
            pvLink.message("value field is not a scalar type", MessageType.fatalError);
            return;
        }      
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#uninitialize()
         */
        public void uninitialize() {
            super.uninitialize();
            valuePVField = null;
            valuePVBoolean = null;
            valuePVEnum = null;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#start()
         */
        public void start() {
            super.start();
            if(!super.checkSupportState(SupportState.ready,supportName)) return;
            Interface iface = device.findInterface(user, "uint32Digital", true);
            if(iface==null) {
                pvLink.message("interface uint32Digital not supported", MessageType.fatalError);
                super.stop();
                return;
            }
            mask = pvMask.get();
            if(mask==0) {
                pvLink.message("mask is 0", MessageType.fatalError);
                super.stop();
                return;
            }
            int i = 1;
            shift = 0;
            while(true) {
                if((mask&i)!=0) break;
                ++shift; i <<= 1;
            }
            uint32Digital = (UInt32Digital)iface;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#stop()
         */
        public void stop() {
            super.stop();
            uint32Digital = null;
        }            
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#process(org.epics.ioc.process.SupportProcessRequester)
         */
        public void process(SupportProcessRequester supportProcessRequester) {
            if(valuePVBoolean!=null) {
                value = valuePVBoolean.get() ? 1 : 0;
            } else if(valuePVEnum!=null)  {
                value = valuePVEnum.getIndex();
            } else {
                value = convert.toInt(valuePVField);
            }
            value <<= shift;
            super.process(supportProcessRequester);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#queueCallback()
         */
        public void queueCallback() {
            Status status = uint32Digital.write(user, value,mask);
            if(status!=Status.success) {
                alarmSupport.setAlarm(user.getMessage(), AlarmSeverity.invalid);
                return;
            }
            deviceTrace.print(Trace.SUPPORT, "%s value = %d", fullName,value);
        }
    }
    
    private static class Float64Input extends AbstractPDRVLinkSupport
    {
        private Float64Input(DBLink dbLink,String supportName) {
            super(supportName,dbLink);
        }

        private PVField valuePVField = null;
        private Float64 float64 = null;
        private double value;
        private Status status = Status.success;
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#initialize()
         */
        public void initialize() {
            super.initialize();
            if(!super.checkSupportState(SupportState.readyForStart,supportName)) return;
            PVField pvField = valueDBField.getPVField();
            Field field = pvField.getField();
            if(field.getType().isScalar()) {
                valuePVField = pvField;
                return;
            }
            super.uninitialize();
            pvLink.message("value field is not a scalar type", MessageType.fatalError);
            return;
        }      
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#uninitialize()
         */
        public void uninitialize() {
            super.uninitialize();
            valuePVField = null;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#start()
         */
        public void start() {
            super.start();
            if(!super.checkSupportState(SupportState.ready,supportName)) return;
            Interface iface = device.findInterface(user, "float64", true);
            if(iface==null) {
                pvLink.message("interface float64 not supported", MessageType.fatalError);
                super.stop();
                return;
            }
            float64 = (Float64)iface;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#stop()
         */
        public void stop() {
            super.stop();
            float64 = null;
        }            
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#processContinue()
         */
        public void processContinue() {
            if(status==Status.success) {
                convert.fromDouble(valuePVField, value);
                valueDBField.postPut();
            } else {
                alarmSupport.setAlarm(user.getMessage(), AlarmSeverity.invalid);
            }
            supportProcessRequester.supportProcessDone(RequestResult.success);
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#queueCallback()
         */
        public void queueCallback() {
            Status status = float64.read(user);
            if(status==Status.success) value = user.getDouble();
            deviceTrace.print(Trace.SUPPORT, "%s value = %d", fullName,value);
        }
    }
    
    private static class Float64InterruptInput extends AbstractPDRVLinkSupport implements Float64InterruptListener
    {
        private Float64InterruptInput(DBLink dbLink,String supportName) {
            super(supportName,dbLink);
        }

        private PVField valuePVField = null;
        private Float64 float64 = null;
        private double value;
        private Status status = Status.success;
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#initialize()
         */
        public void initialize() {
            super.initialize();
            if(!super.checkSupportState(SupportState.readyForStart,supportName)) return;
            PVField pvField = valueDBField.getPVField();
            Field field = pvField.getField();
            if(field.getType().isScalar()) {
                valuePVField = pvField;
                return;
            }
            super.uninitialize();
            pvLink.message("value field is not a scalar type", MessageType.fatalError);
            return;
        }      
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#uninitialize()
         */
        public void uninitialize() {
            super.uninitialize();
            valuePVField = null;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#start()
         */
        public void start() {
            super.start();
            if(!super.checkSupportState(SupportState.ready,supportName)) return;
            Interface iface = device.findInterface(user, "float64", true);
            if(iface==null) {
                pvLink.message("interface float64 not supported", MessageType.fatalError);
                super.stop();
                return;
            }
            float64 = (Float64)iface;
            float64.addInterruptUser(user, this);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#stop()
         */
        public void stop() {
            super.stop();
            float64.removeInterruptUser(user, this);
            float64 = null;
        }            
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#processContinue()
         */
        public void processContinue() {
            if(status==Status.success) {
               putData();
            } else {
                alarmSupport.setAlarm(user.getMessage(), AlarmSeverity.invalid);
            }
            supportProcessRequester.supportProcessDone(RequestResult.success);
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#queueCallback()
         */
        public void queueCallback() {
            Status status = float64.read(user);
            if(status==Status.success) value = user.getDouble();
            deviceTrace.print(Trace.SUPPORT, "%s value = %d", fullName,value);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.interfaces.Float64InterruptListener#interrupt(double)
         */
        public void interrupt(double value) {
            this.value = value;
            if(super.isProcess()) {
                recordProcess.process(this, false, null);
            } else {
                dbRecord.lock();
                try {
                    putData();
                    deviceTrace.print(Trace.FLOW,
                        "%s:%s interrupt and record not processed",
                        fullName,supportName);
                } finally {
                    dbRecord.unlock();
                }
            }
        }
        
        private void putData() {
            convert.fromDouble(valuePVField, value);
            deviceTrace.print(Trace.FLOW,
                "%s:%s putData and  calling postPut",fullName,supportName);
            valueDBField.postPut();
        }
    }
    
    private static class Float64AverageInput extends AbstractPDRVLinkSupport implements Float64InterruptListener
    {
        private Float64AverageInput(DBLink dbLink,String supportName) {
            super(supportName,dbLink);
        }

        private PVField valuePVField = null;
        private Float64 float64 = null;
        private int numValues = 0;
        private double sum = 0.0;
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#initialize()
         */
        public void initialize() {
            super.initialize();
            if(!super.checkSupportState(SupportState.readyForStart,supportName)) return;
            PVField pvField = valueDBField.getPVField();
            Field field = pvField.getField();
            if(field.getType().isScalar()) {
                valuePVField = pvField;
                return;
            }
            super.uninitialize();
            pvLink.message("value field is not a scalar type", MessageType.fatalError);
            return;
        }      
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#uninitialize()
         */
        public void uninitialize() {
            super.uninitialize();
            valuePVField = null;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#start()
         */
        public void start() {
            super.start();
            if(!super.checkSupportState(SupportState.ready,supportName)) return;
            Interface iface = device.findInterface(user, "float64", true);
            if(iface==null) {
                pvLink.message("interface float64 not supported", MessageType.fatalError);
                super.stop();
                return;
            }
            float64 = (Float64)iface;
            float64.addInterruptUser(user, this);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#stop()
         */
        public void stop() {
            super.stop();
            float64.removeInterruptUser(user, this);
            float64 = null;
        }            
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#process(org.epics.ioc.process.SupportProcessRequester)
         */
        public void process(SupportProcessRequester supportProcessRequester) {
            if(!super.checkSupportState(SupportState.ready,supportName)) {
                if(alarmSupport!=null) alarmSupport.setAlarm(
                        fullName + " not ready",
                        AlarmSeverity.major);
                supportProcessRequester.supportProcessDone(RequestResult.failure);
                return;
            }
            if(numValues==0) {
                if(alarmSupport!=null) alarmSupport.setAlarm(
                        fullName + " no new values",
                        AlarmSeverity.major);
            } else {
                double average = sum/numValues;
                convert.fromDouble(valuePVField, average);
                numValues = 0;
                sum = 0.0;
                valueDBField.postPut();
            }
            supportProcessRequester.supportProcessDone(RequestResult.success);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.interfaces.Float64InterruptListener#interrupt(double)
         */
        public void interrupt(double value) {
            dbRecord.lock();
            try {
                sum += (double)value;
                ++numValues;
            } finally {
                dbRecord.unlock();
            }
        }
    }
    
    private static class Float64Output extends AbstractPDRVLinkSupport
    {
        private Float64Output(DBLink dbLink,String supportName) {
            super(supportName,dbLink);
        }

        private PVField valuePVField = null;
        private Float64 float64 = null;
        private double value;
        private Status status = Status.success;
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#initialize()
         */
        public void initialize() {
            super.initialize();
            if(!super.checkSupportState(SupportState.readyForStart,supportName)) return;
            PVField pvField = valueDBField.getPVField();
            Field field = pvField.getField();
            if(field.getType().isScalar()) {
                valuePVField = pvField;
                return;
            }
            super.uninitialize();
            pvLink.message("value field is not a scalar type", MessageType.fatalError);
            return;
        }      
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#uninitialize()
         */
        public void uninitialize() {
            super.uninitialize();
            valuePVField = null;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#start()
         */
        public void start() {
            super.start();
            if(!super.checkSupportState(SupportState.ready,supportName)) return;
            Interface iface = device.findInterface(user, "float64", true);
            if(iface==null) {
                pvLink.message("interface float64 not supported", MessageType.fatalError);
                super.stop();
                return;
            }
            float64 = (Float64)iface;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#stop()
         */
        public void stop() {
            super.stop();
            float64 = null;
        }            
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#process(org.epics.ioc.process.SupportProcessRequester)
         */
        public void process(SupportProcessRequester supportProcessRequester) {
            value = convert.toDouble(valuePVField);
            super.process(supportProcessRequester);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#processContinue()
         */
        public void processContinue() {
            if(status!=Status.success) {
                alarmSupport.setAlarm(user.getMessage(), AlarmSeverity.invalid);
            }
            supportProcessRequester.supportProcessDone(RequestResult.success);
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#queueCallback()
         */
        public void queueCallback() {
            deviceTrace.print(Trace.SUPPORT, "%s value = %d", fullName,value);
            status = float64.write(user, value);
        }
    }
    
    private static class Float64ArrayInput extends AbstractPDRVLinkSupport
    implements AsynAccessListener
    {
        private Float64ArrayInput(DBLink dbLink,String supportName) {
            super(supportName,dbLink);
        }

        private PVArray valuePVArray = null;
        private Float64Array float64Array = null;
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#initialize()
         */
        public void initialize() {
            super.initialize();
            if(!super.checkSupportState(SupportState.readyForStart,supportName)) return;
            PVField pvField = valueDBField.getPVField();
            Field field = pvField.getField();
            Type type = field.getType();
            if(type!=Type.pvArray) {
                super.uninitialize();
                pvLink.message("value field is not an array", MessageType.fatalError);
                return;
            }
            Array array = (Array)field;
            Type elementType = array.getElementType();
            if(elementType.isNumeric()) {
                valuePVArray = (PVArray)pvField;
                return;
            }
            super.uninitialize();
            pvLink.message("value field is not an array with numeric elements", MessageType.fatalError);
            return;
        }      
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#uninitialize()
         */
        public void uninitialize() {
            super.uninitialize();
            valuePVArray = null;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#start()
         */
        public void start() {
            super.start();
            if(!super.checkSupportState(SupportState.ready,supportName)) return;
            Interface iface = device.findInterface(user, "float64Array", true);
            if(iface==null) {
                pvLink.message("interface float64Array not supported", MessageType.fatalError);
                super.stop();
                return;
            }
            float64Array = (Float64Array)iface;          
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#stop()
         */
        public void stop() {
            if(super.getSupportState()!=SupportState.ready) return;
            super.stop();
            float64Array = null;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#process(org.epics.ioc.process.SupportProcessRequester)
         */
        public void process(SupportProcessRequester supportProcessRequester) {
            valuePVArray.asynAccessStart(this);
            super.process(supportProcessRequester);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#processContinue()
         */
        public void processContinue() {
            valuePVArray.asynAccessEnd(this);
            valueDBField.postPut();
            supportProcessRequester.supportProcessDone(RequestResult.success);
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#queueCallback()
         */
        public void queueCallback() {
            deviceTrace.print(Trace.SUPPORT, "%s queueCallback", fullName);
            Status status = float64Array.startRead(user);
            if(status==Status.success) {
                convert.copyArray(float64Array, 0, valuePVArray, 0, float64Array.getLength());
                float64Array.endRead(user);
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.AsynAccessListener#beginSyncAccess()
         */
        public void beginSyncAccess() {
            deviceTrace.print(Trace.FLOW, "%s beginSyncAccess", fullName);
            dbRecord.lock();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.AsynAccessListener#endSyncAccess()
         */
        public void endSyncAccess() {
            deviceTrace.print(Trace.FLOW, "%s endSyncAccess", fullName);
            dbRecord.unlock();
        }
    }
    
    private static class Float64ArrayInterruptInput extends AbstractPDRVLinkSupport
    implements Float64ArrayInterruptListener,AsynAccessListener
    {
        private Float64ArrayInterruptInput(DBLink dbLink,String supportName) {
            super(supportName,dbLink);
        }

        private PVArray valuePVArray = null;
        private Float64Array float64Array = null;
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#initialize()
         */
        public void initialize() {
            super.initialize();
            if(!super.checkSupportState(SupportState.readyForStart,supportName)) return;
            PVField pvField = valueDBField.getPVField();
            Field field = pvField.getField();
            Type type = field.getType();
            if(type!=Type.pvArray) {
                super.uninitialize();
                pvLink.message("value field is not an array", MessageType.fatalError);
                return;
            }
            Array array = (Array)field;
            Type elementType = array.getElementType();
            if(elementType.isNumeric()) {
                valuePVArray = (PVArray)pvField;
                return;
            }
            super.uninitialize();
            pvLink.message("value field is not an array with numeric elements", MessageType.fatalError);
            return;
        }      
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#uninitialize()
         */
        public void uninitialize() {
            super.uninitialize();
            valuePVArray = null;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#start()
         */
        public void start() {
            super.start();
            if(!super.checkSupportState(SupportState.ready,supportName)) return;
            Interface iface = device.findInterface(user, "float64Array", true);
            if(iface==null) {
                pvLink.message("interface float64Array not supported", MessageType.fatalError);
                super.stop();
                return;
            }
            float64Array = (Float64Array)iface;
            float64Array.addInterruptUser(user, this);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#stop()
         */
        public void stop() {
            if(super.getSupportState()!=SupportState.ready) return;
            super.stop();
            float64Array.removeInterruptUser(user, this);
            float64Array = null;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#process(org.epics.ioc.process.SupportProcessRequester)
         */
        public void process(SupportProcessRequester supportProcessRequester) {          
            valuePVArray.asynAccessStart(this);
            super.process(supportProcessRequester);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#processContinue()
         */
        public void processContinue() {
            valuePVArray.asynAccessEnd(this);
            valueDBField.postPut();
            supportProcessRequester.supportProcessDone(RequestResult.success);
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#queueCallback()
         */
        public void queueCallback() {
            Status status = float64Array.startRead(user);
            if(status==Status.success) {
                convert.copyArray(float64Array, 0, valuePVArray, 0, float64Array.getLength());
                float64Array.endRead(user);
            }
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.interfaces.Float64ArrayInterruptListener#interrupt(org.epics.ioc.pdrv.interfaces.Float64Array)
         */
        public void interrupt(Float64Array float64Array) {
            if(super.isProcess()) {
                recordProcess.setActive(this);
                valuePVArray.asynAccessStart(this);
                Status status = float64Array.startRead(user);
                if(status==Status.success) {
                    convert.copyArray(float64Array, 0, valuePVArray, 0, float64Array.getLength());
                    float64Array.endRead(user);
                }
                dbRecord.lock();
                try {
                    valuePVArray.asynAccessEnd(this);
                } finally {
                    dbRecord.unlock();
                }
                recordProcess.process(this, false, null);
            } else {
                boolean isModifier = false;
                dbRecord.lock();
                try {
                    isModifier = valuePVArray.asynAccessStart(this);
                } finally {
                    dbRecord.unlock();
                }
                if(!isModifier) {
                    deviceTrace.print(Trace.ERROR,
                            "%s:%s interrupt but asynNodifyActive",
                            fullName,supportName);
                    return;
                }
                Status status = float64Array.startRead(user);
                if(status==Status.success) {
                    convert.copyArray(float64Array, 0, valuePVArray, 0, float64Array.getLength());
                    float64Array.endRead(user);
                }
                dbRecord.lock();
                try {
                    valuePVArray.asynAccessEnd(this);
                    valueDBField.postPut();
                } finally {
                    dbRecord.unlock();
                }
                deviceTrace.print(Trace.SUPPORT,
                        "%s:%s interrupt and record not processed",
                        fullName,supportName);
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.AsynAccessListener#beginSyncAccess()
         */
        public void beginSyncAccess() {
            deviceTrace.print(Trace.FLOW,
                    "%s beginSyncAccess", fullName);
            dbRecord.lock();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.AsynAccessListener#endSyncAccess()
         */
        public void endSyncAccess() {
            dbRecord.unlock();
            deviceTrace.print(Trace.FLOW,
                    "%s endSyncAccess", fullName);
        }
    }
    
    private static class Float64ArrayOutput extends AbstractPDRVLinkSupport
    {
        private Float64ArrayOutput(DBLink dbLink,String supportName) {
            super(supportName,dbLink);
        }

        private PVArray valuePVArray = null;
        private Float64Array float64Array = null;
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#initialize()
         */
        public void initialize() {
            super.initialize();
            if(!super.checkSupportState(SupportState.readyForStart,supportName)) return;
            PVField pvField = valueDBField.getPVField();
            Field field = pvField.getField();
            Type type = field.getType();
            if(type!=Type.pvArray) {
                super.uninitialize();
                pvLink.message("value field is not an array", MessageType.fatalError);
                return;
            }
            Array array = (Array)field;
            Type elementType = array.getElementType();
            if(elementType.isNumeric()) {
                valuePVArray = (PVArray)pvField;
                return;
            }
            super.uninitialize();
            pvLink.message("value field is not an array with numeric elements", MessageType.fatalError);
            return;
        }      
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#uninitialize()
         */
        public void uninitialize() {
            super.uninitialize();
            valuePVArray = null;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#start()
         */
        public void start() {
            super.start();
            if(!super.checkSupportState(SupportState.ready,supportName)) return;
            Interface iface = device.findInterface(user, "float64Array", true);
            if(iface==null) {
                pvLink.message("interface float64Array not supported", MessageType.fatalError);
                super.stop();
                return;
            }
            float64Array = (Float64Array)iface;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#stop()
         */
        public void stop() {
            if(super.getSupportState()!=SupportState.ready) return;
            super.stop();
            float64Array = null;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#process(org.epics.ioc.process.SupportProcessRequester)
         */
        public void process(SupportProcessRequester supportProcessRequester) {
            super.process(supportProcessRequester);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#processContinue()
         */
        public void processContinue() {
            supportProcessRequester.supportProcessDone(RequestResult.success);
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#queueCallback()
         */
        public void queueCallback() {
            Status status = float64Array.startWrite(user);
            if(status==Status.success) {
                convert.copyArray(valuePVArray, 0, float64Array, 0, valuePVArray.getLength());
                float64Array.endWrite(user);
            }
        }
    }
}
