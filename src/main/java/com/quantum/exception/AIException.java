package com.quantum.exception;

public class AIException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public AIException(String message) {
		super(message);
	}

	public AIException(String message, Throwable cause) {
		super(message, cause);
	}

}
