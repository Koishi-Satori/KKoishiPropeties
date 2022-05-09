package top.kkoishi.proc.property;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public abstract class AbstractPropertiesLoader<K, V> implements PropertiesLoader<K, V> {

    protected static class Token implements Serializable {
        public StringBuilder key;

        public StringBuilder value;

        public Token () {
            this(new StringBuilder(), new StringBuilder());
        }

        public Token (StringBuilder key, StringBuilder value) {
            this.key = key;
            this.value = value;
        }
    }

    protected transient final Map<K, V> proc = new HashMap<>();

    protected transient String commit = null;

    protected AbstractPropertiesLoader () {
    }

    protected AbstractPropertiesLoader (AbstractPropertiesLoader<K, V> loader) {
        load(loader);
    }

    @Override
    public void load (InputStream in, Charset charset) throws IOException, TokenizeException, BuildFailedException, LoaderException {
        try (BufferedInputStream bis = new BufferedInputStream(in)) {
            int len;
            final byte[] buf = new byte[Files.BUF_SIZE];
            final StringBuilder sb = new StringBuilder();
            while ((len = bis.read(buf)) != -1) {
                sb.append(new String(buf, 0, len, charset));
            }
            load0(sb.toString());
        }
    }

    @Override
    public void load (Reader reader) throws TokenizeException, BuildFailedException, LoaderException, IOException {
        final StringBuilder sb = new StringBuilder();
        int len;
        final char[] buf = new char[128];
        while ((len = reader.read(buf)) != -1) {
            sb.append(buf, 0, len);
        }
        load0(sb.toString());
    }

    @Override
    public String getCommit () {
        return commit;
    }

    @Override
    public void load (String in) throws TokenizeException, BuildFailedException, LoaderException {
        load0(in);
    }

    @Override
    public final void load (File src, Charset charset) throws IOException, TokenizeException, BuildFailedException, LoaderException {
        load0(Files.fopen(src, charset));
    }

    @Override
    public final void load (File src, boolean transUnicode) throws IOException, TokenizeException, BuildFailedException, LoaderException {
        if (transUnicode) {
            load0(Unicode.decode(Files.fopen(src, StandardCharsets.US_ASCII)));
        } else {
            load(src, StandardCharsets.UTF_8);
        }
    }

    public final void loadUtf (File src) throws IOException, TokenizeException, BuildFailedException, LoaderException {
        load0(Files.fopen(src, StandardCharsets.UTF_8));
    }

    @Override
    public final void load (PropertiesLoader<K, V> loader) {
        proc.putAll(loader.toMap());
    }

    protected void load0 (String in) throws BuildFailedException, TokenizeException, LoaderException {
        loadImpl(removeCommit(in));
    }

    protected abstract void loadImpl (String noCommit) throws LoaderException, TokenizeException, BuildFailedException;

    @Override
    public abstract String prepare () throws LoaderException;

    protected abstract String removeCommit (String in);

    @Override
    public int size () {
        return proc.size();
    }

    @Override
    public boolean isEmpty () {
        return proc.size() == 0;
    }

    @Override
    @SuppressWarnings("all")
    public boolean containsKey (Object key) {
        return proc.containsKey(key);
    }

    @Override
    @SuppressWarnings("all")
    public boolean containsValue (Object value) {
        return proc.containsValue(value);
    }

    @Override
    @SuppressWarnings("all")
    public V get (Object key) {
        return proc.get(key);
    }

    @Override
    public V put (K key, V value) {
        return proc.put(key, value);
    }

    @Override
    @SuppressWarnings("all")
    public V remove (Object key) {
        return proc.remove(key);
    }

    @Override
    public void putAll (Map<? extends K, ? extends V> m) {
        proc.putAll(m);
    }

    @Override
    public void clear () {
        proc.clear();
    }

    @Override
    public Set<K> keySet () {
        return proc.keySet();
    }

    @Override
    public Collection<V> values () {
        return proc.values();
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet () {
        return proc.entrySet();
    }

    @Override
    public Map<K, V> toMap () {
        return new HashMap<>(proc);
    }

    @Override
    public ConcurrentMap<K, V> toConcurrentMap () {
        return new ConcurrentHashMap<>(proc);
    }

    protected static final class Unicode {

        private static final HashMap<Character, Byte> IGNORE_MAP = new HashMap<>(100) {{
            put('(', null);
            put(')', null);
            put('[', null);
            put(']', null);
            put('"', null);
            put('\'', null);
            put('-', null);
            put('+', null);
            put('*', null);
            put('/', null);
            put('&', null);
            put('^', null);
            put('%', null);
            put('$', null);
            put('#', null);
            put('@', null);
            put('!', null);
            put('~', null);
            put('?', null);
            put('<', null);
            put('>', null);
            put('.', null);
            put(',', null);
            put(' ', null);
            put('{', null);
            put('}', null);
            put('|', null);
            put('`', null);
            put('\t', null);
            put('\n', null);
            put('\r', null);
            put('\b', null);
            put('\\', null);
            put('_', null);
            put('=', null);
            put(';', null);
        }};

        private Unicode () {
        }

        public static String encode (String utfString) {
            final char[] utfChars = utfString.toCharArray();
            final StringBuilder uni = new StringBuilder();
            for (final char utfChar : utfChars) {
                uni.append(encode(utfChar));
            }
            return uni.toString();
        }

        public static String encodeExcept (String utfString) {
            final char[] utfChars = utfString.toCharArray();
            final StringBuilder uni = new StringBuilder();
            for (final char utfChar : utfChars) {
                uni.append(encodeExcept(utfChar));
            }
            return uni.toString();
        }

        public static String encode (char utfChar) {
            final String hexB = Integer.toHexString(utfChar);
            return hexB.length() <= 2 ? "\\u00" + hexB : "\\u" + hexB;
        }

        public static String encodeExcept (char utfChar) {
            if (test(utfChar)) {
                return String.valueOf(utfChar);
            }
            final String hexB = Integer.toHexString(utfChar);
            return hexB.length() <= 2 ? "\\u00" + hexB : "\\u" + hexB;
        }

        public static boolean test (char utfChar) {
            return utfChar >= 'a' && utfChar <= 'z' || (utfChar >= 'A' && utfChar <= 'Z') || (utfChar >= '0' && utfChar <= '9') || IGNORE_MAP.containsKey(utfChar);
        }

        public static String decode (String unicodeStr) {
            StringBuilder sb = new StringBuilder();
            StringBuilder buf;
            final String[] ss = cutUnicode(unicodeStr);
            for (final String s : ss) {
                if (s.length() != 0) {
                    if (s.startsWith("\\u")) {
                        final String hex = s.substring(2, 6);
                        final String rest = s.substring(6);
                        sb.append(decode0(hex)).append(rest);
                    } else {
                        sb.append(s);
                    }
                }
            }
            return sb.toString();
        }

        private static String[] cutUnicode (String unicodeStr) {
            final List<String> arr = new ArrayList<>();
            final char[] charArray = unicodeStr.toCharArray();
            for (int i = 0; i < charArray.length; i++) {
                final StringBuilder buf = new StringBuilder();
                final char c = charArray[i];
                if (c == '\\') {
                    if (i + 1 < charArray.length) {
                        if (charArray[i + 1] == 'u') {
                            buf.append("\\u");
                            ++i;
                            buf.append(charArray[++i]);
                            buf.append(charArray[++i]);
                            buf.append(charArray[++i]);
                            buf.append(charArray[++i]);
                        } else {
                            buf.append(c);
                        }
                    } else {
                        buf.append(c);
                    }
                } else {
                    buf.append(c);
                }
                arr.add(buf.toString());
            }
            final String[] array = new String[arr.size()];
            return arr.toArray(array);
        }

        private static char decode0 (String hexString) {
            return (char) HexFormat.fromHexDigits(hexString);
        }
    }
}
