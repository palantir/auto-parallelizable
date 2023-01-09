<p align="right">
<a href="https://autorelease.general.dmz.palantir.tech/palantir/auto-parallelizable"><img src="https://img.shields.io/badge/Perform%20an-Autorelease-success.svg" alt="Autorelease"></a>
</p>

# auto-parallelizable

An annotation to easily make Gradle Tasks use the [Worker API](https://docs.gradle.org/current/userguide/worker_api.html), which is required for Gradle tasks to run in parallel *within the same project*.

## Usage

Add the library as an annotation processor:

```Gradle
dependencies {
    compileOnly 'com.palantir.gradle.auto-parallelizable:auto-parallelizable-annotations'
    annotationProcessor 'com.palantir.gradle.auto-parallelizable:auto-parallelizable'
}
```

Reload Gradle in IntelliJ.

Now you can write a container class that will generate the required files:

```java
@AutoParallelizable
final class MyCustom {
    // Create a package-private Params interface with you task
    // inputs/outputs as Gradle Managed Properties
    interface Params {
        @Input
        Property<String> getString();

        @OutputFile
        RegularFileProperty getOutput();
    }
    
    // Create a package-private static void action method here
    static void action(Params params) {
        // do your task action here
    }
}
```

Then implement a task next to this:

```java
public abstract class MyCustomTask extends MyCustomTaskImpl {
    public MyCustomTask() {
        // You can initialize values, description etc here
        getString().set("default value");
    }
}
```

Compile your code and the files `MyCustomTaskImpl`, `MyCustomWorkParams`, `MyCustomWorkAction` will be generated for you!
