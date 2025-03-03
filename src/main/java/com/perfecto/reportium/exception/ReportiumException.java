package com.perfecto.reportium.exception;

/**
 * Custom runtime exception
 */
public class ReportiumException extends RuntimeException {

    private static final long serialVersionUID = 1L;

	public ReportiumException(String msg) {
        super(msg);
    }

    public ReportiumException(String msg, Throwable throwable) {
        super(msg, throwable);
    }
}
