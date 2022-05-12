package top.kkoishi;

import top.kkoishi.proc.ini.INIPropertiesLoader;
import top.kkoishi.proc.ini.Section;
import top.kkoishi.proc.json.*;
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
import java.util.Arrays;
import java.util.HashMap;

/**
 * @author KKoishi_
 */
public final class Test {
    public static void main (String[] args) throws TokenizeException, BuildFailedException, LoaderException, IOException {
        //test ini loader.
        iniTest();
        //test java loader.
        propertiesTest();
        //test json parser.
        jsonTest();
    }

    private static void iniTest () throws IOException, TokenizeException, BuildFailedException, LoaderException {
        final PropertiesLoader<String, Section> loader = new INIPropertiesLoader();
        loader.load(new File("./test.ini"), StandardCharsets.UTF_8);
        loader.entrySet().forEach(System.out::println);
        System.out.println("Commit:" + loader.getCommit());
        System.out.println(loader.prepare());
    }

    private static void propertiesTest () throws IOException, TokenizeException {
        final var javaLoader = new JavaPropertiesLoader();
        final FileInputStream fis = new FileInputStream("./test.properties");
        javaLoader.load(fis);
        fis.close();
        javaLoader.entrySet().forEach(System.out::println);
        System.out.println("Commit:" + javaLoader.getCommit());
    }

    @SuppressWarnings({"GrazieInspection", "AlibabaRemoveCommentedCode"})
    private static void jsonTest () throws JsonSyntaxException, BuildFailedException {
        try {
            final var jsonParser = new JsonParser(Files.openAsUtf(new File("./test.json")));
            jsonParser.parse();
//            Invoking this will change the elements in the JsonObject instance.
//            final var temp = MappedJsonObject.cast(t.result(), HashMap.class);
//            System.out.println(temp);
            System.out.println(JsonJavaBridge.cast(Node.class, jsonParser.result()));

            jsonParser.reset(Files.openAsUtf(new File("./bar.json")));
            jsonParser.parse();
            System.out.println(JsonJavaBridge.cast(Bar.class, jsonParser.result()));

            jsonParser.reset(Files.openAsUtf(new File("./node.json")));
            jsonParser.parse();
            System.out.println(JsonJavaBridge.cast(Node.class, jsonParser.result()));
            //test get value.
            System.out.println(MappedJsonObject.cast(jsonParser.result(), HashMap.class).getBool("value"));

            //pressure test->5096k+ when using -Xss32768k
            jsonParser.reset(Files.openAsUtf(new File("./pressure_test.json")));
            jsonParser.parse();
            System.out.println(JsonJavaBridge.cast(Node.class, jsonParser.result()));

            //java to json.
            final Node testNode = new Node();
            testNode.name = "head";
            testNode.value = new Object[]{null, 114514, 1919810L, 114514.1919810, new Node(), true};
            testNode.next = new Node();
            final JsonObject jsonObject = JsonJavaBridge.cast(Node.class, testNode);
            final var convertor = new JsonConvertor(jsonObject);
//            test use.
//            jsonConvertTest(convertor);
        } catch (InvocationTargetException | NoSuchMethodException | InstantiationException | IllegalAccessException | NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    private static void jsonConvertTest (JsonConvertor convertor) throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, NoSuchFieldException, BuildFailedException {
        try {
            final var clz = Class.forName("top.kkoishi.proc.json.JsonParser$Builder");
            final var inst = clz.getConstructor(JsonParser.TokenList.class).newInstance(convertor.tokens());
            clz.getDeclaredMethod("build").invoke(inst);
            final var resField = clz.getDeclaredField("result");
            resField.setAccessible(true);
            final var o = JsonJavaBridge.cast(Node.class, (JsonObject) resField.get(inst));
            System.out.println(o);
        } catch (ClassNotFoundException e) {
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

    private static class Node {
        String name;
        @TargetClass(classNames = "top.kkoishi.Test$Node")
        @ArrayClass(classNames = "top.kkoishi.Test$Node")
        Object value;
        @TargetClass(classNames = "top.kkoishi.Test$Node")
        Object next;

        @Override
        public String toString () {
            return "Node{" +
                    "name='" + name + '\'' +
                    ", value=" + ((value != null && value.getClass().isArray()) ? Arrays.deepToString((Object[]) value) : value) +
                    ", next=" + next +
                    '}';
        }
    }
}
