package com.qmetry.qaf.automation.ui;

public class UnsupportedBrowserException extends RuntimeException{

	private static final long serialVersionUID = 1L;

	public UnsupportedBrowserException(String browserName) {
		super(browserName + " not a valid Browser");
	}
	
}
