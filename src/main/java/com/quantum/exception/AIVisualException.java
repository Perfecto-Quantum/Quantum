package com.quantum.exception;

public class AIVisualException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public AIVisualException(String message) {
		super(message);
	}

	public AIVisualException(String message, Throwable cause) {
		super(message, cause);
	}

}
