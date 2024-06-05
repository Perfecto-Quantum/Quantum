package com.quantum.utils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.impl.LogFactoryImpl;

import com.qmetry.qaf.automation.core.ConfigurationManager;

public class QuantumPatch {

	private static final Log logger = LogFactoryImpl.getLog(QuantumPatch.class);

	@SuppressWarnings({"unchecked" })
	private Map<String, Object> patchCapabilities(Configuration config, Map<String, Object> capabilities, String propertyName) {
		
		Class<? extends VendorPrefixPatch> vendorClass = null;

		if (ConfigurationUtils.isPerfectoExecution()) {
			vendorClass = VendorPrefixPatchPerfecto.class;
		} else {
			String otherVendorsClassPath = ConfigurationManager.getBundle().getString(propertyName, "");

			if (StringUtils.isNoneEmpty(otherVendorsClassPath)) {

				try {
					vendorClass = (Class<? extends VendorPrefixPatch>) Class.forName(otherVendorsClassPath);
				} catch (ClassNotFoundException e) {
					logger.error(e.getMessage());
				}
			}
		}

		if (vendorClass == null) {
			logger.info("No Vendor Prefix Patch provided. No Vendor capability update will happen.");
			return new HashMap<>();
//			capabilities.putAll(new ConfigurationMap(config));
		} else {
			try {

				VendorPrefixPatch vendorClassInstance = vendorClass.getConstructor().newInstance();

				ConfigurationMap capabilitiesMap = vendorClassInstance.injectVendorPrefix(config);

				return capabilitiesMap;

			} catch (Exception e) {
				logger.error(
						"Switching to default Configuration. Vendor Prefix Patch class not found - " + vendorClass);
				
				return new HashMap<>();
			}
		}
	}

	public void capabilitiesPatchSelenium4(Configuration config, Map<String, Object> capabilities) {
		
		
		Map<String, Object> vendorSpecificCaps = convertToMap(config);
		String vendorName = ConfigurationManager.getBundle().getString("quantum.patch.vendor.name", "");
		
		if(StringUtils.isEmpty(vendorName) && ConfigurationUtils.isPerfectoExecution()) {
			vendorName = "perfecto";
		}
		
		if(StringUtils.isEmpty(vendorName)) {
			capabilities.putAll(vendorSpecificCaps);
		}else {
			capabilities.put(String.format("%s:options",vendorName),vendorSpecificCaps);
		}
		
		removeVendorSpecificCapabilities(vendorSpecificCaps, capabilities);

	}
	
	@SuppressWarnings("unchecked")
	private Map<String,Object> convertToMap(Configuration config){
		
		Iterator<String> iter = config.getKeys();
		
		Map<String,Object> capabilities = new HashMap<>();
		
		String key;
		while(iter.hasNext()) {
			key = iter.next();
			capabilities.put(key, config.getProperty(key));
		}
		
		return capabilities;
	}
	
	private void removeVendorSpecificCapabilities(Map<String,Object> patchedCapabilities, Map<String, Object> capabilities) {
		Set<String> vendorSpecificKeys = patchedCapabilities.keySet();
		
		for(String vendorSpecificKey: vendorSpecificKeys) {
			capabilities.remove(vendorSpecificKey);
		}
	}

	public void capabilitiesPatchAppium2(Configuration config, Map<String, Object> capabilities) {

		Map<String,Object> patchedCapabilities = patchCapabilities(config, capabilities, "quantum.patch.appium.vendorprefixclass");
		
		removeVendorSpecificCapabilities(convertToMap(config), capabilities);
		
		if(patchedCapabilities.size()>0) {
			capabilities.putAll(patchedCapabilities);
		}else {
			capabilities.putAll(convertToMap(config));
		}
		
	}

}
