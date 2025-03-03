package com.perfecto.reportium.model.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Common list utilities
 */
public class ListUtils {

    /**
     * Merges the 2 lists and returns a new list without repetitions
     * @param list1 First list to merge
     * @param list2 Second list to merge
     * @return a new list which contains all elements in both lists without repetitions
     */
    public static List<String> mergeLists(List<String> list1, List<String> list2) {
        if (list1 == null || list2 == null) {
            throw new IllegalArgumentException("Arguments cannot be null");
        }
        Set<String> asSet = new HashSet<>(list1);
        asSet.addAll(list2);
        return new ArrayList<>(asSet);
    }
}
