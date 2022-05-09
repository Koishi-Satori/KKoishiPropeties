package top.kkoishi.proc.json;

/**
 * @author KKoishi_
 */
@FunctionalInterface
interface BuilderRef<T> {
    /**
     * Build the instance from json string.
     *
     * @param jsonString json string.
     * @return instance.
     */
    T build (String jsonString);
}
