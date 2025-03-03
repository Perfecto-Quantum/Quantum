package com.perfecto.reportium.model.util;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.perfecto.reportium.client.Constants.SDK.jvmTagsParameterNameV1;
import static com.perfecto.reportium.client.Constants.SDK.jvmTagsParameterNameV2;

/**
 * Extracts values for a known set of predefined env variables as a list of strings
 */
public class TagsExtractor {

    /**
     * Listing a subset of Jenkins predefined env variables
     * See also https://wiki.jenkins-ci.org/display/JENKINS/Building+a+software+project#Buildingasoftwareproject-JenkinsSetEnvironmentVariables
     */
    private enum JenkinsEnvVariables {
        BUILD_NUMBER, BUILD_ID, JOB_NAME, BUILD_TAG, SVN_REVISION, CVS_BRANCH, GIT_COMMIT, GIT_BRANCH
    }

    /**
     * Listing a subset of TravisCI predefined env variables
     * See also https://docs.travis-ci.com/user/environment-variables/#Default-Environment-Variables
     */
    private enum TravisEnvVariables {
        TRAVIS_BRANCH, TRAVIS_BUILD_ID, TRAVIS_BUILD_NUMBER, TRAVIS_COMMIT,
        TRAVIS_COMMIT_RANGE, TRAVIS_JOB_ID, TRAVIS_JOB_NUMBER, TRAVIS_TAG
    }

    /**
     * Listing a subset of CircleCI predefined env variables
     * See also https://circleci.com/docs/environment-variables/
     */
    private enum CircleEnvVariables {
        CIRCLE_PROJECT_USERNAME, CIRCLE_PROJECT_REPONAME, CIRCLE_BRANCH, CIRCLE_TAG,
        CIRCLE_SHA1, CIRCLE_BUILD_NUM, CI_PULL_REQUESTS
    }

    private final static String COMMA = ",";

    private TagsExtractor() {

    }

    /**
     * Returns a list of test context tags from a set of predefined environment variables.
     * If any environment variable is missing then its value is excluded from the returned list,
     * i.e. the returned list is not expected to include null values or empty strings.
     *
     * @return list of test context tags from a set of predefined environment variables.
     */
    public static List<String> getPredefinedContextTags() {
        List<String> tags = new ArrayList<>();

        tags.addAll(getCiServerPredefinedTags());
        tags.addAll(getJvmTags());

        return tags;
    }

    private static List<String> getCiServerPredefinedTags() {
        List<String> tags = new ArrayList<>();
        tags.addAll(extractTags(JenkinsEnvVariables.class));
        tags.addAll(extractTags(TravisEnvVariables.class));
        tags.addAll(extractTags(CircleEnvVariables.class));
        return tags;
    }

    /**
     * Custom tags can be used by adding them as the JVM parameter -DreportiumTags={comma separated list of tags}.
     * This method reads that value and returns the tags as a list of strings.
     *
     * @return tags passed as JVM parameters.
     */
    private static List<String> getJvmTags() {
        List<String> tagsList = Collections.emptyList();
        String tags = SystemPropertyUtils.getSystemProperty(jvmTagsParameterNameV1, jvmTagsParameterNameV2);
        if (!StringUtils.isEmpty(tags)) {
            // Split tags by commas
            String[] tagsArray = StringUtils.split(tags, COMMA);
            tagsList = Arrays.asList(tagsArray);
        }
        return tagsList;
    }

    private static <T extends Enum<T>> List<String> extractTags(Class<T> enumClass) {
        List<String> tags = new ArrayList<>();
        for (T envVariableName : enumClass.getEnumConstants()) {
            String propertyValue = System.getProperty(envVariableName.name());
            if (!StringUtils.isEmpty(propertyValue)) {
                tags.add(propertyValue);
            }
        }
        return tags;
    }
}
