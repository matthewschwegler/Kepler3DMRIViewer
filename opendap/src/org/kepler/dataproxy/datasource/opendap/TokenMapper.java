/////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2009 OPeNDAP, Inc.
// All rights reserved.
// Permission is hereby granted, without written agreement and without
// license or royalty fees, to use, copy, modify, and distribute this
// software and its documentation for any purpose, provided that the above
// copyright notice and the following two paragraphs appear in all copies
// of this software.
//
// IN NO EVENT SHALL OPeNDAP BE LIABLE TO ANY PARTY FOR DIRECT, INDIRECT,
// SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES ARISING OUT OF THE USE OF
// THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF OPeNDAP HAS BEEN ADVISED
// OF THE POSSIBILITY OF SUCH DAMAGE.
//
// OPeNDAP SPECIFICALLY DISCLAIMS ANY WARRANTIES, INCLUDING, BUT NOT
// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
// PARTICULAR PURPOSE. THE SOFTWARE PROVIDED HEREUNDER IS ON AN "AS IS"
// BASIS, AND OPeNDAP HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT,
// UPDATES, ENHANCEMENTS, OR MODIFICATIONS.
//
// Author: Nathan David Potter  <ndp@opendap.org>
// You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.
//
/////////////////////////////////////////////////////////////////////////////

package org.kepler.dataproxy.datasource.opendap;

import java.util.Enumeration;
import java.util.Vector;

import opendap.dap.DArray;
import opendap.dap.DArrayDimension;
import opendap.dap.DByte;
import opendap.dap.DConstructor;
import opendap.dap.DFloat32;
import opendap.dap.DFloat64;
import opendap.dap.DGrid;
import opendap.dap.DInt16;
import opendap.dap.DInt32;
import opendap.dap.DSequence;
import opendap.dap.DString;
import opendap.dap.DStructure;
import opendap.dap.DUInt16;
import opendap.dap.DUInt32;
import opendap.dap.DURL;
import opendap.dap.PrimitiveVector;
import opendap.dap.Server.InvalidParameterException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ptolemy.data.ArrayToken;
import ptolemy.data.DoubleMatrixToken;
import ptolemy.data.DoubleToken;
import ptolemy.data.IntMatrixToken;
import ptolemy.data.IntToken;
import ptolemy.data.LongMatrixToken;
import ptolemy.data.LongToken;
import ptolemy.data.RecordToken;
import ptolemy.data.StringToken;
import ptolemy.data.Token;
import ptolemy.data.UnsignedByteToken;
import ptolemy.kernel.util.IllegalActionException;

public class TokenMapper {

	static Log log;
	static {
		log = LogFactory
				.getLog("org.kepler.dataproxy.datasource.opendap.CombinedMetaDataActorTest");
	}

	public static String metadataName = "metadata";
	public static String valueName = "value";

	/**
	 * Maps a DAP data object to a ptII Token object.
	 * 
	 * @param dapBT
	 *            The DAP obect to map.
	 * @param addMetadataRecord
	 *            controls whether or not dap Attribute metadata is added to the
	 *            returned kepler records.
	 * @return A new ptII Token that contains the value(s) of the passed DAP
	 *         object.
	 * @throws IllegalActionException
	 *             When bad things happen.
	 */
	public static ptolemy.data.Token mapDapObjectToToken(
			opendap.dap.BaseType dapBT, boolean addMetadataRecord)
			throws IllegalActionException {

		return mapDapObjectToToken(dapBT, null, 0, addMetadataRecord);

	}

	/**
	 * This helper method translates a single member of a DAP array to a ptII
	 * Token object.
	 * 
	 * @param pv
	 *            The array storage component of the DAP array.
	 * @param index
	 *            The index of the desired value in the PrimitiveVector.
	 * @return A new ptII Token that contains the value of the PrimitiveVector
	 *         located at index.
	 * @throws IllegalActionException
	 *             When bad things happen.
	 */
	private static ptolemy.data.Token mapPrimtiveVectorMemberToToken(
			PrimitiveVector pv, int index) throws IllegalActionException {

		return mapDapObjectToToken(pv.getTemplate(), pv.getInternalStorage(),
				index, false);

	}

	/**
	 * Provides the core mapping of DAP objects to ptII tokens. This method
	 * creates the ptII token and sets it's value to that of the passed DAP
	 * object. If the DAP object to be mapped was a memeber of a DAP array, then
	 * <code>primitiveArray</code> is cast to the appropriate type and the
	 * <code>index</code> parameter is used to pluck the correct value from the
	 * <code>primitiveArray</code>.
	 * 
	 * @param dapBT
	 *            BaseType that needs it's value mapped to a ptII Token object.
	 * @param primitiveArray
	 *            If the item to be mapped came from an DAP array then this is
	 *            the internal storage of the Dap array class. This should be a
	 *            null if not used.
	 * @param index
	 *            The index in the internal storage array of the value to map.
	 * @param addMetadataRecord
	 *            controls whether or not dap Attribute metadata is added to the
	 *            returned kepler records.
	 * @return A ptII Token object loaded with the data values help in the
	 *         parameter dapBT
	 * @throws IllegalActionException
	 *             When bad things happen.
	 */
	private static ptolemy.data.Token mapDapObjectToToken(
			opendap.dap.BaseType dapBT, Object primitiveArray, int index,
			boolean addMetadataRecord) throws IllegalActionException {

		ptolemy.data.Token token = null;
		ptolemy.data.Token valueToken = null;
		ptolemy.data.Token attToken;

		if (dapBT instanceof DConstructor) {

			if (dapBT instanceof DStructure) {
				DStructure struct = (DStructure) dapBT;
				if (primitiveArray != null) {
					struct = (DStructure) (((opendap.dap.BaseType[]) primitiveArray)[index]);
				}
				token = dapStructureToRecordToken(struct, addMetadataRecord);

			} else if (dapBT instanceof DGrid) {
				DGrid grid = (DGrid) dapBT;
				if (primitiveArray != null) {
					grid = (DGrid) (((opendap.dap.BaseType[]) primitiveArray)[index]);
				}

				token = dapGridToRecordToken(grid, addMetadataRecord);

			} else if (dapBT instanceof DSequence) {
				DSequence seq = (DSequence) dapBT;
				if (primitiveArray != null) {
					seq = (DSequence) (((opendap.dap.BaseType[]) primitiveArray)[index]);
				}
				token = dapSequenceToArrayOfRecordTokens(seq, addMetadataRecord);

			}

		} else if (dapBT instanceof DArray) {

			if (primitiveArray != null) {
				throw new IllegalActionException(
						"ERROR: OPeNDAP Should never allow nested Arrays!");
			}

			log.debug("DArray to be converted to tokens: "
					+ dapBT.getLongName());

			token = dapArrayToPtIIToken((DArray) dapBT, addMetadataRecord);

			log.debug("PtII Array is of type: " + token.getType()
					+ "  ClassName: " + token.getType().getClass().getName());
		} else {
			if (dapBT instanceof DByte) {
				DByte dbyte = (DByte) dapBT;
				if (primitiveArray != null) {
					dbyte.setValue(((byte[]) primitiveArray)[index]);
				}
				long tmp = ((long) dbyte.getValue()) & 0xFFL; // Mask for
																// unsigned
																// behavior
				valueToken = new UnsignedByteToken((byte) tmp);

			} else if (dapBT instanceof DUInt16) {
				DUInt16 ui16 = (DUInt16) dapBT;
				if (primitiveArray != null) {
					ui16.setValue(((short[]) primitiveArray)[index]);
				}
				long tmp = ((long) ui16.getValue()) & 0xFFFFL; // Mask for
																// unsigned
																// behavior

				valueToken = new IntToken((int) tmp);

			} else if (dapBT instanceof DInt16) {
				DInt16 i16 = (DInt16) dapBT;
				if (primitiveArray != null) {
					i16.setValue(((short[]) primitiveArray)[index]);
				}
				valueToken = new IntToken(i16.getValue());

			} else if (dapBT instanceof DUInt32) {
				DUInt32 ui32 = (DUInt32) dapBT;
				if (primitiveArray != null) {
					ui32.setValue(((int[]) primitiveArray)[index]);
				}
				long tmp = ((long) ui32.getValue()) & 0xFFFFFFFFL; // Mask for
																	// unsigned
																	// behavior

				valueToken = new LongToken(tmp);

			} else if (dapBT instanceof DInt32) {
				DInt32 i32 = (DInt32) dapBT;
				if (primitiveArray != null) {
					i32.setValue(((int[]) primitiveArray)[index]);
				}
				valueToken = new LongToken(i32.getValue());

			} else if (dapBT instanceof DFloat32) {
				DFloat32 f32 = (DFloat32) dapBT;
				if (primitiveArray != null) {
					f32.setValue(((float[]) primitiveArray)[index]);
				}
				valueToken = new DoubleToken(f32.getValue());

			} else if (dapBT instanceof DFloat64) {
				DFloat64 f64 = (DFloat64) dapBT;
				if (primitiveArray != null) {
					f64.setValue(((double[]) primitiveArray)[index]);
				}
				valueToken = new DoubleToken(f64.getValue());

			} else if (dapBT instanceof DURL) {
				DURL url = (DURL) dapBT;
				if (primitiveArray != null) {
					url
							.setValue(((DURL) ((opendap.dap.BaseType[]) primitiveArray)[index])
									.getValue());
				}
				valueToken = new StringToken(url.getValue());

			} else if (dapBT instanceof DString) {
				DString s = (DString) dapBT;
				if (primitiveArray != null) {
					s
							.setValue(((DString) ((opendap.dap.BaseType[]) primitiveArray)[index])
									.getValue());
				}
				valueToken = new StringToken(s.getValue());
			}

			if (valueToken == null)
				throw new IllegalActionException("Unrecognized DAP2 type. "
						+ "TypeName=" + dapBT.getTypeName() + "  VariableName="
						+ dapBT.getName());

			if (addMetadataRecord) {
				attToken = AttTypeMapper.convertAttributeToToken(dapBT
						.getAttribute());
				token = new RecordToken(
						new String[] { metadataName, valueName }, new Token[] {
								attToken, valueToken });
			} else
				token = valueToken;
		}

		if (token == null)
			throw new IllegalActionException("Unrecognized DAP2 type. "
					+ "TypeName=" + dapBT.getTypeName() + "  VariableName="
					+ dapBT.getName());

		return token;

	}

	/**
	 * Helper method maps a DAP Structure to a ptII RecordToken.
	 * 
	 * @param s
	 *            The DAP Structure to map.
	 * @param addMetadataRecord
	 *            controls whether or not dap Attribute metadata is added to the
	 *            returned kepler records.
	 * @return The RecordToken containing the values of the DAP Structure.
	 * @throws IllegalActionException
	 *             When bad things happen.
	 */
	private static RecordToken dapStructureToRecordToken(DStructure s,
			boolean addMetadataRecord) throws IllegalActionException {

		RecordToken rt;
		RecordToken token;

		Enumeration e = s.getVariables();

		Token[] tokens = new Token[s.elementCount()];
		String[] names = new String[s.elementCount()];

		opendap.dap.BaseType dapBT;
		int i = 0;
		while (e.hasMoreElements()) {
			dapBT = (opendap.dap.BaseType) e.nextElement();
			names[i] = TypeMapper.replacePeriods(dapBT.getName());
			tokens[i] = mapDapObjectToToken(dapBT, addMetadataRecord);
			i++;
		}

		rt = new RecordToken(names, tokens);

		if (addMetadataRecord) {
			Token attToken = AttTypeMapper.convertAttributeToToken(s
					.getAttribute());
			token = new RecordToken(new String[] { metadataName, valueName },
					new Token[] { attToken, rt });
		} else
			token = rt;

		return token;
	}

	/**
	 * Helper method maps a DAP Sequence to a ptII ArrayToken of RecordTokens.
	 * 
	 * @param s
	 *            The DAP Sequence to map.
	 * @param addMetadataRecord
	 *            controls whether or not dap Attribute metadata is added to the
	 *            returned kepler records.
	 * @return The ArrayToken containing the values of the DAP Sequence.
	 * @throws IllegalActionException
	 *             When bad things happen.
	 */
	private static Token dapSequenceToArrayOfRecordTokens(DSequence s,
			boolean addMetadataRecord) throws IllegalActionException {

		int rowCount = s.getRowCount();
		RecordToken[] rt = new RecordToken[rowCount];

		String[] names = new String[s.elementCount()];
		Token[] elements = new Token[s.elementCount()];
		Token token;
		Vector row;
		opendap.dap.BaseType bt;
		for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {

			row = s.getRow(rowIndex);
			for (int i = 0; i < row.size(); i++) {
				bt = (opendap.dap.BaseType) row.get(i);
				elements[i] = mapDapObjectToToken(bt, addMetadataRecord);
				names[i] = TypeMapper.replacePeriods(bt.getName());
			}

			rt[rowIndex] = new RecordToken(names, elements);

		}
		Token value = new ArrayToken(rt);

		if (addMetadataRecord) {
			Token attToken = AttTypeMapper.convertAttributeToToken(s
					.getAttribute());
			token = new RecordToken(new String[] { metadataName, valueName },
					new Token[] { attToken, value });
		} else
			token = value;

		return token;
	}

	/**
	 * Helper method maps a DAP Grid to a ptII RecordToken.
	 * 
	 * @param g
	 *            The DAP Grid to map.
	 * @param addMetadataRecord
	 *            controls whether or not dap Attribute metadata is added to the
	 *            returned kepler records.
	 * @return The RecordToken containing the values of the DAP Grid.
	 * @throws IllegalActionException
	 *             When bad things happen.
	 */
	private static RecordToken dapGridToRecordToken(DGrid g,
			boolean addMetadataRecord) throws IllegalActionException {

		RecordToken rt;
		RecordToken token;

		Enumeration e = g.getVariables();

		Token[] tokens = new Token[g.elementCount()];
		String[] names = new String[g.elementCount()];

		DArray array = (DArray) e.nextElement();
		tokens[0] = dapArrayToPtIIToken(array, addMetadataRecord);
		names[0] = TypeMapper.replacePeriods(array.getName());

		int i = 1;
		while (e.hasMoreElements()) {
			array = (DArray) e.nextElement();
			names[i] = TypeMapper.replacePeriods(array.getName());
			tokens[i] = dapArrayToPtIIToken(array, addMetadataRecord);
			i++;
		}

		rt = new RecordToken(names, tokens);

		if (addMetadataRecord) {
			Token attToken = AttTypeMapper.convertAttributeToToken(g
					.getAttribute());
			token = new RecordToken(new String[] { metadataName, valueName },
					new Token[] { attToken, rt });
		} else
			token = rt;

		return token;
	}

	/**
	 * Helper method maps a DAP arrays to a ptII Tokens. IF the DAP array is a
	 * 2D array then it is mapped to a MatrixToken. Otherwise it is mapped to a
	 * (possibly nested set of) ArrayToken(s).
	 * 
	 * @param a
	 *            The DAP Grid to map.
	 * @param addMetadataRecord
	 *            controls whether or not dap Attribute metadata is added to the
	 *            returned kepler records.
	 * @return The Token containing the values of the DAP array.
	 * @throws IllegalActionException
	 *             When bad things happen.
	 */
	private static Token dapArrayToPtIIToken(DArray a, boolean addMetadataRecord)
			throws IllegalActionException {

		if (a.numDimensions() == 1) {

			// if(a.getFirstDimension().getSize() == 1) {
			// return map1x1DapArray2Scalar(a);
			// }

			return mapDapArrayToMatrix(a, addMetadataRecord);

		}

		if (a.numDimensions() == 2)
			return mapDapArrayToMatrix(a, addMetadataRecord);

		Token[] values;
		try {
			values = buildArrayTokens(a);
		} catch (InvalidParameterException e) {
			throw new IllegalActionException("Can't build Array Tokens!");
		}

		Token valueToken = new ArrayToken(values);
		Token token = valueToken;
		if (addMetadataRecord) {
			Token attToken = AttTypeMapper.convertAttributeToToken(a
					.getAttribute());
			token = new RecordToken(new String[] { metadataName, valueName },
					new Token[] { attToken, valueToken });
		}

		return token;

	}

	private static Token[] buildArrayTokens(DArray a)
			throws InvalidParameterException, IllegalActionException {

		return buildArrayTokensRecurs(a, 0, null);
	}

	private static Token[] buildArrayTokensRecurs(DArray a, int dimension,
			int[] indices) throws InvalidParameterException,
			IllegalActionException {

		if (dimension == 0) {
			// log.debug("Array Map Starting.");
			indices = new int[a.numDimensions() - 1];

			for (int i = 0; i < a.numDimensions() - 1; i++)
				indices[i] = 0;
		}

		// log.debug("dimension: "+dimension);

		DArrayDimension thisAD = a.getDimension(dimension);
		int thisADSize = thisAD.getSize();
		Token[] values;

		if (dimension == a.numDimensions() - 1) {
			// log.debug("Building inner Row Tokens.");
			// showArray(indices);
			values = innerRowToTokens(a, indices);

		} else {
			// log.debug("Building new ArrayToken array.");

			values = new ArrayToken[thisADSize];

			for (int i = 0; i < thisADSize; i++) {
				indices[dimension] = i;
				// showArray(indices);
				values[i] = new ArrayToken(buildArrayTokensRecurs(a,
						dimension + 1, indices));
			}

			// log.debug("ArrayToken array built.");
			// showArray(indices);

		}

		return values;
	}

	public static void showArray(int[] a) {
		System.out.print("array={");
		for (int i = 0; i < a.length; i++) {
			if (i > 0)
				System.out.print(",");
			System.out.print(a[i]);
		}
		log.debug("}");

	}

	private static Token[] innerRowToTokens(DArray a, int[] indices)
			throws InvalidParameterException, IllegalActionException {

		int startIndex = 0;
		int innerRowSize = a.getDimension(a.numDimensions() - 1).getSize();
		long blockSize;

		// log.debug("begin  -  startIndex: "+startIndex);
		for (int i = 0; i < a.numDimensions() - 1; i++) {
			blockSize = 1;

			for (int j = i + 1; j < a.numDimensions(); j++)
				blockSize *= a.getDimension(j).getSize();

			startIndex += indices[i] * blockSize;
			// log.debug("process  -  startIndex: "+startIndex+
			// "   indices["+i+"]: "+indices[i]+ "   blockSize: "+blockSize);

		}
		// log.debug("FINAL  -  startIndex: "+startIndex);

		Token[] values = new Token[innerRowSize];
		PrimitiveVector pv = a.getPrimitiveVector();

		log.debug("Primitive Vector is: " + pv.getClass().getName());
		log.debug("Primitive Vector Internal Storage is: "
				+ pv.getInternalStorage().getClass().getName());

		int j = 0;
		for (int i = startIndex; i < startIndex + innerRowSize; i++) {
			values[j] = mapPrimtiveVectorMemberToToken(pv, i);
			j++;
		}

		return values;

	}

	private static Token mapDapArrayToMatrix(opendap.dap.DArray a,
			boolean addMetadataRecord) throws IllegalActionException {

		Token mtoken;
		long tmp;

		int numDims = a.numDimensions();

		if (numDims != 1 && numDims != 2)
			throw new IllegalActionException("Only 1D and 2D DAP arrays may "
					+ "be converted to a Matrix datatype.");

		try {

			PrimitiveVector pv = a.getPrimitiveVector();
			opendap.dap.BaseType dapBT = pv.getTemplate();
			Object is = pv.getInternalStorage();

			if (dapBT instanceof DByte) {

				byte[] dbyte = (byte[]) is;
				log
						.debug("Building a IntMatrixToken from a 2D DAP Byte array.");

				int dim0 = a.getDimension(0).getSize();
				int dim1 = 1;

				if (numDims == 2)
					dim1 = a.getDimension(1).getSize();

				int mvals[][] = new int[dim0][dim1];
				int k = 0;
				for (int i = 0; i < dim0; i++) {
					for (int j = 0; j < dim1; j++) {
						tmp = ((long) dbyte[k]) & 0xFFL; // Mask for unsigned
															// behavior
						mvals[i][j] = (int) tmp;
						k++;
					}
				}
				mtoken = new IntMatrixToken(mvals);

			} else if (dapBT instanceof DUInt16) {
				short[] ui16 = (short[]) is;
				log
						.debug("Building a IntMatrixToken from a 2D DAP UInt16 array.");

				int dim0 = a.getDimension(0).getSize();
				int dim1 = 1;

				if (numDims == 2)
					dim1 = a.getDimension(1).getSize();

				int mvals[][] = new int[dim0][dim1];
				int k = 0;
				for (int i = 0; i < dim0; i++) {
					for (int j = 0; j < dim1; j++) {
						tmp = ((long) ui16[k]) & 0xFFFFL; // Mask for unsigned
															// behavior
						mvals[i][j] = (int) tmp;
						k++;
					}
				}
				mtoken = new IntMatrixToken(mvals);

			} else if (dapBT instanceof DInt16) {
				short[] i16 = (short[]) is;
				log
						.debug("Building a IntMatrixToken from a 2D DAP Int16 array.");

				int dim0 = a.getDimension(0).getSize();
				int dim1 = 1;

				if (numDims == 2)
					dim1 = a.getDimension(1).getSize();

				int mvals[][] = new int[dim0][dim1];
				int k = 0;
				for (int i = 0; i < dim0; i++) {
					for (int j = 0; j < dim1; j++) {
						tmp = ((long) i16[k]);
						mvals[i][j] = (int) tmp;
						k++;
					}
				}
				mtoken = new IntMatrixToken(mvals);

			} else if (dapBT instanceof DUInt32) {
				int[] ui32 = (int[]) is;
				log
						.debug("Building a LongMatrixToken from a 2D DAP UInt32 array.");

				int dim0 = a.getDimension(0).getSize();
				int dim1 = 1;

				if (numDims == 2)
					dim1 = a.getDimension(1).getSize();

				long mvals[][] = new long[dim0][dim1];
				int k = 0;
				for (int i = 0; i < dim0; i++) {
					for (int j = 0; j < dim1; j++) {
						tmp = ((long) ui32[k]) & 0xFFFFFFFFL; // Mask for
																// unsigned
																// behavior
						mvals[i][j] = tmp;
						k++;
					}
				}
				mtoken = new LongMatrixToken(mvals);

			} else if (dapBT instanceof DInt32) {
				int[] i32 = (int[]) is;
				log
						.debug("Building a LongMatrixToken from a 2D DAP Int32 array.");

				int dim0 = a.getDimension(0).getSize();
				int dim1 = 1;

				if (numDims == 2)
					dim1 = a.getDimension(1).getSize();

				long mvals[][] = new long[dim0][dim1];
				int k = 0;
				for (int i = 0; i < dim0; i++) {
					for (int j = 0; j < dim1; j++) {
						tmp = ((long) i32[k]);
						mvals[i][j] = tmp;
						k++;
					}
				}
				mtoken = new LongMatrixToken(mvals);

			} else if (dapBT instanceof DFloat32) {
				float[] f32 = (float[]) is;
				log
						.debug("Building a DoubleMatrixToken from a 2D DAP Float32 array.");

				int dim0 = a.getDimension(0).getSize();
				int dim1 = 1;

				if (numDims == 2)
					dim1 = a.getDimension(1).getSize();

				double mvals[][] = new double[dim0][dim1];
				int k = 0;
				for (int i = 0; i < dim0; i++) {
					for (int j = 0; j < dim1; j++) {
						mvals[i][j] = f32[k];
						k++;
					}
				}
				mtoken = new DoubleMatrixToken(mvals);

			} else if (dapBT instanceof DFloat64) {
				double[] f64 = (double[]) is;
				log
						.debug("Building a DoubleMatrixToken from a 2D DAP Float32 array.");

				int dim0 = a.getDimension(0).getSize();
				int dim1 = 1;

				if (numDims == 2)
					dim1 = a.getDimension(1).getSize();

				double mvals[][] = new double[dim0][dim1];
				int k = 0;
				for (int i = 0; i < dim0; i++) {
					for (int j = 0; j < dim1; j++) {
						mvals[i][j] = f64[k];
						k++;
					}
				}
				mtoken = new DoubleMatrixToken(mvals);

			} else {
				// Other DAP data types have no reasonable corresponding
				// MatrixToken data type in ptII. So DString, DStructure,
				// DGrid, and DSequence all get stuffed in to non-optimized
				// arrays of arrays of ptII Tokens.

				mtoken = dapArrayToPtIIToken(a, addMetadataRecord);
			}

			Token token = mtoken;
			if (addMetadataRecord) {
				Token attToken = AttTypeMapper.convertAttributeToToken(a
						.getAttribute());
				token = new RecordToken(
						new String[] { metadataName, valueName }, new Token[] {
								attToken, mtoken });
			}

			return token;

		} catch (InvalidParameterException e) {
			throw new IllegalActionException("Failed to determine the size "
					+ "of a dimension of the DAP array \"" + a.getLongName()
					+ "\"");
		}
	}

}