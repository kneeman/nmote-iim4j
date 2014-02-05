/*
 * Copyright (c) Nmote Ltd. 2004-2014. All rights reserved. 
 * See LICENSE doc in a root of project folder for additional information.
 */

package com.nmote.iim4j.serialize;

import java.io.IOException;

/**
 * SerializationException
 */
public class SerializationException extends IOException {

	private static final long serialVersionUID = 100L;

	public SerializationException() {
	}

	/**
	 * @param message
	 */
	public SerializationException(String message) {
		super(message);
	}
}
