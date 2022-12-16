package app;

import javax.inject.Inject;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.workers.WorkerExecutor;

abstract class SomethingTaskImpl extends DefaultTask implements Something.Params {
    @Inject
    protected abstract WorkerExecutor getWorkerExecutor();

    @TaskAction
    public final void execute() {
        getWorkerExecutor().noIsolation().submit(SomethingWorkAction.class, params -> {
            params.getSomeString().set(getSomeString());
            params.getSomeFile().set(getSomeFile());
        });
    }
}