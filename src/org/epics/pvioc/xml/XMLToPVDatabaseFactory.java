/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS pvData is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.xml;

import java.util.ArrayList;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import java.util.TreeMap;

import org.epics.pvdata.factory.ConvertFactory;
import org.epics.pvdata.factory.FieldFactory;
import org.epics.pvdata.factory.PVDataFactory;
import org.epics.pvdata.pv.Convert;
import org.epics.pvdata.pv.Field;
import org.epics.pvdata.pv.FieldCreate;
import org.epics.pvdata.pv.MessageType;
import org.epics.pvdata.pv.PVAuxInfo;
import org.epics.pvdata.pv.PVDataCreate;
import org.epics.pvdata.pv.PVField;
import org.epics.pvdata.pv.PVInt;
import org.epics.pvdata.pv.PVScalar;
import org.epics.pvdata.pv.PVScalarArray;
import org.epics.pvdata.pv.PVStringArray;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.PVStructureArray;
import org.epics.pvdata.pv.Requester;
import org.epics.pvdata.pv.Scalar;
import org.epics.pvdata.pv.ScalarArray;
import org.epics.pvdata.pv.ScalarType;
import org.epics.pvdata.pv.StringArrayData;
import org.epics.pvdata.pv.Structure;
import org.epics.pvdata.pv.StructureArray;
import org.epics.pvdata.pv.Type;
import org.epics.pvdata.factory.BasePVAuxInfo;
import org.epics.pvioc.database.PVDatabase;
import org.epics.pvioc.database.PVRecord;
import org.epics.pvioc.database.PVRecordCreate;
import org.epics.pvioc.database.PVRecordCreateFactory;



/**
 * Factory to convert an xml file to an IOCDatabase and put it in the database.
 * The only public methods are two versions of convert.
 * @author mrk
 *
 */
public class XMLToPVDatabaseFactory {
	private static final PVRecordCreate pvRecordCreate = PVRecordCreateFactory.getPVRecordCreate();
    private static final PVDataCreate pvDataCreate = PVDataFactory.getPVDataCreate();
    public static final FieldCreate fieldCreate = FieldFactory.getFieldCreate();
    private static final AtomicBoolean isInUse = new AtomicBoolean(false);
    private static IncludeSubstituteXMLReader iocxmlReader = IncludeSubstituteXMLReaderFactory.getReader();
    //  for use by private classes
    private static final Convert convert = ConvertFactory.getConvert();
    private static final Pattern commaPattern = Pattern.compile("[,]");
    private static PVDatabase pvDatabase;
    private static IncludeSubstituteXMLListener isListener;
    private static XMLToPVDatabaseListener pvListener;
    
    /**
     * Convert an xml file to PVDatabase definitions and put the definitions in a database.
     * @param pvDatabase The database into which the new structure and records are added.
     * @param fileName The name of the file containing xml record instance definitions.
     * @param requester The requester.
     * @param reportSubstitutionFailure Should an error be reported if a ${from} does not have a substitution.
     * @param pvListener The XMLToPVDatabaseListener. This can be null.
     * @param isListener The IncludeSubstituteXMLListener listener. This can be null.
     * @param detailsListener The IncludeSubstituteDetailsXMLListener. This can be null;
     * 
     */
    public static void convert(PVDatabase pvDatabase, String fileName,Requester requester,
            boolean reportSubstitutionFailure,
            XMLToPVDatabaseListener pvListener,
            IncludeSubstituteXMLListener isListener,
            IncludeSubstituteDetailsXMLListener detailsListener)
    {
        boolean gotIt = isInUse.compareAndSet(false,true);
        if(!gotIt) {
            requester.message("XMLToIOCDBFactory.convert is already active",MessageType.fatalError);
        }
        try {
            XMLToPVDatabaseFactory.pvDatabase = pvDatabase;
            XMLToPVDatabaseFactory.isListener = isListener;
            XMLToPVDatabaseFactory.pvListener = pvListener;
            IncludeSubstituteXMLListener listener = new Listener();
            try {
                iocxmlReader.parse("database",fileName,requester,reportSubstitutionFailure,listener,detailsListener);
            } catch (RuntimeException e) {
                String message = "iocxmlReader.parse" + e.getMessage();
            	requester.message(message,MessageType.error);
            	
            	e.printStackTrace();
            }
        } finally {
            isInUse.set(false);
        }
        
    }
    /**
     * Convert an xml file to PVDatabase definitions and put the definitions in a database.
     * Calls previous method with reportSubstitutionFailure=true and (pvListener,isListener,detailsListener) all null.
     * @param pvDatabase The database into which the new structure and records are added.
     * @param fileName The name of the file containing xml record instance definitions.
     * @param requester The requester.
     */
    public static void convert(PVDatabase pvDatabase, String fileName,Requester requester)
    {
        XMLToPVDatabaseFactory.convert(pvDatabase,fileName,requester,true,null,null,null);
    }
    
    /**
     * @author mrk
     *
     */
    private static class Listener implements IncludeSubstituteXMLListener
    {
        private enum State {
            idle,
            record,
            structure,
            scalar,
            scalarArray,
            structureArray,
            structureArrayElement,
            auxInfo
        } 
        private State state = State.idle;
        
        private static class StructureState {
            State prevState = null;
            Structure structure = null;
            String structureFieldName = "";
            String fullFieldName = "";
            String fieldName = ""; // for current structure element
          //following for PVStructureArray
            PVStructureArray pvStructureArray = null;
            PVStructure[] pvStructures = new PVStructure[1];
        }
        
        
        private String recordName = null;
        private PVRecord pvRecord = null;
        private String structureName = null;
        private PVStructure pvStructure = null;
        private Stack<StructureState> structureStack = new Stack<StructureState>();
        private StructureState structureState = null;
        
        
        private TreeMap<String,PVField> pvInitialValue = new TreeMap<String,PVField>();
        private TreeMap<String,PVAuxInfo> supportName = new TreeMap<String,PVAuxInfo>();
        
        private PVScalar pvScalar = null;
        private String scalarString = null;
        private State scalarPrevState = null;
        
        private PVScalarArray pvArray = null;
        private String arrayString = null;
        private State arrayPrevState = null;
        private boolean immutable = false;
        private int capacity = 0;
        private int length = 0;
        private int offset = 0;
        private boolean capacityMutable = true;
        
        private ScalarType auxInfoType = null;
        private String auxInfoString = null;
        private State auxInfoPrevState = null;
 
        private String packageName = null;
        private ArrayList<String> importNameList = new ArrayList<String>();
        
        
        /* (non-Javadoc)
         * @see org.epics.pvdata.xml.IncludeSubstituteXMLListener#endDocument()
         */
        public void endDocument() {
            if(isListener!=null) isListener.endDocument();
        }       
        /* (non-Javadoc)
         * @see org.epics.pvdata.xml.IncludeSubstituteXMLListener#startElement(java.lang.String, java.util.Map)
         */
        public void startElement(String name,Map<String,String> attributes)
        {
            if(isListener!=null) isListener.startElement(name, attributes);
            switch(state) {
            case idle:
                if(name.equals("structure")) {
                    startStructureTop(name,attributes);
                } else if(name.equals("record")) {
                    startRecord(name,attributes);
                } else if(name.equals("package")) {
                    String value = attributes.get("name");
                    if(value==null || value.length() == 0) {
                        iocxmlReader.message("name not defined",MessageType.fatalError);
                        throw new IllegalStateException("illegal syntax");
                    } else if(value.indexOf('.')<=0) {
                        iocxmlReader.message("name must have at least one embeded .",MessageType.fatalError);
                        throw new IllegalStateException("illegal syntax");
                    } else {
                        packageName = value;
                    }
                } else if(name.equals("import")) {
                    String value = attributes.get("name");
                    if(value==null || value.length() == 0) {
                        iocxmlReader.message("name not defined",MessageType.fatalError);
                        throw new IllegalStateException("illegal syntax");
                    } else if(value.indexOf('.')<=0) {
                        iocxmlReader.message("name must have at least one embeded .",MessageType.fatalError);
                        throw new IllegalStateException("illegal syntax");
                    } else {
                        importNameList.add(value);
                    }
                } else if(name.equals("auxInfo")) {
                    iocxmlReader.message(
                            "auxInfo not valid in idle state",
                            MessageType.info);
                } else {
                    iocxmlReader.message(
                            "startElement " + name + " not understood",
                            MessageType.info);
                }
                return;
            case record:
            case structure:
            	if(name.equals("structure")) {
            		startStructure(name,attributes);
            	} else if(name.equals("scalar")) {
            		startScalar(name,attributes);
            	} else if(name.equals("array")) {
            		startScalarArray(name,attributes);
            	} else if(name.equals("structureArray")) {
            		startStructureArray(name,attributes);
                } else if(name.equals("auxInfo")) {
                    startAuxInfo(name,attributes);
                } else {
                    iocxmlReader.message(
                            "startElement " + name + " not understood current state is structure",
                            MessageType.fatalError);
                    throw new IllegalStateException("illegal syntax");
                }
                return;
            case scalar:
                if(name.equals("auxInfo")) {
                    startAuxInfo(name,attributes);
                } else {
                    iocxmlReader.message(
                            "startElement " + name + " not understood current state is scalar",
                            MessageType.fatalError);
                    throw new IllegalStateException("illegal syntax");
                }
                return;
            case scalarArray:
                if(name.equals("auxInfo")) {
                    startAuxInfo(name,attributes);
                } else {
                    iocxmlReader.message(
                            "startElement " + name + " not understood current state is scalarArray",
                            MessageType.fatalError);
                    throw new IllegalStateException("illegal syntax");
                }
                return;
            case structureArray :
            	if(name.equals("structure")) {
            		startStructureArrayElement();
                } else {
                    iocxmlReader.message(
                            "startElement " + name + " not understood current state is structureArray",
                            MessageType.fatalError);
                    throw new IllegalStateException("illegal syntax");
                }
                return;
            case structureArrayElement :
            	if(name.equals("structure")) {
            		startStructure(name,attributes);
            	} else if(name.equals("scalar")) {
            		startScalar(name,attributes);
            	} else if(name.equals("array")) {
            		startScalarArray(name,attributes);
                } else if(name.equals("auxInfo")) {
                    startAuxInfo(name,attributes);
                } else {
                    iocxmlReader.message(
                            "startElement " + name + " not understood current state is structureArrayElement",
                            MessageType.fatalError);
                    throw new IllegalStateException("illegal syntax");
                }
                return;
            case auxInfo:
                iocxmlReader.message(
                        "startElement " + name + " not understood current state is auxInfo",
                        MessageType.fatalError);
                throw new IllegalStateException("illegal syntax");
            default:
                iocxmlReader.message(
                        "startElement " + name + " Logic Error in parser",
                        MessageType.fatalError);
                throw new IllegalStateException("illegal syntax");
            }
        }
        /* (non-Javadoc)
         * @see org.epics.pvdata.xml.IncludeSubstituteXMLListener#endElement(java.lang.String)
         */
        public void endElement(String name)
        {
            if(isListener!=null) isListener.endElement(name);
            switch(state) {
            case idle:
                return;
            case record:
                if(name.equals("record")) {
                    endRecord(name);
                } else {
                    iocxmlReader.message(
                            "endElement " + name + " not understood",
                            MessageType.fatalError);
                    throw new IllegalStateException("illegal syntax");
                }
                return;
            case structure:
                if(name.equals("structure")) {
                    endStructure(name);
                } else {
                    iocxmlReader.message(
                            "endElement " + name + " not understood",
                            MessageType.fatalError);
                    throw new IllegalStateException("illegal syntax");
                }
                return;
            case scalar:
                if(name.equals("scalar")) {
                    endScalar(name);
                } else {
                    iocxmlReader.message(
                            "endElement " + name + " not understood",
                            MessageType.fatalError);
                    throw new IllegalStateException("illegal syntax");
                }
                return;
            case scalarArray:
                if(name.equals("array")) {
                    endScalarArray(name);
                } else {
                    iocxmlReader.message(
                            "endElement " + name + " not understood",
                            MessageType.fatalError);
                    throw new IllegalStateException("illegal syntax");
                }
                return;
            case structureArray:
            	if(name.equals("structureArray")) {
                    endStructureArray(name);
                } else {
                    iocxmlReader.message(
                            "endElement " + name + " not understood",
                            MessageType.fatalError);
                    throw new IllegalStateException("illegal syntax");
                }
                return;
            case structureArrayElement:
            	if(name.equals("structure")) {
                    endStructureArrayElement();
                } else {
                    iocxmlReader.message(
                            "endElement " + name + " not understood",
                            MessageType.fatalError);
                    throw new IllegalStateException("illegal syntax");
                }
                return;
            case auxInfo:
                if(name.equals("auxInfo")) {
                    endAuxInfo(name);
                } else {
                    iocxmlReader.message(
                        "endElement " + name + " not understood",
                        MessageType.fatalError);
                    throw new IllegalStateException("illegal syntax");
                }
                return;
            default:
                iocxmlReader.message(
                        "endElement " + name + " Logic Error in parser",
                        MessageType.fatalError);
                throw new IllegalStateException("illegal syntax");
            }
        }
        /* (non-Javadoc)
         * @see org.epics.pvdata.xml.IncludeSubstituteXMLListener#element(java.lang.String)
         */
        public void element(String content) {
            if(isListener!=null) isListener.element(content);
            switch(state) {
            case idle:
                return;
            case structure:
                return;
            case scalar:
                scalarString = content;
                return;
            case scalarArray:
                arrayString = content;
                return;
            case auxInfo:
                auxInfoString = content;
                return;
            default:
                return;
            }
        }
        
        private String findExtendedStructureName(String name) {
            if(name.indexOf('.')>=0) return name;
            String[] databaseNames = pvDatabase.getStructureNames();
            String[] masterNames = pvDatabase.getMaster().getStructureNames();
            for(String importName: importNameList) {
                boolean wildCard = false;
                if(importName.endsWith("*")) {
                    wildCard = true;
                    importName = importName.substring(0, importName.lastIndexOf("*"));
                }
                String[] names = databaseNames;
                while(names!=null) {
                    for(String structName: names) {
                        if(structName.indexOf('.')<0) continue;
                        if(!wildCard) {
                            String trialName = structName.substring(structName.lastIndexOf('.'));
                            if(trialName.equals(name)) {
                                return structName;
                            }
                        } else {
                            if(structName.equals(importName + name)) {
                                return structName;
                            }
                        }
                    }
                    if(names==databaseNames) {
                        names = masterNames;
                    } else {
                        names = null;
                    }
                }
            }
            return name;
        }
        
        private void getValues(String fullFieldName,PVStructure pvStructure)
        {
            PVField[] pvFields = pvStructure.getPVFields();
            for(int i=0; i<pvFields.length; ++i) {
                PVField pvField = pvFields[i];
                String name;
                if(fullFieldName.length()<1) {
                   name = pvField.getFieldName();
                } else {
                    name = fullFieldName + "." + pvField.getFieldName();
                }
                if(pvField.getField().getType()==Type.structure) {
                    getValues(name,(PVStructure)pvField);
                } else {
                    pvInitialValue.put(name,pvField);
                }
            }
        }
        
        private void putValues(PVStructure pvStructure) 
        {
            for(String fullFieldName:pvInitialValue.keySet()) {
                PVField pvFrom = pvInitialValue.get(fullFieldName);
                PVField pvTo = pvStructure.getSubField(fullFieldName);
                convert.copy(pvFrom,pvTo);
            }
            pvInitialValue.clear();
        }
        
        private void getSupport(String fullFieldName,PVStructure pvStructure) {
            PVAuxInfo pvAuxInfo = pvStructure.getPVAuxInfo();
            if(pvAuxInfo!=null) supportName.put(fullFieldName,pvAuxInfo);
            PVField[] pvFields = pvStructure.getPVFields();
            for(int i=0; i<pvFields.length; ++i) {
                PVField pvField = pvFields[i];
                String name;
                if(fullFieldName.length()<1) {
                   name = pvField.getFieldName();
                } else {
                    name = fullFieldName + "." + pvField.getFieldName();
                }
                if(pvField.getField().getType()==Type.structure) {
                    getSupport(name,(PVStructure)pvField);
                } else {
                    pvAuxInfo = pvField.getPVAuxInfo();
                    if(pvAuxInfo!=null) supportName.put(name,pvAuxInfo);
                }
            }
        }
        
        private void putSupport(PVStructure pvStructure) 
        {
            for(String fullFieldName:supportName.keySet()) {
                PVAuxInfo auxInfo = supportName.get(fullFieldName);
                if(auxInfo==null) continue;
                PVAuxInfo pvAuxInfo = null;
                if(fullFieldName.length()<1) {
                    pvAuxInfo = pvStructure.getPVAuxInfo();
                } else {
                    PVField pvField = pvStructure.getSubField(fullFieldName);
                    pvAuxInfo = pvField.getPVAuxInfo();
                }
                Map<String,PVScalar> infos = auxInfo.getInfos();
                for(String key:infos.keySet()) {
                    PVScalar pvFrom = infos.get(key);
                    PVScalar pvTo = pvAuxInfo.createInfo(key, pvFrom.getScalar().getScalarType());
                    convert.copyScalar(pvFrom, pvTo);
                }
            }
            supportName.clear();
        }
        
        private void startRecord(String name,Map<String,String> attributes)
        {
            if(state!=State.idle) {
                iocxmlReader.message(
                        "startElement " + name + " not allowed except as top level structure",
                        MessageType.fatalError);
                throw new IllegalStateException("illegal syntax");
            }
            if(structureStack.size()!=0) {
                iocxmlReader.message(
                        "startElement " + name + " Logic error ?",
                        MessageType.fatalError);
                throw new IllegalStateException("illegal syntax");
            }
            recordName = attributes.get("recordName");
            if(recordName==null || recordName.length() == 0) {
                iocxmlReader.message("recordName not defined",MessageType.fatalError);
                throw new IllegalStateException("illegal syntax");
            }
            structureState = new StructureState();
            structureState.prevState = state;
            String extendsName = attributes.get("extends");
            pvRecord = pvDatabase.findRecord(recordName);
            if(pvRecord!=null) {
                if(extendsName!=null && extendsName.length()>0) {
                    iocxmlReader.message(
                        "extends " + extendsName + " is ignored because record already exists",
                        MessageType.info);
                }
                pvStructure = pvRecord.getPVRecordStructure().getPVStructure();
            } else {
                if(extendsName!=null && extendsName.length()<=0) extendsName = null;
                if(extendsName==null) {
                    String[] fieldNames = new String[0];
                    Field[] fields = new Field[0];
                    structureState.structure = fieldCreate.createStructure(fieldNames, fields);
                } else {
            		extendsName = findExtendedStructureName(extendsName);
            		PVStructure pvStructure = pvDatabase.findStructure(extendsName);
            		if(pvStructure==null) {
            			iocxmlReader.message(
            					"type " + extendsName + " is not a known structure",
            					MessageType.fatalError);
            			throw new IllegalStateException("illegal syntax");
            		} else {
            		    getValues("",pvStructure);
            		    getSupport("",pvStructure);
            		    structureState.structure = pvStructure.getStructure();
            		}
            	}
            }
            state = State.record;
            if(pvListener!=null && pvRecord!=null) pvListener.startRecord(pvRecord);
        }
        
        private void endRecord(String name)
        {
            if(pvRecord==null) {
                if(structureState.structure.getFields().length<1) {
                    iocxmlReader.message(
                            "record " + recordName + " is empty and not added to database",
                            MessageType.warning);
                } else {
                    PVStructure pvStructure = pvDataCreate.createPVStructure(structureState.structure);
                    putValues(pvStructure);
                    putSupport(pvStructure);
                    pvRecord = pvRecordCreate.createPVRecord(recordName,pvStructure);
                    if(pvListener!=null) pvListener.startRecord(pvRecord);
                    if(!pvDatabase.addRecord(pvRecord)) {
                        iocxmlReader.message(
                                "record " + pvRecord.getRecordName() + " not added to database",
                                MessageType.warning);
                    }
                }
            }
            if(pvRecord!=null && pvListener!=null) pvListener.endRecord();
            state = structureState.prevState;
            structureState = null;
        }
        
        private void startStructureTop(String name,Map<String,String> attributes)
        {      
            structureName = attributes.get("structureName");
            if(structureName==null || structureName.length() == 0) {
                iocxmlReader.message("structureName not defined",MessageType.fatalError);
                throw new IllegalStateException("illegal syntax");
            }
            if(packageName!=null) structureName = packageName + "." + structureName;
            structureState = new StructureState();
            structureState.prevState = state;
            pvStructure = pvDatabase.findStructure(structureName);
            if(pvStructure!=null) {
                iocxmlReader.message("structureName already exists and can not be changed",MessageType.fatalError);
                throw new IllegalStateException("illegal syntax");
            }
            String extendsName = attributes.get("extends");
            if(extendsName!=null && extendsName.length()<=0) extendsName = null;
            if(extendsName==null) {
                String[] fieldNames = new String[0];
                Field[] fields = new Field[0];
                structureState.structure = fieldCreate.createStructure(fieldNames, fields);
            } else {
                extendsName = findExtendedStructureName(extendsName);
                PVStructure pvStructure = pvDatabase.findStructure(extendsName);
                if(pvStructure==null) {
                    iocxmlReader.message(extendsName + " not a known structure",
                            MessageType.fatalError);
                    throw new IllegalStateException("illegal syntax");
                } else {
                    getValues("",pvStructure);
                    getSupport("",pvStructure);
                    structureState.structure = pvStructure.getStructure();   
                }
            }
            if(pvListener!=null && pvStructure!=null) pvListener.startStructure(pvStructure);
            state = State.structure;
        }
        
        private void startStructure(String name,Map<String,String> attributes)
        {      
            String fieldName = attributes.get("name");
            if(fieldName==null || fieldName.length() == 0) {
                iocxmlReader.message("name not defined",MessageType.error);
                return;
            }
            structureState.fieldName = fieldName;
            Structure thisStructure = null;
            if(structureState.structure.getField(fieldName)!=null) {
                thisStructure = (Structure) structureState.structure.getField(fieldName);
            }
            String fullFieldName = structureState.fullFieldName;
            if(fullFieldName.length()<1) {
                fullFieldName = fieldName;
            } else if(fieldName.length()<1) {
                // attached to structureS
            } else{
                fullFieldName = fullFieldName + "." + fieldName;
            }
            structureStack.push(structureState);
            structureState = new StructureState();
            structureState.structureFieldName = fieldName;
            structureState.fullFieldName = fullFieldName;
            structureState.prevState = state;
            
            String extendsName = attributes.get("extends");
            if(extendsName!=null && extendsName.length()<=0) extendsName = null;
            if(thisStructure!=null) {
                if(extendsName!=null) {
                    iocxmlReader.message(
                            "extends " + extendsName + " is ignored because parent already defined this field",
                            MessageType.info);
                }
                structureState.structure = thisStructure;
            } else if(extendsName==null) {
                String[] fieldNames = new String[0];
                Field[] fields = new Field[0];
                structureState.structure = fieldCreate.createStructure(fieldNames, fields);
            } else {
                extendsName = findExtendedStructureName(extendsName);
                PVStructure pvStructure = pvDatabase.findStructure(extendsName);
                if(pvStructure==null) {
                    iocxmlReader.message(
                            "type " + extendsName + " not a known structure",
                            MessageType.fatalError);
                    throw new IllegalStateException("illegal syntax");
                } else {
                    getValues(fullFieldName,pvStructure);
                    getSupport(fullFieldName,pvStructure);
                    structureState.structure = pvStructure.getStructure();
                }
            }
            
            if(pvListener!=null) pvListener.newStructureField(pvStructure);
            state = State.structure;
        }
        
        private void endStructure(String name)
        {
            if(structureStack.size()==0) {
                if(pvStructure==null) {
                    pvStructure = pvDataCreate.createPVStructure(structureState.structure);
                }
                putValues(pvStructure);
                putSupport(pvStructure);
                pvDatabase.addStructure(pvStructure,structureName);
                state = structureState.prevState;
                structureState = null;
                if(pvListener!=null) pvListener.endStructureField();
                return;
            }
            if(pvListener!=null) pvListener.endStructure();
            Structure structure = structureState.structure;
            String fieldName = structureState.structureFieldName;
            state = structureState.prevState;
            structureState = structureStack.pop();
            if(pvStructure==null && structureState.structure.getField(fieldName)==null) {
                structureState.structure = fieldCreate.appendField(structureState.structure, fieldName, structure);
            }
        }
        
        
        private void startScalar(String name,Map<String,String> attributes)
        {
        	String fieldName = attributes.get("name");
        	if(fieldName==null || fieldName.length() == 0) {
        		iocxmlReader.message("name not defined",MessageType.fatalError);
        		throw new IllegalStateException("illegal syntax");
        	}
        	structureState.fieldName = fieldName;
        	PVField pvField = null;
        	if(pvStructure!=null) {
        	    String fullFieldName = structureState.fullFieldName;
        	    String xxx;
        	    if(fullFieldName.length()<1) {
        	        xxx = fieldName;
        	    } else {
        	        xxx = fullFieldName + "." + fieldName;
        	    }
        	    pvField = pvStructure.getSubField(xxx);
        	}
        	
        	ScalarType scalarType = null;
        	if(pvField!=null) {
        	    Field field = pvField.getField();
        	    if(field.getType()!=Type.scalar) {
        	        iocxmlReader.message("field is not a scalar",MessageType.fatalError);
        	        throw new IllegalStateException("illegal syntax");
        	    }
        	    scalarType = ((Scalar)field).getScalarType();
        	} else {
        	    Field field = structureState.structure.getField(fieldName);
        	    if(field!=null) {
        	        if(field.getType()!=Type.scalar) {
        	            iocxmlReader.message("field is not a scalar",MessageType.fatalError);
        	            throw new IllegalStateException("illegal syntax");
        	        }
        	        scalarType = ((Scalar)field).getScalarType();
        	    } else {
        	        String temp = attributes.get("scalarType");
        	        if(temp==null) {
        	            iocxmlReader.message("scalarType not defined",MessageType.fatalError);
        	            throw new IllegalStateException("illegal syntax");
        	        }
        	        scalarType = ScalarType.getScalarType(temp);
        	    }
        	}
        	String immutableString = attributes.get("immutable");
        	if(immutableString!=null && immutableString.equals("true")) {
        	    immutable = true;
        	} else {
        	    immutable = false;
        	}
        	scalarString = null;
        	PVScalar pvScalar = null;
        	if(pvField!=null) {
        	    pvScalar = (PVScalar)pvField;
        	} else {
        	    pvScalar= pvDataCreate.createPVScalar(scalarType);
        	    structureState.fieldName = fieldName;
        	}
        	this.pvScalar = pvScalar;
        	if(pvListener!=null) pvListener.startScalar(pvScalar);
        	scalarPrevState = state;
        	state = State.scalar;
        }
        private void endScalar(String name)
        {
            if(scalarString!=null && scalarString.length()>0) {
                convert.fromString(pvScalar, scalarString);
            }
            if(immutable) pvScalar.setImmutable();
            
            if(pvStructure==null) {
                if(structureState.structure.getField(structureState.fieldName)==null) {
                    structureState.structure = fieldCreate.appendField(structureState.structure, structureState.fieldName, pvScalar.getField());
                }
                String fullFieldName = structureState.fullFieldName;
                if(fullFieldName.length()<1) {
                    fullFieldName = structureState.fieldName;
                } else {
                    fullFieldName = fullFieldName + "." + structureState.fieldName;
                }
                pvInitialValue.put(fullFieldName, pvScalar);
            }
            scalarString = null;
            pvScalar = null;
            state = scalarPrevState;
            if(pvListener!=null) pvListener.endScalar();
            scalarPrevState = null;
        }
        
        private void startScalarArray(String name,Map<String,String> attributes)
        {
        	String fieldName = attributes.get("name");
        	if(fieldName==null || fieldName.length() == 0) {
                iocxmlReader.message("name not defined",MessageType.error);
                return;
            }
        	structureState.fieldName = fieldName;
        	PVField pvField = null;
            if(pvStructure!=null) {
                String fullFieldName = structureState.fullFieldName;
                String xxx;
                if(fullFieldName.length()<1) {
                    xxx = fieldName;
                } else {
                    xxx = fullFieldName + "." + fieldName;
                }
                pvField = pvStructure.getSubField(xxx);
            }
            
            ScalarType scalarType = null;
            if(pvField!=null) {
                Field field = pvField.getField();
                if(field.getType()!=Type.scalarArray) {
                    iocxmlReader.message("field is not a scalarArray",MessageType.fatalError);
                    throw new IllegalStateException("illegal syntax");
                }
                scalarType = ((ScalarArray)field).getElementType();
            } else {
                Field field = structureState.structure.getField(fieldName);
                if(field!=null) {
                    if(field.getType()!=Type.scalarArray) {
                        iocxmlReader.message("field is not a scalarArray",MessageType.fatalError);
                        throw new IllegalStateException("illegal syntax");
                    }
                    scalarType = ((ScalarArray)field).getElementType();
                } else {
                    String temp = attributes.get("scalarType");
                    if(temp==null) {
                        iocxmlReader.message("scalarType not defined",MessageType.fatalError);
                        throw new IllegalStateException("illegal syntax");
                    }
                    scalarType = ScalarType.getScalarType(temp);
                }
            }

            String immutableString = attributes.get("immutable");
            if(immutableString!=null && immutableString.equals("true")) {
                immutable = true;
            } else {
                immutable = false;
            }
            arrayString = null;
            PVScalarArray pvArray = null;
            if(pvField!=null) {
                pvArray = (PVScalarArray)pvField;
            } else {
                pvArray= pvDataCreate.createPVScalarArray(scalarType);
            }
            this.pvArray = pvArray;
            arrayPrevState = state;
            capacity = 0;
            length = 0;
            offset = 0;
            capacityMutable = true;
            String value = attributes.get("capacity");
            if(value!=null && value.length()>0) {
                try {
                    capacity = Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    iocxmlReader.message(
                            e.toString(),
                            MessageType.error);
                }
            }
            value = attributes.get("length");
            if(value!=null && value.length()>0) {
                try {
                    length = Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    iocxmlReader.message(
                            e.toString(),
                            MessageType.error);
                }
            }
            value = attributes.get("offset");
            if(value!=null && value.length()>0) {
                try {
                    offset = Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    iocxmlReader.message(
                            e.toString(),
                            MessageType.error);
                }
            }
            value = attributes.get("capacityMutable");
            if(value!=null && value.length()>0) {
                capacityMutable = Boolean.parseBoolean(value);
            }
            state = State.scalarArray;
            if(pvListener!=null) pvListener.startArray(pvArray);
        }
        
        private void endScalarArray(String name)
        {
        	if(capacity>0) pvArray.setCapacity(capacity);
        	if(length>0 && length<=capacity) pvArray.setLength(length);
            String value = arrayString;
            arrayString = null;
            if(value!=null && value.length()>0) {
                if((value.charAt(0)=='[') && value.endsWith("]")) {
                    int offset = value.lastIndexOf(']');
                    value = value.substring(1, offset);
                }
            }
            if(value!=null && value.length()>0) {
                String[] values = null;
                values = commaPattern.split(value);
                ScalarType type = pvArray.getScalarArray().getElementType();
                if(type==ScalarType.pvString) {
                    for(int i=0; i<values.length; i++) {
                        String item = values[i];
                        int len = item.length();
                        if(len>1) {
                            if(item.charAt(0)=='\"' && item.endsWith("\"")) {
                                values[i] = item.substring(1, len-1);
                            }
                        }
                    }
                }
                try {
                    convert.fromStringArray(pvArray,offset,values.length,values,0);
                } catch (NumberFormatException e) {
                    iocxmlReader.message(
                        e.toString(),
                        MessageType.error);
                }
            }
            if(immutable) pvArray.setImmutable();
            if(!capacityMutable) pvArray.setCapacityMutable(false);
            if(pvListener!=null) pvListener.endArray();
            if(pvStructure==null) {
                if(structureState.structure.getField(structureState.fieldName)==null) {
                    structureState.structure = fieldCreate.appendField(structureState.structure, structureState.fieldName, pvArray.getField());
                }
                String fullFieldName = structureState.fullFieldName;
                if(fullFieldName.length()<1) {
                    fullFieldName = structureState.fieldName;
                } else {
                    fullFieldName = fullFieldName + "." + structureState.fieldName;
                }
                pvInitialValue.put(fullFieldName, pvArray);
            }
            pvArray = null;
            state = arrayPrevState;
            arrayPrevState = null;
        }
        
        private void startStructureArray(String name,Map<String,String> attributes)
        {
        	String fieldName = attributes.get("name");
        	if(fieldName==null || fieldName.length() == 0) {
                iocxmlReader.message("name not defined",MessageType.fatalError);
                throw new IllegalStateException("illegal syntax");
            }
        	PVField pvField = null;
            if(pvStructure!=null) {
                String fullFieldName = structureState.fullFieldName;
                String xxx;
                if(fullFieldName.length()<1) {
                    xxx = fieldName;
                } else {
                    xxx = fullFieldName + "." + fieldName;
                }
                pvField = pvStructure.getSubField(xxx);
            }
            if(pvField!=null) {
                Field field = pvField.getField();
                if(field.getType()!=Type.structureArray) {
                    iocxmlReader.message("field is not a scalarArray",MessageType.fatalError);
                    throw new IllegalStateException("illegal syntax");
                }
            }
            String immutableString = attributes.get("immutable");
            if(immutableString!=null && immutableString.equals("true")) {
                immutable = true;
            } else {
                immutable = false;
            }
        	
            PVStructureArray pvStructureArray = null;
        	if(pvField!=null){
        		pvStructureArray = (PVStructureArray)pvField;
        	} else {
        		String extendsName = attributes.get("extends");
        		if(extendsName==null) {
        			iocxmlReader.message("must define extends",MessageType.fatalError);
        			throw new IllegalStateException("illegal syntax");
        		}
        		extendsName = findExtendedStructureName(extendsName);
        		PVStructure pvStructure = pvDatabase.findStructure(extendsName);
        		if(pvStructure==null) {
        			iocxmlReader.message(
        					"type " + extendsName + " not a known structure",
        					MessageType.fatalError);
        			throw new IllegalStateException("illegal syntax");
        		}
        		StructureArray structureArray = fieldCreate.createStructureArray(pvStructure.getStructure());
        		pvStructureArray = pvDataCreate.createPVStructureArray(structureArray);
        		String capacity = attributes.get("capacity");
        		if(capacity!=null) {
        			int length = Integer.parseInt(capacity);
        			if(length>0) pvStructureArray.setCapacity(length);
        		}
        	}
        	if(pvListener!=null) pvListener.newStructureField(pvStructure);
        	structureStack.push(structureState);
        	structureState = new StructureState();
        	structureState.prevState = state;
        	structureState.pvStructureArray = pvStructureArray;
            state = State.structureArray;
        }
        
        private void endStructureArray(String name)
        {
            if(pvListener!=null) pvListener.endStructure();
            state = structureState.prevState;
            structureState = structureStack.pop();
        }
        
        private void startStructureArrayElement() {
        	PVStructure pvStructure = pvDataCreate.createPVStructure(structureState.pvStructureArray.getStructureArray().getStructure());
        	structureState.pvStructures[0] = pvStructure;
        	state = State.structureArrayElement;
        }
        
        private void endStructureArrayElement() {
        	PVStructureArray pvStructureArray = structureState.pvStructureArray;
        	pvStructureArray.put(pvStructureArray.getLength(), 1, structureState.pvStructures, 0);
        	state = State.structureArray;
        }
       
        
        private void startAuxInfo(String name,Map<String,String> attributes)
        {
        	auxInfoString = null;
            String fieldName = attributes.get("name");
            if(fieldName==null || fieldName.length() == 0) {
                iocxmlReader.message("name not defined",MessageType.fatalError);
                throw new IllegalStateException("illegal syntax");
            }
            String typeName = attributes.get("scalarType");
            if(typeName==null || typeName.length() == 0) {
                iocxmlReader.message("type not defined",MessageType.fatalError);
                throw new IllegalStateException("illegal syntax");
            }
            auxInfoType = ScalarType.getScalarType(typeName);
            if(auxInfoType==null) {
                iocxmlReader.message("type not a valid ScalarType",MessageType.fatalError);
                throw new IllegalStateException("illegal syntax");
            }
            auxInfoPrevState = state;
            state = State.auxInfo;
        }
        
        private void endAuxInfo(String name) {
            state = auxInfoPrevState;
            auxInfoPrevState = null;
            String fullName = structureState.fullFieldName;
            String fieldName = structureState.fieldName;
            if (fullName.length() < 1) {
                fullName = fieldName;
            } else if(fieldName.length()<1) {
                  // attached to structureS
            } else {
                fullName = fullName + "." + fieldName;
            }
            PVScalar pvScalar = pvDataCreate.createPVScalar(auxInfoType);
            PVAuxInfo pvAuxInfo = new BasePVAuxInfo(pvScalar);
            PVScalar xxx = pvAuxInfo.createInfo("supportFactory", auxInfoType);
            convert.fromString(xxx, auxInfoString);
            supportName.put(fullName, pvAuxInfo);
            auxInfoString = null;
        }
    }  
}
