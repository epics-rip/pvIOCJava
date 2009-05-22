/*
 * Copyright (c) 2009 by Cosylab
 *
 * The full license specifying the redistribution, modification, usage and other
 * rights and obligations is included with the distribution of this project in
 * the file "LICENSE-CAJ". If the license is not included visit Cosylab web site,
 * <http://www.cosylab.com>.
 *
 * THIS SOFTWARE IS PROVIDED AS-IS WITHOUT WARRANTY OF ANY KIND, NOT EVEN THE
 * IMPLIED WARRANTY OF MERCHANTABILITY. THE AUTHOR OF THIS SOFTWARE, ASSUMES
 * _NO_ RESPONSIBILITY FOR ANY CONSEQUENCE RESULTING FROM THE USE, MODIFICATION,
 * OR REDISTRIBUTION OF THIS SOFTWARE.
 */

package org.epics.ioc.caV4;

import org.epics.ca.CAException;
import org.epics.ca.CAStatus;
import org.epics.ca.CAStatusException;
import org.epics.pvData.pv.Array;
import org.epics.pvData.pv.BooleanArrayData;
import org.epics.pvData.pv.ByteArrayData;
import org.epics.pvData.pv.DoubleArrayData;
import org.epics.pvData.pv.Field;
import org.epics.pvData.pv.FloatArrayData;
import org.epics.pvData.pv.IntArrayData;
import org.epics.pvData.pv.LongArrayData;
import org.epics.pvData.pv.PVBoolean;
import org.epics.pvData.pv.PVBooleanArray;
import org.epics.pvData.pv.PVByte;
import org.epics.pvData.pv.PVByteArray;
import org.epics.pvData.pv.PVDouble;
import org.epics.pvData.pv.PVDoubleArray;
import org.epics.pvData.pv.PVField;
import org.epics.pvData.pv.PVFloat;
import org.epics.pvData.pv.PVFloatArray;
import org.epics.pvData.pv.PVInt;
import org.epics.pvData.pv.PVIntArray;
import org.epics.pvData.pv.PVLong;
import org.epics.pvData.pv.PVLongArray;
import org.epics.pvData.pv.PVShort;
import org.epics.pvData.pv.PVShortArray;
import org.epics.pvData.pv.PVString;
import org.epics.pvData.pv.PVStringArray;
import org.epics.pvData.pv.PVStructure;
import org.epics.pvData.pv.Scalar;
import org.epics.pvData.pv.ShortArrayData;
import org.epics.pvData.pv.StringArrayData;

/**
 * PVData utilities.
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $Id$
 */
public class PVDataUtils {

	/**
	 * Copy data (no conversion is done).
	 * @param from
	 * @param to
	 * @param offset
	 * @throws CAException
	 */
	public static final void copyValue(PVField from, PVField to, int fromOffset, int toOffset, int count) throws CAException
	{
		final Field field = to.getField();
		if (!field.equals(from.getField()))
			// TODO better exception
			throw new CAStatusException(CAStatus.BADTYPE, "data not compatible");
		
		switch (field.getType())
		{
			case scalar:
				switch (((Scalar)field).getScalarType())
				{
				case pvBoolean:
					((PVBoolean)to).put(((PVBoolean)from).get());
					return;
				case pvByte:
					((PVByte)to).put(((PVByte)from).get());
					return;
				case pvDouble:
					((PVDouble)to).put(((PVDouble)from).get());
					return;
				case pvFloat:
					((PVFloat)to).put(((PVFloat)from).get());
					return;
				case pvInt:
					((PVInt)to).put(((PVInt)from).get());
					return;
				case pvLong:
					((PVLong)to).put(((PVLong)from).get());
					return;
				case pvShort:
					((PVShort)to).put(((PVShort)from).get());
					return;
				case pvString:
					((PVString)to).put(((PVString)from).get());
					return;
				default:
					throw new CAStatusException(CAStatus.NOSUPPORT, "type not supported");
				}
			case scalarArray:
				switch (((Array)field).getElementType())
				{
				case pvBoolean:
					{
						BooleanArrayData data = new BooleanArrayData();
						PVBooleanArray fromArray = (PVBooleanArray)from;
						int len = (count < 0) ? fromArray.getLength() : count;
						len = fromArray.get(fromOffset, len, data);
						((PVBooleanArray)to).put(toOffset, len, data.data, data.offset);
						return;
					}
				case pvByte:
					{
						ByteArrayData data = new ByteArrayData();
						PVByteArray fromArray = (PVByteArray)from;
						int len = (count < 0) ? fromArray.getLength() : count;
						len = fromArray.get(fromOffset, len, data);
						((PVByteArray)to).put(toOffset, len, data.data, data.offset);
						return;
					}
				case pvDouble:
					{
						DoubleArrayData data = new DoubleArrayData();
						PVDoubleArray fromArray = (PVDoubleArray)from;
						int len = (count < 0) ? fromArray.getLength() : count;
						len = fromArray.get(fromOffset, len, data);
						((PVDoubleArray)to).put(toOffset, len, data.data, data.offset);
						return;
					}
				case pvFloat:
					{
						FloatArrayData data = new FloatArrayData();
						PVFloatArray fromArray = (PVFloatArray)from;
						int len = (count < 0) ? fromArray.getLength() : count;
						len = fromArray.get(fromOffset, len, data);
						((PVFloatArray)to).put(toOffset, len, data.data, data.offset);
						return;
					}
				case pvInt:
					{
						IntArrayData data = new IntArrayData();
						PVIntArray fromArray = (PVIntArray)from;
						int len = (count < 0) ? fromArray.getLength() : count;
						len = fromArray.get(fromOffset, len, data);
						((PVIntArray)to).put(toOffset, len, data.data, data.offset);
						return;
					}
				case pvLong:
					{
						LongArrayData data = new LongArrayData();
						PVLongArray fromArray = (PVLongArray)from;
						int len = (count < 0) ? fromArray.getLength() : count;
						len = fromArray.get(fromOffset, len, data);
						((PVLongArray)to).put(toOffset, len, data.data, data.offset);
						return;
					}
				case pvShort:
					{
						ShortArrayData data = new ShortArrayData();
						PVShortArray fromArray = (PVShortArray)from;
						int len = (count < 0) ? fromArray.getLength() : count;
						len = fromArray.get(fromOffset, len, data);
						((PVShortArray)to).put(toOffset, len, data.data, data.offset);
						return;
					}
				case pvString:
					{
						StringArrayData data = new StringArrayData();
						PVStringArray fromArray = (PVStringArray)from;
						int len = (count < 0) ? fromArray.getLength() : count;
						len = fromArray.get(fromOffset, len, data);
						((PVStringArray)to).put(toOffset, len, data.data, data.offset);
						return;
					}
				default:
					throw new CAStatusException(CAStatus.NOSUPPORT, "type not supported");
				}

			case structure:
				PVStructure fromStructure = (PVStructure)from;
				PVStructure toStructure = (PVStructure)to;
				for (PVField fromField : fromStructure.getPVFields())
				{
					PVField toField = toStructure.getSubField(fromField.getField().getFieldName());
					if (toField != null)
						copyValue(fromField, toField, 0, 0, -1);
					// TODO really? else do not complain here..
				}
				break;
					
			default:
				throw new CAStatusException(CAStatus.NOSUPPORT, "type not supported");
		}
	}
	
}
