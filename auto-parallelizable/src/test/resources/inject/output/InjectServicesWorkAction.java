package app;

import javax.annotation.processing.Generated;
import javax.inject.Inject;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.process.ExecOperations;
import org.gradle.workers.WorkAction;

@Generated("com.palantir.gradle.autoparallelizable.AutoParallelizableProcessor")
abstract class InjectServicesWorkAction implements WorkAction<InjectServicesWorkParams> {
    @SuppressWarnings("RedundantModifier")
    public InjectServicesWorkAction() {}

    @Inject
    protected abstract ExecOperations getExecOperations();

    @Inject
    protected abstract ProviderFactory getProviderFactory();

    @Override
    public final void execute() {
        InjectServices.action(getParameters(), getExecOperations(), getProviderFactory());
    }
}
