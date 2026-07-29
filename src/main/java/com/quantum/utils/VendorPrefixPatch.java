package com.quantum.utils;

import java.util.Map;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.ConfigurationMap;

public interface VendorPrefixPatch {
	
	public ConfigurationMap injectVendorPrefix(Configuration config,  Map<String, Object> capabilities);

}
