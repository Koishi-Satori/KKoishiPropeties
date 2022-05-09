package top.kkoishi.proc.property;

import java.util.Objects;

@FunctionalInterface
public interface Builder<T> {
    /**
     * Build an instance of T.
     *
     * @param ags arguments.
     * @return instance.
     * @throws BuildFailedException if needed.
     */
    T build (Object... ags) throws BuildFailedException;

    Builder<String> SINGLE_STRING_BUILDER = args -> {
        Objects.requireNonNull(args);
        if (args.length != 1) {
            throw new BuildFailedException("Require single args, but got:" + args.length);
        } else {
            return (String) args[0];
        }
    };
}
