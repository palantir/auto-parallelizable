package app;

import org.gradle.workers.WorkParameters;

interface SomethingWorkParams extends WorkParameters, Something.Params {}
