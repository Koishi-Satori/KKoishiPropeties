package top.kkoishi.proc.property;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public abstract class AbstractBuilderPropertiesLoader<K, V> implements Serializable, PropertiesLoader<K, V> {
    protected final Map<K, V> proc = new HashMap<>();

    protected Builder<K> keyBuilder;

    protected Builder<V> valueBuilder;

    protected AbstractBuilderPropertiesLoader (AbstractBuilderPropertiesLoader<K, V> loader) {
        this(loader.keyBuilder, loader.valueBuilder);
        load(loader);
    }

    protected AbstractBuilderPropertiesLoader (Builder<K> keyBuilder, Builder<V> valueBuilder) {
        this.keyBuilder = keyBuilder;
        this.valueBuilder = valueBuilder;
    }

    public Builder<K> getKeyBuilder () {
        return keyBuilder;
    }

    public void setKeyBuilder (Builder<K> keyBuilder) {
        this.keyBuilder = keyBuilder;
    }

    public Builder<V> getValueBuilder () {
        return valueBuilder;
    }

    public void setValueBuilder (Builder<V> valueBuilder) {
        this.valueBuilder = valueBuilder;
    }

    @Override
    public String getCommit () {
        return null;
    }

    @Override
    public void load (InputStream in, Charset charset) throws IOException, TokenizeException, BuildFailedException, LoaderException {

    }

    @Override
    public void load (Reader reader) throws TokenizeException, BuildFailedException, LoaderException, IOException {

    }

    @Override
    public void load (PropertiesLoader<K, V> loader) {

    }

    @Override
    public void load (String in) throws TokenizeException, BuildFailedException, LoaderException {
        load0(in);
    }

    @Override
    public void load (File src, Charset charset) throws IOException, TokenizeException, BuildFailedException, LoaderException {

    }

    @Override
    public void load (File src, boolean transUnicode) throws IOException, TokenizeException, BuildFailedException, LoaderException {

    }

    @Override
    public String prepare () throws LoaderException {
        return null;
    }

    @Override
    public int size () {
        return 0;
    }

    @Override
    public boolean isEmpty () {
        return false;
    }

    @Override
    public boolean containsKey (Object key) {
        return false;
    }

    @Override
    public boolean containsValue (Object value) {
        return false;
    }

    @Override
    public V get (Object key) {
        return null;
    }

    @Override
    public V put (K key, V value) {
        return null;
    }

    @Override
    public V remove (Object key) {
        return null;
    }

    @Override
    public void putAll (Map<? extends K, ? extends V> m) {

    }

    @Override
    public void clear () {

    }

    @Override
    public Set<K> keySet () {
        return null;
    }

    @Override
    public Collection<V> values () {
        return null;
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet () {
        return null;
    }

    protected void load0 (String in) throws TokenizeException, BuildFailedException, LoaderException {
        loadImpl(removeCommit(in));
    }

    protected abstract void loadImpl (String noCommit) throws TokenizeException, BuildFailedException, LoaderException;

    protected abstract String removeCommit (String in);
}
