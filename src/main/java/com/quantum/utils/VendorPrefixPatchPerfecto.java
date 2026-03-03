package com.quantum.utils;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.MapConfiguration;

import com.qmetry.qaf.automation.core.ConfigurationManager;

public class VendorPrefixPatchPerfecto implements VendorPrefixPatch {

	@Override
	public MapConfiguration injectVendorPrefix(Configuration config) {
		Iterator<String> iter = config.getKeys();
		java.util.Map<String, Object> perfectoCapsMap = new java.util.HashMap<>();
		String capName;
		List<String> ignoreList = Arrays.asList("user", "browserName", "driverClass", "automationVersion");
		Pattern pattern = Pattern.compile("^perfecto.*:", Pattern.CASE_INSENSITIVE);
		Matcher matcher;
		Object propValue;
		String propValueStr;
		while (iter.hasNext()) {
			capName = iter.next();
			propValueStr = config.getProperty(capName).toString();
			if("true".equals(propValueStr) || "false".equals(propValueStr)) {
				propValue = Boolean.valueOf(propValueStr);
			} else {
				propValue = config.getProperty(capName);
			}
			if (!ignoreList.contains(capName)) {
				matcher = pattern.matcher(capName);
				if (!matcher.find()) {
					perfectoCapsMap.put("perfecto:" + capName, propValue);
				} else {
					perfectoCapsMap.put(capName, propValue);
				}
			} else {
				if (!"driverClass".equalsIgnoreCase(capName)) {
					perfectoCapsMap.put(capName, propValue);
				}
			}
		}
		// Security Token for multiple device scenario
		if(!perfectoCapsMap.containsKey("perfecto:securityToken")) {
			String securityToken = ConfigurationManager.getBundle().getString("perfecto.capabilities.securityToken", "");
			perfectoCapsMap.put("perfecto:securityToken", securityToken);
		}
		// By default Appium Version is set to latest version if explicitly not mentioned
		if(!perfectoCapsMap.containsKey("perfecto:appiumVersion")) {
			perfectoCapsMap.put("perfecto:appiumVersion", "latest");
		}
		return new MapConfiguration(perfectoCapsMap);
	}
}
