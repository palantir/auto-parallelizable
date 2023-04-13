package app;

import javax.annotation.processing.Generated;
import org.gradle.workers.WorkAction;

@Generated("com.palantir.gradle.autoparallelizable.AutoParallelizableProcessor")
abstract class NestedWorkAction implements WorkAction<NestedWorkParams> {
    @SuppressWarnings("RedundantModifier")
    public NestedWorkAction() {}

    @Override
    public final void execute() {
        Nested.action(getParameters());
    }
}
