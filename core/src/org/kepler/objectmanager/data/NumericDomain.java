/*
 * Copyright (c) 2003-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: welker $'
 * '$Date: 2010-05-05 22:21:26 -0700 (Wed, 05 May 2010) $' 
 * '$Revision: 24234 $'
 * 
 * Permission is hereby granted, without written agreement and without
 * license or royalty fees, to use, copy, modify, and distribute this
 * software and its documentation for any purpose, provided that the above
 * copyright notice and the following two paragraphs appear in all copies
 * of this software.
 *
 * IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY
 * FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES
 * ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF
 * THE UNIVERSITY OF CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 *
 * THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE SOFTWARE
 * PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE UNIVERSITY OF
 * CALIFORNIA HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES,
 * ENHANCEMENTS, OR MODIFICATIONS.
 *
 */

package org.kepler.objectmanager.data;

/**
 * @author tao This class represents a numberic domain
 */
public class NumericDomain implements Domain {
	private String numberType = null;
	private DataType dataType = null;
	private double precision = 0;
	private Double minimum = null;
	private Double maxmum = null;
	private DataTypeResolver resolver = DataTypeResolver.instanceOf();

	/**
	 * Constructor of numeric domain
	 * 
	 * @param numberType
	 * @param minimum
	 * @param maxmum
	 * @throws UnresolvableTypeException
	 */
	public NumericDomain(String numberType, Double minimum, Double maxmum)
			throws UnresolvableTypeException {
		this.numberType = numberType;
		this.minimum = minimum;
		this.maxmum = maxmum;
		dataType = resolver.resolveDataType(this.numberType, this.minimum,
				this.maxmum);
	}

	/**
	 * Method to get data type
	 */
	public DataType getDataType() {
		return dataType;
	}

	/**
	 * @return Returns the maxmum.
	 */
	public Double getMaxmum() {
		return maxmum;
	}

	/**
	 * @param maxmum
	 *            The maxmum to set.
	 */
	public void setMaxmum(Double maxmum) {
		this.maxmum = maxmum;
	}

	/**
	 * @return Returns the minimum.
	 */
	public Double getMinimum() {
		return minimum;
	}

	/**
	 * @param minimum
	 *            The minimum to set.
	 */
	public void setMinimum(Double minimum) {
		this.minimum = minimum;
	}

	/**
	 * @return Returns the precision.
	 */
	public double getPrecision() {
		return precision;
	}

	/**
	 * @param precision
	 *            The precision to set.
	 */
	public void setPrecision(double precision) {
		this.precision = precision;
	}

}