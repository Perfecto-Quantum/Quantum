package com.perfecto.reportium.model;

import java.util.Objects;

/**
 * Created by michaeld on 01/03/2016.
 */
public class Job {

    private int number; // build number
    private String name; // job name
    private String branch; // feature branch

    public Job() {
    }

    public Job(String name, int number) {
        this.name = name;
        this.number = number;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public Job withBranch(String branch) {
        this.setBranch(branch);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Job job = (Job) o;
        return number == job.number &&
                Objects.equals(name, job.name) &&
                Objects.equals(branch, job.branch);
    }

    @Override
    public int hashCode() {
        return Objects.hash(number, name, branch);
    }
}
