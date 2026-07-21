package com.mig82.folders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cloudbees.hudson.plugins.folder.Folder;
import com.mig82.folders.properties.FolderProperties;
import com.mig82.folders.properties.FolderPropertyResolver;
import com.mig82.folders.properties.PropertiesLoader;
import com.mig82.folders.properties.ResolvedFolderProperties;
import com.mig82.folders.properties.StringProperty;
import java.util.List;
import java.util.Map;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class FolderPropertyResolverTest {

    @Test
    void resolvesHierarchyAndTracksSources(JenkinsRule r) throws Exception {
        Folder parent = folder(r, "resolver-parent", true, property("shared", "parent"), property("parent", "one"));
        Folder child = folder(parent, "child", true, property("shared", "child"), property("child", "two"));
        WorkflowJob job = child.createProject(WorkflowJob.class, "job");

        ResolvedFolderProperties resolved = new FolderPropertyResolver().resolve(job);

        assertEquals(Map.of("shared", "child", "child", "two", "parent", "one"), resolved.getValues());
        assertEquals(resolved.getValues(), resolved.getBuildStartValues());
        assertEquals(resolved.getValues(), PropertiesLoader.loadFolderProperties(job));
        assertEquals("resolver-parent/child", resolved.getSources().get("shared"));
        assertEquals("resolver-parent", resolved.getSources().get("parent"));
    }

    @Test
    void closestDefinitionAlsoControlsEarlyExposure(JenkinsRule r) throws Exception {
        Folder parent = folder(r, "resolver-exposure-parent", true, property("shared", "parent"));
        Folder child = folder(parent, "child", false, property("shared", "child"), property("scoped", "only"));
        WorkflowJob job = child.createProject(WorkflowJob.class, "job");

        ResolvedFolderProperties resolved = new FolderPropertyResolver().resolve(job);

        assertEquals("child", resolved.getValues().get("shared"));
        assertTrue(resolved.getBuildStartValues().isEmpty());
    }

    @Test
    void handlesMissingPropertiesAndDuplicateKeys(JenkinsRule r) throws Exception {
        Folder empty = r.jenkins.createProject(Folder.class, "resolver-empty");
        WorkflowJob emptyJob = empty.createProject(WorkflowJob.class, "empty-job");
        assertTrue(new FolderPropertyResolver().resolve(emptyJob).getValues().isEmpty());

        Folder duplicate = folder(
                r,
                "resolver-duplicate",
                false,
                property("duplicate", "first"),
                property("duplicate", "second"),
                property("empty", ""));
        WorkflowJob duplicateJob = duplicate.createProject(WorkflowJob.class, "duplicate-job");
        ResolvedFolderProperties resolved = new FolderPropertyResolver().resolve(duplicateJob);

        assertEquals(Map.of("duplicate", "first", "empty", ""), resolved.getValues());
        assertEquals(
                List.of("duplicate", "empty"), List.copyOf(resolved.getValues().keySet()));
        assertFalse(resolved.getSources().isEmpty());
    }

    @Test
    void returnsImmutableMaps(JenkinsRule r) throws Exception {
        Folder folder = folder(r, "resolver-immutable", true, property("key", "value"));
        WorkflowJob job = folder.createProject(WorkflowJob.class, "job");
        ResolvedFolderProperties resolved = new FolderPropertyResolver().resolve(job);

        assertThrows(
                UnsupportedOperationException.class, () -> resolved.getValues().put("other", "value"));
        assertThrows(
                UnsupportedOperationException.class,
                () -> resolved.getBuildStartValues().put("other", "value"));
        assertThrows(
                UnsupportedOperationException.class, () -> resolved.getSources().put("other", "folder"));
    }

    static Folder folder(JenkinsRule r, String name, boolean exposeAtBuildStart, StringProperty... properties)
            throws Exception {
        return configure(r.jenkins.createProject(Folder.class, name), exposeAtBuildStart, properties);
    }

    static Folder folder(Folder parent, String name, boolean exposeAtBuildStart, StringProperty... properties)
            throws Exception {
        return configure(parent.createProject(Folder.class, name), exposeAtBuildStart, properties);
    }

    static Folder configure(Folder folder, boolean exposeAtBuildStart, StringProperty... properties) throws Exception {
        FolderProperties<?> folderProperties = new FolderProperties<>();
        folderProperties.setExposeAtBuildStart(exposeAtBuildStart);
        folderProperties.setProperties(properties);
        folder.addProperty(folderProperties);
        return folder;
    }

    static StringProperty property(String key, String value) {
        return new StringProperty(key, value);
    }
}
