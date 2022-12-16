package app;

import org.gradle.workers.WorkAction;

abstract class SomethingWorkAction implements WorkAction<SomethingWorkParams> {
    @Override
    public final void execute() {
        Something.action(getParameters());
    }
}
