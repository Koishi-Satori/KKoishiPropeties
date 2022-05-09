package top.kkoishi;

import top.kkoishi.proc.ini.INIPropertiesLoader;
import top.kkoishi.proc.ini.Section;
import top.kkoishi.proc.json.JsonJavaBridge;
import top.kkoishi.proc.json.JsonParser;
import top.kkoishi.proc.properties.JavaPropertiesLoader;
import top.kkoishi.proc.property.BuildFailedException;
import top.kkoishi.proc.property.Files;
import top.kkoishi.proc.property.LoaderException;
import top.kkoishi.proc.property.PropertiesLoader;
import top.kkoishi.proc.property.TokenizeException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;

/**
 * @author KKoishi_
 */
public final class Test {
    public static void main (String[] args) throws TokenizeException, BuildFailedException, LoaderException, IOException {
        //test ini loader.
        final PropertiesLoader<String, Section> loader = new INIPropertiesLoader();
        loader.load(new File("./test.ini"), StandardCharsets.UTF_8);
        loader.entrySet().forEach(System.out::println);
        System.out.println("Commit:" + loader.getCommit());
        System.out.println(loader.prepare());
        //test java loader.
        final var javaLoader = new JavaPropertiesLoader();
        final FileInputStream fis = new FileInputStream("./test.properties");
        javaLoader.load(fis);
        fis.close();
        javaLoader.entrySet().forEach(System.out::println);
        System.out.println("Commit:" + javaLoader.getCommit());
        //test json parser.
        final var t = new JsonParser(Files.openAsUtf(new File("./test.json")));
        t.parse();
        final var fooParser = new JsonParser(Files.openAsUtf(new File("./foo.json")));
        fooParser.parse();
        try {
            System.out.println(JsonJavaBridge.cast(Foo.class, fooParser.result()));
            fooParser.reset(Files.openAsUtf(new File("./bar.json")));
            fooParser.parse();
            System.out.println(JsonJavaBridge.cast(Bar.class, fooParser.result()));
        } catch (InvocationTargetException | NoSuchMethodException | InstantiationException | IllegalAccessException | NoSuchFieldException e) {
            e.printStackTrace();
        }

    }

    /**
     * Test use class.
     */
    private static class Foo {
        final int bar;

        public Foo (int bar) {
            this.bar = bar;
        }

        @Override
        public String toString () {
            return "Foo{" +
                    "bar=" + bar +
                    '}';
        }
    }

    private static class Bar extends Foo {
        int foo = 1919810;

        public Bar (int bar) {
            super(bar);
        }

        @Override
        public String toString () {
            return "Bar{" +
                    "bar=" + bar +
                    ", foo=" + foo +
                    '}';
        }
    }
}
