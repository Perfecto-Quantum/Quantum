package com.perfecto.reportium.model;

import java.util.Objects;

public class Project {

    private String name;
    private String version;

    public Project() {
    }

    public Project(String name, String version) {
        this.name = name;
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Project project = (Project) o;
        return Objects.equals(name, project.name) &&
                Objects.equals(version, project.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, version);
    }
}
