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
 * Stores the immutable folder property snapshot used by a build that opts into build-start exposure.
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

    /**
     * Creates a persistent snapshot only when at least one resolved property is exposed at build start.
     */
    public static void createForBuildStart(Run<?, ?> run) {
        FolderPropertiesSnapshotAction existing = run.getAction(FolderPropertiesSnapshotAction.class);
        if (existing != null) {
            return;
        }

        synchronized (run) {
            existing = run.getAction(FolderPropertiesSnapshotAction.class);
            if (existing == null) {
                ResolvedFolderProperties resolved = new FolderPropertyResolver().resolve(run.getParent());
                if (!resolved.getBuildStartValues().isEmpty()) {
                    run.addAction(new FolderPropertiesSnapshotAction(resolved));
                }
            }
        }
    }

    /**
     * Returns the persistent build-start snapshot when present, otherwise resolves the current folder configuration.
     */
    public static Map<String, String> resolveValues(Run<?, ?> run) {
        FolderPropertiesSnapshotAction existing = run.getAction(FolderPropertiesSnapshotAction.class);
        if (existing != null) {
            return existing.getValues();
        }
        return new FolderPropertyResolver().resolve(run.getParent()).getValues();
    }
}
