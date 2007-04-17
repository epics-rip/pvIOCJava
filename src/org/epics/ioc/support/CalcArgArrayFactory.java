/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support;

import org.epics.ioc.db.*;
import org.epics.ioc.process.*;
import org.epics.ioc.pv.*;
import org.epics.ioc.util.*;

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
        return new CalcArgArrayImpl((DBNonScalarArray)dbField);
    }
    
    private static String supportName = "calcArgArray";
    
    private static class CalcArgArrayImpl extends AbstractSupport
    implements CalcArgArraySupport,SupportProcessRequester
    {
        private PVField pvField;
        private String processRequesterName = null;
        private DBNonScalarArray calcArgArrayDBField;
        private DBField[] valueDBFields;
        private DBField[] nameDBFields;
        private LinkSupport[] linkSupports = null;
        private int numLinkSupports = 0;
              
        private SupportProcessRequester supportProcessRequester;
        private int numberWait;
        private RequestResult finalResult;
       
        private CalcArgArrayImpl(DBNonScalarArray dbNonScalarArray) {
            super(supportName,dbNonScalarArray);
            pvField = dbNonScalarArray.getPVField();
            processRequesterName = pvField.getFullName();
            calcArgArrayDBField = dbNonScalarArray; 
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.recordSupport.CalcArgArraySupport#getPVDouble(java.lang.String)
         */
        public PVDouble getPVDouble(String argName) {
            for(int i=0; i<nameDBFields.length; i++) {
                PVString pvString = (PVString)nameDBFields[i].getPVField();
                String name = pvString.get();
                if(name.equals(argName)) {
                    return (PVDouble)valueDBFields[i].getPVField();
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
            DBField[] dbFields = calcArgArrayDBField.getElementDBFields();
            int length = dbFields.length;
            valueDBFields = new DBField[length];
            nameDBFields = new DBField[length];
            linkSupports = new LinkSupport[length];
            numLinkSupports = 0;
            for(int i=0; i< length; i++) {
                DBStructure elementDBStructure = (DBStructure)dbFields[i];
                PVStructure elementPVStructure = elementDBStructure.getPVStructure();
                Structure elementStructure = (Structure)elementPVStructure.getField();
                DBField[] elementDBFields = elementDBStructure.getFieldDBFields();
                int index;
                index = elementStructure.getFieldIndex("value");
                valueDBFields[i] = elementDBFields[index];
                index = elementStructure.getFieldIndex("name");
                nameDBFields[i] = elementDBFields[index];
                index = elementStructure.getFieldIndex("input");
                DBLink dbLink = (DBLink)elementDBFields[index];
                LinkSupport linkSupport = (LinkSupport)dbLink.getSupport();
                linkSupports[i] = linkSupport;
                if(linkSupport==null) continue;
                numLinkSupports++;
                linkSupport.initialize();
                linkSupport.setField(valueDBFields[i]);
                if(linkSupport.getSupportState()!=SupportState.readyForStart) {
                    supportState = SupportState.readyForInitialize;
                    for(int j=0; j<i; j++) {
                        if(linkSupports[j]!=null) linkSupports[j].uninitialize();
                    }
                    break;
                }
            }
            setSupportState(supportState);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#start()
         */
        public void start() {
            if(!super.checkSupportState(SupportState.readyForStart,supportName)) return;
            for(LinkSupport linkSupport: linkSupports) {
                if(linkSupport!=null) linkSupport.start();
            }
            setSupportState(SupportState.ready);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#stop()
         */
        public void stop() {
            if(super.getSupportState()!=SupportState.ready) return;
            for(LinkSupport linkSupport: linkSupports) {
                if(linkSupport!=null) linkSupport.stop();
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
            for(LinkSupport linkSupport: linkSupports) {
                if(linkSupport!=null) linkSupport.stop();
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
            if(numLinkSupports<=0) {
                supportProcessRequester.supportProcessDone(RequestResult.success);
                return;
            }
            this.supportProcessRequester = supportProcessRequester;
            numberWait = numLinkSupports;
            finalResult = RequestResult.success;
            for(LinkSupport linkSupport: linkSupports) {
                if(linkSupport!=null) linkSupport.process(this);
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
