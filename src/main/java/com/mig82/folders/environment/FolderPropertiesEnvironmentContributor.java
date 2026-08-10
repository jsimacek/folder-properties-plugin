package com.mig82.folders.environment;

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.EnvironmentContributor;
import hudson.model.Run;
import hudson.model.TaskListener;
import java.io.IOException;

/**
 * Contributes opted-in folder properties before Pipeline and SCM configuration is evaluated.
 */
@Extension
public final class FolderPropertiesEnvironmentContributor extends EnvironmentContributor {

    @Override
    @SuppressWarnings("rawtypes")
    public void buildEnvironmentFor(Run run, EnvVars env, TaskListener listener)
            throws IOException, InterruptedException {
        FolderPropertiesSnapshotAction.createForBuildStart(run);
    }
}
