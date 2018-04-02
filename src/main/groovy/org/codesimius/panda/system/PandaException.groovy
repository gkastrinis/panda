package org.codesimius.panda.system

class PandaException extends RuntimeException {

	Error error

	PandaException(String msg, Error error) {
		super(msg)
		this.error = error
	}

	synchronized Throwable fillInStackTrace() { null }
}
