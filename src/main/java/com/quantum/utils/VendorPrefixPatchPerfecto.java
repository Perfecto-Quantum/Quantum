package com.quantum.utils;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationMap;
import org.apache.commons.configuration.HierarchicalConfiguration;

import com.qmetry.qaf.automation.core.ConfigurationManager;

public class VendorPrefixPatchPerfecto implements VendorPrefixPatch {

	@Override
	public ConfigurationMap injectVendorPrefix(Configuration config) {

		@SuppressWarnings("unchecked")
		Iterator<String> iter = config.getKeys();

		Configuration perfectoCaps = new HierarchicalConfiguration();
		String capName;

		List<String> ignoreList = Arrays
				.asList(new String[] { "user", "browserName", "driverClass", "automationVersion" });

		Pattern pattern = Pattern.compile("^perfecto.*:", Pattern.CASE_INSENSITIVE);

		Matcher matcher;
		
		Object propValue;
		
		String propValueStr;

		while (iter.hasNext()) {

			capName = iter.next();
			
			propValueStr = config.getProperty(capName).toString();
			
			if("true".equals(propValueStr) || "false".equals(propValueStr)) {
				propValue = Boolean.valueOf(propValueStr);
			}else {
				propValue = config.getProperty(capName);
			}
			
			if (!ignoreList.contains(capName)) {

				matcher = pattern.matcher(capName);
				
				if (!matcher.find()) {
					perfectoCaps.addProperty("perfecto:" + capName, propValue);
				}else {
					perfectoCaps.addProperty(capName, propValue);
				}

			} else {
				if (!"driverClass".equalsIgnoreCase(capName)) {
					perfectoCaps.addProperty(capName, propValue);
				}

			}
		}

		// Security Token for multiple device scenario
		if(!perfectoCaps.containsKey("perfecto:securityToken")) {

			String securityToken = ConfigurationManager.getBundle().getString("perfecto.capabilities.securityToken", "");
			
			perfectoCaps.addProperty("perfecto:securityToken", securityToken);
		}
		
		// By default Appium Version is set to latest version if explicitly not mentioned
		if(!perfectoCaps.containsKey("perfecto:appiumVersion")) {
			perfectoCaps.addProperty("perfecto:appiumVersion", "latest");
		}

		return new ConfigurationMap(perfectoCaps);
	}

}
