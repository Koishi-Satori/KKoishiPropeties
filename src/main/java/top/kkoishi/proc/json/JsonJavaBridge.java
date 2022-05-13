package top.kkoishi.proc.json;

import kotlin.Pair;
import sun.misc.Unsafe;
import top.kkoishi.proc.property.BuildFailedException;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

/**
 * Convert json object to java or java object to json.
 *
 * @author KKoishi_
 */
public final class JsonJavaBridge {
    static final Class<Integer> INTEGER_CLASS = int.class;
    static final Class<Long> LONG_CLASS = long.class;
    static final Class<Float> FLOAT_CLASS = float.class;
    static final Class<Double> DOUBLE_CLASS = double.class;
    static final Class<Short> SHORT_CLASS = short.class;
    static final Class<Byte> BYTE_CLASS = byte.class;
    static final Class<Boolean> BOOLEAN_CLASS = boolean.class;
    static Unsafe sharedUnsafe = JsonSupportKt.getUNSAFE();

    private JsonJavaBridge () {
    }

    /**
     * Convert MappedJsonObject instance to Java Object instance.
     * Given class, and it will try to build an instance.
     * And by using <code>@TargetClass</code> Annotate,
     * you can build the class more accuracy.
     * If you do not use this Annotate, all the field
     * which is object will be set as MappedJsonObject instance.
     * <b><h3>But please make sure the key is name of field and
     * the value must be correct type!</h1></b>
     *
     * @param clz class of java object
     * @param jsonObject instance of JsonObject
     * @param <T> type
     * @return instance of java object
     * @see TargetClass
     * @see Unsafe#allocateInstance(Class)
     * @see Unsafe#objectFieldOffset(Field)
     * @see Unsafe#compareAndSwapObject(Object, long, Object, Object)
     * @see JsonJavaBridge#trySetField(Object, long, Object, Object, Class)
     * @see JsonJavaBridge#cast0(Class, MappedJsonObject)
     * @see Unsafe
     * @see JsonJavaBridge#sharedUnsafe
     * @see JsonSupportKt#jsonTokenCast(JsonParser.Token)
     * @throws BuildFailedException when failed to build instance
     * @throws InstantiationException thrown by sun.misc.Unsafe, if failed to allocate instance.
     * @throws NoSuchFieldException if the field does not exist.
     * @throws IllegalAccessException if it can not access the field.
     */
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
            cast_judge:
            if (jsonEntry.getValue() instanceof final MappedJsonObject cpy) {
                final var annotate = f.getAnnotation(TargetClass.class);
                if (annotate != null) {
                    for (final String className : annotate.classNames()) {
                        try {
                            if (sharedUnsafe.compareAndSwapObject(inst, offset, f.get(inst), cast0(Class.forName(className), cpy))) {
                                break cast_judge;
                            }
                        } catch (Exception ignore) {
                        }
                    }
                }
                sharedUnsafe.compareAndSwapObject(inst, offset, f.get(inst), cast0(f.getType(), cpy));
            } else {
                if (jsonEntry.getValue() != null && jsonEntry.getValue().getClass().isArray()) {
                    final var array = ((Object[]) jsonEntry.getValue());
                    int i = 0;
                    for (final Object o : array) {
                        annotation_judge:
                        if (o instanceof final MappedJsonObject cursor) {
                            final var annotate = f.getAnnotation(TargetClass.class);
                            if (annotate != null) {
                                for (final String className : annotate.classNames()) {
                                    try {
                                        array[i++] = cast0(Class.forName(className), cursor);
                                        break annotation_judge;
                                    } catch (Exception ignored) {
                                    }
                                }
                            }
                            sharedUnsafe.compareAndSwapObject(inst, offset, f.get(inst), cursor);
                        } else {
                            trySetField(inst, offset, f.get(inst), jsonEntry.getValue(), f.getType());
                        }
                        ++i;
                    }
                } else {
                    trySetField(inst, offset, f.get(inst), jsonEntry.getValue(), f.getType());
                }
            }
        }
        return inst;
    }

    /**
     * Find field by the given name and class.
     * Different from {@link Class#getDeclaredField(String) Class::getDeclaredField},
     * this method will get the fields belong to the class's superclass.
     *
     * @param clz class.
     * @param name field name.
     * @return field instance.
     * @throws NoSuchFieldException if there is no such field.
     */
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
        if (clz.isPrimitive()) {
            if (clz == INTEGER_CLASS) {
                sharedUnsafe.compareAndSwapInt(inst, offset, 0, ((Number) value).intValue());
            } else if (clz == LONG_CLASS) {
                sharedUnsafe.compareAndSwapLong(inst, offset, 0, ((Number) value).longValue());
            } else if (clz == FLOAT_CLASS) {
                sharedUnsafe.compareAndSwapObject(inst, offset, o, ((Number) value).floatValue());
            } else if (clz == DOUBLE_CLASS) {
                sharedUnsafe.compareAndSwapObject(inst, offset, o, ((Number) value).doubleValue());
            } else if (clz == SHORT_CLASS) {
                sharedUnsafe.compareAndSwapObject(inst, offset, o, ((Number) value).shortValue());
            } else if (clz == BYTE_CLASS) {
                sharedUnsafe.compareAndSwapObject(inst, offset, o, ((Number) value).byteValue());
            } else {
                throw new IllegalArgumentException("The basic type " + clz + " is illegal.");
            }
        } else {
            if (clz == Integer.class) {
                sharedUnsafe.compareAndSwapInt(inst, offset, 0, ((Number) value).intValue());
            } else if (clz == Long.class) {
                sharedUnsafe.compareAndSwapLong(inst, offset, 0, ((Number) value).longValue());
            } else if (clz == Float.class) {
                sharedUnsafe.compareAndSwapObject(inst, offset, o, ((Number) value).floatValue());
            } else if (clz == Double.class) {
                sharedUnsafe.compareAndSwapObject(inst, offset, o, ((Number) value).doubleValue());
            } else if (clz == Short.class) {
                sharedUnsafe.compareAndSwapObject(inst, offset, o, ((Number) value).shortValue());
            } else if (clz == Byte.class) {
                sharedUnsafe.compareAndSwapObject(inst, offset, o, ((Number) value).byteValue());
            } else {
                sharedUnsafe.compareAndSwapObject(inst, offset, o, value);
            }
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
            ss:
            if (jsonEntry.getValue() instanceof final MappedJsonObject cpy) {
                final var annotate = f.getAnnotation(TargetClass.class);
                if (annotate != null) {
                    for (final String className : annotate.classNames()) {
                        try {
                            if (sharedUnsafe.compareAndSwapObject(inst, offset, f.get(inst), cast0(Class.forName(className), cpy))) {
                                break ss;
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
                        s:
                        if (o instanceof final MappedJsonObject cursor) {
                            final var annotate = f.getAnnotation(TargetClass.class);
                            if (annotate != null) {
                                for (final String className : annotate.classNames()) {
                                    try {
                                        array[i++] = cast0(Class.forName(className), cursor);
                                        break s;
                                    } catch (Exception e) {
                                        continue;
                                    }
                                }
                            }
                            sharedUnsafe.compareAndSwapObject(inst, offset, f.get(inst), cursor);
                        } else {
                            trySetField(inst, offset, f.get(inst), jsonEntry.getValue(), f.getType());
                        }
                        ++i;
                    }
                } else {
                    trySetField(inst, offset, f.get(inst), jsonEntry.getValue(), f.getType());
                }
            }
        }
        return inst;
    }

    @SuppressWarnings("unchecked")
    public static <T> T cast (Class<T> clz, JsonObject jsonObject)
            throws InvocationTargetException, NoSuchMethodException, InstantiationException,
            IllegalAccessException, BuildFailedException, NoSuchFieldException {
        if (clz.isArray() || clz.isEnum() || clz.isInterface()) {
            throw new BuildFailedException();
        }
        final T inst = (T) sharedUnsafe.allocateInstance(clz);
        for (final Pair<String, Object> jsonEntry : jsonObject.data) {
            final Field f = findField(clz, jsonEntry.getFirst());
            f.setAccessible(true);
            final long offset = sharedUnsafe.objectFieldOffset(f);
            ss:
            if (jsonEntry.getSecond() instanceof final JsonObject cpy) {
                final var annotate = f.getAnnotation(TargetClass.class);
                if (annotate != null) {
                    for (final String className : annotate.classNames()) {
                        try {
                            if (sharedUnsafe.compareAndSwapObject(inst, offset, f.get(inst), cast0(Class.forName(className), cpy))) {
                                break ss;
                            }
                        } catch (Exception ignored) {
                        }
                    }
                }
                sharedUnsafe.compareAndSwapObject(inst, offset, f.get(inst), cast0(f.getType(), cpy));
            } else {
                if (jsonEntry.getSecond() != null && jsonEntry.getSecond().getClass().isArray()) {
                    final var array = ((Object[]) jsonEntry.getSecond());
                    int i = 0;
                    for (final Object o : array) {
                        s:
                        if (o instanceof final JsonObject cursor) {
                            final var annotate = f.getAnnotation(TargetClass.class);
                            if (annotate != null) {
                                for (final String className : annotate.classNames()) {
                                    try {
                                        array[i++] = cast0(Class.forName(className), cursor);
                                        break s;
                                    } catch (Exception ignore) {
                                    }
                                }
                            }
                            sharedUnsafe.compareAndSwapObject(inst, offset, f.get(inst), cursor);
                        } else {
                            trySetField(inst, offset, f.get(inst), jsonEntry.getSecond(), f.getType());
                        }
                        ++i;
                    }
                } else {
                    trySetField(inst, offset, f.get(inst), jsonEntry.getSecond(), f.getType());
                }
            }
        }
        return inst;
    }

    private static Object cast0 (Class<?> clz, JsonObject jsonObject) throws BuildFailedException, InstantiationException, NoSuchFieldException, IllegalAccessException {
        if (clz.isArray() || clz.isEnum() || clz.isInterface()) {
            throw new BuildFailedException();
        }
        final Object inst = sharedUnsafe.allocateInstance(clz);
        for (final Pair<String, Object> jsonEntry : jsonObject.data) {
            final Field f = findField(clz, jsonEntry.getFirst());
            f.setAccessible(true);
            final long offset = sharedUnsafe.objectFieldOffset(f);
            ss:
            if (jsonEntry.getSecond() instanceof final JsonObject cpy) {
                final var annotate = f.getAnnotation(TargetClass.class);
                if (annotate != null) {
                    for (final String className : annotate.classNames()) {
                        try {
                            if (sharedUnsafe.compareAndSwapObject(inst, offset, f.get(inst), cast0(Class.forName(className), cpy))) {
                                break ss;
                            }
                        } catch (Exception ignored) {
                        }
                    }
                }
                sharedUnsafe.compareAndSwapObject(inst, offset, f.get(inst), cast0(f.getType(), cpy));
            } else {
                if (jsonEntry.getSecond() != null && jsonEntry.getSecond().getClass().isArray()) {
                    final var array = ((Object[]) jsonEntry.getSecond());
                    int i = 0;
                    for (final Object o : array) {
                        s:
                        if (o instanceof final JsonObject cursor) {
                            final var annotate = f.getAnnotation(TargetClass.class);
                            if (annotate != null) {
                                for (final String className : annotate.classNames()) {
                                    try {
                                        array[i++] = cast0(Class.forName(className), cursor);
                                        break s;
                                    } catch (Exception ignore) {
                                    }
                                }
                            }
                            sharedUnsafe.compareAndSwapObject(inst, offset, f.get(inst), cursor);
                        } else {
                            trySetField(inst, offset, f.get(inst), jsonEntry.getSecond(), f.getType());
                        }
                        ++i;
                    }
                } else {
                    trySetField(inst, offset, f.get(inst), jsonEntry.getSecond(), f.getType());
                }
            }
        }
        return inst;
    }

    public static JavaObject castArray (Object array) {
        if (array != null && array.getClass().isArray()) {
            final JsonArrayEncoder encoder = new JsonArrayEncoder((Object[]) array);
            encoder.parse();
            return encoder.result();
        }
        throw new IllegalArgumentException();
    }

    public static <T> JsonObject cast2json (Class<T> clz, Object o)
            throws BuildFailedException, IllegalAccessException {
        if (clz.isInterface() || clz.isArray() || clz.isEnum()) {
            throw new BuildFailedException();
        }
        return JsonEncoderKt.castImpl(clz, o);
    }
}
