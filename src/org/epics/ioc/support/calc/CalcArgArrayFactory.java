/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.calc;

import org.epics.ioc.db.DBField;
import org.epics.ioc.db.DBStructure;
import org.epics.ioc.db.DBStructureArray;
import org.epics.ioc.pv.Array;
import org.epics.ioc.pv.Field;
import org.epics.ioc.pv.PVField;
import org.epics.ioc.pv.PVString;
import org.epics.ioc.pv.PVStructure;
import org.epics.ioc.pv.Structure;
import org.epics.ioc.pv.Type;
import org.epics.ioc.support.AbstractSupport;
import org.epics.ioc.support.Support;
import org.epics.ioc.support.SupportProcessRequester;
import org.epics.ioc.support.SupportState;
import org.epics.ioc.util.MessageType;
import org.epics.ioc.util.RequestResult;

/**
 * Support for an array of calcArg structures.
 * @author mrk
 *
 */
public class CalcArgArrayFactory {
    /**
     * Create support for an array of calcArg structures.
     * @param dbField The array which must be an array of links.
     * @return An interface to the support or null if the supportName was not "linkArray".
     */
    public static Support create(DBField dbField) {
        PVField pvField = dbField.getPVField();
        Field field = pvField.getField();
        Type type = field.getType();
        if(type!=Type.pvArray) {
            pvField.message("type is not an array",MessageType.error);
            return null;
        }
        Array array = (Array)field;
        Type elementType = array.getElementType();
        if(elementType!=Type.pvStructure) {
            pvField.message("element type is not a structure",MessageType.error);
            return null;
        }
        String supportName = pvField.getSupportName();
        if(supportName==null || !supportName.equals(supportName)) {
            pvField.message("does not have support " + supportName,MessageType.error);
            return null;
        }
        return new CalcArgArrayImpl((DBStructureArray)dbField);
    }
    
    private static String supportName = "calcArgArray";
    
    
    private static class CalcArgArrayImpl extends AbstractSupport
    implements CalcArgArraySupport,SupportProcessRequester
    {
        private PVField pvField;
        private String processRequesterName = null;
        private DBStructureArray calcArgArrayDBField;
        private DBField[] valueDBFields;
        private DBField[] nameDBFields;
        private Support[] supports = null;
        private Support[] alarmSupports = null;
        private int numSupports = 0;
              
        private SupportProcessRequester supportProcessRequester;
        private int numberWait;
        private RequestResult finalResult;
       
        private CalcArgArrayImpl(DBStructureArray dbNonScalarArray) {
            super(supportName,dbNonScalarArray);
            pvField = dbNonScalarArray.getPVField();
            processRequesterName = pvField.getFullName();
            calcArgArrayDBField = dbNonScalarArray; 
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.support.CalcArgArraySupport#getPVField(java.lang.String)
         */
        public PVField getPVField(String argName) {
            if(super.getSupportState()==SupportState.readyForInitialize) {
                throw new IllegalStateException("getPVField called but not initialized");
            }
            for(int i=0; i<nameDBFields.length; i++) {
                PVString pvString = (PVString)nameDBFields[i].getPVField();
                String name = pvString.get();
                if(name.equals(argName)) {
                    return valueDBFields[i].getPVField();
                }
            }
            return null;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.SupportProcessRequester#getProcessRequesterName()
         */
        public String getRequesterName() {
            return processRequesterName;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#initialize()
         */
        public void initialize() {
            if(!super.checkSupportState(SupportState.readyForInitialize,supportName)) return;
            SupportState supportState = SupportState.readyForStart;
            DBStructure[] dbFields = calcArgArrayDBField.getElementDBStructures();
            int length = dbFields.length;
            valueDBFields = new DBField[length];
            nameDBFields = new DBField[length];
            supports = new Support[length];
            alarmSupports = new Support[length];
            numSupports = 0;
            for(int i=0; i< length; i++) {
                DBStructure elementDBStructure = dbFields[i];
                PVStructure elementPVStructure = elementDBStructure.getPVStructure();
                Structure elementStructure = (Structure)elementPVStructure.getField();
                DBField[] elementDBFields = elementDBStructure.getDBFields();
                int index;
                index = elementStructure.getFieldIndex("value");
                if(index<0) {
                    elementPVStructure.message("value field not found", MessageType.error);
                    return;
                }
                valueDBFields[i] = elementDBFields[index];
                index = elementStructure.getFieldIndex("name");
                if(index<0) {
                    elementPVStructure.message("name field not found", MessageType.error);
                    return;
                }
                nameDBFields[i] = elementDBFields[index];
                index = elementStructure.getFieldIndex("input");
                if(index<0) {
                    elementPVStructure.message("input field not found", MessageType.error);
                    return;
                }
                DBField dbField = elementDBFields[index];
                Support support = dbField.getSupport();
                supports[i] = support;
                if(support==null) continue;
                numSupports++;
                support.initialize();
                if(support.getSupportState()!=SupportState.readyForStart) {
                    supportState = SupportState.readyForInitialize;
                    for(int j=0; j<i; j++) {
                        if(supports[j]!=null) supports[j].uninitialize();
                    }
                    return;
                }
                index = elementStructure.getFieldIndex("alarm");
                if(index<0) {
                    elementPVStructure.message("alarm field not found", MessageType.error);
                    return;
                }
                dbField = elementDBFields[index];
                support = dbField.getSupport();
                alarmSupports[i] = support;
                if(support!=null) {
                    support.initialize();
                    if(support.getSupportState()!=SupportState.readyForStart) {
                        supportState = SupportState.readyForInitialize;
                        for(int j=0; j<i; j++) {
                            if(supports[j]!=null) supports[j].uninitialize();
                        }
                        return;
                    }
                }
            }
            setSupportState(supportState);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#start()
         */
        public void start() {
            if(!super.checkSupportState(SupportState.readyForStart,supportName)) return;
            for(Support support: supports) {
                if(support!=null) support.start();
            }
            for(Support support: alarmSupports) {
                if(support!=null) support.start();
            }
            setSupportState(SupportState.ready);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#stop()
         */
        public void stop() {
            if(super.getSupportState()!=SupportState.ready) return;
            for(Support support: supports) {
                if(support!=null) support.stop();
            }
            for(Support support: alarmSupports) {
                if(support!=null) support.stop();
            }
            setSupportState(SupportState.readyForStart);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#uninitialize()
         */
        public void uninitialize() {
            if(super.getSupportState()==SupportState.ready) {
                stop();
            }
            if(super.getSupportState()!=SupportState.readyForStart) return;
            for(Support support: supports) {
                if(support!=null) support.stop();
            }
            for(Support support: alarmSupports) {
                if(support!=null) support.stop();
            }
            setSupportState(SupportState.readyForInitialize);
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#process(org.epics.ioc.process.RecordProcessRequester)
         */
        public void process(SupportProcessRequester supportProcessRequester) {
            if(supportProcessRequester==null) {
                throw new IllegalStateException("no processRequestListener");
            }
            if(!super.checkSupportState(SupportState.ready,supportName + ".process")) {
                supportProcessRequester.supportProcessDone(RequestResult.failure);
                return;
            }
            if(numSupports<=0) {
                supportProcessRequester.supportProcessDone(RequestResult.success);
                return;
            }
            this.supportProcessRequester = supportProcessRequester;
            numberWait = numSupports;
            finalResult = RequestResult.success;
            for(Support support: supports) {
                if(support!=null) support.process(this);
            }
        }                
        /* (non-Javadoc)
         * @see org.epics.ioc.process.SupportProcessRequester#supportProcessDone(org.epics.ioc.util.RequestResult)
         */
        public void supportProcessDone(RequestResult requestResult) {
            if(requestResult!=RequestResult.success) {
                if(finalResult!=RequestResult.zombie) {
                    finalResult = requestResult;
                }
            }
            numberWait--;
            if(numberWait>0) return;
            supportProcessRequester.supportProcessDone(finalResult);
        }
    }
}
