package com.mig82.folders.properties;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 * An immutable snapshot of the folder properties resolved for a job.
 */
public final class ResolvedFolderProperties {

    private final Map<String, String> values;
    private final Map<String, String> buildStartValues;
    private final Map<String, String> sources;

    ResolvedFolderProperties(
            Map<String, String> values, Map<String, String> buildStartValues, Map<String, String> sources) {
        this.values = immutableCopy(values);
        this.buildStartValues = immutableCopy(buildStartValues);
        this.sources = immutableCopy(sources);
    }

    /**
     * Returns all resolved values, regardless of their exposure mode.
     */
    public Map<String, String> getValues() {
        return values;
    }

    /**
     * Returns the resolved values whose source folder opted into build-start exposure.
     */
    public Map<String, String> getBuildStartValues() {
        return buildStartValues;
    }

    /**
     * Returns the full name of the folder that supplied each resolved key.
     */
    public Map<String, String> getSources() {
        return sources;
    }

    private static Map<String, String> immutableCopy(Map<String, String> source) {
        Map<String, String> copy = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        copy.putAll(source);
        return Collections.unmodifiableMap(copy);
    }
}
