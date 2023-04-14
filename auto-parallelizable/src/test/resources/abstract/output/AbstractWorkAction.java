package app;

import javax.annotation.processing.Generated;
import org.gradle.workers.WorkAction;

@Generated("com.palantir.gradle.autoparallelizable.AutoParallelizableProcessor")
abstract class AbstractWorkAction implements WorkAction<AbstractWorkParams> {
    @SuppressWarnings("RedundantModifier")
    public AbstractWorkAction() {}

    @Override
    public final void execute() {
        Abstract.action(getParameters());
    }
}
