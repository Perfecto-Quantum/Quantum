package com.perfecto.reportium.exception;

/**
 * Custom runtime exception
 */
public class ReportiumException extends RuntimeException {

    public ReportiumException(String msg) {
        super(msg);
    }

    public ReportiumException(String msg, Throwable throwable) {
        super(msg, throwable);
    }
}
