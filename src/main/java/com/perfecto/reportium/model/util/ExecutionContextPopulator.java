package com.perfecto.reportium.model.util;

import com.perfecto.reportium.exception.ReportiumException;
import com.perfecto.reportium.model.CustomField;
import com.perfecto.reportium.model.Job;
import com.perfecto.reportium.model.Project;
import org.apache.commons.lang3.StringUtils;

import java.util.HashSet;
import java.util.Set;

import static com.perfecto.reportium.client.Constants.SDK.*;

/**
 * Service for automatically updating missing details in {@link Job}, {@link Project} and {@link CustomField}
 * instances from environment variables
 */
public class ExecutionContextPopulator {
    public static final String INVALID_ENV_CUSTOM_FIELD_ERROR = "Failed to parse custom fields parameter: '%s'";

    private final static String COMMA = ",";
    public static final String EQUALS = "=";

    /**
     * Returns a new {@link Job} with the same properties as the given source.
     * If the source is missing some properties then this method will try to get values for
     * them based on well-defined environment variable names.
     *
     * @param src job source
     * @return New job based on the given source. Properties that are missing in the source
     * job are read from system variables and populated in the returned value.
     * Returns <code>null</code> if the interpolated job name is empty
     */
    public static Job populateMissingJobPropertiesFromEnvVariables(Job src) {
        Job target = new Job();

        // Copy initial values
        if (src != null) {
            target.setNumber(src.getNumber());
            target.setName(src.getName());
            target.setBranch(src.getBranch());
        }

        // Fill missing properties from environment variables
        if (target.getNumber() == 0) {
            String property = SystemPropertyUtils.getSystemProperty(jobNumberParameterNameV2, jobNumberParameterNameV1);
            if (StringUtils.isNotBlank(property)) {
                target.setNumber(Integer.parseInt(property));
            }
        }

        if (StringUtils.isBlank(target.getBranch())) {
            String property = SystemPropertyUtils.getSystemProperty(jobBranchParameterBranchV2, jobBranchParameterBranchV1);
            if (StringUtils.isNotBlank(property)) {
                target.setBranch(property);
            }
        }

        if (StringUtils.isBlank(target.getName())) {
            String property = SystemPropertyUtils.getSystemProperty(jobNameParameterNameV2, jobNameParameterNameV1);
            target.setName(property);
        }

        if (StringUtils.isBlank(target.getName())) {
            return null;
        }
        return target;
    }

    /**
     * Returns a new {@link Project} with the same properties as the given source.
     * If the source is missing some properties then this method will try to get values for
     * them based on well-defined environment variable names.
     *
     * @param src project source
     * @return New project based on the given source. Properties that are missing in the source
     * project are read from system variables and populated in the returned value.
     * Returns <code>null</code> if the interpolated project name is empty
     */
    public static Project populateMissingProjectPropertiesFromEnvVariables(Project src) {
        Project target = new Project();

        // Copy initial values
        if (src != null) {
            target.setVersion(src.getVersion());
            target.setName(src.getName());
        }

        // Fill missing properties from environment variables
        if (StringUtils.isBlank(target.getVersion())) {
            target.setVersion(SystemPropertyUtils.getSystemProperty(projectVersionParameterNameV2, projectVersionParameterNameV1));
        }

        if (StringUtils.isBlank(target.getName())) {
            target.setName(SystemPropertyUtils.getSystemProperty(projectNameParameterNameV2, projectNameParameterNameV1));
        }

        if (StringUtils.isBlank(target.getName()) && StringUtils.isBlank(target.getVersion())) {
            return null;
        }
        return target;
    }

    // * Returns a new {@link #parse(Set, CustomField) parse(Set&lt;CustomField&gt;, CustomField)         } with both properties as the given source and processed values
    /**
     * Returns a new Set&lt;{@link CustomField}&gt; with both properties as the given source and processed values
     * from a set of predefined environment variables.
     * If the source is missing some properties then this method will try to get values for
     * them based on well-defined environment variable names.
     * In case of name duplication the src will be preferred.
     *
     * @param src Set&lt;CustomField&gt;
     * @return new Set&lt;CustomField&gt; based on both give src and env variables
     */
    public static Set<CustomField> populateMissingCustomFieldsPropertiesFromEnvVariables(Set<CustomField> src) {
        Set<CustomField> target = new HashSet<>();

        String systemProperties = SystemPropertyUtils.getSystemProperty(jvmCustomFieldsParameterName, jvmCustomFieldsParameterName);
        Set<String> systemPropertiesNames = new HashSet<>();

        if (StringUtils.isNotEmpty(systemProperties)) {
            String[] customFieldsArray = StringUtils.split(systemProperties, COMMA);

            for (String item : customFieldsArray) {
                item = item.trim();
                if (StringUtils.isBlank(item) || !item.contains(EQUALS) || item.startsWith(EQUALS)) {
                    throw new ReportiumException(String.format(INVALID_ENV_CUSTOM_FIELD_ERROR, systemProperties));
                }
                String[] nameAndValue = item.split(EQUALS, 2);
                String name = nameAndValue[0].trim();
                String value = nameAndValue.length > 1 ? nameAndValue[1].trim() : null;
                target.add(new CustomField(name, value));
                systemPropertiesNames.add(name);
            }
        }

        if (src != null) {
            for (CustomField customField : src) {
                if (!systemPropertiesNames.contains(customField.getName())) {
                    target.add(customField);
                }
            }
        }

        return target;
    }
}
