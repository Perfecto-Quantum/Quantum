package com.quantum.utils;

import java.util.Map;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.impl.LogFactoryImpl;

import com.qmetry.qaf.automation.core.ConfigurationManager;

public class QuantumPatch {
	
	private static final Log logger = LogFactoryImpl.getLog(QuantumPatch.class);

	@SuppressWarnings("unchecked")
	public static void capabilitiesPatchAppium2(Configuration config, Map<String, Object> capabilities) {
		
		if (ConfigurationUtils.isPerfectoExecution()) {
			VendorPrefixPatch perfectoConf = new VendorPrefixPatchPerfecto();
			ConfigurationMap perfecto = perfectoConf.injectVendorPrefix(config);
			capabilities.putAll(perfecto);
		}

		String otherVendorsClassPath = ConfigurationManager.getBundle().getString("quantum.patch.vendorprefixclass", "");

		if (!"".equals(otherVendorsClassPath)) {
			try {
				Class<? extends VendorPrefixPatch> otherVendor = (Class<? extends VendorPrefixPatch>) Class.forName(otherVendorsClassPath);

				VendorPrefixPatch otherVendorInstance = otherVendor.getConstructor().newInstance();

				ConfigurationMap others = otherVendorInstance.injectVendorPrefix(config);

				capabilities.putAll(others);

			} catch (Exception e) {
				logger.error("Switching to default Configuration. Vendor Prefix Patch class not found - " + otherVendorsClassPath);
				capabilities.putAll(new ConfigurationMap(config));
			}

		} else {
			capabilities.putAll(new ConfigurationMap(config));
		}

	}

}
