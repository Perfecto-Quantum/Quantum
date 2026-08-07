package com.quantum.utils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.configuration2.BaseHierarchicalConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.ConfigurationMap;


public class VendorPrefixPatchPerfecto implements VendorPrefixPatch {

	@Override
	public ConfigurationMap injectVendorPrefix(Configuration config,  Map<String, Object> capabilities) {

		@SuppressWarnings("unchecked")
		Iterator<String> iter = config.getKeys();

		Configuration perfectoCaps = new BaseHierarchicalConfiguration();

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
			String securityToken = ConfigurationUtils.getSecurityToken();
			perfectoCaps.addProperty("perfecto:securityToken", securityToken);
		}
		
		// By default Appium Version is set to latest version if explicitly not mentioned
//		if(!perfectoCaps.containsKey("perfecto:appiumVersion")) {
			try {
				perfectoCaps.clearProperty("perfecto:appiumVersion");
			} catch (Exception e) {
				e.printStackTrace();
			}
			if(capabilities.containsKey("perfecto:useVirtualDevice")) {
				perfectoCaps.addProperty("perfecto:appiumVersion", "2.19");
			}
			else
				perfectoCaps.addProperty("perfecto:appiumVersion", "latest");
//		}else {
//			
//			String appiumVersion = perfectoCaps.getString("perfecto:appiumVersion");
//			Map<String, Object> stringKeyMap = new HashMap<>();
//			config.getKeys().forEachRemaining(key -> stringKeyMap.put(key, config.getProperty(key)));
//			if(capabilities.containsKey("perfecto:useVirtualDevice")) {
//				stringKeyMap.put("appiumVersion", capabilities.get("appiumVersion"));
//			}
//		}

		return new ConfigurationMap(perfectoCaps);
	}

}
