/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.caV4;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;

import org.epics.pvData.factory.AbstractPVField;
import org.epics.pvData.factory.BaseField;
import org.epics.pvData.factory.FieldFactory;
import org.epics.pvData.pv.Array;
import org.epics.pvData.pv.Field;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVArray;
import org.epics.pvData.pv.PVBoolean;
import org.epics.pvData.pv.PVByte;
import org.epics.pvData.pv.PVDouble;
import org.epics.pvData.pv.PVField;
import org.epics.pvData.pv.PVFloat;
import org.epics.pvData.pv.PVInt;
import org.epics.pvData.pv.PVListener;
import org.epics.pvData.pv.PVLong;
import org.epics.pvData.pv.PVShort;
import org.epics.pvData.pv.PVString;
import org.epics.pvData.pv.PVStructure;
import org.epics.pvData.pv.Scalar;
import org.epics.pvData.pv.ScalarType;
import org.epics.pvData.pv.Structure;
import org.epics.pvData.pv.Type;

class DynamicSubsetOfPVStructure extends AbstractPVField implements PVStructure
{
	class DynamicSubsetOfStructure extends BaseField implements Structure
    {
    	private Field[] cachedFields;
    	
    	public DynamicSubsetOfStructure() {
    		super(null, Type.structure);
    	}

		/* (non-Javadoc)
		 * @see org.epics.pvData.pv.Structure#getField(java.lang.String)
		 */
		public Field getField(String fieldName) {
			return pvFieldsMap.get(fieldName).getField();
		}

		/* (non-Javadoc)
		 * @see org.epics.pvData.pv.Structure#getFieldIndex(java.lang.String)
		 */
		public int getFieldIndex(String fieldName) {
			throw new UnsupportedOperationException("dynamic type");
		}

		/* (non-Javadoc)
		 * @see org.epics.pvData.pv.Structure#getFieldNames()
		 */
		public String[] getFieldNames() {
			return pvFieldsMap.keySet().toArray(new String[pvFieldsMap.size()]);
		}

		/* (non-Javadoc)
		 * @see org.epics.pvData.pv.Structure#getFields()
		 */
		public Field[] getFields() {
			if (cachedFields == null) {
				cachedFields = new Field[pvFieldsMap.size()];
				final Collection<PVField> pvFields = pvFieldsMap.values(); 
				int i = 0;
				for (PVField pvField : pvFields)
					cachedFields[i++] = pvField.getField();
			}
			return cachedFields;
		}
		
		public void changed() {
			cachedFields = null;
		}
		
	    /* (non-Javadoc)
	     * @see org.epics.pvData.factory.BaseField#toString()
	     */
	    public String toString() { return getString(0);}
	    /* (non-Javadoc)
	     * @see org.epics.pvData.factory.BaseField#toString(int)
	     */
	    public String toString(int indentLevel) {
	        return getString(indentLevel);
	    }

	    private String getString(int indentLevel) {
	        StringBuilder builder = new StringBuilder();
	        builder.append(super.toString(indentLevel));
	        convert.newLine(builder,indentLevel);
	        builder.append(String.format("structure  {"));
	        final Field[] fields = getFields();
	        for(int i=0, n= fields.length; i < n; i++) {
	            builder.append(fields[i].toString(indentLevel + 1));
	        }
	        convert.newLine(builder,indentLevel);
	        builder.append("}");
	        return builder.toString();
	    }

	    /* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			final int PRIME = 31;
			int result = super.hashCode();
			result = PRIME * result + Arrays.hashCode(getFields());
			return result;
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!super.equals(obj))
				return false;
			if (DynamicSubsetOfStructure.this.getClass() != obj.getClass())
				return false;
			final DynamicSubsetOfStructure other = (DynamicSubsetOfStructure) obj;
			if (!Arrays.equals(getFields(), other.getFields()))
				return false;
			return true;
		}
		
    	
    }

    private PVStructure supersetStructure;
    private LinkedHashMap<String, PVField> pvFieldsMap = new LinkedHashMap<String, PVField>();
    private PVField[] cachedPVFieldsArray = null;
	
    private static final Field DUMMY_FIELD = FieldFactory.getFieldCreate().createStructure(null, new Field[0]); 
	
    public DynamicSubsetOfPVStructure(PVStructure supersetStructure) {
    	// not nice...
    	super(null, DUMMY_FIELD);
		replaceField(new DynamicSubsetOfStructure());
		this.supersetStructure = supersetStructure;
	}
	
	/* (non-Javadoc)
     * @see org.epics.pvData.factory.AbstractPVField#postPut()
     */
    public void postPut() {
        super.postPut();
        for(PVField pvField : pvFieldsMap.values()) {
            postPutNoParent((AbstractPVField)pvField);
        }
    }

    /* (non-Javadoc)
     * @see org.epics.pvData.pv.PVStructure#postPut(org.epics.pvData.pv.PVField)
     */
    public void postPut(PVField subField) {
        Iterator<PVListener> iter;
        iter = super.pvListenerList.iterator();
        while(iter.hasNext()) {
            PVListener pvListener = iter.next();
            pvListener.dataPut(this,subField);
        }
        PVStructure pvParent = super.getParent();
        if(pvParent!=null) pvParent.postPut(subField);
    }
    
    /**
     * Clear (remove all fields) from this structure.
     */
    public void clear() {
    	pvFieldsMap.clear();
    	changed();
    }

    /**
     * Notify structire change. Used to reset cache.
     */
    private void changed() {
		cachedPVFieldsArray = null;
		((DynamicSubsetOfStructure)getField()).changed();
    }
	/* (non-Javadoc)
	 * @see org.epics.pvData.pv.PVStructure#replacePVField(java.lang.String, org.epics.pvData.pv.PVField)
	 */
	public void replacePVField(String fieldName, PVField newPVField) {
		pvFieldsMap.remove(fieldName);
		appendPVField(newPVField);
	}

	/* (non-Javadoc)
	 * @see org.epics.pvData.pv.PVStructure#removePVField(java.lang.String)
	 */
	public void removePVField(String fieldName) {
		pvFieldsMap.remove(fieldName);
		changed();
	}
	
	/* (non-Javadoc)
	 * @see org.epics.pvData.pv.PVStructure#appendPVField(org.epics.pvData.pv.PVField)
	 */
	public void appendPVField(PVField pvField) {
		pvFieldsMap.put(pvField.getFullFieldName(), pvField);
		changed();
	}

	/* (non-Javadoc)
	 * @see org.epics.pvData.pv.PVStructure#getPVFields()
	 */
	public PVField[] getPVFields() {
		if (cachedPVFieldsArray == null)
			cachedPVFieldsArray = pvFieldsMap.values().toArray(new PVField[pvFieldsMap.size()]);
		return cachedPVFieldsArray;
	}

	/* (non-Javadoc)
	 * @see org.epics.pvData.pv.PVStructure#getStructure()
	 */
	public Structure getStructure() {
		return (Structure)getField();
	}

	/* (non-Javadoc)
	 * @see org.epics.pvData.pv.PVStructure#getSubField(java.lang.String)
	 */
	public PVField getSubField(String fieldName) {
		return findSubField(fieldName,this);
	}

    private PVField findSubField(String fieldName,PVStructure pvStructure) {
        if(fieldName==null || fieldName.length()<1) return null;
        int index = fieldName.indexOf('.');
        String name = fieldName;
        String restOfName = null;
        if(index>0) {
            name = fieldName.substring(0, index);
            if(fieldName.length()>index) {
                restOfName = fieldName.substring(index+1);
            }
        }
        // TODO
        //PVField pvField = pvFieldsMap.get(name);

        PVField[] pvFields = pvStructure.getPVFields();
        PVField pvField = null;
        for(PVField pvf : pvFields) {
            if(pvf.getField().getFieldName().equals(name)) {
                pvField = pvf;
                break;
            }
        }
        if(pvField==null) return null;
        if(restOfName==null) return pvField;
        if(pvField.getField().getType()!=Type.structure) return null;
        return findSubField(restOfName,(PVStructure)pvField);
    }
    
    /* (non-Javadoc)
     * @see org.epics.pvData.pv.PVStructure#getBooleanField(java.lang.String)
     */
    public PVBoolean getBooleanField(String fieldName) {
        PVField pvField = findSubField(fieldName,this);
        if(pvField==null) return null;
        if(pvField.getField().getType()==Type.scalar) {
            Scalar scalar = (Scalar)pvField.getField();
            if(scalar.getScalarType()==ScalarType.pvBoolean) {
                return (PVBoolean)pvField;
            }
        }
        super.message("fieldName " + fieldName + " does not have type boolean ",
                MessageType.error);
        return null;
    }
    /* (non-Javadoc)
     * @see org.epics.pvData.pv.PVStructure#getByteField(java.lang.String)
     */
    public PVByte getByteField(String fieldName) {
        PVField pvField = findSubField(fieldName,this);
        if(pvField==null) return null;
        if(pvField.getField().getType()==Type.scalar) {
            Scalar scalar = (Scalar)pvField.getField();
            if(scalar.getScalarType()==ScalarType.pvByte) {
                return (PVByte)pvField;
            }
        }
        super.message("fieldName " + fieldName + " does not have type byte ",
                MessageType.error);
        return null;
    }
    /* (non-Javadoc)
     * @see org.epics.pvData.pv.PVStructure#getShortField(java.lang.String)
     */
    public PVShort getShortField(String fieldName) {
        PVField pvField = findSubField(fieldName,this);
        if(pvField==null) return null;
        if(pvField.getField().getType()==Type.scalar) {
            Scalar scalar = (Scalar)pvField.getField();
            if(scalar.getScalarType()==ScalarType.pvShort) {
                return (PVShort)pvField;
            }
        }
        super.message("fieldName " + fieldName + " does not have type short ",
                MessageType.error);
        return null;
    }
    /* (non-Javadoc)
     * @see org.epics.pvData.pv.PVStructure#getIntField(java.lang.String)
     */
    public PVInt getIntField(String fieldName) {
        PVField pvField = findSubField(fieldName,this);
        if(pvField==null) return null;
        if(pvField.getField().getType()==Type.scalar) {
            Scalar scalar = (Scalar)pvField.getField();
            if(scalar.getScalarType()==ScalarType.pvInt) {
                return (PVInt)pvField;
            }
        }
        super.message("fieldName " + fieldName + " does not have type int ",
                MessageType.error);
        return null;
    }
    /* (non-Javadoc)
     * @see org.epics.pvData.pv.PVStructure#getLongField(java.lang.String)
     */
    public PVLong getLongField(String fieldName) {
        PVField pvField = findSubField(fieldName,this);
        if(pvField==null) return null;
        if(pvField.getField().getType()==Type.scalar) {
            Scalar scalar = (Scalar)pvField.getField();
            if(scalar.getScalarType()==ScalarType.pvLong) {
                return (PVLong)pvField;
            }
        }
        super.message("fieldName " + fieldName + " does not have type long ",
                MessageType.error);
        return null;
    }
    /* (non-Javadoc)
     * @see org.epics.pvData.pv.PVStructure#getFloatField(java.lang.String)
     */
    public PVFloat getFloatField(String fieldName) {
        PVField pvField = findSubField(fieldName,this);
        if(pvField==null) return null;
        if(pvField.getField().getType()==Type.scalar) {
            Scalar scalar = (Scalar)pvField.getField();
            if(scalar.getScalarType()==ScalarType.pvFloat) {
                return (PVFloat)pvField;
            }
        }
        super.message("fieldName " + fieldName + " does not have type float ",
                MessageType.error);
        return null;
    }
    /* (non-Javadoc)
     * @see org.epics.pvData.pv.PVStructure#getDoubleField(java.lang.String)
     */
    public PVDouble getDoubleField(String fieldName) {
        PVField pvField = findSubField(fieldName,this);
        if(pvField==null) return null;
        if(pvField.getField().getType()==Type.scalar) {
            Scalar scalar = (Scalar)pvField.getField();
            if(scalar.getScalarType()==ScalarType.pvDouble) {
                return (PVDouble)pvField;
            }
        }
        super.message("fieldName " + fieldName + " does not have type double ",
                MessageType.error);
        return null;
    }
    /* (non-Javadoc)
     * @see org.epics.pvData.pv.PVStructure#getStringField(java.lang.String)
     */
    public PVString getStringField(String fieldName) {
        PVField pvField = findSubField(fieldName,this);
        if(pvField==null) return null;
        if(pvField.getField().getType()==Type.scalar) {
            Scalar scalar = (Scalar)pvField.getField();
            if(scalar.getScalarType()==ScalarType.pvString) {
                return (PVString)pvField;
            }
        }
        super.message("fieldName " + fieldName + " does not have type string ",
                MessageType.error);
        return null;
    }
    /* (non-Javadoc)
     * @see org.epics.pvData.pv.PVStructure#getStructureField(java.lang.String)
     */
    public PVStructure getStructureField(String fieldName) {
        PVField pvField = findSubField(fieldName,this);
        if(pvField==null) return null;
        Field field = pvField.getField();
        Type type = field.getType();
        if(type!=Type.structure) {
            super.message(
                "fieldName " + fieldName + " does not have type structure ",
                MessageType.error);
            return null;
        }
        return (PVStructure)pvField;
    }
    /* (non-Javadoc)
     * @see org.epics.pvData.pv.PVStructure#getArrayField(java.lang.String, org.epics.pvData.pv.ScalarType)
     */
    public PVArray getArrayField(String fieldName, ScalarType elementType) {
        PVField pvField = findSubField(fieldName,this);
        if(pvField==null) return null;
        Field field = pvField.getField();
        Type type = field.getType();
        if(type!=Type.scalarArray) {
            super.message(
                "fieldName " + fieldName + " does not have type array ",
                MessageType.error);
            return null;
        }
        Array array = (Array)field;
        if(array.getElementType()!=elementType) {
            super.message(
                    "fieldName "
                    + fieldName + " is array but does not have elementType " + elementType.toString(),
                    MessageType.error);
                return null;
        }
        return (PVArray)pvField;
    }
    
	/* (non-Javadoc)
	 * @see org.epics.pvData.pv.Serializable#serialize(java.nio.ByteBuffer)
	 */
	public void serialize(ByteBuffer buffer) {
        for (PVField pvField : pvFieldsMap.values())
        	pvField.serialize(buffer);
	}
	/* (non-Javadoc)
	 * @see org.epics.pvData.pv.Serializable#deserialize(java.nio.ByteBuffer)
	 */
	public void deserialize(ByteBuffer buffer) {
        for (PVField pvField : pvFieldsMap.values())
        	pvField.deserialize(buffer);
	}

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        String prefix = "structure " + super.getField().getFieldName();
        return toString(prefix,0);
    }
    /* (non-Javadoc)
     * @see org.epics.pvData.factory.AbstractPVField#toString(int)
     */
    public String toString(int indentLevel) {
        return toString("structure",indentLevel);
    }       
    /**
     * Called by BasePVRecord.
     * @param prefix A prefix for the generated stting.
     * @param indentLevel The indentation level.
     * @return String showing the PVStructure.
     */
    protected String toString(String prefix,int indentLevel) {
        return getString(prefix,indentLevel);
    }
    
    private String getString(String prefix,int indentLevel) {
        StringBuilder builder = new StringBuilder();
        builder.append(prefix);
        builder.append(super.toString(indentLevel));
        convert.newLine(builder,indentLevel);
        builder.append("{");
        for(PVField pvField : pvFieldsMap.values()) {
            convert.newLine(builder,indentLevel + 1);
            //Field field = pvField.getField();
            //builder.append(field.getFieldName() + " = ");
            builder.append(pvField.getFullFieldName() + " = ");
            builder.append(pvField.toString(indentLevel + 1));            
        }
        convert.newLine(builder,indentLevel);
        builder.append("}");
        return builder.toString();
    }
    
    /* (non-Javadoc)
	 * @see org.epics.pvData.pv.PVStructure#getExtendsStructureName()
	 */
	public String getExtendsStructureName() {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.epics.pvData.pv.PVStructure#putExtendsStructureName(java.lang.String)
	 */
	public boolean putExtendsStructureName(String extendsStructureName) {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.epics.pvData.pv.Serializable#serialize(java.nio.ByteBuffer, int, int)
	 */
	public void serialize(ByteBuffer buffer, int offset, int count) {
		serialize(buffer);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + Arrays.hashCode(cachedPVFieldsArray);
		result = PRIME * result + ((supersetStructure == null) ? 0 : supersetStructure.hashCode());
		result = PRIME * result + ((pvFieldsMap == null) ? 0 : pvFieldsMap.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (this.getClass() != obj.getClass())
			return false;
		final DynamicSubsetOfPVStructure other = (DynamicSubsetOfPVStructure) obj;
		if (supersetStructure == null) {
			if (other.supersetStructure != null)
				return false;
		} else if (!supersetStructure.equals(other.supersetStructure))
			return false;
		if (pvFieldsMap == null) {
			if (other.pvFieldsMap != null)
				return false;
		} else if (!pvFieldsMap.equals(other.pvFieldsMap))
			return false;
		return true;
	}

}