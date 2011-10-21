/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.pdrv;

import org.epics.ioc.database.PVRecordField;
import org.epics.ioc.database.PVRecordStructure;
import org.epics.ioc.install.AfterStart;
import org.epics.ioc.pdrv.Device;
import org.epics.ioc.pdrv.Factory;
import org.epics.ioc.pdrv.Port;
import org.epics.ioc.pdrv.QueuePriority;
import org.epics.ioc.pdrv.QueueRequestCallback;
import org.epics.ioc.pdrv.Status;
import org.epics.ioc.pdrv.Trace;
import org.epics.ioc.pdrv.User;
import org.epics.ioc.pdrv.interfaces.DriverUser;
import org.epics.ioc.pdrv.interfaces.Interface;
import org.epics.ioc.support.AbstractSupport;
import org.epics.ioc.support.ProcessContinueRequester;
import org.epics.ioc.support.RecordProcess;
import org.epics.ioc.support.Support;
import org.epics.ioc.support.SupportProcessRequester;
import org.epics.ioc.support.SupportState;
import org.epics.ioc.support.alarm.AlarmSupport;
import org.epics.ioc.support.alarm.AlarmSupportFactory;
import org.epics.ioc.util.RequestResult;
import org.epics.pvData.property.AlarmSeverity;
import org.epics.pvData.property.AlarmStatus;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVDouble;
import org.epics.pvData.pv.PVField;
import org.epics.pvData.pv.PVString;
import org.epics.pvData.pv.PVStructure;

/**
 * Factory to create support for a link to a Port Driver.
 * @author mrk
 *
 */
public class PortDriverLinkFactory {
    public static Support create(PVRecordStructure pvRecordStructure) {
        return new PortDriverLinkImpl(PortDriverLink,pvRecordStructure);
    }
    
    private static final String PortDriverLink = "org.epics.ioc.portDriverLink";
    
    /**
     * @author mrk
     *
     */
    private static class PortDriverLinkImpl extends AbstractSupport
    implements PortDriverLink,QueueRequestCallback,ProcessContinueRequester
    {
        /**
         * Constructor for derived support.
         * @param supportName The support name.
         * @param pvRecordStructure The link interface.
         */
        private PortDriverLinkImpl(String supportName,PVRecordStructure pvRecordStructure) {
            super(supportName,pvRecordStructure);
            this.pvRecordStructure = pvRecordStructure;
        }  

        private static final String emptyString = "";
        private final PVRecordStructure pvRecordStructure;
        private RecordProcess recordProcess = null;
        private PVString pvPortName = null;
        private PVString pvDeviceName = null;
        
        private PVDouble pvTimeout = null;
        private PVStructure pvDrvParams = null;
        
        private User user = null;
        private Port port = null;
        private Device device = null;
        private Trace deviceTrace = null;
        
        private DriverUser driverUser = null;
        private AlarmSupport alarmSupport = null;
        private PortDriverSupport[] portDriverSupports = null;
        private byte[] byteBuffer = null;
        
        private SupportProcessRequester supportProcessRequester = null;
        
        /* (non-Javadoc)
         * @see org.epics.ioc.support.AbstractSupport#initialize()
         */
        @Override
        public void initialize() {
            if(!super.checkSupportState(SupportState.readyForInitialize,super.getSupportName())) return;
            recordProcess = pvRecordStructure.getPVRecord().getRecordProcess();
            PVStructure pvStructure = pvRecordStructure.getPVStructure();
            alarmSupport = AlarmSupportFactory.findAlarmSupport(pvRecordStructure);
            if(alarmSupport==null) {
                pvRecordStructure.message("no alarm field", MessageType.error);
                return;
            }
            pvPortName = pvStructure.getStringField("portName");
            if(pvPortName==null) return;
            pvDeviceName = pvStructure.getStringField("deviceName");
            if(pvDeviceName==null) return;
            pvTimeout = pvStructure.getDoubleField("timeout");
            if(pvTimeout==null) return;
            PVField pvField = pvStructure.getSubField("drvParams");
            if(pvField!=null) {
                pvDrvParams = pvStructure.getStructureField("drvParams");
            }
            PVRecordField[] pvRecordFields = pvRecordStructure.getPVRecordFields();
            int n = pvRecordFields.length;
            int numberSupport = 0;
            for(int i=0; i< n; i++) {
                pvField = pvRecordFields[i].getPVField();
                String fieldName = pvField.getField().getFieldName();
                // alarm is a special case
                if(fieldName.equals("alarm")) continue;
                Support support = pvRecordFields[i].getSupport();
                if(support==null) continue;
                if(!(support instanceof PortDriverSupport)) {
                    pvRecordFields[i].message("support is not PortDriverSupport", MessageType.error);
                    return;
                }
                numberSupport++;
            }
            portDriverSupports = new PortDriverSupport[numberSupport];
            int indSupport = 0;
            for(int i=0; i< n; i++) {
                pvField = pvRecordFields[i].getPVField();
                String fieldName = pvField.getField().getFieldName();
                if(fieldName.equals("alarm")) continue;
                Support support = pvRecordFields[i].getSupport();
                if(support==null) continue;
                if(!(support instanceof PortDriverSupport)) continue;
                portDriverSupports[indSupport] = (PortDriverSupport)support;
                support.initialize();
                if(support.getSupportState()!=SupportState.readyForStart) {
                    for(int j=0; j<indSupport-1; j++) {
                        portDriverSupports[j].uninitialize();
                    }
                    return;
                }
                indSupport++;
            }
            if(alarmSupport!=null) {
                alarmSupport.initialize();
                if(alarmSupport.getSupportState()!=SupportState.readyForStart) {
                    for(PortDriverSupport portDriverSupport : portDriverSupports) {
                        portDriverSupport.uninitialize();
                    }
                    return;
                }
            }
            setSupportState(SupportState.readyForStart);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.support.AbstractSupport#uninitialize()
         */
        @Override
        public void uninitialize() {
            if(super.getSupportState()==SupportState.ready) stop();
            alarmSupport.uninitialize();
            for(PortDriverSupport portDriverSupport : portDriverSupports) {
                portDriverSupport.uninitialize();
            }
            if(super.getSupportState()!=SupportState.readyForStart) return;
            portDriverSupports = null;
            pvDrvParams = null;
            pvTimeout = null;
            pvDeviceName = null;
            pvPortName = null;
            alarmSupport = null;
            setSupportState(SupportState.readyForInitialize);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.support.AbstractSupport#start(org.epics.ioc.install.AfterStart)
         */
        @Override
        public void start(AfterStart afterStart) {
            if(!super.checkSupportState(SupportState.readyForStart,super.getSupportName())) return;
            PVStructure pvStructure = (PVStructure)super.getPVRecordField().getPVField();
            
            user = Factory.createUser(this);
            port = user.connectPort(pvPortName.get());
            if(port==null) {
                pvStructure.message(user.getMessage(),MessageType.error);
                return;
            }
            device = user.connectDevice(pvDeviceName.get());
            if(device==null) {
                pvStructure.message(user.getMessage(),MessageType.error);
                return;
            }
            deviceTrace = device.getTrace();
            alarmSupport.start(null);
            if(alarmSupport.getSupportState()!=SupportState.ready) return;
            Interface iface = device.findInterface(user, "driverUser");
            if(iface!=null) {
                driverUser = (DriverUser)iface;
                driverUser.create(user,pvDrvParams);
            }
            for(int i=0; i<portDriverSupports.length; i++) {
                PortDriverSupport portDriverSupport = portDriverSupports[i];
                portDriverSupport.setPortDriverLink(this);
                portDriverSupport.start(null);
                if(portDriverSupport.getSupportState()!=SupportState.ready) {
                    for(int j=0; j<i-1; j++) {
                        portDriverSupports[j].stop();
                    }
                    if(driverUser!=null) driverUser.dispose(user);
                    alarmSupport.stop();
                    return;
                }
            }
            setSupportState(SupportState.ready);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.support.AbstractSupport#stop()
         */
        @Override
        public void stop() {
            if(super.getSupportState()!=SupportState.ready) return;
            alarmSupport.stop();
            for(PortDriverSupport portDriverSupport : portDriverSupports) {
                portDriverSupport.stop();
            }
            user.disconnectPort();
            if(driverUser!=null) {
                driverUser.dispose(user);
            }
            driverUser = null;
            device = null;
            deviceTrace = null;
            port = null;
            user = null;
            setSupportState(SupportState.readyForStart);
        }   
        /* (non-Javadoc)
         * @see org.epics.ioc.support.AbstractSupport#process(org.epics.ioc.process.SupportProcessRequester)
         */
        @Override
        public void process(SupportProcessRequester supportProcessRequester) {
            alarmSupport.beginProcess();
            String fullName = super.getPVRecordField().getFullName();
            for(PortDriverSupport portDriverSupport : portDriverSupports) {
                portDriverSupport.beginProcess();
            }
            if((deviceTrace.getMask()&Trace.FLOW)!=0) {
                deviceTrace.print(Trace.FLOW,
                    "pv %s support %s process calling queueRequest", fullName,super.getSupportName());
            }
            user.setMessage(null);
            user.setTimeout(pvTimeout.get());
            this.supportProcessRequester = supportProcessRequester;
            user.queueRequest(QueuePriority.medium);
            if(!port.canBlock()) {
                processContinue();
            }
        }   
        /* (non-Javadoc)
         * @see org.epics.ioc.support.pdrv.PortDriverLink#getUser()
         */
        @Override
        public User getUser() {
            return user;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.support.pdrv.PortDriverLink#expandByteBuffer(int)
         */
        @Override
        public byte[] expandByteBuffer(int size) {
            if(byteBuffer!=null && size<=byteBuffer.length) return byteBuffer;
            byte[] old = byteBuffer;
            byteBuffer = new byte[size];
            if(old!=null) {
                System.arraycopy(old, 0, byteBuffer, 0, old.length);
            }
            return byteBuffer;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.support.pdrv.PortDriverLink#getAlarmSupport()
         */
        @Override
        public AlarmSupport getAlarmSupport() {
            return alarmSupport;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.support.pdrv.PortDriverLink#getByteBuffer()
         */
        @Override
        public byte[] getByteBuffer() {
            if(byteBuffer==null) byteBuffer = new byte[80];
            return byteBuffer;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.QueueRequestCallback#callback(org.epics.ioc.pdrv.Status, org.epics.ioc.pdrv.User)
         */
        @Override
        public void callback(Status status, User user) {
            String fullName = super.getPVRecordField().getFullName();
            if(status==Status.success) {
                if((deviceTrace.getMask()&Trace.FLOW)!=0) {
                    deviceTrace.print(Trace.FLOW,
                        "pv %s callback calling queueCallback", fullName);
                }
                for(PortDriverSupport portDriverSupport : portDriverSupports) {
                    portDriverSupport.queueCallback();
                }
            } else {
                if((deviceTrace.getMask()&Trace.FLOW)!=0) {
                    deviceTrace.print(Trace.FLOW,
                        "pv %s support %s callback error %s",
                        fullName,super.getSupportName(),user.getMessage());
                }
                alarmSupport.setAlarm(user.getMessage(), AlarmSeverity.INVALID,AlarmStatus.DRIVER);
            }
            if(port.canBlock()) {
                if((deviceTrace.getMask()&Trace.FLOW)!=0) {
                    deviceTrace.print(Trace.FLOW,
                        "pv %s callback calling processContinue", fullName);
                }
                recordProcess.processContinue(this);
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.support.ProcessContinueRequester#processContinue()
         */
        @Override
        public void processContinue() {
            for(PortDriverSupport portDriverSupport : portDriverSupports) {
                portDriverSupport.endProcess();
            }
            String fullName = super.getPVRecordField().getFullName();
            if((deviceTrace.getMask()&Trace.FLOW)!=0) {
                deviceTrace.print(Trace.FLOW,
                    "pv %s processContinue calling supportProcessDone",fullName);
            }
            String message = user.getMessage();
            if(message!=null && message!=emptyString) {
                alarmSupport.setAlarm(message, AlarmSeverity.MINOR,AlarmStatus.DRIVER);
            }
            alarmSupport.endProcess();
            supportProcessRequester.supportProcessDone(RequestResult.success);
        }
    }
}
