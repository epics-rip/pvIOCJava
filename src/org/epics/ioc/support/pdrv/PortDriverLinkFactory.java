/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.pdrv;

import org.epics.ioc.install.AfterStart;
import org.epics.ioc.install.LocateSupport;
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
    public static Support create(PVStructure pvStructure) {
        return new PortDriverLinkImpl(PortDriverLink,pvStructure);
    }
    
    private static final String PortDriverLink = "portDriverLink";
    
    private static class PortDriverLinkImpl extends AbstractSupport
    implements PortDriverLink,QueueRequestCallback,ProcessContinueRequester
    {
        /**
         * Constructor for derived support.
         * @param supportName The support name.
         * @param dbStructure The link interface.
         */
        private PortDriverLinkImpl(String supportName,PVStructure pvStructure) {
            super(supportName,pvStructure);
        }  

        private static final String emptyString = "";
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
        
        public void initialize(LocateSupport recordSupport) {
            if(!super.checkSupportState(SupportState.readyForInitialize,supportName)) return;
            recordProcess = recordSupport.getRecordProcess();
            PVStructure pvStructure = (PVStructure)super.getPVField();
            alarmSupport = AlarmSupportFactory.findAlarmSupport(pvStructure,recordSupport);
            if(alarmSupport==null) {
                super.getPVField().message("no alarm field", MessageType.error);
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
            PVField[] pvFields = pvStructure.getPVFields();
            int n = pvFields.length;
            int numberSupport = 0;
            for(int i=0; i< n; i++) {
                pvField = pvFields[i];
                String fieldName = pvField.getField().getFieldName();
                // alarm is a special case
                if(fieldName.equals("alarm")) continue;
                Support support = recordSupport.getSupport(pvFields[i]);
                if(support==null) continue;
                if(!(support instanceof PortDriverSupport)) {
                    pvFields[i].message("support is not PortDriverSupport", MessageType.error);
                    return;
                }
                numberSupport++;
            }
            portDriverSupports = new PortDriverSupport[numberSupport];
            int indSupport = 0;
            for(int i=0; i< n; i++) {
                pvField = pvFields[i];
                String fieldName = pvField.getField().getFieldName();
                if(fieldName.equals("alarm")) continue;
                Support support = recordSupport.getSupport(pvField);
                if(support==null) continue;
                if(!(support instanceof PortDriverSupport)) continue;
                portDriverSupports[indSupport] = (PortDriverSupport)support;
                support.initialize(recordSupport);
                if(support.getSupportState()!=SupportState.readyForStart) {
                    for(int j=0; j<indSupport-1; j++) {
                        portDriverSupports[j].uninitialize();
                    }
                    return;
                }
                indSupport++;
            }
            if(alarmSupport!=null) {
                alarmSupport.initialize(recordSupport);
                if(alarmSupport.getSupportState()!=SupportState.readyForStart) {
                    for(PortDriverSupport portDriverSupport : portDriverSupports) {
                        portDriverSupport.uninitialize();
                    }
                    return;
                }
            }
            setSupportState(SupportState.readyForStart);
        }
        
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
        
        public void start(AfterStart afterStart) {
            if(!super.checkSupportState(SupportState.readyForStart,supportName)) return;
            PVStructure pvStructure = (PVStructure)super.getPVField();
            
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
        public void process(SupportProcessRequester supportProcessRequester) {
            alarmSupport.beginProcess();
            String fullName = super.getPVField().getFullName();
            for(PortDriverSupport portDriverSupport : portDriverSupports) {
                portDriverSupport.beginProcess();
            }
            if((deviceTrace.getMask()&Trace.FLOW)!=0) {
                deviceTrace.print(Trace.FLOW,
                    "pv %s support %s process calling queueRequest", fullName,supportName);
            }
            user.setMessage(null);
            user.setTimeout(pvTimeout.get());
            this.supportProcessRequester = supportProcessRequester;
            user.queueRequest(QueuePriority.medium);
            
        }   
        /* (non-Javadoc)
         * @see org.epics.ioc.support.pdrv.PortDriverLink#getUser()
         */
        public User getUser() {
            return user;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.support.pdrv.PortDriverLink#expandByteBuffer(int)
         */
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
        public AlarmSupport getAlarmSupport() {
            return alarmSupport;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.support.pdrv.PortDriverLink#getByteBuffer()
         */
        public byte[] getByteBuffer() {
            if(byteBuffer==null) byteBuffer = new byte[80];
            return byteBuffer;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.QueueRequestCallback#callback(org.epics.ioc.pdrv.Status, org.epics.ioc.pdrv.User)
         */
        public void callback(Status status, User user) {
            String fullName = super.getPVField().getFullName();
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
                        fullName,supportName,user.getMessage());
                }
                alarmSupport.setAlarm(user.getMessage(), AlarmSeverity.invalid);
            }
            if((deviceTrace.getMask()&Trace.FLOW)!=0) {
                deviceTrace.print(Trace.FLOW,
                    "pv %s callback calling processContinue", fullName);
            }
            recordProcess.processContinue(this);
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.support.ProcessContinueRequester#processContinue()
         */
        public void processContinue() {
            for(PortDriverSupport portDriverSupport : portDriverSupports) {
                portDriverSupport.endProcess();
            }
            String fullName = super.getPVField().getFullName();
            if((deviceTrace.getMask()&Trace.FLOW)!=0) {
                deviceTrace.print(Trace.FLOW,
                    "pv %s processContinue calling supportProcessDone",fullName);
            }
            String message = user.getMessage();
            if(message!=null && message!=emptyString) {
                alarmSupport.setAlarm(message, AlarmSeverity.minor);
            }
            alarmSupport.endProcess();
            supportProcessRequester.supportProcessDone(RequestResult.success);
        }
    }
}
