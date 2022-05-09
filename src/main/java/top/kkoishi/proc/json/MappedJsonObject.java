package top.kkoishi.proc.json;

import kotlin.Pair;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

/**
 * Json object, but use map to store.
 *
 * @author KKoishi_
 */
public final class MappedJsonObject {
    private final Map<String, Object> data;

    public static MappedJsonObject cast (JsonObject o, Class<? extends Map<String, Object>> clz)
            throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        final Constructor<? extends Map<String, Object>> constructor = clz.getDeclaredConstructor();
        constructor.setAccessible(true);
        final MappedJsonObject mjo = new MappedJsonObject(constructor.newInstance());
        for (final Pair<String, Object> datum : o.data) {
            if (datum.getSecond() instanceof final JsonObject cpy) {
                mjo.data.put(datum.getFirst(), cast0(cpy, constructor));
            } else {
                mjo.data.put(datum.getFirst(), datum.getSecond());
            }
        }
        return mjo;
    }

    private static MappedJsonObject cast0 (JsonObject o, Constructor<? extends Map<String, Object>> constructor)
            throws InvocationTargetException, InstantiationException, IllegalAccessException {
        final MappedJsonObject mjo = new MappedJsonObject(constructor.newInstance());
        for (final Pair<String, Object> datum : o.data) {
            if (datum.getSecond() instanceof final JsonObject cpy) {
                mjo.data.put(datum.getFirst(), cast0(cpy, constructor));
            } else {
                mjo.data.put(datum.getFirst(), datum.getSecond());
            }
        }
        return mjo;
    }

    private MappedJsonObject (Map<String, Object> data) {
        this.data = data;
    }

    public Object get (String key) {
        return data.get(key);
    }

    public String getString (String key) {
        return (String) data.get(key);
    }

    public Number getNumber (String key) {
        return (Number) data.get(key);
    }

    public boolean getBool (String key) {
        return (boolean) data.get(key);
    }

    public MappedJsonObject getJsonObject (String key) {
        return (MappedJsonObject) data.get(key);
    }
}
