package org.clyze.deepdoop.system

class DeepDoopException extends RuntimeException {

	Error error

	DeepDoopException(String msg, Error error) {
		super(msg)
		this.error = error
	}

	synchronized Throwable fillInStackTrace() { null }
}