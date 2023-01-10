package app;

import javax.annotation.processing.Generated;
import org.gradle.workers.WorkAction;

@Generated("com.palantir.gradle.autoparallelizable.AutoParallelizableProcessor")
abstract class SomethingWorkAction implements WorkAction<SomethingWorkParams> {
    @SuppressWarnings("RedundantModifier")
    public SomethingWorkAction() {}

    @Override
    public final void execute() {
        Something.action(getParameters());
    }
}
