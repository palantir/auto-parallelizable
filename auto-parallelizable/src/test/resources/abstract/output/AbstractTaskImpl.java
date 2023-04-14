package app;

import javax.annotation.processing.Generated;
import javax.inject.Inject;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.workers.WorkerExecutor;

@Generated("com.palantir.gradle.autoparallelizable.AutoParallelizableProcessor")
abstract class AbstractTaskImpl extends DefaultTask implements Abstract.Params {
    @Inject
    protected abstract WorkerExecutor getWorkerExecutor();

    @TaskAction
    public final void execute() {
        getWorkerExecutor().noIsolation().submit(AbstractWorkAction.class, params -> {
            params.getAbstractParams()
                    .getSettableNonNestedString()
                    .set(this.getAbstractParams().getSettableNonNestedString());
        });
    }
}
