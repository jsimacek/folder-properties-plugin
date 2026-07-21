package com.mig82.folders.properties;

import hudson.EnvVars;
import hudson.model.Job;

/**
 * A PropertiesLoader which can be used in both Freestyle Job build wrapper and custom pipeline step.
 * The loader loads StringProperty from current job folder to its ancient recursively.
 *
 * @author Miguelangel Fernandez Mendoza and Gong Yi
 */
public class PropertiesLoader {
    private static final FolderPropertyResolver RESOLVER = new FolderPropertyResolver();

    public static EnvVars loadFolderProperties(Job job) {
        return new EnvVars(RESOLVER.resolve(job).getValues());
    }
}
