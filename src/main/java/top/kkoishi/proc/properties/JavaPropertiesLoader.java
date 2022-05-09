package top.kkoishi.proc.properties;

import top.kkoishi.proc.property.AbstractPropertiesLoader;
import top.kkoishi.proc.property.LoaderException;
import top.kkoishi.proc.property.TokenizeException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import static top.kkoishi.proc.ini.INIPropertiesLoader.GENERIC_LINE_SEP;
import static top.kkoishi.proc.ini.INIPropertiesLoader.WINDOWS_LINE_SEP;

/**
 * @author DELL
 */
public class JavaPropertiesLoader extends AbstractPropertiesLoader<Object, Object> {

    protected static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss");

    protected static final String OS_NAME = System.getProperty("os.name");

    protected static final String USR_NAME = System.getProperty("user.name");

    protected static List<String> lines (String in) {
        final List<String> lines = new LinkedList<>();
        StringBuilder sb = new StringBuilder();
        final char[] cs = in.toCharArray();
        for (int i = 0; i < cs.length; i++) {
            final char c = cs[i];
            if (c == WINDOWS_LINE_SEP) {
                if (cs[i + 1] != GENERIC_LINE_SEP) {
                    sb.append(c);
                    continue;
                }
                ++i;
                if (sb.isEmpty()) {
                    continue;
                }
                lines.add(sb.toString());
                sb = new StringBuilder();
                continue;
            } else if (c == GENERIC_LINE_SEP) {
                lines.add(sb.toString());
                sb = new StringBuilder();
                continue;
            }
            sb.append(c);
        }
        if (!sb.isEmpty()) {
            lines.add(sb.toString());
        }
        return lines;
    }

    @Override
    public final void load (InputStream in) throws IOException, TokenizeException {
        loadImpl(removeCommit(AbstractPropertiesLoader.Unicode.decode(new String(in.readNBytes(in.available()),
                StandardCharsets.US_ASCII))));
    }

    @Override
    public final void load (Reader reader) throws IOException, TokenizeException {
        final StringBuilder sb = new StringBuilder();
        final char[] buf = new char[1024];
        int len;
        while ((len = reader.read(buf)) != -1) {
            sb.append(buf, 0, len);
        }
        loadImpl(removeCommit(AbstractPropertiesLoader.Unicode.decode(sb.toString())));
    }

    public final void store (OutputStream os, String commit) throws LoaderException, IOException {
        if (commit == null) {
            super.store(os);
        } else {
            os.write(AbstractPropertiesLoader.Unicode.encodeExcept(prepare0(commit)).getBytes(StandardCharsets.US_ASCII));
            os.flush();
        }
    }

    public final void store (Writer writer, String commit) throws LoaderException, IOException {
        if (commit == null) {
            super.store(writer);
        } else {
            writer.write(AbstractPropertiesLoader.Unicode.encodeExcept(prepare0(commit)));
            writer.flush();
        }
    }

    @Override
    protected void loadImpl (String noCommit) throws TokenizeException {
        Object key = null;
        Object value;
        StringBuilder entryBuilder;
        final ListIterator<String> listIterator = lines(noCommit).listIterator();
        while (listIterator.hasNext()) {
            final String line = listIterator.next();
            final StringBuilder sub = new StringBuilder(line);
            if (line.endsWith("\\")) {
                sub.deleteCharAt(sub.length() - 1);
                while (true) {
                    if (!listIterator.hasNext()) {
                        break;
                    }
                    final String next = listIterator.next();
                    final char[] multiLineSub = next.toCharArray();
                    final int subLen = next.endsWith("\\") ? multiLineSub.length - 1 : multiLineSub.length;
                    int endPos = 0;
                    //Skip all the tab and whitespace at the beginning.
                    SubLoop:
                    for (int i = 0; i < subLen; i++, endPos++) {
                        switch (multiLineSub[i]) {
                            case ' ':
                            case '\t': {
                                break;
                            }
                            default: {
                                break SubLoop;
                            }
                        }
                    }
                    for (int i = endPos; i < subLen; i++) {
                        sub.append(multiLineSub[i]);
                    }
                    if (!next.endsWith("\\")) {
                        break;
                    }
                }

                entryBuilder = new StringBuilder();
                final char[] cs = sub.toString().toCharArray();
                boolean skip = false;
                for (final char c : cs) {
                    if (skip) {
                        entryBuilder.append(c);
                    } else if (c == '=') {
                        skip = true;
                        key = entryBuilder.toString();
                        entryBuilder = new StringBuilder();
                    } else {
                        entryBuilder.append(c);
                    }
                }
                if (key == null || !skip) {
                    throw new TokenizeException("The entry has illegal format:" + line);
                }
            } else {
                entryBuilder = new StringBuilder();
                final char[] charArray = line.toCharArray();
                boolean skip = false;
                for (final char c : charArray) {
                    if (skip) {
                        entryBuilder.append(c);
                    } else if (c == '=') {
                        skip = true;
                        key = entryBuilder.toString();
                        entryBuilder = new StringBuilder();
                    } else {
                        entryBuilder.append(c);
                    }
                }
                if (key == null || !skip) {
                    throw new TokenizeException("The entry has illegal format:" + line);
                }
            }
            value = entryBuilder.toString();
            super.put(key, value);
        }
    }

    public final String getProperty (Object key) {
        return super.get(key).toString();
    }

    @Override
    public String prepare () {
        return prepare0(commit);
    }

    protected final String prepare0 (String commit) {
        final StringBuilder sb = new StringBuilder();
        if (commit != null) {
            sb.append('#').append(commit).append('\n');
        } else {
            final Date date = new Date(System.currentTimeMillis());
            sb.append('#').append("Committed at:").append(DATE_FORMAT.format(date))
                    .append(" by ").append(USR_NAME).append(" at platform[")
                    .append(OS_NAME).append("]\n");
        }
        super.proc.forEach((key, val) -> sb.append(key).append('=').append(val).append('\n'));
        return sb.toString();
    }

    @Override
    protected String removeCommit (String in) {
        final StringBuilder sb = new StringBuilder();
        final Iterator<String> iterator = lines(in).iterator();
        if (iterator.hasNext()) {
            String line = iterator.next();
            if (!line.isEmpty()) {
                if (line.charAt(0) == '#') {
                    super.commit = line.substring(1);
                } else {
                    sb.append(line).append('\n');
                }
            }
            while (iterator.hasNext()) {
                line = iterator.next();
                if (!line.isEmpty() && line.charAt(0) != '#') {
                    sb.append(line).append('\n');
                }
            }
        }
        return sb.toString();
    }
}
