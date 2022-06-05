package top.kkoishi.proc.json;

import kotlin.Pair;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * This json object is also used as json AST.
 *
 * @author KKoishi_
 */
public final class JsonObject {
    final ArrayList<Pair<String, Object>> data;

    public JsonObject () {
        this(8);
    }

    public JsonObject (int initialCapability) {
        this(new ArrayList<>(initialCapability));
    }

    private JsonObject (ArrayList<Pair<String, Object>> data) {
        this.data = data;
    }

    public Pair<String, Object> get (int index) {
        return data.get(index);
    }

    public Pair<String, Object> set (int index, Pair<String, Object> nValue) {
        final var oValue = data.get(index);
        data.set(index, nValue);
        return oValue;
    }

    public Pair<String, Object> set (int index, String key, Object value) {
        return set(index, new Pair<>(key, value));
    }

    public ArrayList<Pair<String, Object>> getData () {
        return data;
    }

    @SuppressWarnings({"unchecked", "unused"})
    public static <T> T cast (Class<T> clz, JsonObject jsonObject)
            throws InstantiationException, NoSuchFieldException, IllegalAccessException {
        final T inst = (T) JsonSupportKt.getUNSAFE().allocateInstance(clz);
        for (Pair<String, Object> datum : jsonObject.data) {
            final var f = clz.getDeclaredField(datum.getFirst());
            f.setAccessible(true);
            f.set(jsonObject, datum.getSecond() instanceof final JsonObject obj ? cast(Object.class, obj) : datum.getSecond());
        }
        return inst;
    }

    @Override
    public String toString () {
        final StringBuilder sb = new StringBuilder("JsonObject{");
        if (!data.isEmpty()) {
            final int size = data.size() - 1;
            for (int i = 0; i < size; i++) {
                final var datum = data.get(i);
                sb.append('(').append(datum.getFirst()).append(" = ");
                if (datum.getSecond() != null && datum.getSecond().getClass().isArray()) {
                    sb.append(Arrays.deepToString((Object[]) datum.getSecond()));
                } else {
                    sb.append(datum.getSecond());
                }
                sb.append("), ");
            }
            final var datum = data.get(size);
            sb.append('(').append(datum.getFirst()).append(" = ");
            if (datum.getSecond() != null && datum.getSecond().getClass().isArray()) {
                try {
                    sb.append(Arrays.deepToString((Object[]) datum.getSecond()));
                } catch (Exception e) {
                    sb.append(datum.getSecond());
                }
            } else {
                sb.append(datum.getSecond());
            }
            sb.append(')');
        }
        return sb.append('}').toString();
    }
}
