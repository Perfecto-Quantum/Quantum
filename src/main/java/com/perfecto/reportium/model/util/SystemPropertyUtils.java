package com.perfecto.reportium.model.util;

import org.apache.commons.lang3.StringUtils;

public class SystemPropertyUtils {
    public static String getSystemProperty(String... propertyNames) {
        String result = null;
        for (String propertyName : propertyNames) {
            result = System.getProperty(propertyName);
            if (StringUtils.isBlank(result)) {
                result = System.getenv(propertyName);
            }
            if (StringUtils.isNotBlank(result)) {
                break;
            }
        }
        return result;
    }

}
