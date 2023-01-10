package app;

import javax.annotation.processing.Generated;
import org.gradle.workers.WorkParameters;

@Generated("com.palantir.gradle.autoparallelizable.AutoParallelizableProcessor")
interface SomethingWorkParams extends WorkParameters, Something.Params {}
