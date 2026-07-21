package com.mig82.folders.properties;

import com.cloudbees.hudson.plugins.folder.AbstractFolder;
import hudson.model.Item;
import hudson.model.ItemGroup;
import hudson.model.Job;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Resolves folder properties without coupling lookup to a particular build integration.
 */
public final class FolderPropertyResolver {

    private static final Logger LOGGER = Logger.getLogger(FolderPropertyResolver.class.getName());

    public ResolvedFolderProperties resolve(Job<?, ?> job) {
        Map<String, String> values = environmentMap();
        Map<String, String> buildStartValues = environmentMap();
        Map<String, String> sources = environmentMap();
        ItemGroup<?> parent = job.getParent();

        while (parent != null) {
            if (parent instanceof AbstractFolder<?> folder) {
                FolderProperties<?> folderProperties = folder.getProperties().get(FolderProperties.class);
                if (folderProperties != null) {
                    addProperties(folder, folderProperties, values, buildStartValues, sources);
                }
            }

            if (parent instanceof Item item) {
                parent = item.getParent();
            } else {
                break;
            }
        }

        LOGGER.log(
                Level.FINER, "Resolved {0} folder property keys for {1}", new Object[] {values.size(), job.getFullName()
                });
        return new ResolvedFolderProperties(values, buildStartValues, sources);
    }

    private static Map<String, String> environmentMap() {
        return new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    }

    private static void addProperties(
            AbstractFolder<?> folder,
            FolderProperties<?> folderProperties,
            Map<String, String> values,
            Map<String, String> buildStartValues,
            Map<String, String> sources) {
        for (StringProperty property : folderProperties.getProperties()) {
            String key = property.getKey();
            if (values.containsKey(key)) {
                continue;
            }

            values.put(key, property.getValue());
            sources.put(key, folder.getFullName());
            if (folderProperties.isExposeAtBuildStart()) {
                buildStartValues.put(key, property.getValue());
            }
        }
    }
}
