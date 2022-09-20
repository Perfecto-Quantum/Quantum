package com.quantum.utils;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationMap;

public interface VendorPrefixPatch {
	
	public ConfigurationMap injectVendorPrefix(Configuration config);

}
