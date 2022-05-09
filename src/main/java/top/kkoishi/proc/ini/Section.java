package top.kkoishi.proc.ini;

import top.kkoishi.proc.property.BiMutiBuilder;
import top.kkoishi.proc.property.BuildFailedException;
import top.kkoishi.proc.property.Builder;

import java.io.Serializable;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class Section implements Serializable {

    static final Builder<Section> SECTION_BUILDER = (BiMutiBuilder<Section, INIEntry>) Section::getFromArray;

    static Section getFromArray (INIEntry[] args) {
        return new Section(Arrays.asList(args));
    }

    static final class NamelessSection extends Section {
        NamelessSection (List<INIEntry> entries) {
            super(entries);
        }
    }

    public static class INIEntry implements Serializable {
        String key;

        String value;

        public INIEntry (String key, String value) {
            this.key = key;
            this.value = value;
        }

        public String getKey () {
            return key;
        }

        public void setKey (String key) {
            this.key = key;
        }

        public String getValue () {
            return value;
        }

        public void setValue (String value) {
            this.value = value;
        }

        static Builder<INIEntry> builder () {
            return args -> {
                Objects.requireNonNull(args);
                if (args.length != 2) {
                    throw new BuildFailedException();
                } else {
                    return new INIEntry((String) args[0], (String) args[1]);
                }
            };
        }

        @Override
        public String toString () {
            return key + " = " + value;
        }
    }

    public static Section getInstance () {
        return new Section(new LinkedList<>());
    }

    public List<INIEntry> entries;

    Section (List<INIEntry> entries) {
        this.entries = entries;
    }

    public Section add (INIEntry entry) {
        entries.add(entry);
        return this;
    }

    @Override
    public String toString () {
        return "Section{" +
                "entries=" + entries +
                '}';
    }
}
