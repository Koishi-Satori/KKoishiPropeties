package top.kkoishi.proc.ini;

import top.kkoishi.proc.property.AbstractPropertiesLoader;
import top.kkoishi.proc.property.BuildFailedException;
import top.kkoishi.proc.property.LoaderException;
import top.kkoishi.proc.property.PropertiesLoader;
import top.kkoishi.proc.property.TokenizeException;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class INIPropertiesLoader extends AbstractPropertiesLoader<String, Section> {

    public static INIPropertiesLoader getInstance (File src) throws TokenizeException, BuildFailedException, LoaderException, IOException {
        final INIPropertiesLoader loader = new INIPropertiesLoader();
        loader.load(src, false);
        return loader;
    }

    public static INIPropertiesLoader getInstance (File src, boolean transUnicode) throws TokenizeException, BuildFailedException, LoaderException, IOException {
        final INIPropertiesLoader loader = new INIPropertiesLoader();
        loader.load(src, transUnicode);
        return loader;
    }

    public static INIPropertiesLoader getInstance (File src, Charset charset) throws TokenizeException, BuildFailedException, LoaderException, IOException {
        final INIPropertiesLoader loader = new INIPropertiesLoader();
        loader.load(src, charset);
        return loader;
    }

    public static INIPropertiesLoader getInstance (Map<String, Section> proc, String commit) {
        final INIPropertiesLoader loader = new INIPropertiesLoader();
        proc.forEach(loader.proc::putIfAbsent);
        loader.commit = commit;
        return loader;
    }

    public static INIPropertiesLoader getInstance (Map<String, Section> proc) {
        return getInstance(proc, null);
    }

    public static INIPropertiesLoader getInstance (PropertiesLoader<String, Section> loader0) {
        final INIPropertiesLoader loader = new INIPropertiesLoader();
        for (final Map.Entry<String, Section> e : loader0.entrySet()) {
            loader.putIfAbsent(e.getKey(), e.getValue());
        }
        loader.commit = loader0.getCommit();
        return loader;
    }

    public static INIPropertiesLoader getInstance (Collection<Section.INIEntry> iniEntries, String commit) {
        final INIPropertiesLoader loader = new INIPropertiesLoader();
        final Section section = new Section.NamelessSection(new LinkedList<>());
        for (final Section.INIEntry e : iniEntries) {
            section.add(e);
        }
        loader.put(EMPTY_SECTION_NAME, section);
        loader.commit = commit;
        return loader;
    }

    public static INIPropertiesLoader getInstance (Collection<Section.INIEntry> iniEntries) {
        return getInstance(iniEntries, null);
    }

    private static final char SECTION_START = '[';

    private static final char SECTION_END = ']';

    public static final char GENERIC_LINE_SEP = '\n';

    public static final char WINDOWS_LINE_SEP = '\r';

    protected static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss");

    protected static final String OS_NAME = System.getProperty("os.name");

    protected static final String USR_NAME = System.getProperty("user.name");

    public static final char ANNOTATION_MARK = ';';

    public static final String EMPTY_SECTION_NAME = "section_empty";

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
                if (sb.isEmpty()) {
                    continue;
                }
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

    protected static boolean isSection (String line) {
        return line.charAt(0) == SECTION_START && line.charAt(line.length() - 1) == SECTION_END;
    }

    public INIPropertiesLoader () {
    }

    @Override
    protected String removeCommit (String in) {
        boolean commitSep;
        if (!in.isEmpty() && in.charAt(0) == ANNOTATION_MARK) {
            commitSep = false;
        } else {
            return in;
        }
        final StringBuilder sb = new StringBuilder();
        final char[] cs = in.toCharArray();
        final StringBuilder buff = new StringBuilder();
        for (int i = 1; i < cs.length; i++) {
            final char c = cs[i];
            if (!commitSep) {
                if (c == WINDOWS_LINE_SEP) {
                    if (cs[i + 1] != GENERIC_LINE_SEP) {
                        sb.append(c);
                        continue;
                    }
                    ++i;
                    commitSep = true;
                    commit = sb.toString();
                } else {
                    if (c == GENERIC_LINE_SEP) {
                        commitSep = true;
                        commit = sb.toString();
                        continue;
                    }
                }
                sb.append(c);
            } else {
                buff.append(c);
            }
        }
        return buff.toString();
    }

    @Override
    protected void load0 (String in) throws BuildFailedException, TokenizeException, LoaderException {
        super.load0(in);
    }

    @Override
    protected void loadImpl (String noCommit) throws LoaderException, TokenizeException, BuildFailedException {
        Section section = null;
        String name = null;
        for (final String line : lines(noCommit)) {
            if (isSection(line)) {
                if (section != null) {
                    if (proc.containsKey(name)) {
                        throw new LoaderException("The section " + name + " repeated.");
                    }
                    proc.put(name, section);
                }
                section = Section.getInstance();
                name = line.substring(1).substring(0, line.length() - 2);
            } else {
                if (section == null) {
                    section = new Section.NamelessSection(new LinkedList<>());
                    name = EMPTY_SECTION_NAME;
                }
                section.entries.add(buildEntry(line));
            }
        }
        if (section != null && !proc.containsKey(name)) {
            proc.put(name, section);
        }
    }

    protected static Section.INIEntry buildEntry (String line) throws BuildFailedException, TokenizeException {
        final StringBuilder keyBuff = new StringBuilder();
        final StringBuilder valBuff = new StringBuilder();
        boolean keyParsed = false;
        for (final char c : line.toCharArray()) {
            if (keyParsed) {
                if (c == '=') {
                    throw new BuildFailedException("The entry of INI has illegal format:" + line);
                }
                valBuff.append(c);
            } else if (c == '=') {
                keyParsed = true;
            } else {
                keyBuff.append(c);
            }
        }
        if (keyBuff.isEmpty() || valBuff.isEmpty()) {
            throw new TokenizeException("Failed to tokenize the line:" + line);
        }
        return new Section.INIEntry(keyBuff.toString(), valBuff.toString());
    }

    @Override
    public String prepare () {
        final StringBuilder sb = new StringBuilder();
        //write the commit at the top of the file.
        if (commit != null) {
            sb.append(';').append(commit).append('\n');
        } else {
            final Date date = new Date(System.currentTimeMillis());
            sb.append(';').append("Committed at:").append(DATE_FORMAT.format(date))
                    .append(" by ").append(USR_NAME).append(" at platform[")
                    .append(OS_NAME).append("]\n");
        }
        //write the sections.
        for (final Map.Entry<String, Section> entry : proc.entrySet()) {
            sb.append(section2str(entry.getValue(), entry.getKey()));
        }
        return sb.toString();
    }

    protected static String section2str (Section section, String name) {
        final StringBuilder sb = new StringBuilder();
        if (!(section instanceof Section.NamelessSection) && !name.equals(EMPTY_SECTION_NAME)) {
            sb.append('[').append(name).append("]\n");
        }
        for (final Section.INIEntry entry : section.entries) {
            sb.append(entry.key).append('=').append(entry.value).append('\n');
        }
        return sb.toString();
    }
}
