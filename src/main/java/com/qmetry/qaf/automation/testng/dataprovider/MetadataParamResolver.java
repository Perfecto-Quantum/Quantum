package com.qmetry.qaf.automation.testng.dataprovider;

import static com.qmetry.qaf.automation.core.ConfigurationManager.getBundle;

import java.util.Map;

import org.apache.commons.text.StringSubstitutor;

import com.qmetry.qaf.automation.testng.DataProviderException;

/**
 * Resolves ${property} tokens in scenario metadata values using
 * TestNG parameters and the QAF bundle (application.properties / -D system properties).
 */
public class MetadataParamResolver {

    /**
     * Resolves ${property} in a String metadata value in-place.
     * No-op if the value is absent or already a non-String type.
     */
    public static void resolveString(Map<String, Object> metadata, String key,
            Map<String, String> testNGParam) {
        Object val = metadata.get(key);
        if (val instanceof String) {
            String resolved = StringSubstitutor.replace((String) val, testNGParam);
            resolved = String.valueOf(getBundle().getInterpolator().interpolate(resolved));
            if(resolved.contains("${")) {
				metadata.remove(key);
				return;
			}
            metadata.put(key, resolved);
        }
    }

    /**
     * Resolves ${property} in a String metadata value and converts to Integer.
     * No-op if the value is absent or already a Number.
     */
    public static void resolveInt(Map<String, Object> metadata, String key,
            Map<String, String> testNGParam) {
        Object val = metadata.get(key);
        if (val instanceof String) {
            String resolved = StringSubstitutor.replace((String) val, testNGParam);
            resolved = String.valueOf(getBundle().getInterpolator().interpolate(resolved));
            if(resolved.contains("${")) {
				metadata.remove(key);
				return;
			}
            try {
                metadata.put(key, Integer.parseInt(resolved.trim()));
            } catch (NumberFormatException e) {
                throw new DataProviderException("Invalid integer value for '" + key + "': " + resolved);
            }
        }
    }
}