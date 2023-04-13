package app;

import javax.annotation.processing.Generated;
import javax.inject.Inject;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.workers.WorkerExecutor;

@Generated("com.palantir.gradle.autoparallelizable.AutoParallelizableProcessor")
abstract class NestedTaskImpl extends DefaultTask implements Nested.Params {
    @Inject
    protected abstract WorkerExecutor getWorkerExecutor();

    @TaskAction
    public final void execute() {
        getWorkerExecutor().noIsolation().submit(NestedWorkAction.class, params -> {
            params.getSomeString().set(this.getSomeString());
            params.getNestedInterface()
                    .getString()
                    .set(this.getNestedInterface().getString());
            params.getDoublyNestedInterface()
                    .getDoubleString()
                    .set(this.getDoublyNestedInterface().getDoubleString());
            params.getDoublyNestedInterface()
                    .getNestedInterface()
                    .getString()
                    .set(this.getDoublyNestedInterface().getNestedInterface().getString());
            params.getTripleNestedInterface()
                    .getTripleString()
                    .set(this.getTripleNestedInterface().getTripleString());
            params.getTripleNestedInterface()
                    .getTripleInteger()
                    .set(this.getTripleNestedInterface().getTripleInteger());
            params.getTripleNestedInterface()
                    .getDoublyNestedInterface()
                    .getDoubleString()
                    .set(this.getTripleNestedInterface()
                            .getDoublyNestedInterface()
                            .getDoubleString());
            params.getTripleNestedInterface()
                    .getDoublyNestedInterface()
                    .getNestedInterface()
                    .getString()
                    .set(this.getTripleNestedInterface()
                            .getDoublyNestedInterface()
                            .getNestedInterface()
                            .getString());
        });
    }
}
