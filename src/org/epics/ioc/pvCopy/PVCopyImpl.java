/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS pvData is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pvCopy;

import java.util.ArrayList;
import java.util.regex.Pattern;

import org.epics.ioc.database.PVListener;
import org.epics.ioc.database.PVRecord;
import org.epics.ioc.database.PVRecordField;
import org.epics.ioc.database.PVRecordStructure;
import org.epics.pvData.factory.ConvertFactory;
import org.epics.pvData.factory.FieldFactory;
import org.epics.pvData.factory.PVDataFactory;
import org.epics.pvData.misc.BitSet;
import org.epics.pvData.pv.Convert;
import org.epics.pvData.pv.Field;
import org.epics.pvData.pv.FieldCreate;
import org.epics.pvData.pv.PVArray;
import org.epics.pvData.pv.PVDataCreate;
import org.epics.pvData.pv.PVField;
import org.epics.pvData.pv.PVScalar;
import org.epics.pvData.pv.PVString;
import org.epics.pvData.pv.PVStructure;
import org.epics.pvData.pv.PVStructureArray;
import org.epics.pvData.pv.Structure;
import org.epics.pvData.pv.Type;

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
		impl.init(pvStruct);
		return impl;
	}
	
	private static final FieldCreate fieldCreate = FieldFactory.getFieldCreate();
    private static final PVDataCreate pvDataCreate = PVDataFactory.getPVDataCreate();
    private static final Convert convert = ConvertFactory.getConvert();
    private static final Pattern commaPattern = Pattern.compile("[,]");
    
    static class Node {
        boolean isStructure = false;
        int structureOffset = 0; // In the copy
        int nfields = 0;
        boolean shareData = false;
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
         * @see org.epics.pvData.pvCopy.PVCopy#getpvStructure()
         */
        @Override
        public PVRecord getPVRecord() {
            return pvRecord;
        }
        /* (non-Javadoc)
         * @see org.epics.pvData.pvCopy.PVCopy#getStructure()
         */
        @Override
        public Structure getStructure() {
            return structure;
        }
        /* (non-Javadoc)
         * @see org.epics.pvData.pvCopy.PVCopy#createPVStructure()
         */
        @Override
        public PVStructure createPVStructure() {
            if(cacheInitStructure!=null) {
                PVStructure save = cacheInitStructure;
                cacheInitStructure = null;
                return save;
            }
            PVStructure pvStructure =  pvDataCreate.createPVStructure(null, structure);
            if(headNode!=null) {
                referenceImmutable(pvStructure,headNode);
            }
            return pvStructure;
        }
        @Override
		public int getCopyOffset(PVRecordField recordPVField) {
           if(!headNode.isStructure) {
               RecordNode recordNode = (RecordNode)headNode;
               if(recordNode.recordPVField==recordPVField) return headNode.structureOffset;
               return -1;
           }
           RecordNode recordNode = getCopyOffset((StructureNode)headNode,recordPVField);
           if(recordNode!=null) return recordNode.structureOffset;
           return -1;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvCopy.PVCopy#getCopyOffset(org.epics.ioc.database.PVRecordStructure, org.epics.ioc.database.PVRecordField)
         */
        @Override
		public int getCopyOffset(PVRecordStructure recordPVStructure,PVRecordField recordPVField) {
            RecordNode recordNode = null;
            if(!headNode.isStructure) {
                recordNode = (RecordNode)headNode;
                if(recordNode.recordPVField!=recordPVStructure) return -1;
            } else {
                recordNode = getCopyOffset((StructureNode)headNode,recordPVStructure);
            }
            if(recordNode==null) return -1;
            int diff = recordPVField.getPVField().getFieldOffset() - recordPVStructure.getPVStructure().getFieldOffset();
            return recordNode.structureOffset + diff;
        }
        /* (non-Javadoc)
         * @see org.epics.pvData.pvCopy.PVCopy#getPVField(int)
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
            	throw new IllegalArgumentException("structureOffset not valid");
            }
            int diff = structureOffset - recordNode.structureOffset;
            PVRecordField pvRecordField = recordNode.recordPVField;
            if(diff==0) return pvRecordField;
            PVStructure pvStructure = (PVStructure)pvRecordField.getPVField();
            PVField pvField = pvStructure.getSubField(pvRecordField.getPVField().getFieldOffset() + diff);
            return pvRecord.findPVRecordField(pvField);
        }
        /* (non-Javadoc)
         * @see org.epics.pvData.pvCopy.PVCopy#initCopy(org.epics.pvData.pv.PVStructure, org.epics.pvData.misc.BitSet)
         */
        @Override
        public void initCopy(PVStructure copyPVStructure, BitSet bitSet, boolean lockRecord) {
            bitSet.clear();
            bitSet.set(0);
            updateCopyFromBitSet(copyPVStructure,bitSet,lockRecord);
        }
        /* (non-Javadoc)
         * @see org.epics.pvData.pvCopy.PVCopy#updateCopySetBitSet(org.epics.pvData.pv.PVStructure, org.epics.pvData.misc.BitSet, boolean)
         */
        @Override
        public void updateCopySetBitSet(PVStructure copyPVStructure,BitSet bitSet, boolean lockRecord)
        {
            if(lockRecord) pvRecord.lock();
            try {
                if(headNode.isStructure) {
                    updateStructureNodeSetBitSet(copyPVStructure,(StructureNode)headNode,bitSet);
                } else {
                    RecordNode recordNode = (RecordNode)headNode;
                    updateSubFieldSetBitSet(copyPVStructure,recordNode.recordPVField,bitSet);
                }
            } finally {
                if(lockRecord) pvRecord.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.pvData.pvCopy.PVCopy#updateCopyFromBitSet(org.epics.pvData.pv.PVStructure, org.epics.pvData.misc.BitSet, boolean)
         */
        @Override
        public void updateCopyFromBitSet(PVStructure copyPVStructure,BitSet bitSet, boolean lockRecord) {
            boolean doAll = bitSet.get(0);
            if(lockRecord) pvRecord.lock();
            try {
                if(headNode.isStructure) {
                    updateStructureNodeFromBitSet(copyPVStructure,(StructureNode)headNode,bitSet, true,doAll);
                } else {
                    RecordNode recordNode = (RecordNode)headNode;
                    updateSubFieldFromBitSet(copyPVStructure,recordNode.recordPVField,bitSet, true,doAll);
                }
            } finally {
                if(lockRecord) pvRecord.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.pvData.pvCopy.PVCopy#updateRecord(org.epics.pvData.pv.PVStructure, org.epics.pvData.misc.BitSet, boolean)
         */
        @Override
        public void updateRecord(PVStructure copyPVStructure, BitSet bitSet,boolean lockRecord) {
            boolean doAll = bitSet.get(0);
            if(lockRecord) pvRecord.lock();
            try {
                pvRecord.beginGroupPut();
                if(headNode.isStructure) {
                    updateStructureNodeFromBitSet(copyPVStructure,(StructureNode)headNode,bitSet, false,doAll);
                } else {
                    RecordNode recordNode = (RecordNode)headNode;
                    updateSubFieldFromBitSet(copyPVStructure,recordNode.recordPVField,bitSet, false,doAll);
                }
                pvRecord.endGroupPut();
            } finally {
                if(lockRecord) pvRecord.unlock();
            }
        }
         
        /* (non-Javadoc)
         * @see org.epics.pvData.pvCopy.PVCopy#createPVCopyMonitor(org.epics.pvData.pvCopy.PVCopyMonitorRequester)
         */
        @Override
        public PVCopyMonitor createPVCopyMonitor(PVCopyMonitorRequester pvCopyMonitorRequester) {
            return new CopyMonitor(pvCopyMonitorRequester);
        }
        
        private final PVRecord pvRecord;
        private Structure structure = null;
        private Node headNode = null;
        private PVStructure cacheInitStructure = null;
        
        private void init(PVStructure pvRequest) {
            PVRecordStructure pvRecordStructure = pvRecord.getPVRecordStructure();
            if(pvRequest.getPVFields().length==0) {
                // asking for entire record is special case.
                structure = pvRecordStructure.getPVStructure().getStructure();
                RecordNode recordNode = new RecordNode();
                headNode = recordNode;
                recordNode.isStructure = false;
                recordNode.structureOffset = 0;
                recordNode.recordPVField = pvRecordStructure;
                recordNode.nfields = pvRecordStructure.getPVStructure().getNumberFields();
                return;
            }
//System.out.println("pvRequest");
//System.out.println(pvRequest);
            structure = createStructure(pvRecordStructure.getPVStructure(),pvRequest,"");
//System.out.println("structure");
//System.out.println(structure);
            cacheInitStructure = createPVStructure();
            StructureNode structureNode = new StructureNode();
            createStructureNodes(pvRecord,structureNode,pvRequest,cacheInitStructure);
            headNode = structureNode;
            referenceImmutable(cacheInitStructure,headNode);
        }
        
        private static Structure createStructure(PVStructure pvRecord,PVStructure pvFromRequest,String fieldName) {
            PVField[] pvFromFields = pvFromRequest.getPVFields();
            int length = pvFromFields.length;
            ArrayList<Field> fieldList = new ArrayList<Field>(length);
            ArrayList<String> fieldNameList = new ArrayList<String>(length);
            for(int i=0; i<length; i++) {
            	PVField pvField = pvFromFields[i];
            	if(pvField.getField().getType()==Type.structure) {
            		PVStructure pvStruct = (PVStructure)pvField;
            		PVField pvLeaf = pvStruct.getSubField("leaf.source");
            		if(pvLeaf!=null && (pvLeaf instanceof PVString)){
            			PVString pvString = (PVString)pvLeaf;
            			PVField pvRecordField = pvRecord.getSubField(pvString.get());
            			if(pvRecordField!=null) {
            				Field field = pvRecordField.getField();
            				fieldList.add(field);
            				fieldNameList.add(pvRecordField.getFieldName());
            			}
            		} else {
            			fieldList.add(pvField.getField());
            			fieldNameList.add(pvField.getFieldName());
            		}
            	} else {
            		PVString pvString = (PVString)pvFromFields[i];
            		if(pvString.getFieldName().equals("fieldList")) {
            			String[] fieldNames = commaPattern.split(pvString.get());
            			for(int j=0; j<fieldNames.length; j++) {
            				PVField pvRecordField = pvRecord.getSubField(fieldNames[j].trim());
            				if(pvRecordField!=null) {
            					fieldList.add(pvRecordField.getField());
            					fieldNameList.add(pvRecordField.getFieldName());
            				}
            			}
            		} else {
            			PVField pvRecordField = pvRecord.getSubField(pvString.get().trim());
            			if(pvRecordField!=null) {
            				fieldList.add(pvRecordField.getField());
            				fieldNameList.add(pvRecordField.getFieldName());
            			}
            		}
            	}
            }
            Field[] fields = new Field[fieldList.size()];
            String[] fieldNames = new String[fieldNameList.size()];
            fields = fieldList.toArray(fields);
            fieldNames = fieldNameList.toArray(fieldNames);
            return fieldCreate.createStructure(fieldNames, fields);
        }
        
        private static void createStructureNodes(
        		PVRecord pvRecord,
        		StructureNode structureNode,
                PVStructure pvFromRequest,
                PVStructure pvFromStructure)
        {
            PVRecordStructure pvRecordStructure = pvRecord.getPVRecordStructure();
            PVField[] pvFromStructureFields = pvFromStructure.getPVFields();
            PVField[] pvFromRequestFields = pvFromRequest.getPVFields();
            int length = pvFromStructureFields.length;
            structureNode.isStructure = true;
            structureNode.nodes = new Node[length];
            structureNode.structureOffset = pvFromStructure.getFieldOffset();
            structureNode.nfields = pvFromStructure.getNumberFields();
//System.out.println("nfields " + structureNode.nfields + " length " + length);
//System.out.println("pvRecord");
//System.out.println(pvRecordStructure.getPVStructure());
//System.out.println("pvFromStructure");
//System.out.println(pvFromStructure);
            int nextOffset = structureNode.structureOffset + 1;
            int index = 0;
            for(int indRequestFields= 0; indRequestFields <pvFromRequestFields.length;indRequestFields++) {
            	PVField pvRequest = pvFromRequestFields[indRequestFields];
            	if(pvRequest.getField().getType()==Type.structure) {
            		PVStructure pvStruct = (PVStructure)pvRequest;
            		PVField pvLeaf = pvStruct.getSubField("leaf.source");
            		if(pvLeaf!=null && (pvLeaf instanceof PVString)){
            			PVString pvString = (PVString)pvLeaf;
            			PVField pvField = pvRecord.getPVRecordStructure().getPVStructure().getSubField(pvString.get());
            			if(pvField!=null) {
            				PVRecordField pvRecordField = pvRecord.findPVRecordField(pvField);
            				PVField pvShareData = pvStruct.getSubField("shareData");
            				boolean shareData = false;
            				if(pvShareData!=null) {
            					pvString = (PVString)pvShareData;
            					if(pvString.get().equals("true")) {
            						shareData = true;
            					}
            				}
            				RecordNode recordNode = new RecordNode();
            				recordNode.shareData = shareData;
            				recordNode.recordPVField = pvRecordField;
            				structureNode.nodes[index++] = recordNode;
            				recordNode.nfields = pvRecordField.getPVField().getNumberFields();
            				recordNode.structureOffset = nextOffset;
            				nextOffset += recordNode.nfields;
            			}
            		} else {
            			PVStructure pvFromStruct = (PVStructure)pvFromStructureFields[index];
            			StructureNode newNode = new StructureNode();
            			createStructureNodes(pvRecord,newNode,(PVStructure)pvRequest,pvFromStruct);
            			structureNode.nodes[index++] = newNode;
            			nextOffset = pvFromStruct.getNextFieldOffset();
            		}
            		continue;
            	} else {
            		PVString pvString = (PVString)pvFromRequestFields[indRequestFields];
            		if(pvString.getFieldName().equals("fieldList")) {
            			String[] fieldNames = commaPattern.split(pvString.get());
            			for(int j=0; j<fieldNames.length; j++) {
            				PVField pvField = pvRecordStructure.getPVStructure().getSubField(fieldNames[j].trim());
            				if(pvField!=null) {
            					PVRecordField pvRecordField = pvRecord.findPVRecordField(pvField);
            					RecordNode recordNode = new RecordNode();
            					recordNode.recordPVField = pvRecordField;
//System.out.println("fieldName " + fieldNames[j] + " index " + index + " j "+ j + " structureNode " + structureNode.nfields + " len " + structureNode.nodes.length);
//if(j>=structureNode.nodes.length) {
//System.out.println("problem");
//}
            					structureNode.nodes[index++] = recordNode;
            					recordNode.nfields = pvRecordField.getPVField().getNumberFields();
            					recordNode.structureOffset = nextOffset;
            					nextOffset += recordNode.nfields;
            				}
            			}
            		} else {
            			PVField pvField = pvRecordStructure.getPVStructure().getSubField(pvString.get().trim());
            			if(pvField!=null) {
            				PVRecordField pvRecordField = pvRecord.findPVRecordField(pvField);
            				RecordNode recordNode = new RecordNode();
            				recordNode.recordPVField = pvRecordField;
            				structureNode.nodes[index++] = recordNode;
            				recordNode.nfields = pvRecordField.getPVField().getNumberFields();
            				recordNode.structureOffset = nextOffset;
            				nextOffset += recordNode.nfields;
            			}
            		}
            	}
            }
        }

        private void referenceImmutable(PVField pvField,Node node) {
            if(node.isStructure) {
                StructureNode structureNode = (StructureNode)node;
                Node[] nodes = structureNode.nodes;
                PVStructure pvStructure = (PVStructure)pvField;
                for(Node nextNode : nodes) {
                    referenceImmutable(pvStructure.getSubField(nextNode.structureOffset),nextNode);
                }
            } else {
                RecordNode recordNode = (RecordNode)node;
                PVRecordField recordPVField = recordNode.recordPVField;
            	if(node.shareData) {
                    makeShared(pvField,recordNode.recordPVField);
                } else {
                    referenceImmutable(pvField,recordPVField);
                }
            }
        }
        
        private void referenceImmutable(PVField copyPVField,PVRecordField recordPVField) {
            if(recordPVField.getPVField().getField().getType()==Type.structure) {
                PVField[] copyPVFields = ((PVStructure)copyPVField).getPVFields();
                PVRecordField[] recordPVFields = ((PVRecordStructure)recordPVField).getPVRecordFields();
                for(int i=0; i<copyPVFields.length; i++) {
                    referenceImmutable(copyPVFields[i],recordPVFields[i]);
                }
                return;
            }
            if(recordPVField.getPVField().isImmutable()) convert.copy(recordPVField.getPVField(), copyPVField);
        }
        
        private void makeShared(PVField copyPVField,PVRecordField recordPVField) {
        	PVField pvField = recordPVField.getPVField();
            switch(pvField.getField().getType()) {
            case structure: {
                PVField[] copyPVFields = ((PVStructure)copyPVField).getPVFields();
                PVRecordField[] recordPVFields = ((PVRecordStructure)recordPVField).getPVRecordFields();
                for(int i=0; i<copyPVFields.length; i++) {
                    makeShared(copyPVFields[i],recordPVFields[i]);
                }
                break;
            }
            case scalar:
                PVShareFactory.replace(recordPVField.getPVRecord(),(PVScalar)copyPVField,(PVScalar)recordPVField);
                break;
            case scalarArray:
                PVShareFactory.replace(recordPVField.getPVRecord(),(PVArray)copyPVField,(PVArray)recordPVField);
            case structureArray:
            	PVShareFactory.replace(recordPVField.getPVRecord(),(PVStructureArray)copyPVField,(PVStructureArray)recordPVField);
            }
        }
        
        private void updateStructureNodeSetBitSet(PVStructure pvCopy,StructureNode structureNode,BitSet bitSet) {
            for(int i=0; i<structureNode.nodes.length; i++) {
                Node node = structureNode.nodes[i];
                PVField pvField = pvCopy.getSubField(node.structureOffset);
                if(node.isStructure) {
                    updateStructureNodeSetBitSet((PVStructure)pvField,(StructureNode)node,bitSet); 
                } else {
                    RecordNode recordNode = (RecordNode)node;
                    if(node.shareData) {
                    	bitSet.set(pvField.getFieldOffset());
                    } else {
                        updateSubFieldSetBitSet(pvField,recordNode.recordPVField,bitSet);
                    }
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
            PVRecordStructure pvRecordStructure = (PVRecordStructure)pvRecord;
            PVField[] pvCopyFields = pvCopyStructure.getPVFields();
            PVRecordField[] pvRecordFields = pvRecordStructure.getPVRecordFields();
            int length = pvCopyFields.length;
            for(int i=0; i<length; i++) {
                updateSubFieldSetBitSet(pvCopyFields[i],pvRecordFields[i],bitSet);
            }
        }
        
        private void updateStructureNodeFromBitSet(PVStructure pvCopy,StructureNode structureNode,BitSet bitSet,boolean toCopy,boolean doAll) {
            int offset = structureNode.structureOffset;
            int nextSet = bitSet.nextSetBit(offset);
            if(nextSet==-1) return;
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
            for(Node node : structureNode.nodes) {
                if(!node.isStructure) {
                    RecordNode recordNode = (RecordNode)node;
                    if(recordNode.recordPVField==recordPVField) return recordNode;
                } else {
                    StructureNode subNode = (StructureNode)node;
                    RecordNode recordNode = getCopyOffset(subNode,recordPVField);
                    if(recordNode!=null) return recordNode;
                }
            }
            return null;
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
             * @see org.epics.pvData.pvCopy.PVCopyMonitor#startMonitoring(org.epics.pvData.pv.PVStructure, org.epics.pvData.misc.BitSet, org.epics.pvData.misc.BitSet)
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
             * @see org.epics.pvData.pvCopy.PVCopyMonitor#stopMonitoring()
             */
            @Override
            public void stopMonitoring() {
                pvRecord.unregisterListener(this);
            }
            /* (non-Javadoc)
             * @see org.epics.pvData.pvCopy.PVCopyMonitor#updateCopy(org.epics.pvData.misc.BitSet, org.epics.pvData.misc.BitSet, boolean)
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
             * @see org.epics.pvData.pv.PVListener#beginGroupPut(org.epics.pvData.pv.PVRecord)
             */
            @Override
            public void beginGroupPut(PVRecord pvRecord) {
                isGroupPut = true;
                dataChanged = false;
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.database.PVListener#dataPut(org.epics.ioc.database.PVRecordField)
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
             * @see org.epics.ioc.database.PVListener#dataPut(org.epics.ioc.database.PVRecordStructure, org.epics.ioc.database.PVRecordField)
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
             * @see org.epics.pvData.pv.PVListener#endGroupPut(org.epics.pvData.pv.PVRecord)
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
             * @see org.epics.pvData.pv.PVListener#unlisten(org.epics.pvData.pv.PVRecord)
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
