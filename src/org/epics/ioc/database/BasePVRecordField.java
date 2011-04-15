/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS pvData is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.database;

import org.epics.ioc.support.Support;
import org.epics.pvData.misc.LinkedList;
import org.epics.pvData.misc.LinkedListCreate;
import org.epics.pvData.misc.LinkedListNode;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVField;
import org.epics.pvData.pv.PVStructure;
import org.epics.pvData.pv.PostHandler;
import org.epics.pvData.pv.Type;

/**
 * @author mrk
 *
 */
public class BasePVRecordField implements PVRecordField, PostHandler{
	private static LinkedListCreate<PVListener> linkedListCreate = new LinkedListCreate<PVListener>();
	private Support support = null;
	private PVField pvField = null;
	private BasePVRecord pvRecord = null;
	private PVRecordStructure parent = null;
	private boolean isStructure = false;
	private LinkedList<PVListener> pvListenerList = linkedListCreate.create();
	private String fullName = null;
	private String fullFieldName = null;

	BasePVRecordField(PVField pvField,PVRecordStructure parent,BasePVRecord pvRecord) {
		this.pvField = pvField;
		this.parent = parent;
		this.pvRecord = pvRecord;
		if(pvField.getField().getType()==Type.structure) isStructure = true;
		pvField.setPostHandler(this);
	}
	/* (non-Javadoc)
	 * @see org.epics.ioc.database.PVRecordField#getSupport()
	 */
	@Override
	public Support getSupport() {
		return support;
	}
	/* (non-Javadoc)
	 * @see org.epics.ioc.database.PVRecordField#setSupport(org.epics.ioc.support.Support)
	 */
	@Override
	public void setSupport(Support support) {
		if(this.support!=null) {
			throw new IllegalStateException("a support has already been assigned to this field");
		}
		this.support = support;
	}
	
	@Override
    public String getRequesterName() {
        return pvRecord.getRecordName();
    }
    @Override
    public void message(String message, MessageType messageType) {
        pvRecord.message(getFullName() + " " + message, messageType);
    }
    @Override
	public PVRecordStructure getParent() {
		return parent;
	}
	@Override
	public String getFullFieldName() {
		if(fullFieldName==null) createNames();
		return fullFieldName;
	}
	@Override
	public String getFullName() {
		if(fullName==null) createNames();
		return fullName;
	}
	@Override
	public PVField getPVField() {
		return pvField;
	}
	@Override
	public void replacePVField(PVField newField) {
		pvField.replacePVField(newField);
		pvField = newField;
		pvField.setPostHandler(this);
		if(pvField.getField().getType()==Type.structure) {
			PVStructure pvStructure = (PVStructure)pvField;
			PVField[] pvFields = pvStructure.getPVFields();
			PVRecordStructure pvRecordStructure = (PVRecordStructure)this;
			PVRecordField[] pvRecordFields = pvRecordStructure.getPVRecordFields();
			for(int i=0; i<pvRecordFields.length; i++) {
				PVRecordField pvrf = pvRecordFields[i];
				pvrf.replacePVField(pvFields[i]);
			}
		}
	}
	/* (non-Javadoc)
	 * @see org.epics.pvData.pv.PVField#getPVRecord()
	 */
	@Override
	public PVRecord getPVRecord() {
		return pvRecord;
	}
	/* (non-Javadoc)
	 * @see org.epics.ioc.database.PVRecordField#renameField(java.lang.String)
	 */
	@Override
	public void renameField(String newName) {
		pvField.renameField(newName);
		createNames();
	}
	/* (non-Javadoc)
	 * @see org.epics.pvData.pv.PVField#addListener(org.epics.pvData.pv.PVListener)
	 */
	@Override
	public boolean addListener(PVListener pvListener) {
		if(!pvRecord.isRegisteredListener(pvListener)) return false;
		LinkedListNode<PVListener> listNode = linkedListCreate.createNode(pvListener);
		pvListenerList.addTail(listNode);
		return true;
	}
	/* (non-Javadoc)
	 * @see org.epics.pvData.pv.PVRecordField#removeListener(org.epics.pvData.pv.PVListener)
	 */
     @Override
     public void removeListener(PVListener pvListener) {
         pvListenerList.remove(pvListener);
         if(isStructure) {
        	 PVRecordStructure recordStructure = (PVRecordStructure)this;
        	 PVRecordField[] pvRecordFields = recordStructure.getPVRecordFields();
        	 for(PVRecordField pvRecordField: pvRecordFields) {
        		 pvRecordField.removeListener(pvListener);
        	 }
         }
     }
     
     /* (non-Javadoc)
      * @see org.epics.pvData.pv.PVField#postPut()
      */
     @Override
     public void postPut() {
    	 if(pvField.getNextFieldOffset()==0) return; // setOffsets has never been called.
    	 callListener();
    	 if(parent!=null) {
    		 BasePVRecordField pvf = (BasePVRecordField)parent;
    		 pvf.postParent(this);
    	 }
    	 if(isStructure) {
    		 PVRecordStructure recordStructure = (PVRecordStructure)this;
    		 PVRecordField[] pvRecordFields = recordStructure.getPVRecordFields();
    		 for(int i=0; i<pvRecordFields.length; i++) {
    			 BasePVRecordField pv = (BasePVRecordField)pvRecordFields[i];
    			 postSubField(pv);
    		 }
    	 }
     }
      
     private void postParent(PVRecordField subField) {
    	 LinkedListNode<PVListener> listNode = pvListenerList.getHead();
    	 while(listNode!=null) {
    		 PVListener pvListener = listNode.getObject();
             pvListener.dataPut((PVRecordStructure)this,subField);
             listNode = pvListenerList.getNext(listNode);
         }
    	 if(parent!=null) {
    		 BasePVRecordField pv = (BasePVRecordField)parent;
    		 pv.postParent(subField);
    	 }
     }
     
     private void postSubField(BasePVRecordField pvRecordField) {
         pvRecordField.callListener();
         if(pvRecordField.pvField.getField().getType()==Type.structure) {
        	 PVRecordStructure pvrs = (PVRecordStructure)this;
        	 PVRecordField[] pvrfs = pvrs.getPVRecordFields();
        	 for(int i=0;i<pvrfs.length; i++) {
        		 BasePVRecordField pv = (BasePVRecordField)pvrfs[i];
        		 postSubField(pv);
        	 }
         }
     }
     
     private void callListener() {
    	 LinkedListNode<PVListener> listNode = pvListenerList.getHead();
    	 while(listNode!=null) {
    		 PVListener pvListener = listNode.getObject();
             pvListener.dataPut(this);
             listNode = pvListenerList.getNext(listNode);
         }
     }
     
     private void createNames(){
    	 StringBuilder builder = new StringBuilder();
    	 PVField pvField = getPVField();
    	 builder.append(pvField.getField().getFieldName());
    	 pvField = pvField.getParent();
    	 while(pvField!=null) {
    		 String fieldName = pvField.getField().getFieldName();
    		 if(fieldName==null || fieldName.length()<1) break;
    		 builder.insert(0, '.');
    		 builder.insert(0, fieldName);
    		 pvField = pvField.getParent();
    	 }
    	 fullFieldName = builder.toString();
    	 if(fullFieldName.length()>0) builder.insert(0, '.');
    	 builder.insert(0, pvRecord.getRecordName());
    	 fullName = builder.toString();
     }
}
