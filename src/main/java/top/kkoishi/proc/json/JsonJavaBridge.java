package top.kkoishi.proc.json;

import sun.misc.Unsafe;
import top.kkoishi.proc.property.BuildFailedException;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

/**
 * Convert json object to java or java object to json.
 *
 * @author KKoishi_
 */
public final class JsonJavaBridge {
    static Unsafe sharedUnsafe = JsonSupportKt.getUNSAFE();

    private JsonJavaBridge () {
    }

    @Deprecated
    @SuppressWarnings({"unchecked", "EnhancedSwitchMigration"})
    public static <T> T build (MappedJsonObject jsonObject, JsonBuildInfo<T> info)
            throws InstantiationException, BuildFailedException, NoSuchFieldException,
            IllegalAccessException {
        final T inst = (T) sharedUnsafe.allocateInstance(info.getClz());
        for (final JsonBuildInfo.FieldRef field : info.fields()) {
            final Field f = info.getClz().getDeclaredField(field.getName());
            f.setAccessible(true);
            final long offset = sharedUnsafe.objectFieldOffset(f);
            if (field.getClz() == null) {
                switch (field.getType()) {
                    case INT: {
                        sharedUnsafe.compareAndSwapInt(inst, offset, 0,
                                jsonObject.getNumber(field.getName()).intValue());
                        break;
                    }
                    case LONG: {
                        sharedUnsafe.compareAndSwapLong(info, offset, 0,
                                jsonObject.getNumber(field.getName()).longValue());
                        break;
                    }
                    case FLOAT: {
                        sharedUnsafe.compareAndSwapObject(info, offset, f.get(inst),
                                jsonObject.getNumber(field.getName()).floatValue());
                        break;
                    }
                    case SHORT: {
                        sharedUnsafe.compareAndSwapObject(info, offset, f.get(inst),
                                jsonObject.getNumber(field.getName()).shortValue());
                        break;
                    }
                    case DOUBLE: {
                        sharedUnsafe.compareAndSwapObject(info, offset, f.get(inst),
                                jsonObject.getNumber(field.getName()).doubleValue());
                        break;
                    }
                    case STRING: {
                        sharedUnsafe.compareAndSwapObject(info, offset, f.get(inst),
                                jsonObject.getString(field.getName()));
                        break;
                    }
                    case BOOLEAN: {
                        sharedUnsafe.compareAndSwapObject(info, offset, f.get(inst),
                                jsonObject.getBool(field.getName()));
                        break;
                    }
                    case BYTE: {
                        sharedUnsafe.compareAndSwapObject(info, offset, f.get(inst),
                                jsonObject.getNumber(field.getName()).byteValue());
                        break;
                    }
                    default: {
                        throw new BuildFailedException();
                    }
                }
            } else {
                switch (field.getType()) {
                    case CLASS: {

                        break;
                    }
                    case ARRAY: {
                        //create array
                        break;
                    }
                    default: {
                        throw new BuildFailedException();
                    }
                }
            }
        }
        return inst;
    }

    @SuppressWarnings("unchecked")
    public static <T> T cast (Class<T> clz, MappedJsonObject jsonObject)
            throws BuildFailedException, InstantiationException, NoSuchFieldException,
            IllegalAccessException {
        if (clz.isArray() || clz.isEnum() || clz.isInterface()) {
            throw new BuildFailedException();
        }
        final T inst = (T) sharedUnsafe.allocateInstance(clz);
        for (final Map.Entry<String, Object> jsonEntry : jsonObject.entrySet()) {
            final Field f = findField(clz, jsonEntry.getKey());
            f.setAccessible(true);
            final long offset = sharedUnsafe.objectFieldOffset(f);
            if (jsonEntry.getValue() instanceof final MappedJsonObject cpy) {
                sharedUnsafe.compareAndSwapObject(inst, offset, f.get(inst), cast0(f.getType(), cpy));
            } else {
                trySetField(inst, offset, f.get(inst), jsonEntry.getValue(), f.getType());
            }
        }
        return inst;
    }

    public static Field findField (Class<?> clz, String name) throws NoSuchFieldException {
        Field f;
        try {
            f = clz.getDeclaredField(name);
            return f;
        } catch (NoSuchFieldException e) {
            final Class<?> superClz = clz.getSuperclass();
            if (superClz == Object.class || superClz == null) {
                throw new NoSuchFieldException(e.getMessage() + "<-while searching the class:" + clz);
            }
            return findField(superClz, name);
        }
    }

    private static <T> void trySetField (T inst, long offset, Object o, Object value, Class<?> clz) {
        if (clz == int.class) {
            sharedUnsafe.compareAndSwapInt(inst, offset, 0, ((Number) value).intValue());
        } else if (clz == long.class) {
            sharedUnsafe.compareAndSwapLong(inst, offset, 0, ((Number) value).longValue());
        } else if (clz == float.class) {
            sharedUnsafe.compareAndSwapObject(inst, offset, o, ((Number) value).floatValue());
        } else if (clz == double.class) {
            sharedUnsafe.compareAndSwapObject(inst, offset, o, ((Number) value).doubleValue());
        } else if (clz == short.class) {
            sharedUnsafe.compareAndSwapObject(inst, offset, o, ((Number) value).shortValue());
        } else if (clz == byte.class) {
            sharedUnsafe.compareAndSwapObject(inst, offset, o, ((Number) value).byteValue());
        } else {
            sharedUnsafe.compareAndSwapObject(inst, offset, o, value);
        }
    }

    @SuppressWarnings("UnnecessaryContinue")
    private static Object cast0 (Class<?> clz, MappedJsonObject jsonObject)
            throws BuildFailedException, InstantiationException, NoSuchFieldException,
            IllegalAccessException {
        if (clz.isArray() || clz.isEnum() || clz.isInterface()) {
            throw new BuildFailedException();
        }
        final Object inst = sharedUnsafe.allocateInstance(clz);
        for (final Map.Entry<String, Object> jsonEntry : jsonObject.entrySet()) {
            final Field f = findField(clz, jsonEntry.getKey());
            f.setAccessible(true);
            final long offset = sharedUnsafe.objectFieldOffset(f);
            if (jsonEntry.getValue() instanceof final MappedJsonObject cpy) {
                final var annotate = f.getAnnotation(TargetClass.class);
                if (annotate != null) {
                    for (final String className : annotate.classNames()) {
                        try {
                            if (sharedUnsafe.compareAndSwapObject(inst, offset, f.get(inst), cast0(Class.forName(className), cpy))) {
                                break;
                            }
                        } catch (Exception e) {
                            continue;
                        }
                    }
                }
                sharedUnsafe.compareAndSwapObject(inst, offset, f.get(inst), cast0(f.getType(), cpy));
            } else {
                if (jsonEntry.getValue() != null && jsonEntry.getValue().getClass().isArray()) {
                    final var array = ((Object[]) jsonEntry.getValue());
                    int i = 0;
                    for (final Object o : array) {
                        if (o instanceof final MappedJsonObject cursor) {
                            final var annotate = f.getAnnotation(TargetClass.class);
                            if (annotate != null) {
                                for (final String className : annotate.classNames()) {
                                    System.out.println("Looking for:" + className);
                                    try {
                                        array[i] = cast0(Class.forName(className), cursor);
                                        break;
                                    } catch (Exception e) {
                                        continue;
                                    }
                                }
                            }
                            sharedUnsafe.compareAndSwapObject(inst, offset, f.get(inst), cursor);
                        } else {
                            sharedUnsafe.compareAndSwapObject(inst, offset, f.get(inst), jsonEntry.getValue());
                        }
                        ++i;
                    }
                } else {
                    sharedUnsafe.compareAndSwapObject(inst, offset, f.get(inst), jsonEntry.getValue());
                }
            }
        }
        return inst;
    }

    public static <T> T cast (Class<T> clz, JsonObject jsonObject)
            throws InvocationTargetException, NoSuchMethodException, InstantiationException,
            IllegalAccessException, BuildFailedException, NoSuchFieldException {
        return cast(clz, MappedJsonObject.cast(jsonObject, HashMap.class));
    }
}