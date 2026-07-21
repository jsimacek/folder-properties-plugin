package com.mig82.folders.environment;

import com.mig82.folders.properties.FolderPropertyResolver;
import com.mig82.folders.properties.ResolvedFolderProperties;
import hudson.EnvVars;
import hudson.model.EnvironmentContributingAction;
import hudson.model.InvisibleAction;
import hudson.model.Run;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Stores the immutable folder property snapshot used throughout one build.
 */
public final class FolderPropertiesSnapshotAction extends InvisibleAction implements EnvironmentContributingAction {

    private final Map<String, String> values;
    private final Map<String, String> buildStartValues;
    private final Map<String, String> sources;

    private FolderPropertiesSnapshotAction(ResolvedFolderProperties resolved) {
        values = new LinkedHashMap<>(resolved.getValues());
        buildStartValues = new LinkedHashMap<>(resolved.getBuildStartValues());
        sources = new LinkedHashMap<>(resolved.getSources());
    }

    public Map<String, String> getValues() {
        return Collections.unmodifiableMap(values);
    }

    public Map<String, String> getBuildStartValues() {
        return Collections.unmodifiableMap(buildStartValues);
    }

    public Map<String, String> getSources() {
        return Collections.unmodifiableMap(sources);
    }

    @Override
    public void buildEnvironment(Run<?, ?> run, EnvVars env) {
        EnvVars defaults = new EnvVars();
        for (Map.Entry<String, String> entry : buildStartValues.entrySet()) {
            if (!env.containsKey(entry.getKey())) {
                defaults.put(entry.getKey(), entry.getValue());
            }
        }
        env.overrideExpandingAll(defaults);
    }

    public static FolderPropertiesSnapshotAction getOrCreate(Run<?, ?> run) {
        FolderPropertiesSnapshotAction existing = run.getAction(FolderPropertiesSnapshotAction.class);
        if (existing != null) {
            return existing;
        }

        synchronized (run) {
            existing = run.getAction(FolderPropertiesSnapshotAction.class);
            if (existing == null) {
                existing = new FolderPropertiesSnapshotAction(new FolderPropertyResolver().resolve(run.getParent()));
                run.addAction(existing);
            }
            return existing;
        }
    }
}
