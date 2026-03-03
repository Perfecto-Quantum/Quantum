package com.quantum.utils;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.MapConfiguration;

public interface VendorPrefixPatch {
	public MapConfiguration injectVendorPrefix(Configuration config);
}
