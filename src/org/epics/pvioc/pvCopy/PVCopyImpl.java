/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS pvData is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.pvCopy;

import java.util.ArrayList;

import org.epics.pvdata.factory.ConvertFactory;
import org.epics.pvdata.factory.FieldFactory;
import org.epics.pvdata.factory.PVDataFactory;
import org.epics.pvdata.misc.BitSet;
import org.epics.pvdata.pv.Convert;
import org.epics.pvdata.pv.Field;
import org.epics.pvdata.pv.FieldCreate;
import org.epics.pvdata.pv.PVDataCreate;
import org.epics.pvdata.pv.PVField;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.Structure;
import org.epics.pvdata.pv.Type;
import org.epics.pvioc.database.PVListener;
import org.epics.pvioc.database.PVRecord;
import org.epics.pvioc.database.PVRecordField;
import org.epics.pvioc.database.PVRecordStructure;

/**
 * @author mrk
 *
 */
class PVCopyImpl {
	static PVCopy create(PVRecord pvRecord,PVStructure pvRequest,String structureName) {
		if(structureName!=null && structureName.length()>0) {
			if(pvRequest.getPVFields().length>0) {
				pvRequest = pvRequest.getStructureField(structureName);
				if(pvRequest==null) return null;
			}
		}
		ThePVCopyImpl impl = new ThePVCopyImpl(pvRecord);
		PVStructure pvStruct = pvRequest;
		if(pvRequest.getSubField("field")!=null) {
			pvStruct = pvRequest.getStructureField("field");
		}
		boolean result = impl.init(pvStruct);
		if(result) return impl;
		return null;
	}
	
	private static final FieldCreate fieldCreate = FieldFactory.getFieldCreate();
    private static final PVDataCreate pvDataCreate = PVDataFactory.getPVDataCreate();
    private static final Convert convert = ConvertFactory.getConvert();
    
    static class Node {
        boolean isStructure = false;
        int structureOffset = 0; // In the copy
        int nfields = 0;
        PVStructure options = null;
    }
    
    static class RecordNode extends Node{
        PVRecordField recordPVField;
    }
    
    static class StructureNode extends Node {
        Node[] nodes;
    }
    
    
    private static final class ThePVCopyImpl implements PVCopy{
        
		ThePVCopyImpl(PVRecord pvRecord) {
            this.pvRecord = pvRecord;
        }
        /* (non-Javadoc)
         * @see org.epics.pvdata.pvCopy.PVCopy#getpvStructure()
         */
        @Override
        public PVRecord getPVRecord() {
            return pvRecord;
        }
        /* (non-Javadoc)
         * @see org.epics.pvdata.pvCopy.PVCopy#getStructure()
         */
        @Override
        public Structure getStructure() {
            return structure;
        }
        /* (non-Javadoc)
         * @see org.epics.pvdata.pvCopy.PVCopy#createPVStructure()
         */
        @Override
        public PVStructure createPVStructure() {
            if(cacheInitStructure!=null) {
                PVStructure save = cacheInitStructure;
                cacheInitStructure = null;
                return save;
            }
            PVStructure pvStructure =  pvDataCreate.createPVStructure(structure);
            return pvStructure;
        }
        @Override
        public PVStructure getOptions(PVStructure copyPVStructure,int fieldOffset)
        {
            Node node = headNode;
            while(true) {
                if(!node.isStructure) {
                    if(node.structureOffset==fieldOffset) return node.options;
                    return null;
                }
                StructureNode structNode = (StructureNode)node;
                Node[] nodes = structNode.nodes;
                boolean okToContinue = false;
                for(int i=0; i< nodes.length; i++) {
                    node = nodes[i];
                    int soff = node.structureOffset;
                    if(fieldOffset>=soff && fieldOffset<soff+node.nfields) {
                        if(fieldOffset==soff) return node.options;
                        if(!node.isStructure) {
                            return null;
                        }
                        okToContinue = true;
                        break;
                    }
                }
                if(okToContinue) continue;
                throw new IllegalArgumentException("fieldOffset not valid");
            }
        }
        @Override
        public String dump() {
              StringBuilder builder = new StringBuilder();
              dump(builder,headNode,0);
              return builder.toString();
              
        }
        static private void dump(StringBuilder builder,Node node,int indentLevel) {
            convert.newLine(builder, indentLevel);
            String kind;
            if(node.isStructure) {
                kind = "structureNode";
            } else {
                kind = "recordNode";
            }
            builder.append(kind);
            builder.append((" isStructure " + (node.isStructure ? "true" : "false")));
            builder.append(" structureOffset " + node.structureOffset);
            builder.append(" nfields " + node.nfields);
            PVStructure options = node.options;
            if(options!=null) {
                convert.newLine(builder, indentLevel+1);
                options.toString(builder, indentLevel+1);
                convert.newLine(builder, indentLevel);
            }
            if(!node.isStructure) {
                RecordNode recordNode = (RecordNode)node;
                String name = recordNode.recordPVField.getFullName();
                builder.append(" recordField " + name);
                return;
            }
            StructureNode structureNode = (StructureNode)node;
            Node[] nodes =structureNode.nodes;
            for(int i=0 ; i<nodes.length; i++){
                if(nodes[i]==null) {
                    convert.newLine(builder, indentLevel+1);
                    builder.append("node[" + i + "] is null");
                    continue;
                }
                dump(builder,nodes[i],indentLevel+1);
            }
        }
        @Override
        public int getCopyOffset(PVRecordField recordPVField) {
            if(!headNode.isStructure) {
                RecordNode recordNode = (RecordNode)headNode;
                if(recordNode.recordPVField.equals(recordPVField)) return headNode.structureOffset;
                PVStructure parent = recordPVField.getPVField().getParent();
                int offsetParent = parent.getFieldOffset();
                int off = recordPVField.getPVField().getFieldOffset();
                int offdiff = off -offsetParent;
                if(offdiff<recordNode.nfields) return headNode.structureOffset + offdiff;
                return -1;
            }
            RecordNode recordNode = getCopyOffset((StructureNode)headNode,recordPVField);
            if(recordNode!=null) {
                int offset = recordPVField.getPVField().getFieldOffset() - recordNode.recordPVField.getPVField().getFieldOffset();
                return recordNode.structureOffset + offset;
            }
            return -1;
        }
        
        public int getCopyOffset(PVRecordStructure recordPVStructure,PVRecordField recordPVField) {
            RecordNode recordNode = null;
            if(!headNode.isStructure) {
                recordNode = (RecordNode)headNode;
                if(recordNode.recordPVField!=recordPVStructure) {
                    return headNode.structureOffset + 1;
                }
            } else {
                recordNode = getCopyOffset((StructureNode)headNode,recordPVStructure);
            }
            if(recordNode==null) return -1;
            int diff = recordPVField.getPVField().getFieldOffset() - recordPVStructure.getPVStructure().getFieldOffset();
            return recordNode.structureOffset + diff;
        }
        
        private RecordNode getRecordNode(StructureNode structureNode,int structureOffset) {
            for(Node node : structureNode.nodes) {
                if(structureOffset>=(node.structureOffset + node.nfields)) continue;
                if(!node.isStructure) return (RecordNode)node; 
                StructureNode subNode = (StructureNode)node;
                return  getRecordNode(subNode,structureOffset);
            }
            return null;
        }
        /* (non-Javadoc)
         * @see org.epics.pvdata.pvCopy.PVCopy#getPVField(int)
         */
        @Override
        public PVRecordField getRecordPVField(int structureOffset) {
            RecordNode recordNode = null;
            if(!headNode.isStructure) {
                recordNode = (RecordNode)headNode;
            } else {
                recordNode = getRecordNode((StructureNode)headNode,structureOffset);
            }
            if(recordNode==null) {
                System.err.printf("PVCopy::PVRecordField getRecordPVField(int structureOffset) illegal structureOffset %d %s%n",structureOffset,dump());
            	throw new IllegalArgumentException("structureOffset not valid");
            }
            int diff = structureOffset - recordNode.structureOffset;
            PVRecordField pvRecordField = recordNode.recordPVField;
            if(diff==0) return pvRecordField;
            PVStructure pvStructure = (PVStructure)pvRecordField.getPVField();
            PVField pvField = pvStructure.getSubField(pvRecordField.getPVField().getFieldOffset() + diff);
            PVRecordField xxx = pvRecord.findPVRecordField(pvField);
            if(xxx==null) {
                System.err.printf("PVCopy::PVRecordField getRecordPVField(int structureOffset) illegal structureOffset %d %s%n",structureOffset,dump());
                throw new IllegalArgumentException("structureOffset not valid");
            }
            return xxx;
        }
        /* (non-Javadoc)
         * @see org.epics.pvdata.pvCopy.PVCopy#initCopy(org.epics.pvdata.pv.PVStructure, org.epics.pvdata.misc.BitSet)
         */
        @Override
        public void initCopy(PVStructure copyPVStructure, BitSet bitSet) {
            bitSet.clear();
            bitSet.set(0);
            pvRecord.lock();
            try {
                updateCopyFromBitSet(copyPVStructure,bitSet);
            } finally {
                pvRecord.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.pvCopy.PVCopy#updateCopySetBitSet(org.epics.pvdata.pv.PVStructure, org.epics.pvdata.misc.BitSet)
         */
        @Override
        public void updateCopySetBitSet(PVStructure copyPVStructure,BitSet bitSet)
        {
            if(headNode.isStructure) {
                updateStructureNodeSetBitSet(copyPVStructure,(StructureNode)headNode,bitSet);
            } else {
                RecordNode recordNode = (RecordNode)headNode;
                PVRecordField pvRecordField= recordNode.recordPVField;
                PVField copyPVField = copyPVStructure;
                PVField[] pvCopyFields = copyPVStructure.getPVFields();
                if(pvCopyFields.length==1) {
                    copyPVField = pvCopyFields[0];
                }
                PVField  pvField = pvRecordField.getPVField();
                if(pvField.getField().getType()==Type.structure) {
                    updateSubFieldSetBitSet(copyPVField,pvRecordField,bitSet);
                    return;
                }
                if(pvCopyFields.length!=1) {
                    throw new IllegalStateException("Logic error");
                }
                boolean isEqual = copyPVField.equals(pvField);
                if(!isEqual) {
                    convert.copy(pvField, copyPVField);
                    bitSet.set(copyPVField.getFieldOffset());
                }
            }
        }
        /* (non-Javadoc)
         * @see org.epics.pvdata.pvCopy.PVCopy#updateCopyFromBitSet(org.epics.pvdata.pv.PVStructure, org.epics.pvdata.misc.BitSet)
         */
        @Override
        public void updateCopyFromBitSet(PVStructure copyPVStructure,BitSet bitSet) {
            boolean doAll = bitSet.get(0);
            if(headNode.isStructure) {
                updateStructureNodeFromBitSet(copyPVStructure,(StructureNode)headNode,bitSet, true,doAll);
            } else {
                RecordNode recordNode = (RecordNode)headNode;
                PVField[] pvCopyFields = copyPVStructure.getPVFields();
                if(pvCopyFields.length==1) {
                    updateSubFieldFromBitSet(pvCopyFields[0],recordNode.recordPVField,bitSet,true,doAll);
                } else {
                    updateSubFieldFromBitSet(copyPVStructure,recordNode.recordPVField,bitSet, true,doAll);
                }
            }
        }
        /* (non-Javadoc)
         * @see org.epics.pvdata.pvCopy.PVCopy#updateRecord(org.epics.pvdata.pv.PVStructure, org.epics.pvdata.misc.BitSet)
         */
        @Override
        public void updateRecord(PVStructure copyPVStructure, BitSet bitSet) {
            boolean doAll = bitSet.get(0);
            pvRecord.beginGroupPut();
            if(headNode.isStructure) {
                updateStructureNodeFromBitSet(copyPVStructure,(StructureNode)headNode,bitSet, false,doAll);
            } else {
                RecordNode recordNode = (RecordNode)headNode;
                PVField[] pvCopyFields = copyPVStructure.getPVFields();
                if(pvCopyFields.length==1) {
                    updateSubFieldFromBitSet(pvCopyFields[0],recordNode.recordPVField,bitSet,false,doAll);
                } else {
                    updateSubFieldFromBitSet(copyPVStructure,recordNode.recordPVField,bitSet,false,doAll);
                }
            }
            pvRecord.endGroupPut();
        }
         
        /* (non-Javadoc)
         * @see org.epics.pvdata.pvCopy.PVCopy#createPVCopyMonitor(org.epics.pvdata.pvCopy.PVCopyMonitorRequester)
         */
        @Override
        public PVCopyMonitor createPVCopyMonitor(PVCopyMonitorRequester pvCopyMonitorRequester) {
            return new CopyMonitor(pvCopyMonitorRequester);
        }
        
        private final PVRecord pvRecord;
        private Structure structure = null;
        private Node headNode = null;
        private PVStructure cacheInitStructure = null;
        
        private boolean  init(PVStructure pvRequest) {
            PVRecordStructure pvRecordStructure = pvRecord.getPVRecordStructure();
//System.out.println("pvRecord");
//System.out.println(pvRecordStructure.getPVStructure());
            int len = pvRequest.getPVFields().length;
            boolean entireRecord = false;
            if(len==0) entireRecord = true;
            PVStructure pvOptions = null;
            if(len==1 && pvRequest.getSubField("_options")!=null) {
                pvOptions = pvRequest.getStructureField("_options");
                entireRecord = true;
            }
            if(entireRecord) {
                // asking for entire record is special case.
                structure = pvRecordStructure.getPVStructure().getStructure();
                RecordNode recordNode = new RecordNode();
                headNode = recordNode;
                recordNode.options = pvOptions;
                recordNode.isStructure = false;
                recordNode.structureOffset = 0;
                recordNode.recordPVField = pvRecordStructure;
                recordNode.nfields = pvRecordStructure.getPVStructure().getNumberFields();
                return true;
            }
            structure = createStructure(pvRecordStructure.getPVStructure(),pvRequest);
            if(structure==null) return false;
            cacheInitStructure = createPVStructure();
            headNode = createStructureNodes(pvRecord.getPVRecordStructure(),pvRequest,cacheInitStructure);
            return true;
        }
        
        private static String getFullName(PVStructure pvFromRequest,String nameFromRecord) {
            PVField[] pvFields = pvFromRequest.getPVFields();
            int len = pvFields.length;
            if(len==1) {
                pvFromRequest = (PVStructure) pvFields[0];
                if(pvFromRequest.getFieldName().equals("_options")) return nameFromRecord;
                if(nameFromRecord.length()!=0) nameFromRecord += ".";
                nameFromRecord += pvFromRequest.getFieldName();
                return getFullName(pvFromRequest,nameFromRecord);
            }
            if(len==2) {
                PVField subField = null;
                if(pvFields[0].getFieldName().equals("_options")) {
                    subField = pvFields[1];
                } else if(pvFields[1].getFieldName().equals("_options")) {
                    subField = pvFields[1];
                }
                if(subField!=null) {
                    if(nameFromRecord.length()!=0) nameFromRecord += ".";
                    nameFromRecord += subField.getFieldName();
                    return getFullName((PVStructure)subField,nameFromRecord);
                }
            }
            return nameFromRecord;
        }
        
        private static PVStructure getSubStructure(PVStructure pvFromRequest,String nameFromRecord) {
            PVField[] pvFields = pvFromRequest.getPVFields();
            int len = pvFields.length;
            if(len==1) {
                pvFromRequest = (PVStructure) pvFields[0];
                if(pvFromRequest.getFieldName().equals("_options")) return pvFromRequest;
                if(nameFromRecord.length()!=0) nameFromRecord += ".";
                nameFromRecord += pvFromRequest.getFieldName();
                return getSubStructure(pvFromRequest,nameFromRecord);
            }
            if(len==2) {
                PVField subField = null;
                if(pvFields[0].getFieldName().equals("_options")) {
                    subField = pvFields[1];
                } else if(pvFields[1].getFieldName().equals("_options")) {
                    subField = pvFields[1];
                }
                if(subField!=null) {
                    if(nameFromRecord.length()!=0) nameFromRecord += ".";
                    nameFromRecord += subField.getFieldName();
                    return getSubStructure((PVStructure)subField,nameFromRecord);
                }
            }
            return pvFromRequest;
        }
        
        private static PVStructure getOptions(PVStructure pvFromRequest,String nameFromRecord) {
            PVField[] pvFields = pvFromRequest.getPVFields();
            int len = pvFields.length;
            if(len==1) {
                pvFromRequest = (PVStructure) pvFields[0];
                if(pvFromRequest.getFieldName().equals("_options")) return pvFromRequest;
                if(nameFromRecord.length()!=0) nameFromRecord += ".";
                nameFromRecord += pvFromRequest.getFieldName();
                return getSubStructure((PVStructure)pvFields[0],nameFromRecord);
            }
            if(len==2) {
                if(pvFields[0].getFieldName().equals("_options")) {
                    return((PVStructure)pvFields[0]);
                } else if(pvFields[1].getFieldName().equals("_options")) {
                    return((PVStructure)pvFields[1]);
                }
                
            }
            return null;
        }
        
        private static Structure createStructure(PVStructure pvRecord,PVStructure pvFromRequest) {
            if(pvFromRequest.getStructure().getFieldNames().length==0) {
                return pvRecord.getStructure();
            }
            Field field = createField(pvRecord,pvFromRequest);
            if(field==null) return null;            
            if(field.getType()==Type.structure) return (Structure)field;
            String[] fieldNames = new String[1];
            Field[] fields = new Field[1];
            String name = getFullName(pvFromRequest,"");
            int ind = name.lastIndexOf('.');
            if(ind>0) name = name.substring(ind+1);
            fieldNames[0] = name;
            fields[0] = field;
            return fieldCreate.createStructure(fieldNames, fields);
        }
        
        private static Field createField(PVStructure pvRecord,PVStructure pvFromRequest) {
            PVField[] pvFromRequestFields = pvFromRequest.getPVFields();
            String[] fromRequestFieldNames = pvFromRequest.getStructure().getFieldNames();
            int length = pvFromRequestFields.length;
            int number = 0;
            int indopt = -1;
            for(int i=0; i<length; i++) {
                if(!fromRequestFieldNames[i].equals("_options")) {
                    number++;
                } else {
                    indopt = i;
                }
            }
            if(number==0) return pvRecord.getStructure();
            if(number==1) {
                String nameFromRecord = "";
                nameFromRecord = getFullName(pvFromRequest,nameFromRecord);
                PVField pvRecordField = pvRecord.getSubField(nameFromRecord);
                if(pvRecordField==null) return null;
                Type recordFieldType = pvRecordField.getField().getType();
                if(recordFieldType!=Type.structure) return pvRecordField.getField();
                PVStructure pvSubFrom = (PVStructure)pvFromRequest.getSubField(nameFromRecord);
                PVField[] pvs = pvSubFrom.getPVFields();
                length = pvs.length;
                number = 0;
                for(int i=0; i<length; i++) {
                    if(!pvs[i].getFieldName().equals("_options")) {
                        number++;
                    }
                }
                Field[] fields = new Field[1];
                String[] fieldNames = new String[1];
                fieldNames[0] = pvRecordField.getFieldName();
                if(number==0) {
                    fields[0] = pvRecordField.getField();
                } else {
                fields[0] = createField((PVStructure)pvRecordField,pvSubFrom);
                }
                return fieldCreate.createStructure(fieldNames, fields);
            }
            ArrayList<Field> fieldList = new ArrayList<Field>(number);
            ArrayList<String> fieldNameList = new ArrayList<String>(number);
            for(int i=0; i<length; i++) {
                if(i==indopt) continue;
                PVStructure arg = (PVStructure)pvFromRequestFields[i];
                PVStructure yyy = (PVStructure)pvFromRequestFields[i];
                String zzz = getFullName(yyy,"");
                String full = fromRequestFieldNames[i];
                if(zzz.length()>0) {
                    full += "." + zzz;
                    arg = getSubStructure(yyy,zzz);
                }
                PVField pvRecordField = pvRecord.getSubField(full);
                if(pvRecordField==null) continue;
                Field field = pvRecordField.getField();
                if(field.getType()!=Type.structure) {
                    fieldNameList.add(full);
                    fieldList.add(field);
                    continue;
                }
                Field xxx = createField((PVStructure)pvRecordField,arg);
                if(xxx!=null) {
                    fieldNameList.add(fromRequestFieldNames[i]);
                    fieldList.add(xxx);
                }
            }
            Field[] fields = new Field[fieldList.size()];
            String[] fieldNames = new String[fieldNameList.size()];
            fields = fieldList.toArray(fields);
            fieldNames = fieldNameList.toArray(fieldNames);
            boolean makeValue = true;
            int indValue = -1;
            for(int i=0;i<fieldNames.length; i++) {
                if(fieldNames[i].endsWith("value")) {
                    if(indValue==-1) {
                        indValue = i;
                    } else {
                        makeValue = false;
                    }
                }
            }
            for(int i=0;i<fieldNames.length; i++) {
                if(makeValue==true&&indValue==i) {
                    fieldNames[i] = "value";
                } else {
                    String xxx = fieldNames[i];
                    int ind = xxx.indexOf('.');
                    if(ind>0) fieldNames[i] = xxx.substring(0, ind);
                }
            }
            return fieldCreate.createStructure(fieldNames, fields);
        }
        
        private static Node createStructureNodes(
                PVRecordStructure pvRecordStructure,
                PVStructure pvFromRequest,
                PVField pvFromField)
        {
            PVField[] pvFromRequestFields = pvFromRequest.getPVFields();
            String[] fromRequestFieldNames = pvFromRequest.getStructure().getFieldNames();
            int length = pvFromRequestFields.length;
            int number = 0;
            int indopt = -1;
            PVStructure pvOptions = null;
            for(int i=0; i<length; i++) {
                if(!fromRequestFieldNames[i].equals("_options")) {
                    number++;
                } else {
                    indopt = i;
                    pvOptions = (PVStructure)pvFromRequestFields[i];
                }
            }
            if(number==0) {
                RecordNode recordNode = new RecordNode();
                recordNode.options = pvOptions;
                recordNode.isStructure = false;
                recordNode.recordPVField = pvRecordStructure;
                recordNode.nfields = pvFromField.getNumberFields();
                recordNode.structureOffset = pvFromField.getFieldOffset();
                return recordNode;
            }
            if(number==1) {
                String nameFromRecord = "";
                nameFromRecord = getFullName(pvFromRequest,nameFromRecord);
                PVField pvField = pvRecordStructure.getPVStructure().getSubField(nameFromRecord);
                if(pvField==null) return null;
                PVRecordField pvRecordField = pvRecordStructure.getPVRecord().findPVRecordField(pvField);
                int structureOffset = pvFromField.getFieldOffset();
                PVStructure pvParent = pvFromField.getParent();
                //if(type!=Type.structure && pvParent==null) {
                if(pvParent==null) {
                    structureOffset++;
                }
                RecordNode recordNode = new RecordNode();
                recordNode.options = getOptions(pvFromRequest,nameFromRecord);
                recordNode.isStructure = false;
                recordNode.recordPVField = pvRecordField;
                recordNode.nfields = pvFromField.getNumberFields();
                recordNode.structureOffset = structureOffset;
                return recordNode;
            }
            ArrayList<Node> nodeList = new ArrayList<Node>(number);
            PVStructure pvFromStructure = (PVStructure)pvFromField;
            PVField[] pvFromStructureFields = pvFromStructure.getPVFields();
            length = pvFromStructureFields.length;
            int indFromStructure = 0;
            for(int i= 0; i <pvFromRequestFields.length;i++) {
                if(i==indopt) continue;
                PVStructure arg = (PVStructure)pvFromRequestFields[i];
                PVStructure yyy = (PVStructure)pvFromRequestFields[i];
                String zzz = getFullName(yyy,"");
                String full = fromRequestFieldNames[i];
                if(zzz.length()>0) {
                    full += "." + zzz;
                    arg = getSubStructure(yyy,zzz);
                }
                PVField pvField = pvRecordStructure.getPVStructure().getSubField(full);
                if(pvField==null) continue;
                PVRecordField pvRecordField = pvRecordStructure.getPVRecord().findPVRecordField(pvField);
                Node node = null;
                if(pvRecordField.getPVField().getField()==pvFromStructureFields[indFromStructure].getField()) {
                    pvField = pvFromStructureFields[indFromStructure];
                    RecordNode recordNode = new RecordNode();
                    recordNode.options = getOptions(yyy,zzz);
                    recordNode.isStructure = false;
                    recordNode.recordPVField = pvRecordField;
                    recordNode.nfields = pvField.getNumberFields();
                    recordNode.structureOffset = pvField.getFieldOffset();
                    node = recordNode;
                } else {
                    node = createStructureNodes((PVRecordStructure)pvRecordField,arg,pvFromStructureFields[indFromStructure]);
                }
                if(node==null) continue;
                nodeList.add(node);
                ++indFromStructure;
            }
            int len = nodeList.size();
            if(len<=0) return null;
            StructureNode structureNode = new StructureNode();
            Node[] nodes = new Node[len];
            nodeList.toArray(nodes);
            structureNode.isStructure = true;
            structureNode.nodes = nodes;
            structureNode.structureOffset = pvFromStructure.getFieldOffset();
            structureNode.nfields = pvFromStructure.getNumberFields();
            structureNode.options = pvOptions;
            return structureNode;
        }

        private void updateStructureNodeSetBitSet(PVStructure pvCopy,StructureNode structureNode,BitSet bitSet) {
            for(int i=0; i<structureNode.nodes.length; i++) {
                Node node = structureNode.nodes[i];
                PVField pvField = pvCopy.getSubField(node.structureOffset);
                if(node.isStructure) {
                    updateStructureNodeSetBitSet((PVStructure)pvField,(StructureNode)node,bitSet); 
                } else {
                    RecordNode recordNode = (RecordNode)node;
                    updateSubFieldSetBitSet(pvField,recordNode.recordPVField,bitSet);
                }
            }
        }
        
        private void updateSubFieldSetBitSet(PVField pvCopy,PVRecordField pvRecord,BitSet bitSet) {
        	Field field = pvCopy.getField();
        	Type type = field.getType();
            if(type!=Type.structure) {
            	boolean isEqual = pvCopy.equals(pvRecord.getPVField());
            	if(isEqual) {
            		if(type==Type.structureArray) {
            			// always act as though a change occurred. Note that array elements are shared.
        				bitSet.set(pvCopy.getFieldOffset());
            		}
            	}
                if(isEqual) return;
                convert.copy(pvRecord.getPVField(), pvCopy);
                bitSet.set(pvCopy.getFieldOffset());
                return;
            }
            PVStructure pvCopyStructure = (PVStructure)pvCopy;
            PVField[] pvCopyFields = pvCopyStructure.getPVFields();
            PVRecordStructure pvRecordStructure = (PVRecordStructure)pvRecord;
            PVRecordField[] pvRecordFields = pvRecordStructure.getPVRecordFields();
            int length = pvCopyFields.length;
            for(int i=0; i<length; i++) {
                updateSubFieldSetBitSet(pvCopyFields[i],pvRecordFields[i],bitSet);
            }
        }
        
        private void updateStructureNodeFromBitSet(PVStructure pvCopy,StructureNode structureNode,BitSet bitSet,boolean toCopy,boolean doAll) {
            int offset = structureNode.structureOffset;
            if(!doAll) {
                int nextSet = bitSet.nextSetBit(offset);
                if(nextSet==-1) return;
            }
            if(offset>=pvCopy.getNextFieldOffset()) return;
            if(!doAll) doAll = bitSet.get(offset);
            Node[] nodes = structureNode.nodes;
            for(int i=0; i<nodes.length; i++) {
                Node node = nodes[i];
                PVField pvField = pvCopy.getSubField(node.structureOffset);
                if(node.isStructure) {
                    StructureNode subStructureNode = (StructureNode)node;
                    updateStructureNodeFromBitSet((PVStructure)pvField,subStructureNode,bitSet,toCopy,doAll);
                } else {
                    RecordNode recordNode = (RecordNode)node;
                    updateSubFieldFromBitSet(pvField,recordNode.recordPVField,bitSet,toCopy,doAll);
                }
            }
        }
        
       
        private void updateSubFieldFromBitSet(PVField pvCopy,PVRecordField pvRecordField,BitSet bitSet,boolean toCopy,boolean doAll) {
            if(!doAll) {
                doAll = bitSet.get(pvCopy.getFieldOffset());
            }
            if(!doAll) {
                int offset = pvCopy.getFieldOffset();
                int nextSet = bitSet.nextSetBit(offset);
                if(nextSet==-1) return;
                if(nextSet>=pvCopy.getNextFieldOffset()) return;
            }
            if(pvCopy.getField().getType()==Type.structure) {
                PVStructure pvCopyStructure = (PVStructure)pvCopy;
                PVField[] pvCopyFields = pvCopyStructure.getPVFields();
                if(pvRecordField.getPVField().getField().getType()!=Type.structure) {
                    if(pvCopyFields.length!=1) {
                        throw new IllegalStateException("Logic error");
                    }
                    if(toCopy) {
                        convert.copy(pvRecordField.getPVField(), pvCopyFields[0]);
                    } else {
                        convert.copy(pvCopyFields[0], pvRecordField.getPVField());
                    }
                    return;
                }
                PVRecordStructure pvRecordStructure = (PVRecordStructure)pvRecordField;
                PVRecordField[] pvRecordFields = pvRecordStructure.getPVRecordFields();
                for(int i=0; i<pvCopyFields.length; i++) {
                    updateSubFieldFromBitSet(pvCopyFields[i],pvRecordFields[i],bitSet,toCopy,doAll);
                }
            } else {
                if(toCopy) {
                    convert.copy(pvRecordField.getPVField(), pvCopy);
                } else {
                    convert.copy(pvCopy, pvRecordField.getPVField());
                }
            }
        }
        

        private RecordNode getCopyOffset(StructureNode structureNode,PVRecordField recordPVField) {
            int offset = recordPVField.getPVField().getFieldOffset();
            for(Node node : structureNode.nodes) {
                if(!node.isStructure) {
                    RecordNode recordNode = (RecordNode)node;
                    int off = recordNode.recordPVField.getPVField().getFieldOffset();
                    int nextOffset = recordNode.recordPVField.getPVField().getNextFieldOffset(); 
                    if(offset>= off && offset<nextOffset) return recordNode;
                } else {
                    StructureNode subNode = (StructureNode)node;
                    RecordNode recordNode = getCopyOffset(subNode,recordPVField);
                    if(recordNode!=null) return recordNode;
                }
            }
            return null;
        }
         
        private final class CopyMonitor implements PVCopyMonitor, PVListener {
            private final PVCopyMonitorRequester pvCopyMonitorRequester;
            private BitSet changeBitSet = null;
            private BitSet overrunBitSet = null;
            private boolean isGroupPut = false;
            private boolean dataChanged = false;
            
            private CopyMonitor(PVCopyMonitorRequester pvCopyMonitorRequester) {
                this.pvCopyMonitorRequester = pvCopyMonitorRequester;
            }
            /* (non-Javadoc)
             * @see org.epics.pvdata.pvCopy.PVCopyMonitor#startMonitoring(org.epics.pvdata.pv.PVStructure, org.epics.pvdata.misc.BitSet, org.epics.pvdata.misc.BitSet)
             */
            @Override
            public void startMonitoring(BitSet changeBitSet, BitSet overrunBitSet) {
                this.changeBitSet = changeBitSet;
                this.overrunBitSet = overrunBitSet;
                isGroupPut = false;
                pvRecord.registerListener(this);
                addListener(headNode);
                pvRecord.lock();
                try {
                    changeBitSet.clear();
                    overrunBitSet.clear();
                    changeBitSet.set(0);
                    pvCopyMonitorRequester.dataChanged();
                } finally {
                    pvRecord.unlock();
                }
            }

            /* (non-Javadoc)
             * @see org.epics.pvdata.pvCopy.PVCopyMonitor#stopMonitoring()
             */
            @Override
            public void stopMonitoring() {
                pvRecord.unregisterListener(this);
            }
            /* (non-Javadoc)
             * @see org.epics.pvdata.pvCopy.PVCopyMonitor#updateCopy(org.epics.pvdata.misc.BitSet, org.epics.pvdata.misc.BitSet, boolean)
             */
            @Override
            public void switchBitSets(BitSet newChangeBitSet,BitSet newOverrunBitSet, boolean lockRecord) {
                if(lockRecord) pvRecord.lock();
                try {
                    changeBitSet = newChangeBitSet;
                    overrunBitSet = newOverrunBitSet;
                } finally {
                    if(lockRecord) pvRecord.unlock();
                }
            }
            /* (non-Javadoc)
             * @see org.epics.pvdata.pv.PVListener#beginGroupPut(org.epics.pvdata.pv.PVRecord)
             */
            @Override
            public void beginGroupPut(PVRecord pvRecord) {
                isGroupPut = true;
                dataChanged = false;
            }
            /* (non-Javadoc)
             * @see org.epics.pvioc.database.PVListener#dataPut(org.epics.pvioc.database.PVRecordField)
             */
            @Override
			public void dataPut(PVRecordField pvRecordField) {
            	Node node = findNode(headNode,pvRecordField);
            	if(node==null) {
            		throw new IllegalStateException("Logic error");
            	}
            	int offset = node.structureOffset;
            	synchronized(changeBitSet) {
            		if (changeBitSet.getAndSet(offset))
            			overrunBitSet.set(offset);
            	}
            	if(!isGroupPut) pvCopyMonitorRequester.dataChanged();
                dataChanged = true;
            }
            /* (non-Javadoc)
             * @see org.epics.pvioc.database.PVListener#dataPut(org.epics.pvioc.database.PVRecordStructure, org.epics.pvioc.database.PVRecordField)
             */
            @Override
			public void dataPut(PVRecordStructure requested,PVRecordField pvRecordField) {
            	Node node = findNode(headNode,requested);
            	if(node==null || node.isStructure) {
            		throw new IllegalStateException("Logic error");
            	}
            	RecordNode recordNode = (RecordNode)node;
            	int offset = recordNode.structureOffset
            	+ (pvRecordField.getPVField().getFieldOffset() - recordNode.recordPVField.getPVField().getFieldOffset());
            	synchronized(changeBitSet) {
            		if (changeBitSet.getAndSet(offset))
            			overrunBitSet.set(offset);
            	}
            	if(!isGroupPut) pvCopyMonitorRequester.dataChanged();
                dataChanged = true;

            }
            /* (non-Javadoc)
             * @see org.epics.pvdata.pv.PVListener#endGroupPut(org.epics.pvdata.pv.PVRecord)
             */
            @Override
            public void endGroupPut(PVRecord pvRecord) {
                isGroupPut = false;
                if(dataChanged) {
                    dataChanged = false;
                    pvCopyMonitorRequester.dataChanged();
                }
            }

            /* (non-Javadoc)
             * @see org.epics.pvdata.pv.PVListener#unlisten(org.epics.pvdata.pv.PVRecord)
             */
            @Override
            public void unlisten(PVRecord pvRecord) {
                pvCopyMonitorRequester.unlisten();
            }
            
            private void addListener(Node node) {
                if(!node.isStructure) {
                    PVRecordField pvRecordField = getRecordPVField(node.structureOffset);
                    pvRecordField.addListener(this);
                    return;
                }
                StructureNode structureNode = (StructureNode)node;
                for(int i=0; i<structureNode.nodes.length; i++) {
                    addListener(structureNode.nodes[i]);
                }
            }
            
            private Node findNode(Node node,PVRecordField pvRecordField) {
                if(!node.isStructure) {
                    RecordNode recordNode = (RecordNode)node;
                    if(recordNode.recordPVField==pvRecordField) return node;
                    return null;
                }
                StructureNode structureNode = (StructureNode)node;
                for(int i=0; i<structureNode.nodes.length; i++) {
                    node = findNode(structureNode.nodes[i],pvRecordField);
                    if(node!=null) return node;
                }
                return null;
            }    
        }
    }
}
