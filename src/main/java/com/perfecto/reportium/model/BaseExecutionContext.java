package com.perfecto.reportium.model;


import com.perfecto.reportium.model.util.ExecutionContextPopulator;
import com.perfecto.reportium.model.util.ListUtils;
import com.perfecto.reportium.model.util.TagsExtractor;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

public class BaseExecutionContext {
    private final Job job;
    private final Project project;
    private final List<String> contextTags;
    private final Set<CustomField> customFields;

    protected BaseExecutionContext(Builder builder) {
        this.job = ExecutionContextPopulator.populateMissingJobPropertiesFromEnvVariables(builder.job);
        this.project = ExecutionContextPopulator.populateMissingProjectPropertiesFromEnvVariables(builder.project);
        this.contextTags = ListUtils.mergeLists(TagsExtractor.getPredefinedContextTags(), builder.contextTags);
        this.customFields = builder.customFields;
    }

    public Job getJob() {
        return job;
    }

    public Project getProject() {
        return project;
    }

    public List<String> getContextTags() {
        return contextTags;
    }

    public Set<CustomField> getCustomFields() {
        return customFields;
    }

    protected static class Builder<T extends Builder<T>> {
        private Job job;
        private Project project;
        private List<String> contextTags;
        private Set<CustomField> customFields;

        public Builder() {
            this.contextTags = new ArrayList<>();
            this.customFields = new HashSet<>();
        }

        public Builder(BaseExecutionContext copy) {
            this.job = copy.job;
            this.project = copy.project;
            this.contextTags = copy.contextTags;
            this.customFields = copy.customFields;
        }

        @SuppressWarnings("unchecked")
        public T withJob(Job job) {
            this.job = job;
            return (T)this;
        }

        @SuppressWarnings("unchecked")
        public T withProject(Project project) {
            this.project = project;
            return (T)this;
        }

        @SuppressWarnings("unchecked")
        public T withContextTags(Collection<String> contextTags) {
            if (contextTags != null && contextTags.size() > 0) {
                for (String contextTag : contextTags) {
                    if (StringUtils.isNotBlank(contextTag)) {
                        this.contextTags.add(contextTag);
                    }
                }
            }
            return (T)this;
        }

        @SuppressWarnings("unchecked")
        public T withContextTags(String... contextTags) {
            if (contextTags != null) {
                withContextTags(Arrays.asList(contextTags));
            }
            return (T)this;
        }

        @SuppressWarnings("unchecked")
        public T withCustomFields(Collection<CustomField> customFields) {
            if (customFields != null && customFields.size() > 0) {
                for (CustomField customField : customFields) {
                    if (customField != null) {
                        this.customFields.add(customField);
                    }
                }
            }
            return (T) this;
        }

        @SuppressWarnings("unchecked")
        public T withCustomFields(CustomField... customFields) {
            if (customFields != null) {
                withCustomFields(Arrays.asList(customFields));
            }
            return (T) this;
        }

        public BaseExecutionContext build() {
            return new BaseExecutionContext(this);
        }
    }
}
