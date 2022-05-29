package top.kkoishi.proc.xml.convert;

import org.jetbrains.annotations.NotNull;
import sun.misc.Unsafe;
import top.kkoishi.Test;
import top.kkoishi.proc.json.TargetClass;
import top.kkoishi.proc.property.BuildFailedException;
import top.kkoishi.proc.xml.dom.*;

import java.lang.reflect.Field;
import java.util.Objects;

/**
 * @author KKoishi_
 */
public final class XmlJavaBridge {
    static final Class<Integer> INTEGER_CLASS = int.class;
    static final Class<Long> LONG_CLASS = long.class;
    static final Class<Float> FLOAT_CLASS = float.class;
    static final Class<Double> DOUBLE_CLASS = double.class;
    static final Class<Short> SHORT_CLASS = short.class;
    static final Class<Byte> BYTE_CLASS = byte.class;
    static final Class<Boolean> BOOLEAN_CLASS = boolean.class;
    static Unsafe sharedUnsafe;

    private XmlJavaBridge () {
        throw new IllegalArgumentException(XmlJavaBridge.class.descriptorString());
    }

    static {
        try {
            sharedUnsafe = accessUnsafe();
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
            throw new RuntimeException("Can not access sun.misc.Unsafe.class");
        }
    }

    private static Unsafe accessUnsafe () throws NoSuchFieldException, IllegalAccessException {
        final Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        return (Unsafe) unsafeField.get(null);
    }

    @SuppressWarnings("unchecked")
    public static <T> T cast (Class<T> clz, XmlDocTree dom)
            throws InstantiationException, NoSuchFieldException, IllegalAccessException {
        return (T) cast(clz, XmlJavaBridgeKt.INSTANCE.getRootNode$Properties(dom));
    }

    private static Object cast (Class<?> clz, XmlNodeImpl root)
            throws InstantiationException, NoSuchFieldException, IllegalAccessException {
        final Object inst = sharedUnsafe.allocateInstance(clz);
        set_field_loop:
        for (final AbstractXmlNode child : root.children()) {
            final Field f = getField(((XmlElementInfoDesc) Objects.requireNonNull(child.value())).getTitle(), clz);
            final var annotate = f.getAnnotation(TargetClass.class);
            if (annotate != null) {
                for (final String className : annotate.classNames()) {
                    try {
                        final var c = Class.forName(className);
                        if (XmlJavaBridgeKt.INSTANCE.isBasicType$Properties(c)) {
                            if (child.hasChildren()) {
                                throw new BuildFailedException();
                            }
                            trySetField(f, XmlTypeSystemKt.cast(((XmlElementInfoDesc) Objects.requireNonNull(child.value())).getTitle(), c)
                                    , XmlJavaBridgeKt.INSTANCE.basicTypeNilValue$Properties(c), f.get(inst), c);
                        } else {
                            if (!child.hasChildren()) {
                                throw new BuildFailedException();
                            }
                            trySetField(f, cast(c, (XmlNodeImpl) child), null, f.get(inst), c);
                        }
                        continue set_field_loop;
                    } catch (Exception ignore) {
                    }
                }
                trySetField(f, cast(f.getType(), (XmlNodeImpl) child), null, f.get(inst), f.getType());
            } else {
                if (child.hasChildren()) {
                    trySetField(f, cast(f.getType(), (XmlNodeImpl) child), null, f.get(inst), f.getType());
                } else {
                    final var cpy = (XmlLeafNode) child;
                    if (XmlJavaBridgeKt.INSTANCE.isBasicType$Properties(f.getType())) {

                    }
                }
            }
        }
        return todo();
    }

    private static void trySetField (Field f, Object value, Object old, Object obj, Class<?> c) {
        final long offset = sharedUnsafe.objectFieldOffset(f);
    }

    private static Field getField (String name, Class<?> clz) throws NoSuchFieldException {
        final Field f;
        try {
            (f = clz.getDeclaredField(name)).setAccessible(true);
            return f;
        } catch (Exception e) {
            final var superClass = clz.getSuperclass();
            if (superClass == null || superClass == Object.class) {
                throw new NoSuchFieldException(e.getLocalizedMessage());
            }
            return getField(name, superClass);
        }
    }

    static <T> T todo () {
        throw new Test.TODOException();
    }
}
