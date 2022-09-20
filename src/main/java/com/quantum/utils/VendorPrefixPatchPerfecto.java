package com.quantum.utils;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationMap;
import org.apache.commons.configuration.HierarchicalConfiguration;

public class VendorPrefixPatchPerfecto implements VendorPrefixPatch{
	
	@Override
	public ConfigurationMap injectVendorPrefix(Configuration config) {
		
		@SuppressWarnings("unchecked")
		Iterator<String> iter = config.getKeys();
		
		Configuration perfectoCaps = new HierarchicalConfiguration();
		String capName;
		
		List<String> ignoreList = Arrays.asList(new String[]{"browserName","driverClass","automationVersion"});
		
		while(iter.hasNext()) {
			
			capName = iter.next();
			if(!ignoreList.contains(capName)) {
				perfectoCaps.addProperty("perfecto:" + capName, config.getProperty(capName));
			}else {
				perfectoCaps.addProperty(capName, config.getProperty(capName));
			}
		}
		
		return new ConfigurationMap(perfectoCaps);
	}

}
