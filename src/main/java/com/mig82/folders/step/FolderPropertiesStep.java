package com.mig82.folders.step;

import com.mig82.folders.environment.FolderPropertiesSnapshotAction;
import hudson.EnvVars;
import hudson.Extension;
import hudson.model.Run;
import hudson.model.TaskListener;
import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.util.Set;
import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * A custom pipeline step to retrieve folder properties
 * This step is able to run out of a node
 *
 * @author Miguelangel Fernandez Mendoza and Gong Yi
 */
public class FolderPropertiesStep extends Step implements Serializable {
    @DataBoundConstructor
    public FolderPropertiesStep() {}

    @Override
    public StepExecution start(StepContext stepContext) throws Exception {
        return new Execution(stepContext);
    }

    private static class Execution extends StepExecution {
        @Serial
        private static final long serialVersionUID = 1;

        public Execution(StepContext context) {
            super(context);
        }

        @Override
        public boolean start() throws Exception {
            Run<?, ?> run = getContext().get(Run.class);
            EnvVars envVars = new EnvVars(FolderPropertiesSnapshotAction.resolveValues(run));
            BodyInvoker bodyInvoker = getContext().newBodyInvoker();
            if (!envVars.isEmpty()) {
                bodyInvoker.withContext(EnvironmentExpander.merge(
                        getContext().get(EnvironmentExpander.class), new ExpanderImpl(envVars)));
            }
            bodyInvoker.withCallback(BodyExecutionCallback.wrap(getContext())).start();
            return false;
        }
    }

    private static final class ExpanderImpl extends EnvironmentExpander {
        @Serial
        private static final long serialVersionUID = 1;

        private final EnvVars overrides;

        ExpanderImpl(EnvVars overrides) {
            this.overrides = /* ensure serializability*/ new EnvVars(overrides);
        }

        @Override
        public void expand(EnvVars env) throws IOException, InterruptedException {
            // Distinct from EnvironmentExpander.constant since we are also expanding variables.
            env.overrideExpandingAll(overrides);
        }
    }

    @Extension
    public static class DescriptorImpl extends StepDescriptor {
        @Override
        public Set<Class<?>> getRequiredContext() {
            return Set.of(Run.class, TaskListener.class);
        }

        @Override
        public String getFunctionName() {
            return "withFolderProperties";
        }

        @Override
        public String getDisplayName() {
            return "A step to retrieve folder properties";
        }

        @Override
        public boolean takesImplicitBlockArgument() {
            return true;
        }
    }
}
