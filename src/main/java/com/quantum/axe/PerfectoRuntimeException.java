package com.quantum.axe;

public class PerfectoRuntimeException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public PerfectoRuntimeException(final String reason) {
		super(reason);
	}

	public PerfectoRuntimeException(final String reason, final Throwable e) {
		super(reason, e);
	}
}
