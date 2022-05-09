package top.kkoishi.proc.property;

@FunctionalInterface
public interface BiMutiBuilder<T, R> extends Builder<T> {
    /**
     * Build an instance of T.
     *
     * @param args arguments.
     * @return instance.
     * @throws BuildFailedException if needed.
     */
    @Override
    @SuppressWarnings("all")
    default T build (Object... args) throws BuildFailedException {
        return get((R[]) args);
    }

    T get (R[] args) throws BuildFailedException;
}
