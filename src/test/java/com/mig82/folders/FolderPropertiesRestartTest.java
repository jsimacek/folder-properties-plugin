package com.mig82.folders;

import static com.mig82.folders.FolderPropertyResolverTest.folder;
import static com.mig82.folders.FolderPropertyResolverTest.property;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.cloudbees.hudson.plugins.folder.Folder;
import com.mig82.folders.environment.FolderPropertiesSnapshotAction;
import java.util.concurrent.atomic.AtomicInteger;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.jvnet.hudson.test.junit.jupiter.JenkinsSessionExtension;

class FolderPropertiesRestartTest {

    @RegisterExtension
    final JenkinsSessionExtension sessions = new JenkinsSessionExtension();

    @Test
    void resumesBodyAfterControllerRestart() throws Throwable {
        AtomicInteger buildNumber = new AtomicInteger();

        sessions.then(r -> {
            Folder parent = folder(r, "issue-48", false, property("key", "restart-value"));
            WorkflowJob job = PipelineTestHelper.createJob(parent, "pipeline", """
                    withFolderProperties {
                      echo "before restart: ${env.key}"
                      sleep time: 5, unit: 'SECONDS'
                      echo "after restart: ${env.key}"
                    }
                    """);

            WorkflowRun run = job.scheduleBuild2(0).waitForStart();
            buildNumber.set(run.getNumber());
            r.waitForMessage("Sleeping for 5 sec", run);
        });

        sessions.then(r -> {
            WorkflowJob job = r.jenkins.getItemByFullName("issue-48/pipeline", WorkflowJob.class);
            WorkflowRun run = job.getBuildByNumber(buildNumber.get());

            r.assertBuildStatusSuccess(r.waitForCompletion(run));
            FolderPropertiesSnapshotAction snapshot = run.getAction(FolderPropertiesSnapshotAction.class);
            assertNotNull(snapshot);
            assertEquals("restart-value", snapshot.getValues().get("key"));
            r.assertLogContains("before restart: restart-value", run);
            r.assertLogContains("after restart: restart-value", run);
            r.assertLogNotContains("SynchronousResumeNotSupportedException", run);
        });
    }
}
