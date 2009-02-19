 /**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.pdrv;

import org.epics.ioc.pdrv.Status;
import org.epics.ioc.pdrv.Trace;
import org.epics.ioc.pdrv.interfaces.Float64;
import org.epics.ioc.pdrv.interfaces.Float64Array;
import org.epics.ioc.pdrv.interfaces.Float64ArrayInterruptListener;
import org.epics.ioc.pdrv.interfaces.Float64InterruptListener;
import org.epics.ioc.pdrv.interfaces.Int32;
import org.epics.ioc.pdrv.interfaces.Int32Array;
import org.epics.ioc.pdrv.interfaces.Int32ArrayInterruptListener;
import org.epics.ioc.pdrv.interfaces.Int32InterruptListener;
import org.epics.ioc.pdrv.interfaces.Interface;
import org.epics.ioc.pdrv.interfaces.Octet;
import org.epics.ioc.pdrv.interfaces.OctetInterruptListener;
import org.epics.ioc.pdrv.interfaces.UInt32Digital;
import org.epics.ioc.pdrv.interfaces.UInt32DigitalInterruptListener;
import org.epics.ioc.support.RecordSupport;
import org.epics.ioc.support.Support;
import org.epics.ioc.support.SupportProcessRequester;
import org.epics.ioc.support.SupportState;
import org.epics.ioc.util.RequestResult;
import org.epics.pvData.factory.ConvertFactory;
import org.epics.pvData.misc.Enumerated;
import org.epics.pvData.misc.EnumeratedFactory;
import org.epics.pvData.property.AlarmSeverity;
import org.epics.pvData.property.PVProperty;
import org.epics.pvData.property.PVPropertyFactory;
import org.epics.pvData.pv.Array;
import org.epics.pvData.pv.Convert;
import org.epics.pvData.pv.Field;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVArray;
import org.epics.pvData.pv.PVAuxInfo;
import org.epics.pvData.pv.PVBoolean;
import org.epics.pvData.pv.PVDouble;
import org.epics.pvData.pv.PVDoubleArray;
import org.epics.pvData.pv.PVField;
import org.epics.pvData.pv.PVInt;
import org.epics.pvData.pv.PVIntArray;
import org.epics.pvData.pv.PVScalar;
import org.epics.pvData.pv.PVString;
import org.epics.pvData.pv.PVStringArray;
import org.epics.pvData.pv.PVStructure;
import org.epics.pvData.pv.ScalarType;
import org.epics.pvData.pv.Type;

/**
 * Factory to create portDriver link support.
 * @author mrk
 *
 */
public class PDRVSupportFactory {
    /**
     * Create support for portDriver.
     * @param pvStructure The field for which to create support.
     * @return A LinkSupport interface or null failure.
     */
    public static Support create(PVStructure pvStructure) {
        PVAuxInfo pvAuxInfo = pvStructure.getPVAuxInfo();
        PVScalar pvScalar = pvAuxInfo.getInfo("supportFactory");
        if(pvScalar==null) {
            pvStructure.message("no pvAuxInfo with name support. Why??", MessageType.error);
            return null;
        }
        if(pvScalar.getScalar().getScalarType()!=ScalarType.pvString) {
            pvStructure.message("pvAuxInfo for support is not a string. Why??", MessageType.error);
            return null;
        }
        String supportName = ((PVString)pvScalar).get();
        if(supportName.equals(pdrvOctetInputSupportName))
            return new OctetInput(pvStructure,pdrvOctetInputSupportName);
        if(supportName.equals(pdrvOctetInterruptInputSupportName))
            return new OctetInterruptInput(pvStructure,pdrvOctetInterruptInputSupportName);
        if(supportName.equals(pdrvOctetOutputSupportName))
            return new OctetOutput(pvStructure,pdrvOctetOutputSupportName);
        if(supportName.equals(pdrvInt32InputSupportName))
            return new Int32Input(pvStructure,pdrvInt32InputSupportName);
        if(supportName.equals(pdrvInt32InterruptInputSupportName))
            return new Int32InterruptInput(pvStructure,pdrvInt32InterruptInputSupportName);
        if(supportName.equals(pdrvInt32AverageInputSupportName))
            return new Int32AverageInput(pvStructure,pdrvInt32AverageInputSupportName);
        if(supportName.equals(pdrvInt32OutputSupportName))
            return new Int32Output(pvStructure,pdrvInt32OutputSupportName);
        if(supportName.equals(pdrvInt32ArrayInputSupportName))
            return new Int32ArrayInput(pvStructure,pdrvInt32ArrayInputSupportName);
        if(supportName.equals(pdrvInt32ArrayInterruptInputSupportName))
            return new Int32ArrayInterruptInput(pvStructure,pdrvInt32ArrayInterruptInputSupportName);
        if(supportName.equals(pdrvInt32ArrayOutputSupportName))
            return new Int32ArrayOutput(pvStructure,pdrvInt32ArrayOutputSupportName);
        if(supportName.equals(pdrvFloat64InputSupportName))
            return new Float64Input(pvStructure,pdrvFloat64InputSupportName);
        if(supportName.equals(pdrvFloat64InterruptInputSupportName))
            return new Float64InterruptInput(pvStructure,pdrvFloat64InterruptInputSupportName);
        if(supportName.equals(pdrvFloat64AverageInputSupportName))
            return new Float64AverageInput(pvStructure,pdrvFloat64AverageInputSupportName);
        if(supportName.equals(pdrvFloat64OutputSupportName))
            return new Float64Output(pvStructure,pdrvFloat64OutputSupportName);
        if(supportName.equals(pdrvFloat64ArrayInputSupportName))
            return new Float64ArrayInput(pvStructure,pdrvFloat64ArrayInputSupportName);
        if(supportName.equals(pdrvFloat64ArrayInterruptInputSupportName))
            return new Float64ArrayInterruptInput(pvStructure,pdrvFloat64ArrayInterruptInputSupportName);
        if(supportName.equals(pdrvFloat64ArrayOutputSupportName))
            return new Float64ArrayOutput(pvStructure,pdrvFloat64ArrayOutputSupportName);
        if(supportName.equals(pdrvUInt32DigitalInputSupportName))
            return new UInt32DigitalInput(pvStructure,pdrvUInt32DigitalInputSupportName);
        if(supportName.equals(pdrvUInt32DigitalInterruptInputSupportName))
            return new UInt32DigitalInterruptInput(pvStructure,pdrvUInt32DigitalInterruptInputSupportName);
        if(supportName.equals(pdrvUInt32DigitalOutputSupportName))
            return new UInt32DigitalOutput(pvStructure,pdrvUInt32DigitalOutputSupportName);
        pvStructure.message("no support for " + supportName, MessageType.fatalError);
        return null;
    }
    private static final String pdrvOctetInputSupportName = "pdrvOctetInputFactory";
    private static final String pdrvOctetInterruptInputSupportName = "pdrvOctetInterruptInputFactory";
    private static final String pdrvOctetOutputSupportName = "pdrvOctetOutputFactory";
    private static final String pdrvInt32InputSupportName = "pdrvInt32InputFactory";
    private static final String pdrvInt32InterruptInputSupportName = "pdrvInt32InterruptInputFactory";
    private static final String pdrvInt32AverageInputSupportName = "pdrvInt32AverageInputFactory";
    private static final String pdrvInt32OutputSupportName = "pdrvInt32OutputFactory";
    private static final String pdrvInt32ArrayInputSupportName = "pdrvInt32ArrayInputFactory";
    private static final String pdrvInt32ArrayInterruptInputSupportName = "pdrvInt32ArrayInterruptInputFactory";
    private static final String pdrvInt32ArrayOutputSupportName = "pdrvInt32ArrayOutputFactory";
    private static final String pdrvFloat64InputSupportName = "pdrvFloat64InputFactory";
    private static final String pdrvFloat64InterruptInputSupportName = "pdrvFloat64InterruptInputFactory";
    private static final String pdrvFloat64AverageInputSupportName = "pdrvFloat64AverageInputFactory";
    private static final String pdrvFloat64OutputSupportName = "pdrvFloat64OutputFactory";
    private static final String pdrvFloat64ArrayInputSupportName = "pdrvFloat64ArrayInputFactory";
    private static final String pdrvFloat64ArrayInterruptInputSupportName = "pdrvFloat64ArrayInterruptInputFactory";
    private static final String pdrvFloat64ArrayOutputSupportName = "pdrvFloat64ArrayOutputFactory";
    private static final String pdrvUInt32DigitalInputSupportName = "pdrvUInt32DigitalInputFactory";
    private static final String pdrvUInt32DigitalInterruptInputSupportName = "pdrvUInt32DigitalInterruptInputFactory";
    private static final String pdrvUInt32DigitalOutputSupportName = "pdrvUInt32DigitalOutputFactory";
    
    private static Convert convert = ConvertFactory.getConvert();
    private static PVProperty pvProperty = PVPropertyFactory.getPVProperty(); 
    
    private static enum OctetValueType {
        string,
        bool,
        numeric,
        array
    }
    
    private static class OctetInput extends AbstractPDRVSupport
    {
        private OctetInput(PVStructure pvStructure,String supportName) {
            super(supportName,pvStructure);
        }
        
        private OctetValueType octetValueType;
        private int size = 0;
        
        private Octet octet = null;
        private byte[] octetArray = null;
        private char[] charArray = null;
        private int nbytes = 0;
        private Status status = Status.success;
        
        /* (non-Javadoc)
         * @see org.epics.ioc.support.pdrv.AbstractPDRVSupport#initialize(org.epics.ioc.support.RecordSupport)
         */
        public void initialize(RecordSupport recordSupport) {
            super.initialize(recordSupport);
            if(!super.checkSupportState(SupportState.readyForStart,supportName)) return;
            Field field = valuePVField.getField();
            if(field.getType()==Type.scalarArray) {
                Array array = (Array)field;
                ScalarType elementType = array.getElementType();
                if(!elementType.isNumeric()) {
                    pvStructure.message("value field is not a supported type", MessageType.fatalError);
                    super.uninitialize();
                    return;
                }
                octetValueType = OctetValueType.array;
            } else if(field.getType()==Type.scalar) {
                PVScalar pvScalar = (PVScalar)valuePVField;
                ScalarType scalarType = pvScalar.getScalar().getScalarType();
                if(scalarType==ScalarType.pvString) {
                    octetValueType = OctetValueType.string;
                } else if(scalarType==ScalarType.pvBoolean) {
                    octetValueType = OctetValueType.bool;
                } else if(scalarType.isNumeric()) {
                    octetValueType = OctetValueType.numeric;
                } 
            } else {
                pvStructure.message("value field is not a supported type", MessageType.fatalError);
                super.uninitialize();
                return;
            }
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
                pvStructure.message("interface octet not supported", MessageType.fatalError);
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
                    convert.fromByteArray((PVArray)valuePVField, 0, nbytes, octetArray, 0);
                } else {
                    for(int i=0; i<nbytes; i++) charArray[i] = (char)octetArray[i];
                    String string = String.copyValueOf(charArray, 0, nbytes);
                    convert.fromString((PVScalar)valuePVField, string);
                }
            } else {
                alarmSupport.setAlarm(user.getMessage(), AlarmSeverity.invalid);
            }
            super.processContinue();
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#queueCallback()
         */
        public void queueCallback() {
            deviceTrace.print(Trace.FLOW,
                "%s:%s queueCallback calling read ",fullName,supportName);
            status = octet.read(user, octetArray, size);
            if(status!=Status.success) {
            	deviceTrace.print(Trace.ERROR,
                        "%s:%s octet.read failed", fullName,supportName);
                return;
            }
            nbytes = user.getInt();
            deviceTrace.printIO(Trace.SUPPORT, octetArray, user.getInt(), "%s", fullName);
        }
    }
    
    private static class OctetInterruptInput extends AbstractPDRVSupport
    implements OctetInterruptListener
    {
        private OctetInterruptInput(PVStructure pvStructure,String supportName) {
            super(supportName,pvStructure);
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
        public void initialize(RecordSupport recordSupport) {
            super.initialize(recordSupport);
            if(!super.checkSupportState(SupportState.readyForStart,supportName)) return;
            Field field = valuePVField.getField();
            if(field.getType()==Type.scalarArray) {
                Array array = (Array)field;
                ScalarType elementType = array.getElementType();
                if(!elementType.isNumeric()) {
                    pvStructure.message("value field is not a supported type", MessageType.fatalError);
                    super.uninitialize();
                    return;
                }
                octetValueType = OctetValueType.array;
            } else if(field.getType()==Type.scalar) {
                PVScalar pvScalar = (PVScalar)valuePVField;
                ScalarType scalarType = pvScalar.getScalar().getScalarType();
                if(scalarType==ScalarType.pvString) {
                    octetValueType = OctetValueType.string;
                } else if(scalarType==ScalarType.pvBoolean) {
                    octetValueType = OctetValueType.bool;
                } else if(scalarType.isNumeric()) {
                    octetValueType = OctetValueType.numeric;
                } 
            } else {
                pvStructure.message("value field is not a supported type", MessageType.fatalError);
                super.uninitialize();
                return;
            }
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
                pvStructure.message("interface octet not supported", MessageType.fatalError);
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
            super.processContinue();
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
                pvRecord.lock();
                try {
                    putData();
                    deviceTrace.print(Trace.FLOW,
                        "%s:%s interrupt and record not processed",
                        fullName,supportName);
                } finally {
                    pvRecord.unlock();
                }
            }
        }
        
        private void putData() {
            if(octetValueType==OctetValueType.array) {
                convert.fromByteArray((PVArray)valuePVField, 0, nbytes, octetArray, 0);
            } else {
                for(int i=0; i<nbytes; i++) charArray[i] = (char)octetArray[i];
                String string = String.copyValueOf(charArray, 0, nbytes);
                convert.fromString((PVScalar)valuePVField, string);
            }
            deviceTrace.print(Trace.FLOW,
                "%s:%s putData ",fullName,supportName);
        }
    }
    
    private static class OctetOutput extends AbstractPDRVSupport
    {
        private OctetOutput(PVStructure pvStructure,String supportName) {
            super(supportName,pvStructure);
        }
        
        private OctetValueType octetValueType;
        private int size = 0;
        
        private Octet octet = null;
        private byte[] octetArray = null;
        private int nbytes = 0;
        private Status status = Status.success;
        
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVSupport#initialize(org.epics.ioc.support.RecordSupport)
         */
        public void initialize(RecordSupport recordSupport) {
            super.initialize(recordSupport);
            if(!super.checkSupportState(SupportState.readyForStart,supportName)) return;
            Field field = valuePVField.getField();
            if(field.getType()==Type.scalarArray) {
                Array array = (Array)field;
                ScalarType elementType = array.getElementType();
                if(!elementType.isNumeric()) {
                    pvStructure.message("value field is not a supported type", MessageType.fatalError);
                    super.uninitialize();
                    return;
                }
                octetValueType = OctetValueType.array;
            } else if(field.getType()==Type.scalar) {
                PVScalar pvScalar = (PVScalar)valuePVField;
                ScalarType scalarType = pvScalar.getScalar().getScalarType();
                if(scalarType==ScalarType.pvString) {
                    octetValueType = OctetValueType.string;
                } else if(scalarType==ScalarType.pvBoolean) {
                    octetValueType = OctetValueType.bool;
                } else if(scalarType.isNumeric()) {
                    octetValueType = OctetValueType.numeric;
                } 
            } else {
                pvStructure.message("value field is not a supported type", MessageType.fatalError);
                super.uninitialize();
                return;
            }
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
                pvStructure.message("interface octet not supported", MessageType.fatalError);
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
                nbytes = convert.toByteArray((PVArray)valuePVField, 0, size, octetArray, 0);
            } else {
                String string = convert.getString(valuePVField);
                if(string==null) string = "";
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
            super.processContinue();
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#queueCallback()
         */
        public void queueCallback() {
            deviceTrace.print(Trace.FLOW,
                    "%s:%s queueCallback calling write",
                    fullName,supportName);
            deviceTrace.printIO(Trace.SUPPORT, octetArray, nbytes, "%s", fullName);
            status = octet.write(user, octetArray, nbytes);
            if(status!=Status.success) {
            	deviceTrace.print(Trace.ERROR,
                        "%s:%s octet.write failed", fullName,supportName);
                return;
            }
            deviceTrace.printIO(Trace.SUPPORT, octetArray, user.getInt(), "%s", fullName);
        }
    }
    
    private static class Int32Input extends AbstractPDRVSupport
    {
        private Int32Input(PVStructure pvStructure,String supportName) {
            super(supportName,pvStructure);
        }

        private Int32 int32 = null;
        private int value;
        private Status status = Status.success;
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVSupport#initialize(org.epics.ioc.support.RecordSupport)
         */
        public void initialize(RecordSupport recordSupport) {
            super.initialize(recordSupport);
            if(!super.checkSupportState(SupportState.readyForStart,supportName)) return;
            if(valuePVField.getField().getType()==Type.scalar) return;
            super.uninitialize();
            pvStructure.message("value field is not a scalar type", MessageType.fatalError);
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
                pvStructure.message("interface int32 not supported", MessageType.fatalError);
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
                convert.fromInt((PVScalar)valuePVField, value);
            } else {
                alarmSupport.setAlarm(user.getMessage(), AlarmSeverity.invalid);
            }
            super.processContinue();
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#queueCallback()
         */
        public void queueCallback() {
        	deviceTrace.print(Trace.FLOW,
                    "%s:%s queueCallback calling read ",fullName,supportName);
            Status status = int32.read(user);
            if(status!=Status.success) {
            	deviceTrace.print(Trace.ERROR,
                        "%s:%s int32.read failed", fullName,supportName);
                return;
            }
            value = user.getInt();
            deviceTrace.print(Trace.SUPPORT, "%s value = %d", fullName,value);
        }
    }
    
    private static class Int32InterruptInput extends AbstractPDRVSupport
    implements Int32InterruptListener
    {
        private Int32InterruptInput(PVStructure pvStructure,String supportName) {
            super(supportName,pvStructure);
        }
        
        private Int32 int32 = null;
        private int value;
        private Status status = Status.success;
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVSupport#initialize(org.epics.ioc.support.RecordSupport)
         */
        public void initialize(RecordSupport recordSupport) {
            super.initialize(recordSupport);
            if(!super.checkSupportState(SupportState.readyForStart,supportName)) return;
            if(valuePVField.getField().getType()==Type.scalar) return;
            super.uninitialize();
            pvStructure.message("value field is not a scalar type", MessageType.fatalError);
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
                pvStructure.message("interface int32 not supported", MessageType.fatalError);
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
            super.processContinue();
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.interfaces.Int32InterruptListener#interrupt(int)
         */
        public void interrupt(int value) {
            this.value = value;
            if(super.isProcess()) {
                recordProcess.process(this, false, null);
            } else {
                pvRecord.lock();
                try {
                    putData();
                    deviceTrace.print(Trace.FLOW,
                        "%s:%s interrupt and record not processed",
                        fullName,supportName);
                } finally {
                    pvRecord.unlock();
                }
            }
        }
        
        private void putData() {
            convert.fromInt((PVScalar)valuePVField, value);
            deviceTrace.print(Trace.FLOW,
                "%s:%s putData",fullName,supportName);
        }
    }
    
    private static class Int32AverageInput extends AbstractPDRVSupport
    implements Int32InterruptListener
    {
        private Int32AverageInput(PVStructure pvStructure,String supportName) {
            super(supportName,pvStructure);
        }

        private Int32 int32 = null;
        private int numValues = 0;
        private long sum = 0;
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVSupport#initialize(org.epics.ioc.support.RecordSupport)
         */
        public void initialize(RecordSupport recordSupport) {
            super.initialize(recordSupport);
            if(!super.checkSupportState(SupportState.readyForStart,supportName)) return;
            if(valuePVField.getField().getType()==Type.scalar) return;
            super.uninitialize();
            pvStructure.message("value field is not a scalar type", MessageType.fatalError);
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
                pvStructure.message("interface int32 not supported", MessageType.fatalError);
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
                convert.fromDouble((PVScalar)valuePVField, average);
                numValues = 0;
                sum = 0;
            }
            supportProcessRequester.supportProcessDone(RequestResult.success);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.interfaces.Int32InterruptListener#interrupt(int)
         */
        public void interrupt(int value) {
            pvRecord.lock();
            try {
                sum += (long)value;
                ++numValues;
            } finally {
                pvRecord.unlock();
            }
        }
    }
    
    private static class Int32Output extends AbstractPDRVSupport
    {
        private Int32Output(PVStructure pvStructure,String supportName) {
            super(supportName,pvStructure);
        }

        private Int32 int32 = null;
        private int value;
        private Status status = Status.success;
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVSupport#initialize(org.epics.ioc.support.RecordSupport)
         */
        public void initialize(RecordSupport recordSupport) {
            super.initialize(recordSupport);
            if(!super.checkSupportState(SupportState.readyForStart,supportName)) return;
            if(valuePVField.getField().getType()==Type.scalar) return;
            super.uninitialize();
            pvStructure.message("value field is not a scalar type", MessageType.fatalError);
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
                pvStructure.message("interface int32 not supported", MessageType.fatalError);
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
            value = convert.toInt((PVScalar)valuePVField);
            super.process(supportProcessRequester);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#processContinue()
         */
        public void processContinue() {
            if(status!=Status.success) {
                alarmSupport.setAlarm(user.getMessage(), AlarmSeverity.invalid);
            }
            super.processContinue();
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#queueCallback()
         */
        public void queueCallback() {
            deviceTrace.print(Trace.SUPPORT, "%s value = %d", fullName,value);
            status = int32.write(user, value);
            if(status!=Status.success) {
            	deviceTrace.print(Trace.ERROR,
                        "%s:%s int32.write failed", fullName,supportName);
                return;
            }
        }
    }
    
    private static class Int32ArrayInput extends AbstractPDRVSupport
    {
        private Int32ArrayInput(PVStructure pvStructure,String supportName) {
            super(supportName,pvStructure);
        }

        private PVArray valuePVArray = null;
        private Int32Array int32Array = null;
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVSupport#initialize(org.epics.ioc.support.RecordSupport)
         */
        public void initialize(RecordSupport recordSupport) {
            super.initialize(recordSupport);
            if(!super.checkSupportState(SupportState.readyForStart,supportName)) return;
            if(valuePVField.getField().getType()==Type.scalarArray) {
                valuePVArray = (PVArray)valuePVField;
                if(valuePVArray.getArray().getElementType().isNumeric()) return;   
            }
            super.uninitialize();
            pvStructure.message("value field is not an array with numeric elements", MessageType.fatalError);
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
                pvStructure.message("interface int32Array not supported", MessageType.fatalError);
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
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#queueCallback()
         */
        public void queueCallback() {
        	deviceTrace.print(Trace.SUPPORT, "%s startRead", fullName);           ;
        	Status status = int32Array.startRead(user);
        	if(status!=Status.success) {
        		deviceTrace.print(Trace.ERROR,
        				"%s:%s int32Array.startRead failed", fullName,supportName);
        		return;
        	}
        	PVIntArray pvIntArray = int32Array.getPVIntArray();
        	convert.copyArray(pvIntArray, 0, valuePVArray, 0, pvIntArray.getLength());
        	int32Array.endRead(user);
        }
    }
    
    private static class Int32ArrayInterruptInput extends AbstractPDRVSupport
    implements Int32ArrayInterruptListener
    {
        private Int32ArrayInterruptInput(PVStructure pvStructure,String supportName) {
            super(supportName,pvStructure);
        }

        private PVArray valuePVArray = null;
        private Int32Array int32Array = null;
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVSupport#initialize(org.epics.ioc.support.RecordSupport)
         */
        public void initialize(RecordSupport recordSupport) {
            super.initialize(recordSupport);
            if(!super.checkSupportState(SupportState.readyForStart,supportName)) return;
            if(valuePVField.getField().getType()==Type.scalarArray) {
                valuePVArray = (PVArray)valuePVField;
                if(valuePVArray.getArray().getElementType().isNumeric()) return;   
            }
            super.uninitialize();
            pvStructure.message("value field is not an array with numeric elements", MessageType.fatalError);
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
                pvStructure.message("interface int32Array not supported", MessageType.fatalError);
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
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#queueCallback()
         */
        public void queueCallback() {
        	deviceTrace.print(Trace.SUPPORT, "%s startRead", fullName);     
        	Status status = int32Array.startRead(user);
        	if(status!=Status.success) {
        		deviceTrace.print(Trace.ERROR,
        				"%s:%s int32Array.startRead failed", fullName,supportName);
        		alarmSupport.setAlarm(user.getMessage(),AlarmSeverity.invalid);
        		return;
        	}
        	PVIntArray pvIntArray = int32Array.getPVIntArray();
            convert.copyArray(pvIntArray, 0, valuePVArray, 0, pvIntArray.getLength());
        	int32Array.endRead(user);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.interfaces.Int32ArrayInterruptListener#interrupt(org.epics.ioc.pv.PVIntArray)
         */
        public void interrupt(Int32Array int32Array) {
            PVIntArray pvIntArray = int32Array.getPVIntArray();
            if(super.isProcess()) {
                recordProcess.setActive(this);
                Status status = int32Array.startRead(user);
                if(status==Status.success) {
                    convert.copyArray(pvIntArray, 0, valuePVArray, 0, pvIntArray.getLength());
                    int32Array.endRead(user);
                }
                recordProcess.process(this, false, null);
            } else {
                pvRecord.lock();
                try {
                    Status status = int32Array.startRead(user);
                    if(status==Status.success) {
                        convert.copyArray(pvIntArray, 0, valuePVArray, 0, pvIntArray.getLength());
                        int32Array.endRead(user);
                    } else {
                        alarmSupport.setAlarm(user.getMessage(),AlarmSeverity.invalid);
                    } 
                }finally {
                    pvRecord.unlock();
                }
                deviceTrace.print(Trace.SUPPORT,
                        "%s:%s interrupt and record not processed",
                        fullName,supportName);
            }
        }
    }
    
    private static class Int32ArrayOutput extends AbstractPDRVSupport
    {
        private Int32ArrayOutput(PVStructure pvStructure,String supportName) {
            super(supportName,pvStructure);
        }

        private PVArray valuePVArray = null;
        private Int32Array int32Array = null;
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVSupport#initialize(org.epics.ioc.support.RecordSupport)
         */
        public void initialize(RecordSupport recordSupport) {
            super.initialize(recordSupport);
            if(!super.checkSupportState(SupportState.readyForStart,supportName)) return;
            if(valuePVField.getField().getType()==Type.scalarArray) {
                valuePVArray = (PVArray)valuePVField;
                if(valuePVArray.getArray().getElementType().isNumeric()) return;   
            }
            super.uninitialize();
            pvStructure.message("value field is not an array with numeric elements", MessageType.fatalError);
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
                pvStructure.message("interface int32Array not supported", MessageType.fatalError);
                super.stop();
                return;
            }
            int32Array = (Int32Array)iface;PVIntArray pvIntArray = int32Array.getPVIntArray();
            convert.copyArray(pvIntArray, 0, valuePVArray, 0, pvIntArray.getLength());
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
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#queueCallback()
         */
        public void queueCallback() {
        	deviceTrace.print(Trace.SUPPORT, "%s startWrite", fullName);   
        	Status status = int32Array.startWrite(user);
        	if(status!=Status.success) {
        		deviceTrace.print(Trace.ERROR,
        				"%s:%s int32Array.startWrite failed", fullName,supportName);
        		return;
        	}
        	PVIntArray pvIntArray = int32Array.getPVIntArray();
        	convert.copyArray(valuePVArray, 0, pvIntArray, 0, valuePVArray.getLength());
        	int32Array.endWrite(user);
        }
    }
    
    private static class UInt32DigitalInput extends AbstractPDRVSupport
    {
        private UInt32DigitalInput(PVStructure pvStructure,String supportName) {
            super(supportName,pvStructure);
        }

        private PVBoolean valuePVBoolean = null;
        private ScalarType valueScalarType = null;
        private PVInt pvIndex = null;
        private UInt32Digital uint32Digital = null;
        private int value;
        private int mask;
        private int shift;
        private Enumerated enumerated = null;
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVSupport#initialize(org.epics.ioc.support.RecordSupport)
         */
        public void initialize(RecordSupport recordSupport) {
            super.initialize(recordSupport);
            if(!super.checkSupportState(SupportState.readyForStart,supportName)) return;
            if(valuePVField.getField().getType()==Type.scalar) {
                PVScalar pvScalar = (PVScalar)valuePVField;
                valueScalarType = pvScalar.getScalar().getScalarType();
                if(valueScalarType==ScalarType.pvBoolean) {
                    valuePVBoolean = (PVBoolean)valuePVField;
                    return;
                } else if(valueScalarType==ScalarType.pvInt) {
                    pvIndex = (PVInt)valuePVField;
                    return;
                }
            }
            enumerated = EnumeratedFactory.getEnumerated(valuePVField);
            if(enumerated!=null) {
            	pvIndex = enumerated.getIndex();
            	return;
            }
            pvStructure.message("value field is not a valid type", MessageType.fatalError);
            super.uninitialize();
            return;
        }      
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#uninitialize()
         */
        public void uninitialize() {
            super.uninitialize();
            valuePVBoolean = null;
            pvIndex = null;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#start()
         */
        public void start() {
            super.start();
            if(!super.checkSupportState(SupportState.ready,supportName)) return;
            Interface iface = device.findInterface(user, "uint32Digital", true);
            if(iface==null) {
                pvStructure.message("interface uint32Digital not supported", MessageType.fatalError);
                super.stop();
                return;
            }
            uint32Digital = (UInt32Digital)iface;
            if(enumerated!=null) {
            	String[] choices = uint32Digital.getChoices(user);
            	if(choices!=null) {
            		PVStringArray pvStringArray = enumerated.getChoices();
            		pvStringArray.put(0, choices.length, choices, 0);
            		pvStringArray.postPut();
            	}
            }
            if(valueScalarType!=null) {
            	mask = pvMask.get();
            	if(mask==0) {
            		pvStructure.message("mask is 0", MessageType.fatalError);
            		super.stop();
            		return;
            	}
            	int i = 1;
            	shift = 0;
            	while(true) {
            		if((mask&i)!=0) break;
            		++shift; i <<= 1;
            	}
            }
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
        	if(valueScalarType!=null) {
                value = value&mask;
                value >>>= shift;
        	}
            if(valuePVBoolean!=null) {
                boolean oldValue = valuePVBoolean.get();
                boolean newValue = ((value==0) ? false : true);
                if(oldValue!=newValue) {
                    valuePVBoolean.put(newValue);
                    valuePVBoolean.postPut();
                }
            } else if(pvIndex!=null)  {
                pvIndex.put(value);
                pvIndex.postPut();
            } else {
                pvStructure.message(" logic error", MessageType.fatalError);
            }
            super.processContinue();
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#queueCallback()
         */
        public void queueCallback() {
        	deviceTrace.print(Trace.FLOW,
                    "%s:%s queueCallback calling read ",fullName,supportName);
            Status status = uint32Digital.read(user,mask);
            if(status!=Status.success) {
        		deviceTrace.print(Trace.ERROR,
        				"%s:%s uint32Digital.read failed", fullName,supportName);
        		return;
        	}
            value = user.getInt();
            deviceTrace.print(Trace.SUPPORT, "%s value = %d", fullName,value);
        }
    }
    
    private static class UInt32DigitalInterruptInput
    extends AbstractPDRVSupport implements UInt32DigitalInterruptListener
    {
        private UInt32DigitalInterruptInput(PVStructure pvStructure,String supportName) {
            super(supportName,pvStructure);
        }
        
        private ScalarType valueScalarType = null;
        private PVBoolean valuePVBoolean = null;
        private PVInt pvIndex = null;
        private UInt32Digital uint32Digital = null;
        private int value;
        private int mask;
        private int shift = 0;
        private Enumerated enumerated = null;
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVSupport#initialize(org.epics.ioc.support.RecordSupport)
         */
        public void initialize(RecordSupport recordSupport) {
            super.initialize(recordSupport);
            if(!super.checkSupportState(SupportState.readyForStart,supportName)) return;
            if(valuePVField.getField().getType()==Type.scalar) {
                PVScalar pvScalar = (PVScalar)valuePVField;
                valueScalarType = pvScalar.getScalar().getScalarType();
                if(valueScalarType==ScalarType.pvBoolean) {
                    valuePVBoolean = (PVBoolean)valuePVField;
                    return;
                } else if(valueScalarType==ScalarType.pvInt) {
                    pvIndex = (PVInt)valuePVField;
                    return;
                }
            }
            enumerated = EnumeratedFactory.getEnumerated(valuePVField);
            if(enumerated!=null) {
                pvIndex = enumerated.getIndex();
                return;
            }
            pvStructure.message("value field is not a valid type", MessageType.fatalError);
            super.uninitialize();
            return;
        }      
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#uninitialize()
         */
        public void uninitialize() {
            super.uninitialize();
            valuePVBoolean = null;
            pvIndex = null;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#start()
         */
        public void start() {
            super.start();
            if(!super.checkSupportState(SupportState.ready,supportName)) return;
            Interface iface = device.findInterface(user, "uint32Digital", true);
            if(iface==null) {
                pvStructure.message("interface uint32Digital not supported", MessageType.fatalError);
                super.stop();
                return;
            }
            uint32Digital = (UInt32Digital)iface;
            if(enumerated!=null) {
            	String[] choices = uint32Digital.getChoices(user);
            	if(choices!=null) {
            		PVStringArray pvStringArray = enumerated.getChoices();
            		pvStringArray.put(0, choices.length, choices, 0);
            		pvStringArray.postPut();
            	}
            }
            if(valueScalarType!=null) {
            	mask = pvMask.get();
            	if(mask==0) {
            		pvStructure.message("mask is 0", MessageType.fatalError);
            		super.stop();
            		return;
            	}
            	int i = 1;
            	shift = 0;
            	while(true) {
            		if((mask&i)!=0) break;
            		++shift; i <<= 1;
            	}
            }
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
            super.processContinue();
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.interfaces.UInt32DigitalInterruptListener#interrupt(int)
         */
        public void interrupt(int value) {
            this.value = value;
            if(super.isProcess()) {
                recordProcess.process(this, false, null);
            } else {
                pvRecord.lock();
                try {
                    putData();
                    deviceTrace.print(Trace.FLOW,
                        "%s:%s interrupt and record not processed",
                        fullName,supportName);
                } finally {
                    pvRecord.unlock();
                }
            }
        }
        
        private void putData() {
        	if(valueScalarType!=null) {
                value = value&mask;
                value >>>= shift;
        	}
            if(valuePVBoolean!=null) {
                valuePVBoolean.put((value==0) ? false : true);
                valuePVBoolean.postPut();
            } else if(pvIndex!=null)  {
                pvIndex.put(value);
                pvIndex.postPut();
            } else {
                pvStructure.message(" logic error", MessageType.fatalError);
            }
            deviceTrace.print(Trace.FLOW,
                    "%s:%s putData and  calling postPut",fullName,supportName);
        }
    }
    
    private static class UInt32DigitalOutput extends AbstractPDRVSupport
    {
        private UInt32DigitalOutput(PVStructure pvStructure,String supportName) {
            super(supportName,pvStructure);
        }
        private ScalarType valueScalarType = null;
        private PVBoolean valuePVBoolean = null;
        private PVInt pvIndex = null;
        private UInt32Digital uint32Digital = null;
        private int value;
        private int mask;
        private int shift;
        private Enumerated enumerated = null;
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVSupport#initialize(org.epics.ioc.support.RecordSupport)
         */
        public void initialize(RecordSupport recordSupport) {
            super.initialize(recordSupport);
            if(!super.checkSupportState(SupportState.readyForStart,supportName)) return;
            if(valuePVField.getField().getType()==Type.scalar) {
                PVScalar pvScalar = (PVScalar)valuePVField;
                valueScalarType = pvScalar.getScalar().getScalarType();
                if(valueScalarType==ScalarType.pvBoolean) {
                    valuePVBoolean = (PVBoolean)valuePVField;
                    return;
                } else if(valueScalarType==ScalarType.pvInt) {
                    pvIndex = (PVInt)valuePVField;
                    return;
                }
            }
            enumerated = EnumeratedFactory.getEnumerated(valuePVField);
            if(enumerated!=null) {
                pvIndex = enumerated.getIndex();
                return;
            }
            pvStructure.message("value field is not a valid type", MessageType.fatalError);
            super.uninitialize();
            return;
        }      
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#uninitialize()
         */
        public void uninitialize() {
            super.uninitialize();
            valuePVBoolean = null;
            pvIndex = null;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#start()
         */
        public void start() {
            super.start();
            if(!super.checkSupportState(SupportState.ready,supportName)) return;
            Interface iface = device.findInterface(user, "uint32Digital", true);
            if(iface==null) {
                pvStructure.message("interface uint32Digital not supported", MessageType.fatalError);
                super.stop();
                return;
            }
            uint32Digital = (UInt32Digital)iface;
            if(enumerated!=null) {
            	String[] choices = uint32Digital.getChoices(user);
            	if(choices!=null) {
            		PVStringArray pvStringArray = enumerated.getChoices();
            		pvStringArray.put(0, choices.length, choices, 0);
            		pvStringArray.postPut();
            	}
            }
            if(valueScalarType!=null) {
            	mask = pvMask.get();
            	if(mask==0) {
            		pvStructure.message("mask is 0", MessageType.fatalError);
            		super.stop();
            		return;
            	}
            	int i = 1;
            	shift = 0;
            	while(true) {
            		if((mask&i)!=0) break;
            		++shift; i <<= 1;
            	}
            }
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
            } else if(pvIndex!=null)  {
                value = pvIndex.get();
            } else {
                pvStructure.message(" logic error", MessageType.fatalError);
            }
            if(valueScalarType!=null) {
                value <<= shift;
            }
            super.process(supportProcessRequester);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#queueCallback()
         */
        public void queueCallback() {
        	deviceTrace.print(Trace.SUPPORT, "%s value = %d", fullName,value);
            Status status = uint32Digital.write(user, value,mask);
            if(status!=Status.success) {
        		deviceTrace.print(Trace.ERROR,
        				"%s:%s uint32Digital.write failed", fullName,supportName);
        		return;
        	}
        }
    }
    
    private static class Float64Input extends AbstractPDRVSupport
    {
        private Float64Input(PVStructure pvStructure,String supportName) {
            super(supportName,pvStructure);
        }

        private PVField valuePVField = null;
        private Float64 float64 = null;
        private double value;
        private Status status = Status.success;
        private PVDouble pvLowLimit = null;
        private PVDouble pvHighLimit = null;
        private PVString pvUnits = null;
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVSupport#initialize(org.epics.ioc.support.RecordSupport)
         */
        public void initialize(RecordSupport recordSupport) {
            super.initialize(recordSupport);
            if(!super.checkSupportState(SupportState.readyForStart,supportName)) return;
            PVField pvDisplay = pvProperty.findProperty(valuePVField,"display");
            if(pvDisplay!=null) {
                PVField pvTemp = pvProperty.findProperty(pvDisplay,"units");
                if(pvTemp!=null && pvTemp.getField().getType()==Type.scalar) {
                    PVScalar pvScalar = (PVScalar)pvTemp;
                    if(pvScalar.getScalar().getScalarType()==ScalarType.pvString) {
                        pvUnits = (PVString)pvTemp;
                    }
                }
                pvTemp = pvProperty.findProperty(pvDisplay,"limit");
                if(pvTemp!=null) {
                    PVField pvTemp1 = pvProperty.findProperty(pvTemp,"low");
                    if(pvTemp1!=null && pvTemp1.getField().getType()==Type.scalar) {
                        PVScalar pvScalar = (PVScalar)pvTemp1;
                        if(pvScalar.getScalar().getScalarType()==ScalarType.pvDouble) {
                            pvLowLimit = (PVDouble)pvTemp1;
                        }
                    }
                    pvTemp1 = pvProperty.findProperty(pvTemp,"high");
                    if(pvTemp1!=null && pvTemp1.getField().getType()==Type.scalar) {
                        PVScalar pvScalar = (PVScalar)pvTemp1;
                        if(pvScalar.getScalar().getScalarType()==ScalarType.pvDouble) {
                            pvHighLimit = (PVDouble)pvTemp1;
                        }
                    }
                }
            }
            if(valuePVField.getField().getType()==Type.scalar) return;
            super.uninitialize();
            pvStructure.message("value field is not a scalar type", MessageType.fatalError);
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
                pvStructure.message("interface float64 not supported", MessageType.fatalError);
                super.stop();
                return;
            }
            float64 = (Float64)iface;
            if(pvUnits!=null && (pvUnits.get()==null || pvUnits.get().length()==0)) {
            	String units = float64.getUnits(user);
            	pvUnits.put(units);
            	pvUnits.postPut();
            }
            if(pvLowLimit!=null && pvHighLimit!=null) {
            	if(pvLowLimit.get()==pvHighLimit.get()) {
            		double[] limits = float64.getDisplayLimits(user);
            		if(limits!=null) {
            		    pvLowLimit.put(limits[0]);
            		    pvLowLimit.postPut();
            		    pvHighLimit.put(limits[1]);
            		    pvHighLimit.postPut();
            		}
            	}
            }
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
                convert.fromDouble((PVScalar)valuePVField, value);
            } else {
                alarmSupport.setAlarm(user.getMessage(), AlarmSeverity.invalid);
            }
            super.processContinue();
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#queueCallback()
         */
        public void queueCallback() {
        	deviceTrace.print(Trace.FLOW,
        			"%s:%s queueCallback calling read ",fullName,supportName);
        	Status status = float64.read(user);
        	if(status!=Status.success) {
        		deviceTrace.print(Trace.ERROR,
        				"%s:%s float64.read failed", fullName,supportName);
        		alarmSupport.setAlarm(user.getMessage(),AlarmSeverity.invalid);
        		return;
        	}
        	value = user.getDouble();
        	deviceTrace.print(Trace.SUPPORT, "%s value = %e", fullName,value);
        }
    }
    
    private static class Float64InterruptInput extends AbstractPDRVSupport implements Float64InterruptListener
    {
        private Float64InterruptInput(PVStructure pvStructure,String supportName) {
            super(supportName,pvStructure);
        }

        private PVField valuePVField = null;
        private Float64 float64 = null;
        private double value;
        private Status status = Status.success;
        private PVDouble pvLowLimit = null;
        private PVDouble pvHighLimit = null;
        private PVString pvUnits = null;
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVSupport#initialize(org.epics.ioc.support.RecordSupport)
         */
        public void initialize(RecordSupport recordSupport) {
            super.initialize(recordSupport);
            if(!super.checkSupportState(SupportState.readyForStart,supportName)) return;
            PVField pvDisplay = pvProperty.findProperty(valuePVField,"display");
            if(pvDisplay!=null) {
                PVField pvTemp = pvProperty.findProperty(pvDisplay,"units");
                if(pvTemp!=null && pvTemp.getField().getType()==Type.scalar) {
                    PVScalar pvScalar = (PVScalar)pvTemp;
                    if(pvScalar.getScalar().getScalarType()==ScalarType.pvString) {
                        pvUnits = (PVString)pvTemp;
                    }
                }
                pvTemp = pvProperty.findProperty(pvDisplay,"limit");
                if(pvTemp!=null) {
                    PVField pvTemp1 = pvProperty.findProperty(pvTemp,"low");
                    if(pvTemp1!=null && pvTemp1.getField().getType()==Type.scalar) {
                        PVScalar pvScalar = (PVScalar)pvTemp1;
                        if(pvScalar.getScalar().getScalarType()==ScalarType.pvDouble) {
                            pvLowLimit = (PVDouble)pvTemp1;
                        }
                    }
                    pvTemp1 = pvProperty.findProperty(pvTemp,"high");
                    if(pvTemp1!=null && pvTemp1.getField().getType()==Type.scalar) {
                        PVScalar pvScalar = (PVScalar)pvTemp1;
                        if(pvScalar.getScalar().getScalarType()==ScalarType.pvDouble) {
                            pvHighLimit = (PVDouble)pvTemp1;
                        }
                    }
                }
            }
            if(valuePVField.getField().getType()==Type.scalar) return;
            super.uninitialize();
            pvStructure.message("value field is not a scalar type", MessageType.fatalError);
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
                pvStructure.message("interface float64 not supported", MessageType.fatalError);
                super.stop();
                return;
            }
            float64 = (Float64)iface;
            if(pvUnits!=null && (pvUnits.get()==null || pvUnits.get().length()==0)) {
            	String units = float64.getUnits(user);
            	pvUnits.put(units);
            	pvUnits.postPut();
            }
            if(pvLowLimit!=null && pvHighLimit!=null) {
            	if(pvLowLimit.get()==pvHighLimit.get()) {
            		double[] limits = float64.getDisplayLimits(user);
            		if(limits!=null) {
            		    pvLowLimit.put(limits[0]);
            		    pvLowLimit.postPut();
            		    pvHighLimit.put(limits[1]);
            		    pvHighLimit.postPut();   
            		}
            	}
            }
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
            super.processContinue();
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.interfaces.Float64InterruptListener#interrupt(double)
         */
        public void interrupt(double value) {
            this.value = value;
            if(super.isProcess()) {
                recordProcess.process(this, false, null);
            } else {
                pvRecord.lock();
                try {
                    putData();
                    deviceTrace.print(Trace.FLOW,
                        "%s:%s interrupt and record not processed",
                        fullName,supportName);
                } finally {
                    pvRecord.unlock();
                }
            }
        }
        
        private void putData() {
            convert.fromDouble((PVScalar)valuePVField, value);
            deviceTrace.print(Trace.FLOW,
                "%s:%s putData ",fullName,supportName);
        }
    }
    
    private static class Float64AverageInput extends AbstractPDRVSupport implements Float64InterruptListener
    {
        private Float64AverageInput(PVStructure pvStructure,String supportName) {
            super(supportName,pvStructure);
        }

        private PVField valuePVField = null;
        private Float64 float64 = null;
        private int numValues = 0;
        private double sum = 0.0;
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVSupport#initialize(org.epics.ioc.support.RecordSupport)
         */
        public void initialize(RecordSupport recordSupport) {
            super.initialize(recordSupport);
            if(!super.checkSupportState(SupportState.readyForStart,supportName)) return;
            if(valuePVField.getField().getType()==Type.scalar) return;
            super.uninitialize();
            pvStructure.message("value field is not a scalar type", MessageType.fatalError);
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
                pvStructure.message("interface float64 not supported", MessageType.fatalError);
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
                convert.fromDouble((PVScalar)valuePVField, average);
                numValues = 0;
                sum = 0.0;
            }
            supportProcessRequester.supportProcessDone(RequestResult.success);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.interfaces.Float64InterruptListener#interrupt(double)
         */
        public void interrupt(double value) {
            pvRecord.lock();
            try {
                sum += (double)value;
                ++numValues;
            } finally {
                pvRecord.unlock();
            }
        }
    }
    
    private static class Float64Output extends AbstractPDRVSupport
    {
        private Float64Output(PVStructure pvStructure,String supportName) {
            super(supportName,pvStructure);
        }

        private PVField valuePVField = null;
        private Float64 float64 = null;
        private double value;
        private Status status = Status.success;
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVSupport#initialize(org.epics.ioc.support.RecordSupport)
         */
        public void initialize(RecordSupport recordSupport) {
            super.initialize(recordSupport);
            if(!super.checkSupportState(SupportState.readyForStart,supportName)) return;
            if(valuePVField.getField().getType()==Type.scalar) return;
            super.uninitialize();
            pvStructure.message("value field is not a scalar type", MessageType.fatalError);
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
                pvStructure.message("interface float64 not supported", MessageType.fatalError);
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
            value = convert.toDouble((PVScalar)valuePVField);
            super.process(supportProcessRequester);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#processContinue()
         */
        public void processContinue() {
            if(status!=Status.success) {
                alarmSupport.setAlarm(user.getMessage(), AlarmSeverity.invalid);
            }
            super.processContinue();
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#queueCallback()
         */
        public void queueCallback() {
            deviceTrace.print(Trace.SUPPORT, "%s value = %e", fullName,value);
            status = float64.write(user, value);
        }
    }
    
    private static class Float64ArrayInput extends AbstractPDRVSupport
    {
        private Float64ArrayInput(PVStructure pvStructure,String supportName) {
            super(supportName,pvStructure);
        }

        private PVArray valuePVArray = null;
        private Float64Array float64Array = null;
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVSupport#initialize(org.epics.ioc.support.RecordSupport)
         */
        public void initialize(RecordSupport recordSupport) {
            super.initialize(recordSupport);
            if(!super.checkSupportState(SupportState.readyForStart,supportName)) return;
            if(valuePVField.getField().getType()==Type.scalarArray) {
                valuePVArray = (PVArray)valuePVField;
                if(valuePVArray.getArray().getElementType().isNumeric()) return;   
            }
            super.uninitialize();
            pvStructure.message("value field is not an array with numeric elements", MessageType.fatalError);
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
                pvStructure.message("interface float64Array not supported", MessageType.fatalError);
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
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#queueCallback()
         */
        public void queueCallback() {
        	deviceTrace.print(Trace.SUPPORT, "%s queueCallback", fullName);
        	Status status = float64Array.startRead(user);
        	if(status!=Status.success) {
        		deviceTrace.print(Trace.ERROR,
        				"%s:%s float64Array.startRead failed", fullName,supportName);
        		return;
        	}
        	PVDoubleArray pvDoubleArray = float64Array.getPVDoubleArray();
        	convert.copyArray(pvDoubleArray, 0, valuePVArray, 0, pvDoubleArray.getLength());
        	float64Array.endRead(user);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.AsynAccessListener#beginSyncAccess()
         */
        public void beginSyncAccess() {
            deviceTrace.print(Trace.FLOW, "%s beginSyncAccess", fullName);
            pvRecord.lock();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.AsynAccessListener#endSyncAccess()
         */
        public void endSyncAccess() {
            deviceTrace.print(Trace.FLOW, "%s endSyncAccess", fullName);
            pvRecord.unlock();
        }
    }
    
    private static class Float64ArrayInterruptInput extends AbstractPDRVSupport
    implements Float64ArrayInterruptListener
    {
        private Float64ArrayInterruptInput(PVStructure pvStructure,String supportName) {
            super(supportName,pvStructure);
        }

        private PVArray valuePVArray = null;
        private Float64Array float64Array = null;
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVSupport#initialize(org.epics.ioc.support.RecordSupport)
         */
        public void initialize(RecordSupport recordSupport) {
            super.initialize(recordSupport);
            if(!super.checkSupportState(SupportState.readyForStart,supportName)) return;
            if(valuePVField.getField().getType()==Type.scalarArray) {
                valuePVArray = (PVArray)valuePVField;
                if(valuePVArray.getArray().getElementType().isNumeric()) return;   
            }
            super.uninitialize();
            pvStructure.message("value field is not an array with numeric elements", MessageType.fatalError);
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
                pvStructure.message("interface float64Array not supported", MessageType.fatalError);
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
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#queueCallback()
         */
        public void queueCallback() {
            Status status = float64Array.startRead(user);
            if(status==Status.success) {
                PVDoubleArray pvDoubleArray = float64Array.getPVDoubleArray();
                convert.copyArray(pvDoubleArray, 0, valuePVArray, 0, pvDoubleArray.getLength());
                float64Array.endRead(user);
            } else {
                alarmSupport.setAlarm(user.getMessage(),AlarmSeverity.invalid);
            }
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.interfaces.Float64ArrayInterruptListener#interrupt(org.epics.ioc.pdrv.interfaces.Float64Array)
         */
        public void interrupt(Float64Array float64Array) {
            PVDoubleArray pvDoubleArray = float64Array.getPVDoubleArray();
            if(super.isProcess()) {
                recordProcess.setActive(this);
                Status status = float64Array.startRead(user);
                if(status==Status.success) {
                    convert.copyArray(pvDoubleArray, 0, valuePVArray, 0, pvDoubleArray.getLength());
                    float64Array.endRead(user);
                }
                recordProcess.process(this, false, null);
            } else {
                pvRecord.lock();
                try {

                    Status status = float64Array.startRead(user);
                    if(status==Status.success) {
                        convert.copyArray(pvDoubleArray, 0, valuePVArray, 0, pvDoubleArray.getLength());
                        float64Array.endRead(user);
                    } else {
                        alarmSupport.setAlarm(user.getMessage(),AlarmSeverity.invalid);
                    } 
                } finally {
                    pvRecord.unlock();
                }
                deviceTrace.print(Trace.SUPPORT,
                        "%s:%s interrupt and record not processed",
                        fullName,supportName);
            }
        }
    }
    
    private static class Float64ArrayOutput extends AbstractPDRVSupport
    {
        private Float64ArrayOutput(PVStructure pvStructure,String supportName) {
            super(supportName,pvStructure);
        }

        private PVArray valuePVArray = null;
        private Float64Array float64Array = null;
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.support.AbstractPDRVSupport#initialize(org.epics.ioc.support.RecordSupport)
         */
        public void initialize(RecordSupport recordSupport) {
            super.initialize(recordSupport);
            if(!super.checkSupportState(SupportState.readyForStart,supportName)) return;
            if(valuePVField.getField().getType()==Type.scalarArray) {
                valuePVArray = (PVArray)valuePVField;
                if(valuePVArray.getArray().getElementType().isNumeric()) return;   
            }
            super.uninitialize();
            pvStructure.message("value field is not an array with numeric elements", MessageType.fatalError);
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
                pvStructure.message("interface float64Array not supported", MessageType.fatalError);
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
         * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#queueCallback()
         */
        public void queueCallback() {
            Status status = float64Array.startWrite(user);
            if(status==Status.success) {
                PVDoubleArray pvDoubleArray = float64Array.getPVDoubleArray();
                convert.copyArray(valuePVArray, 0, pvDoubleArray, 0, valuePVArray.getLength());
                float64Array.endWrite(user);
            }
        }
    }
}
