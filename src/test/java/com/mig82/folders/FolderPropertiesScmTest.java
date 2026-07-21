package com.mig82.folders;

import static com.mig82.folders.FolderPropertyResolverTest.folder;
import static com.mig82.folders.FolderPropertyResolverTest.property;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cloudbees.hudson.plugins.folder.Folder;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.plugins.git.BranchSpec;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.UserRemoteConfig;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import org.jenkinsci.plugins.workflow.cps.CpsScmFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class FolderPropertiesScmTest {

    @Test
    void expandsRepositoryAndBranchBeforeFreestyleCheckout(JenkinsRule r) throws Exception {
        Path repository = createRepository(r, "FREESTYLE_SCM_EXPANSION_WORKED");
        Folder parent = folder(
                r,
                "freestyle-scm",
                false,
                property("SCM_URL", repository.toUri().toString()),
                property("SCM_BRANCH", "*/main"));
        FreeStyleProject project = FreestyleTestHelper.createJob(parent, "job");
        project.setScm(scm("${SCM_URL}", "${SCM_BRANCH}"));

        boolean previous = GitSCM.ALLOW_LOCAL_CHECKOUT;
        GitSCM.ALLOW_LOCAL_CHECKOUT = true;
        try {
            FreeStyleBuild build = r.assertBuildStatusSuccess(project.scheduleBuild2(0));
            assertNotNull(build.getWorkspace());
            assertTrue(build.getWorkspace().child("ci/Jenkinsfile").exists());
        } finally {
            GitSCM.ALLOW_LOCAL_CHECKOUT = previous;
        }
    }

    @Test
    void expandsRepositoryBranchAndScriptPathForFullCheckout(JenkinsRule r) throws Exception {
        Path repository = createRepository(r, "FULL_SCM_EXPANSION_WORKED");
        Folder parent = folder(
                r,
                "issue-47-full",
                true,
                property("SCM_URL", repository.toUri().toString()),
                property("SCM_BRANCH", "*/main"),
                property("SCRIPT_PATH", "ci/Jenkinsfile"));
        GitSCM scm = scm("${SCM_URL}", "${SCM_BRANCH}");

        WorkflowRun run = buildFromScm(r, parent, scm, "${SCRIPT_PATH}", false);

        r.assertLogContains("FULL_SCM_EXPANSION_WORKED", run);
    }

    @Test
    void expandsBranchAndScriptPathForLightweightCheckoutWithFixedUrl(JenkinsRule r) throws Exception {
        Path repository = createRepository(r, "LIGHTWEIGHT_SCM_EXPANSION_WORKED");
        Folder parent = folder(
                r,
                "issue-47-lightweight",
                true,
                property("SCM_BRANCH", "*/main"),
                property("SCRIPT_PATH", "ci/Jenkinsfile"));
        GitSCM scm = scm(repository.toUri().toString(), "${SCM_BRANCH}");

        WorkflowRun run = buildFromScm(r, parent, scm, "${SCRIPT_PATH}", true);

        r.assertLogContains("LIGHTWEIGHT_SCM_EXPANSION_WORKED", run);
    }

    private static WorkflowRun buildFromScm(
            JenkinsRule r, Folder parent, GitSCM scm, String scriptPath, boolean lightweight) throws Exception {
        boolean previous = GitSCM.ALLOW_LOCAL_CHECKOUT;
        GitSCM.ALLOW_LOCAL_CHECKOUT = true;
        try {
            WorkflowJob job = parent.createProject(WorkflowJob.class, "pipeline");
            CpsScmFlowDefinition definition = new CpsScmFlowDefinition(scm, scriptPath);
            definition.setLightweight(lightweight);
            job.setDefinition(definition);
            return r.assertBuildStatusSuccess(job.scheduleBuild2(0));
        } finally {
            GitSCM.ALLOW_LOCAL_CHECKOUT = previous;
        }
    }

    private static GitSCM scm(String url, String branch) {
        return new GitSCM(
                List.of(new UserRemoteConfig(url, null, null, null)),
                List.of(new BranchSpec(branch)),
                false,
                Collections.emptyList(),
                null,
                null,
                Collections.emptyList());
    }

    private static Path createRepository(JenkinsRule r, String message) throws Exception {
        Path repository = Files.createTempDirectory(r.jenkins.getRootDir().toPath(), "folder-properties-git-");
        runGit(repository, "init", "--initial-branch=main");
        Path pipelineDirectory = Files.createDirectories(repository.resolve("ci"));
        Files.writeString(
                pipelineDirectory.resolve("Jenkinsfile"), "node { echo '" + message + "' }\n", StandardCharsets.UTF_8);
        runGit(repository, "add", "ci/Jenkinsfile");
        runGit(
                repository,
                "-c",
                "user.name=Folder Properties Test",
                "-c",
                "user.email=folder-properties@example.invalid",
                "commit",
                "-m",
                "Add Jenkinsfile");
        return repository;
    }

    private static void runGit(Path directory, String... arguments) throws Exception {
        String[] command = new String[arguments.length + 1];
        command[0] = "git";
        System.arraycopy(arguments, 0, command, 1, arguments.length);
        Process process = new ProcessBuilder(command)
                .directory(directory.toFile())
                .redirectErrorStream(true)
                .start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        if (process.waitFor() != 0) {
            throw new IOException("git command failed: " + output);
        }
    }
}
