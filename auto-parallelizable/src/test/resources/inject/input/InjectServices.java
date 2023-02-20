package app;

import com.palantir.gradle.autoparallelizable.AutoParallelizable;
import com.palantir.gradle.autoparallelizable.AutoParallelizable.Inject;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.process.ExecOperations;

@AutoParallelizable
public final class InjectServices {
    public abstract class SomethingTask extends InjectServicesTaskImpl {}

    interface Params {}

    static void action(
            Params _params, @Inject ExecOperations _execOperations, @Inject ProviderFactory _providerFactory) {}
}
