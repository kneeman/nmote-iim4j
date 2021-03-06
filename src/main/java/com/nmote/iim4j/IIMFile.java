/*
 * Copyright (c) Nmote Ltd. 2004-2015. All rights reserved.
 * See LICENSE doc in a root of project folder for additional information.
 */

package com.nmote.iim4j;

import java.io.IOException;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.nmote.iim4j.dataset.ConstraintViolation;
import com.nmote.iim4j.dataset.DataSet;
import com.nmote.iim4j.dataset.DataSetInfo;
import com.nmote.iim4j.dataset.DataSetInfoFactory;
import com.nmote.iim4j.dataset.DefaultDataSet;
import com.nmote.iim4j.dataset.InvalidDataSetException;
import com.nmote.iim4j.dataset.UnsupportedDataSetException;
import com.nmote.iim4j.serialize.DefaultSerializationContext;
import com.nmote.iim4j.serialize.SerializationContext;
import com.nmote.iim4j.serialize.SerializationException;
import com.nmote.iim4j.serialize.Serializer;

/**
 * IIMFile holds a set of data set records, and supports reading and writing to
 * files/streams.
 */
public class IIMFile extends DefaultSerializationContext implements Serializable, Cloneable {

	private static final long serialVersionUID = About.SERIAL_VERSION_UID;

	/**
	 * Creates a new IIMFile using default (IIM version 4) data set info
	 * factory.
	 */
	public IIMFile() {
		this(IIMDataSetInfoFactory.VERSION_4);
	}

	/**
	 * Creates a new IIMFile using passed data set info factory.
	 *
	 * @param dsiFactory
	 *            data set info factory
	 */
	public IIMFile(DataSetInfoFactory dsiFactory) {
		this.dsiFactory = dsiFactory;
	}

	/**
	 * Adds a data set to IIM file.
	 *
	 * @param dataSet
	 *            to add
	 */
	public void add(DataSet dataSet) {
		dataSets.add(dataSet);
	}

	/**
	 * Adds a data set to IIM file.
	 *
	 * @param ds
	 *            data set id (see constants in IIM class)
	 * @param value
	 *            data set value. Null values are silently ignored.
	 * @throws SerializationException
	 *             if value can't be serialized by data set's serializer
	 * @throws InvalidDataSetException
	 *             if data set isn't defined
	 */
	public void add(int ds, Object value) throws SerializationException, InvalidDataSetException {
		if (value == null) {
			return;
		}

		DataSetInfo dsi = dsiFactory.create(ds);
		byte[] data = dsi.getSerializer().serialize(value, activeSerializationContext);
		DataSet dataSet = new DefaultDataSet(dsi, data);
		dataSets.add(dataSet);
	}

	/**
	 * Adds a data set with date-time value to IIM file.
	 *
	 * @param ds
	 *            data set id (see constants in IIM class)
	 * @param date
	 *            date to set. Null values are silently ignored.
	 * @throws SerializationException
	 *             if value can't be serialized by data set's serializer
	 * @throws InvalidDataSetException
	 *             if data set isn't defined
	 */
	public void addDateTimeHelper(int ds, Date date) throws SerializationException, InvalidDataSetException {
		if (date == null) {
			return;
		}

		DataSetInfo dsi = dsiFactory.create(ds);

		SimpleDateFormat df = new SimpleDateFormat(dsi.getSerializer().toString());
		String value = df.format(date);
		byte[] data = dsi.getSerializer().serialize(value, activeSerializationContext);
		DataSet dataSet = new DefaultDataSet(dsi, data);
		add(dataSet);
	}

	public void addDateTimeHelper(int dsDate, int dsTime, Date date) throws SerializationException,
			InvalidDataSetException {
		if (date == null)
			return;

		addDateTimeHelper(dsDate, date);
		addDateTimeHelper(dsTime, date);
	}

	/**
	 * Makes a copy of this instance.
	 *
	 * @see java.lang.Object#clone()
	 * @return IIMFile copy
	 */
	public IIMFile clone() {
		IIMFile file = new IIMFile(dsiFactory);
		file.dataSets = new ArrayList<DataSet>(dataSets);
		file.serializationContext = serializationContext == this ? file : serializationContext;
		file.activeSerializationContext = activeSerializationContext == this ? file : activeSerializationContext;
		return file;
	}

	/**
	 * Gets a first data set value.
	 *
	 * @param dataSet
	 *            IIM record and dataset code (See constants in {@link IIM})
	 * @return data set value
	 * @throws SerializationException
	 *             if value can't be deserialized from binary representation
	 */
	public Object get(int dataSet) throws SerializationException {
		Object result = null;
		for (Iterator<DataSet> i = dataSets.iterator(); i.hasNext();) {
			DataSet ds = i.next();
			DataSetInfo info = ds.getInfo();
			if (info.getDataSetNumber() == dataSet) {
				result = getData(ds);
				break;
			}
		}
		return result;
	}

	/**
	 * Gets all data set values.
	 *
	 * @param dataSet
	 *            IIM record and dataset code (See constants in {@link IIM})
	 * @return data set value
	 * @throws SerializationException
	 *             if value can't be deserialized from binary representation
	 */
	public List<Object> getAll(int dataSet) throws SerializationException {
		List<Object> result = new ArrayList<Object>();
		for (Iterator<DataSet> i = dataSets.iterator(); i.hasNext();) {
			DataSet ds = i.next();
			DataSetInfo info = ds.getInfo();
			if (info.getDataSetNumber() == dataSet) {
				result.add(getData(ds));
			}
		}
		return result;
	}

	/**
	 * Gets all data sets in IIM file.
	 *
	 * @return Returns the dataSets
	 */
	public List<DataSet> getDataSets() {
		return this.dataSets;
	}

	/**
	 * Gets combined date/time value from two data sets.
	 *
	 * @param dateDataSet
	 *            data set containing date value
	 * @param timeDataSet
	 *            data set containing time value
	 * @return date/time instance
	 * @throws SerializationException
	 *             if data sets can't be deserialized from binary format or
	 *             can't be parsed
	 */
	public Date getDateTimeHelper(int dateDataSet, int timeDataSet) throws SerializationException {
		DataSet dateDS = null;
		DataSet timeDS = null;
		for (Iterator<DataSet> i = dataSets.iterator(); (dateDS == null || timeDS == null) && i.hasNext();) {
			DataSet ds = i.next();
			DataSetInfo info = ds.getInfo();
			if (info.getDataSetNumber() == dateDataSet) {
				dateDS = ds;
			} else if (info.getDataSetNumber() == timeDataSet) {
				timeDS = ds;
			}
		}

		Date result = null;
		if (dateDS != null && timeDS != null) {
			DataSetInfo dateDSI = dateDS.getInfo();
			DataSetInfo timeDSI = timeDS.getInfo();
			SimpleDateFormat format = new SimpleDateFormat(dateDSI.getSerializer().toString()
					+ timeDSI.getSerializer().toString());
			StringBuffer date = new StringBuffer(20);
			try {
				date.append(getData(dateDS));
				date.append(getData(timeDS));
				result = format.parse(date.toString());
			} catch (ParseException e) {
				throw new SerializationException("Failed to read date (" + e.getMessage() + ") with format " + date);
			}
		}

		return result;
	}

	/**
	 * Gets serialization context for this IIM file instance.
	 *
	 * @return Returns the serializationContext
	 */
	public SerializationContext getSerializationContext() {
		return this.serializationContext;
	}

	public boolean isRecoverFromIIMFormat() {
		return recoverFromIIMFormat;
	}

	public boolean isRecoverFromInvalidDataSet() {
		return recoverFromInvalidDataSet;
	}

	public boolean isRecoverFromUnsupportedDataSet() {
		return recoverFromUnsupportedDataSet;
	}

	public boolean isStopAfter9_10() {
		return stopAfter9_10;
	}

	/**
	 * Reads data sets from a passed reader and attempt to recover from as much
	 * errors as possible.
	 *
	 * @param reader
	 *            data sets source
	 * @throws IOException
	 *             if reader can't read underlying stream
	 * @throws InvalidDataSetException
	 *             if invalid/undefined data set is encountered
	 */
	public void readFrom(IIMReader reader) throws IOException, InvalidDataSetException {
		readFrom(reader, Integer.MAX_VALUE);
	}

	/**
	 * Reads data sets from a passed reader.
	 *
	 * @param reader
	 *            data sets source
	 * @param recover
	 *            max number of errors reading process will try to recover from.
	 *            Set to 0 to fail immediately
	 * @throws IOException
	 *             if reader can't read underlying stream
	 * @throws InvalidDataSetException
	 *             if invalid/undefined data set is encountered
	 */
	public void readFrom(IIMReader reader, int recover) throws IOException, InvalidDataSetException {
		final boolean doLog = log != null;
		for (;;) {
			try {
				DataSet ds = reader.read();
				if (ds == null) {
					break;
				}

				if (doLog) {
					log.debug("Read data set " + ds);
				}

				DataSetInfo info = ds.getInfo();
				Serializer s = info.getSerializer();
				if (s != null) {
					if (info.getDataSetNumber() == IIM.DS(1, 90)) {
						setCharacterSet((String) s.deserialize(ds.getData(), activeSerializationContext));
					}
				}

				dataSets.add(ds);

				if (stopAfter9_10 && info.getDataSetNumber() == IIM.DS(9, 10))
					break;
			} catch (IIMFormatException e) {
				if (recoverFromIIMFormat && recover-- > 0) {
					boolean r = reader.recover();
					if (doLog) {
						log.debug(r ? "Recoved from " + e : "Failed to recover from " + e);
					}
					if (!r)
						break;
				} else {
					throw e;
				}
			} catch (UnsupportedDataSetException e) {
				if (recoverFromUnsupportedDataSet && recover-- > 0) {
					boolean r = reader.recover();
					if (doLog) {
						log.debug(r ? "Recoved from " + e : "Failed to recover from " + e);
					}
					if (!r)
						break;
				} else {
					throw e;
				}
			} catch (InvalidDataSetException e) {
				if (recoverFromInvalidDataSet && recover-- > 0) {
					boolean r = reader.recover();
					if (doLog) {
						log.debug(r ? "Recoved from " + e : "Failed to recover from " + e);
					}
					if (!r)
						break;
				} else {
					throw e;
				}
			} catch (IOException e) {
				if (recover-- > 0 && !dataSets.isEmpty()) {
					if (doLog) {
						log.error("IOException while reading, however some data sets where recovered, " + e);
					}
					return;
				} else {
					throw e;
				}
			}
		}
	}

	public boolean remove(int dataSet) {
		boolean result = false;
		for (Iterator<DataSet> i = dataSets.iterator(); i.hasNext();) {
			DataSet ds = i.next();
			DataSetInfo info = ds.getInfo();
			if (info.getDataSetNumber() == dataSet) {
				i.remove();
				result = true;
			}
		}
		return result;
	}

	public boolean removeRecord(int record) {
		boolean result = false;
		for (Iterator<DataSet> i = dataSets.iterator(); i.hasNext();) {
			DataSet ds = i.next();
			DataSetInfo info = ds.getInfo();
			if ((info.getDataSetNumber() >> 8) == record) {
				i.remove();
				result = true;
			}
		}
		return result;
	}

	/**
	 * @param dataSets
	 *            The dataSets to set.
	 */
	public void setDataSets(List<DataSet> dataSets) {
		this.dataSets = dataSets;
	}

	/**
	 * Sets logger for this IIMFile.
	 *
	 * @param log
	 *            logger to use with this file.
	 */
	public void setLog(LoggerAdapter log) {
		this.log = log;
	}

	/**
	 * Should we recover from the IIM format violations, default is true.
	 *
	 * @param recoverFromIIMFormat
	 *            true to recover, false to fail
	 */
	public void setRecoverFromIIMFormat(boolean recoverFromIIMFormat) {
		this.recoverFromIIMFormat = recoverFromIIMFormat;
	}

	/**
	 * Should we recover from the invalid data sets, default is true.
	 *
	 * @param recoverFromInvalidDataSet
	 *            true to recover, false to fail
	 */
	public void setRecoverFromInvalidDataSet(boolean recoverFromInvalidDataSet) {
		this.recoverFromInvalidDataSet = recoverFromInvalidDataSet;
	}

	/**
	 * Should we recover from the unsupported data sets, default is true.
	 *
	 * @param recoverFromUnsupportedDataSet
	 *            true to recover, false to fail
	 */
	public void setRecoverFromUnsupportedDataSet(boolean recoverFromUnsupportedDataSet) {
		this.recoverFromUnsupportedDataSet = recoverFromUnsupportedDataSet;
	}

	/**
	 * @param serializationContext
	 *            The serializationContext to set
	 */
	public void setSerializationContext(SerializationContext serializationContext) {
		this.serializationContext = serializationContext;
		activeSerializationContext = serializationContext != null ? serializationContext : this;
	}

	/**
	 * Controls if reading should stop after data set record 9,10. Default true.
	 *
	 * @param stopAfter9_10
	 *            true to stop reading, false to stop
	 */
	public void setStopAfter9_10(boolean stopAfter9_10) {
		this.stopAfter9_10 = stopAfter9_10;
	}

	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append("IIMFile(");
		b.append("dataSets=");
		b.append(dataSets);
		b.append(')');
		return b.toString();
	}

	/**
	 * Writes this IIMFile to writer.
	 *
	 * @param writer
	 *            writer to write to
	 * @throws IOException
	 *             if file can't be written to
	 */
	public void writeTo(IIMWriter writer) throws IOException {
		final boolean doLog = log != null;
		for (Iterator<DataSet> i = dataSets.iterator(); i.hasNext();) {
			DataSet ds = i.next();
			writer.write(ds);
			if (doLog) {
				log.debug("Wrote data set " + ds);
			}
		}
	}

	private Object getData(DataSet ds) throws SerializationException {
		DataSetInfo info = ds.getInfo();
		Serializer s = info.getSerializer();
		Object result;
		if (s != null) {
			result = s.deserialize(ds.getData(), activeSerializationContext);
		} else {
			result = ds.getData();
		}
		return result;
	}

	/**
	 * Checks if data set is mandatory but missing or non repeatable but having
	 * multiple values in this IIM instance.
	 *
	 * @param info
	 *            IIM data set to check
	 * @return list of constraint violations, empty set if data set is valid
	 */
	public Set<ConstraintViolation> validate(DataSetInfo info) {
		Set<ConstraintViolation> errors = new LinkedHashSet<ConstraintViolation>();
		try {
			if (info.isMandatory() && get(info.getDataSetNumber()) == null) {
				errors.add(new ConstraintViolation(info, ConstraintViolation.MANDATORY_MISSING));
			}
			if (!info.isRepeatable() && getAll(info.getDataSetNumber()).size() > 1) {
				errors.add(new ConstraintViolation(info, ConstraintViolation.REPEATABLE_REPEATED));
			}
		} catch (SerializationException e) {
			errors.add(new ConstraintViolation(info, ConstraintViolation.INVALID_VALUE));
		}
		return errors;
	}

	/**
	 * Checks all data sets in a given record for constraint violations.
	 *
	 * @param record
	 *            IIM record (1,2,3, ...) to check
	 *
	 * @return list of constraint violations, empty set if IIM file is valid
	 */
	public Set<ConstraintViolation> validate(int record) {
		Set<ConstraintViolation> errors = new LinkedHashSet<ConstraintViolation>();
		for (int ds = 0; ds < 250; ++ds) {
			try {
				DataSetInfo dataSetInfo = dsiFactory.create(IIM.DS(record, ds));
				errors.addAll(validate(dataSetInfo));
			} catch (InvalidDataSetException ignored) {
				// DataSetFactory doesn't know about this ds, so will skip it
			}
		}
		return errors;
	}

	/**
	 * Checks all data sets in IIM records 1, 2 and 3 for constraint violations.
	 *
	 * @return list of constraint violations, empty set if IIM file is valid
	 */
	public Set<ConstraintViolation> validate() {
		Set<ConstraintViolation> errors = new LinkedHashSet<ConstraintViolation>();
		for (int record = 1; record <= 3; ++record) {
			errors.addAll(validate(record));
		}
		return errors;
	}

	private SerializationContext activeSerializationContext = this;
	private List<DataSet> dataSets = new ArrayList<DataSet>();
	private DataSetInfoFactory dsiFactory;
	private LoggerAdapter log;
	private boolean recoverFromIIMFormat = true;
	private boolean recoverFromInvalidDataSet = true;
	private boolean recoverFromUnsupportedDataSet = true;
	private SerializationContext serializationContext;
	private boolean stopAfter9_10 = true;
}