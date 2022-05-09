package top.kkoishi.proc.property;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

public interface PropertiesLoader<K, V> extends Serializable {

    String getCommit ();

    void load (InputStream in, Charset charset) throws IOException, TokenizeException, BuildFailedException, LoaderException;

    default void load (InputStream in) throws TokenizeException, BuildFailedException, LoaderException, IOException {
        load(in, StandardCharsets.UTF_8);
    }

    void load (Reader reader) throws TokenizeException, BuildFailedException, LoaderException, IOException;

    void load (PropertiesLoader<K, V> loader);

    void load (String in) throws TokenizeException, BuildFailedException, LoaderException;

    void load (File src, Charset charset) throws IOException, TokenizeException, BuildFailedException, LoaderException;

    void load (File src, boolean transUnicode) throws IOException, TokenizeException, BuildFailedException, LoaderException;

    default void store (OutputStream out, Charset charset) throws IOException, LoaderException {
        out.write(prepare().getBytes(charset));
    }

    default void store (OutputStream out) throws IOException, LoaderException {
        store(out, StandardCharsets.UTF_8);
    }

    default void store (Writer writer) throws IOException, LoaderException {
        writer.write(prepare());
    }

    default void store (File target, Charset charset) throws LoaderException, IOException {
        Files.fWrite(target, prepare().getBytes(charset));
    }

    default void store (File file, boolean transUnicode) throws LoaderException, IOException {
        if (transUnicode) {
            Files.fWrite(file, (AbstractPropertiesLoader.Unicode.encodeExcept(prepare())).getBytes(StandardCharsets.US_ASCII));
        } else {
            store(file, StandardCharsets.UTF_8);
        }
    }

    String prepare () throws LoaderException;

    int size ();

    boolean isEmpty ();

    boolean containsKey (Object key);

    boolean containsValue (Object value);

    V get (Object key);

    V put (K key, V value);

    V remove (Object key);

    void putAll (Map<? extends K, ? extends V> m);

    void clear ();

    Set<K> keySet ();

    Collection<V> values ();

    Set<Map.Entry<K, V>> entrySet ();

    default V getOrDefault (Object key, V defaultValue) {
        V v;
        return (((v = get(key)) != null) || containsKey(key)) ?
                v : defaultValue;
    }

    default void forEach (BiConsumer<? super K, ? super V> action) {
        Objects.requireNonNull(action);
        for (Map.Entry<K, V> entry : entrySet()) {
            K k;
            V v;
            try {
                k = entry.getKey();
                v = entry.getValue();
            } catch (IllegalStateException ise) {
                // this usually means the entry is no longer in the map.
                throw new ConcurrentModificationException(ise);
            }
            action.accept(k, v);
        }
    }

    default void replaceAll (BiFunction<? super K, ? super V, ? extends V> function) {
        Objects.requireNonNull(function);
        for (Map.Entry<K, V> entry : entrySet()) {
            K k;
            V v;
            try {
                k = entry.getKey();
                v = entry.getValue();
            } catch (IllegalStateException ise) {
                // this usually means the entry is no longer in the map.
                throw new ConcurrentModificationException(ise);
            }
            // ise thrown from function is not a cme.
            v = function.apply(k, v);
            try {
                entry.setValue(v);
            } catch (IllegalStateException ise) {
                // this usually means the entry is no longer in the map.
                throw new ConcurrentModificationException(ise);
            }
        }
    }

    default V putIfAbsent (K key, V value) {
        V v = get(key);
        if (v == null) {
            v = put(key, value);
        }
        return v;
    }

    default boolean remove (Object key, Object value) {
        Object curValue = get(key);
        if (!Objects.equals(curValue, value) ||
                (curValue == null && !containsKey(key))) {
            return false;
        }
        remove(key);
        return true;
    }

    default boolean replace (K key, V oldValue, V newValue) {
        Object curValue = get(key);
        if (!Objects.equals(curValue, oldValue) ||
                (curValue == null && !containsKey(key))) {
            return false;
        }
        put(key, newValue);
        return true;
    }

    default V replace (K key, V value) {
        V curValue;
        if (((curValue = get(key)) != null) || containsKey(key)) {
            curValue = put(key, value);
        }
        return curValue;
    }

    default V computeIfAbsent (K key, Function<? super K, ? extends V> mappingFunction) {
        Objects.requireNonNull(mappingFunction);
        V v;
        if ((v = get(key)) == null) {
            V newValue;
            if ((newValue = mappingFunction.apply(key)) != null) {
                put(key, newValue);
                return newValue;
            }
        }
        return v;
    }

    default V computeIfPresent (K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        Objects.requireNonNull(remappingFunction);
        V oldValue;
        if ((oldValue = get(key)) != null) {
            V newValue = remappingFunction.apply(key, oldValue);
            if (newValue != null) {
                put(key, newValue);
                return newValue;
            } else {
                remove(key);
                return null;
            }
        } else {
            return null;
        }
    }

    default V compute (K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        Objects.requireNonNull(remappingFunction);
        V oldValue = get(key);

        V newValue = remappingFunction.apply(key, oldValue);
        if (newValue == null) {
            if (oldValue != null || containsKey(key)) {
                remove(key);
            }
            return null;
        } else {
            put(key, newValue);
            return newValue;
        }
    }

    default V merge (K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        Objects.requireNonNull(remappingFunction);
        Objects.requireNonNull(value);
        V oldValue = get(key);
        V newValue = (oldValue == null) ? value :
                remappingFunction.apply(oldValue, value);
        if (newValue == null) {
            remove(key);
        } else {
            put(key, newValue);
        }
        return newValue;
    }

    default Map<K, V> toMap () {
        return entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> b));
    }

    default ConcurrentMap<K, V> toConcurrentMap () {
        return entrySet().stream().collect(Collectors.toConcurrentMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> b));
    }
}
