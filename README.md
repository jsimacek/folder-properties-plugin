# Folder Properties

The Folder Properties plugin allows users with config permission to define
properties for a folder which can then be used by jobs contained
within it or in any of its sub-folders.

The aim here is to remove the need to specify the same properties over
and over again for all the jobs inside a folder.

## Requirements

The plugin supports Jenkins 2.568.1 LTS and newer releases. Supported Jenkins
controllers must run on Java 21 or a newer Java version supported by Jenkins.
Older Jenkins and Java 17 installations are not supported.

## How to use?

To configure, just create a
[folder](https://plugins.jenkins.io/cloudbees-folder/),
go to its configuration page and add as many properties as you need
under the `Folder Properties` section.

In structures where two or more folders are nested, any property defined for a folder will be overridden by any other
property of the same name defined by one of its sub-folders.

Property names follow Jenkins environment semantics and are case-insensitive
for precedence. For example, `FOO` and `foo` are treated as the same key, and
the definition in the closest folder wins.

![](docs/images/folder-properties-config.png)

By default, folder properties keep their original opt-in behavior: Freestyle
jobs need the build wrapper and Pipeline jobs need `withFolderProperties` or
the corresponding Declarative option.

Enable **Expose these properties at build start** on a folder when its values
must be available before a wrapper or Pipeline step can run. This is required
for top-level Declarative `environment` expressions and Pipeline-from-SCM job
definitions. The setting applies to properties defined on that folder. If a
closer folder defines the same key, the closer definition controls both its
value and whether it is exposed at build start.

Build-start values are defaults: build parameters and explicit Pipeline
environment declarations can override them. When at least one resolved key is
exposed at build start, Jenkins snapshots the resolved folder properties the
first time the build requests its environment. The same values are then used
for SCM configuration, Pipeline execution, and controller restart recovery
even if folder configuration changes while the build runs.

When build-start exposure is not enabled, no persistent snapshot is added to
the build. Freestyle wrappers and `withFolderProperties` resolve the current
folder configuration when they run, preserving the behavior of earlier plugin
versions.

## Freestyle Jobs

Freestyle jobs must opt into the `Folder Properties` build wrapper from
the `Build Environment` section of their configuration page in order to
be able to access these properties as they would any other environment
variable.

![](docs/images/folder-properties-freestyle-config.png)

Only then will they inherit properties defined by their parent or ancestor folders —e.g. Running `echo $FOO` in a Shell build step :

![](docs/images/freestyle-example-1.png)

#### SCM Step in Freestyle Jobs

Freestyle jobs can also use folder properties to **define SCM parameters** — e.g. By defining an `SCM_URL` property pointing to the Git repository and a `BRANCH_SELECTOR` property pointing to the branch, tag or commit to be checked out:

![](docs/images/freestyle-example-scm-1.png)

Then, descendant freestyle jobs can use that either as `$SCM_URL` and `$BRANCH_SELECTOR` :

![](docs/images/freestyle-example-scm-2.png)

 or as `${SCM_URL}` and `${BRANCH_SELECTOR}` :

![](docs/images/freestyle-example-scm-3.png)

## Pipeline Jobs

Pipeline jobs can use step `withFolderProperties` to access them :

**Using folder properties in a pipeline job**

``` groovy
withFolderProperties{
    echo("Foo: ${env.FOO}")
}
```

Declarative pipeline jobs can also use the `options` directive to leverage folder properties as follows:

``` groovy
pipeline {
    agent any
    options {
        withFolderProperties()
    }
    stages {
        stage('Test') {
            steps {
                echo("Foo: ${env.FOO}")
            }
        }
    }
}
```

When build-start exposure is enabled on the folder, a top-level Declarative
`environment` block can derive values from folder properties:

```groovy
pipeline {
    agent any
    options {
        withFolderProperties()
    }
    environment {
        ARTIFACT_PATH = "${env.PROJECT_NAME}/artifacts"
    }
    stages {
        stage('Test') {
            steps {
                echo("Artifact path: ${env.ARTIFACT_PATH}")
            }
        }
    }
}
```

The block step propagates its body's result and can remain active across a
Jenkins controller restart.

### Pipeline from SCM

Build-start properties are also available while Jenkins creates a
Pipeline-from-SCM definition. They can be used in the Git repository URL,
branch, and Jenkinsfile script path when lightweight checkout is disabled.

Git lightweight checkout currently expands the branch and script path, but the
Git plugin does not expand environment variables in the remote URL on that
path. Use a fixed remote URL or disable lightweight checkout when the URL
contains a folder property reference.

## Job DSL

In Job DSL scripts you can define folder properties like so :

**Job DSL example**

``` groovy
folder('my folder') {
    properties {
        folderProperties {
            exposeAtBuildStart(true)
            properties {
                stringProperty {
                    key('FOO')
                    value('foo1')
                }
            }
        }
    }
}
```

Folder properties are ordinary environment variables, not credentials. Do not
store secrets in them; use Jenkins credentials and a credentials-binding step
for sensitive values.

## Upgrade compatibility

Existing folder configurations do not require migration. The new
`exposeAtBuildStart` setting defaults to `false`, so existing jobs retain their
scoped and dynamically resolved behavior.

The corrected `withFolderProperties` step now returns its body's result instead
of always returning `null`. Pipelines that explicitly depended on the old
incorrect `null` result should be updated.

Before upgrading from a version with the synchronous implementation, allow
builds currently executing inside `withFolderProperties` to finish. Those old
in-flight executions were not restartable. Builds started after the upgrade use
the resumable implementation.

Builds using build-start exposure persist a
`FolderPropertiesSnapshotAction`. Back up Jenkins before deployment and avoid
downgrading without validating old-data handling while such build records are
retained, since older plugin versions do not contain that action class.

## Authors & Contributors

* [Miguelángel Fernández Mendoza](https://github.com/mig82).
* [GongYi](https://github.com/topikachu).
* [Stefan Hirche](https://github.com/StefanHirche)
* [Deepak Gupta](https://github.com/Mr-DG-Wick)

## References

* [Site](https://plugins.jenkins.io/folder-properties/)
* [Dependencies](https://plugins.jenkins.io/folder-properties/dependencies/)
* [Javadoc](https://javadoc.jenkins.io/plugin/folder-properties/)
