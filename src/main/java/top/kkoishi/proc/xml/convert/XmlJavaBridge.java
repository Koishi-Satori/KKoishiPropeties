package top.kkoishi.proc.xml.convert;

import sun.misc.Unsafe;
import top.kkoishi.Test;
import top.kkoishi.proc.xml.dom.XmlDocTree;

import java.lang.reflect.Field;

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

    public static <T> T cast(Class<T> clz, XmlDocTree dom) {

        throw new Test.TODOException();
    }

    private static Field getField (String name, Class<?> clz) throws NoSuchFieldException {
        final Field f;
        try {
            f = clz.getDeclaredField(name);
            f.setAccessible(true);
            return f;
        } catch (Exception e) {
            final var superClass = clz.getSuperclass();
            if (superClass == null || superClass == Object.class) {
                throw new NoSuchFieldException(e.getLocalizedMessage());
            }
            return getField(name, superClass);
        }
    }
}
