package app;

import com.palantir.gradle.autoparallelizable.AutoParallelizable;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;

@AutoParallelizable
public final class Something {
    public abstract class SomethingTask extends SomethingTaskImpl {
        public SomethingTask() {
            setDescription("lol");
        }
    }

    interface Params {
        Property<String> getSomeString();

        RegularFileProperty getSomeFile();
    }

    static void action(Params params) {
        System.out.println("Hello " + params.getSomeString().get());
    }
}
