package app;

import org.gradle.workers.WorkAction;

abstract class SomethingWorkAction implements WorkAction<SomethingWorkParams> {
    @SuppressWarnings("RedundantModifier")
    public SomethingWorkAction() {}

    @Override
    public final void execute() {
        Something.action(getParameters());
    }
}
