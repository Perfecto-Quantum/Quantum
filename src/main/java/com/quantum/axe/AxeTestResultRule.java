package com.quantum.axe;



import java.util.List;

public class AxeTestResultRule {

    public  String ruleId;

    public  String description;

    public  String help;

    public  String helpUrl;

    public  List<String> tags;


    @Override
    public String toString() {
        return ruleId;
    }


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((description == null) ? 0 : description.hashCode());
        result = prime * result + ((help == null) ? 0 : help.hashCode());
        result = prime * result + ((helpUrl == null) ? 0 : helpUrl.hashCode());
        result = prime * result + ((ruleId == null) ? 0 : ruleId.hashCode());
        result = prime * result + ((tags == null) ? 0 : tags.hashCode());
        return result;
    }


    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final AxeTestResultRule other = (AxeTestResultRule) obj;
        if (description == null) {
            if (other.description != null) {
                return false;
            }
        } else if (!description.equals(other.description)) {
            return false;
        }
        if (help == null) {
            if (other.help != null) {
                return false;
            }
        } else if (!help.equals(other.help)) {
            return false;
        }
        if (helpUrl == null) {
            if (other.helpUrl != null) {
                return false;
            }
        } else if (!helpUrl.equals(other.helpUrl)) {
            return false;
        }
        if (ruleId == null) {
            if (other.ruleId != null) {
                return false;
            }
        } else if (!ruleId.equals(other.ruleId)) {
            return false;
        }
        if (tags == null) {
            if (other.tags != null) {
                return false;
            }
        } else if (!tags.equals(other.tags)) {
            return false;
        }
        return true;
    }
}

