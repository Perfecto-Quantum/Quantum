package com.perfecto.reportium.client;

/**
 * Reportium client constants
 */
public interface Constants {
    interface SDK {
        /**
         * Custom JVM tag for passing tags thru environment variables
         */
        String jvmTagsParameterNameV1 = "reportium-tags";
        String jvmTagsParameterNameV2 = "ReportiumTags";

        /**
         * Custom JVM tag for passing customFields thru environment variables
         */
        String jvmCustomFieldsParameterName = "ReportiumCustomFields";

        /**
         * Custom JVM tag for passing job name, number and branch via environment variables
         */
        String jobNumberParameterNameV1 = "reportium-job-number";
        String jobNumberParameterNameV2 = "ReportiumJobNumber";
        String jobNameParameterNameV1 = "reportium-job-name";
        String jobNameParameterNameV2 = "ReportiumJobName";
        String jobBranchParameterBranchV1 = "reportium-job-branch";
        String jobBranchParameterBranchV2 = "ReportiumJobBranch";

        /**
         * Custom JVM tag for passing project name and version via environment variables
         */
        String projectVersionParameterNameV1 = "reportium-project-version";
        String projectVersionParameterNameV2 = "ReportiumProjectVersion";
        String projectNameParameterNameV1 = "reportium-project-name";
        String projectNameParameterNameV2 = "ReportiumProjectName";
    }

    interface Capabilities {
        String executionReportUrl = "testGridReportUrl";
        String executionId = "executionId";
    }
}
