package com.mig82.folders;

import static com.mig82.folders.FolderPropertyResolverTest.folder;
import static com.mig82.folders.FolderPropertyResolverTest.property;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cloudbees.hudson.plugins.folder.Folder;
import com.mig82.folders.environment.FolderPropertiesSnapshotAction;
import com.mig82.folders.properties.FolderProperties;
import hudson.model.ParametersAction;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.Result;
import hudson.model.StringParameterDefinition;
import hudson.model.StringParameterValue;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class FolderPropertiesPipelineRegressionTest {

    @Test
    void propagatesBodyResultInDeclarativeWhen(JenkinsRule r) throws Exception {
        Folder parent = folder(r, "issue-45", false, property("key", "value"));
        WorkflowJob job = PipelineTestHelper.createJob(parent, "pipeline", """
                pipeline {
                  agent any
                  stages {
                    stage('conditional') {
                      when {
                        expression {
                          withFolderProperties {
                            return true
                          }
                        }
                      }
                      steps {
                        echo 'ISSUE_45_STAGE_RAN'
                      }
                    }
                  }
                }
                """);

        WorkflowRun run = r.assertBuildStatusSuccess(job.scheduleBuild2(0));

        r.assertLogContains("ISSUE_45_STAGE_RAN", run);
    }

    @Test
    void propagatesNonBooleanBodyResult(JenkinsRule r) throws Exception {
        Folder parent = folder(r, "pipeline-body-result", false, property("key", "value"));
        WorkflowJob job = PipelineTestHelper.createJob(parent, "pipeline", """
                def result = withFolderProperties {
                  return 'BODY_RESULT'
                }
                echo "result: ${result}"
                """);

        WorkflowRun run = r.assertBuildStatusSuccess(job.scheduleBuild2(0));

        r.assertLogContains("result: BODY_RESULT", run);
    }

    @Test
    void exposesPropertiesBeforeRootDeclarativeEnvironment(JenkinsRule r) throws Exception {
        Folder parent = folder(r, "issue-46", true, property("TEST_VAR", "TRUE"));
        WorkflowJob job = PipelineTestHelper.createJob(parent, "pipeline", """
                pipeline {
                  agent none
                  options {
                    withFolderProperties()
                  }
                  environment {
                    IS_SET = "${env.TEST_VAR}"
                  }
                  stages {
                    stage('test') {
                      agent any
                      steps {
                        echo "TEST_VAR: ${env.TEST_VAR}!!!"
                        echo "IS_SET: ${env.IS_SET}!!!"
                      }
                    }
                  }
                }
                """);

        WorkflowRun run = r.assertBuildStatusSuccess(job.scheduleBuild2(0));

        r.assertLogContains("TEST_VAR: TRUE!!!", run);
        r.assertLogContains("IS_SET: TRUE!!!", run);
    }

    @Test
    void preservesScopedExpansionAndRemovesValuesAfterBody(JenkinsRule r) throws Exception {
        Folder parent =
                folder(r, "pipeline-scope", false, property("BASE", "value"), property("DERIVED", "${BASE}-derived"));
        WorkflowJob job = PipelineTestHelper.createJob(parent, "pipeline", """
                echo "before: ${env.BASE}"
                withFolderProperties {
                  echo "inside: ${env.DERIVED}"
                }
                echo "after: ${env.BASE}"
                """);

        WorkflowRun run = r.assertBuildStatusSuccess(job.scheduleBuild2(0));

        r.assertLogContains("before: null", run);
        r.assertLogContains("inside: value-derived", run);
        r.assertLogContains("after: null", run);
    }

    @Test
    void propagatesBodyFailure(JenkinsRule r) throws Exception {
        Folder parent = folder(r, "pipeline-failure", false, property("key", "value"));
        WorkflowJob job = PipelineTestHelper.createJob(parent, "pipeline", """
                withFolderProperties {
                  error('EXPECTED_BODY_FAILURE')
                }
                """);

        WorkflowRun run = r.assertBuildStatus(Result.FAILURE, job.scheduleBuild2(0));

        r.assertLogContains("EXPECTED_BODY_FAILURE", run);
    }

    @Test
    void snapshotsPropertiesOncePerRun(JenkinsRule r) throws Exception {
        Folder parent = folder(r, "pipeline-snapshot", true, property("key", "initial"));
        WorkflowJob job = PipelineTestHelper.createJob(parent, "pipeline", "echo \"key: ${env.key}\"");

        WorkflowRun run = r.assertBuildStatusSuccess(job.scheduleBuild2(0));
        FolderPropertiesSnapshotAction snapshot = run.getAction(FolderPropertiesSnapshotAction.class);

        assertTrue(snapshot != null && snapshot.getValues().containsKey("key"));
        assertFalse(snapshot.getSources().isEmpty());
        assertTrue(snapshot.getValues().get("key").equals("initial"));
    }

    @Test
    void persistsEarlyExposureConfiguration(JenkinsRule r) throws Exception {
        Folder configured = folder(r, "pipeline-persistence", true, property("key", "value"));

        Folder reloaded = r.configRoundtrip(configured);
        FolderProperties<?> folderProperties = reloaded.getProperties().get(FolderProperties.class);

        assertTrue(folderProperties.isExposeAtBuildStart());
    }

    @Test
    void expandsEarlyValuesAndLetsParametersOverrideDefaults(JenkinsRule r) throws Exception {
        Folder parent = folder(
                r, "pipeline-early-defaults", true, property("BASE", "folder"), property("DERIVED", "${BASE}-derived"));
        WorkflowJob job = PipelineTestHelper.createJob(parent, "pipeline", """
                echo "base: ${env.BASE}"
                echo "derived: ${env.DERIVED}"
                """);
        job.addProperty(new ParametersDefinitionProperty(new StringParameterDefinition("BASE", "default")));

        WorkflowRun run = r.assertBuildStatusSuccess(
                job.scheduleBuild2(0, new ParametersAction(new StringParameterValue("BASE", "parameter"))));

        r.assertLogContains("base: parameter", run);
        r.assertLogContains("derived: parameter-derived", run);
    }

    @Test
    void letsDeclarativeEnvironmentOverrideEarlyDefaults(JenkinsRule r) throws Exception {
        Folder parent = folder(r, "pipeline-early-declarative-override", true, property("KEY", "folder"));
        WorkflowJob job = PipelineTestHelper.createJob(parent, "pipeline", """
                pipeline {
                  agent any
                  environment {
                    KEY = 'pipeline'
                  }
                  stages {
                    stage('test') {
                      steps {
                        echo "key: ${env.KEY}"
                      }
                    }
                  }
                }
                """);

        WorkflowRun run = r.assertBuildStatusSuccess(job.scheduleBuild2(0));

        r.assertLogContains("key: pipeline", run);
    }

    @Test
    void reusesSnapshotWhenFolderChangesDuringBuild(JenkinsRule r) throws Exception {
        Folder parent = folder(r, "pipeline-snapshot-change", true, property("key", "initial"));
        WorkflowJob job = PipelineTestHelper.createJob(parent, "pipeline", """
                echo "first: ${env.key}"
                sleep time: 2, unit: 'SECONDS'
                withFolderProperties {
                  echo "second: ${env.key}"
                }
                """);

        WorkflowRun run = job.scheduleBuild2(0).waitForStart();
        r.waitForMessage("Sleeping for 2 sec", run);
        FolderProperties<?> properties = parent.getProperties().get(FolderProperties.class);
        properties.getProperties()[0].setValue("changed");
        parent.save();

        r.assertBuildStatusSuccess(r.waitForCompletion(run));
        r.assertLogContains("first: initial", run);
        r.assertLogContains("second: initial", run);
    }

    @Test
    void preservesDynamicResolutionWhenBuildStartExposureIsDisabled(JenkinsRule r) throws Exception {
        Folder parent = folder(r, "pipeline-legacy-dynamic", false, property("key", "initial"));
        WorkflowJob job = PipelineTestHelper.createJob(parent, "pipeline", """
                withFolderProperties {
                  echo "first: ${env.key}"
                }
                sleep time: 2, unit: 'SECONDS'
                withFolderProperties {
                  echo "second: ${env.key}"
                }
                """);

        WorkflowRun run = job.scheduleBuild2(0).waitForStart();
        r.waitForMessage("Sleeping for 2 sec", run);
        FolderProperties<?> properties = parent.getProperties().get(FolderProperties.class);
        properties.getProperties()[0].setValue("changed");
        parent.save();

        r.assertBuildStatusSuccess(r.waitForCompletion(run));
        assertNull(run.getAction(FolderPropertiesSnapshotAction.class));
        r.assertLogContains("first: initial", run);
        r.assertLogContains("second: changed", run);
    }

    @Test
    void caseVariantInCloserFolderSuppressesAncestorEarlyValue(JenkinsRule r) throws Exception {
        Folder ancestor = folder(r, "pipeline-case-precedence", true, property("foo", "ancestor"));
        Folder parent = folder(ancestor, "child", false, property("FOO", "child"));
        WorkflowJob job = PipelineTestHelper.createJob(parent, "pipeline", """
                echo "before: ${env.FOO}"
                withFolderProperties {
                  echo "inside: ${env.foo}"
                }
                """);

        WorkflowRun run = r.assertBuildStatusSuccess(job.scheduleBuild2(0));

        assertNull(run.getAction(FolderPropertiesSnapshotAction.class));
        r.assertLogContains("before: null", run);
        r.assertLogContains("inside: child", run);
    }
}
