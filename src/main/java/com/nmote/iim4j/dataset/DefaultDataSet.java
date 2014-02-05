/*
 * Copyright (c) Nmote Ltd. 2004-2014. All rights reserved. 
 * See LICENSE doc in a root of project folder for additional information.
 */

package com.nmote.iim4j.dataset;

import com.nmote.iim4j.serialize.DefaultSerializationContext;
import com.nmote.iim4j.serialize.SerializationException;

/**
 * DefaultDataSet is default implementation of DataSet interface.
 */
public class DefaultDataSet implements DataSet {

	/**
	 * Creates DataSet instance
	 * 
	 * @param info
	 * @param data
	 * @throws NullPointerException
	 *             if info or data is null
	 */
	public DefaultDataSet(DataSetInfo info, byte[] data) {
		setInfo(info);
		setData(data);
	}

	/**
	 * Subclass can skip setting data in constructor as it could be lazily
	 * initialized.
	 * 
	 * @param info
	 */
	protected DefaultDataSet(DataSetInfo info) {
		setInfo(info);
	}

	/**
	 * @return
	 */
	public DataSetInfo getInfo() {
		return info;
	}

	/**
	 * @return
	 */
	public byte[] getData() {
		return data;
	}

	/**
	 * @param bs
	 * @throws NullPointerException
	 *             if info is null
	 */
	public void setData(byte[] data) {
		if (data == null) {
			throw new NullPointerException("data == null");
		}
		this.data = data;
	}

	/**
	 * @param info
	 * @throws NullPointerException
	 *             if info is null
	 */
	public void setInfo(DataSetInfo info) {
		if (info == null) {
			throw new NullPointerException("info == null");
		}
		this.info = info;
	}

	/**
	 * @see com.nmote.iim4j.dataset.DataSet#getLength()
	 */
	public int getLength() {
		return data.length;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append("DataSet(");
		b.append(info);
		b.append(", name=").append(info.getName());
		b.append(", length=").append(getLength());
		try {
			Object dd = info.getSerializer().deserialize(getData(), new DefaultSerializationContext());
			if (dd instanceof byte[])
				dd = "[bytes]";
			b.append(", data=").append(dd);
		} catch (Exception e) {
			b.append(", data=").append("<error: ").append(e.toString()).append('>');
		}
		b.append(')');
		return b.toString();
	}

	public Object getValue() throws SerializationException {
		Object value;
		value = info.getSerializer().deserialize(getData(), new DefaultSerializationContext());
		return value;
	}

	private DataSetInfo info;
	private byte[] data;
}
