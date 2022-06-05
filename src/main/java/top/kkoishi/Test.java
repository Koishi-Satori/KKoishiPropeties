package top.kkoishi;

import top.kkoishi.proc.ini.INIPropertiesLoader;
import top.kkoishi.proc.ini.Section;
import top.kkoishi.proc.json.*;
import top.kkoishi.proc.properties.JavaPropertiesLoader;
import top.kkoishi.proc.property.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;

/**
 * @author KKoishi_
 */
public final class Test {
    public static void main (String[] args) throws TokenizeException, BuildFailedException, LoaderException, IOException, NoSuchFieldException, InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchMethodException {
        //test ini loader.
        //iniTest();
        //test java loader.
        //propertiesTest();
        //test json parser.
        //jsonTest();
        //test json properties
        //jsonPropertiesTest();

        jsonConvertTest();
    }

    private static void jsonPropertiesTest () throws IOException, BuildFailedException, LoaderException, TokenizeException {
        final ClassInstanceProperties<Node> nodeClassInstanceProperties = new JsonProperties<>(Node.class);
        final InputStream in = new FileInputStream("./node.json");
        nodeClassInstanceProperties.load(in);
        in.close();
        nodeClassInstanceProperties.parse();
        System.out.println(nodeClassInstanceProperties.instance());
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
            final JsonObject jsonObject = JsonJavaBridge.cast2json(Node.class, testNode);
            final var convertor = new JsonConvertor(jsonObject);
//            test use.
//            jsonConvertTest(convertor);
            convertor.convert();
            System.out.println(convertor.result());
        } catch (InvocationTargetException | NoSuchMethodException | InstantiationException | IllegalAccessException | NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    private static void jsonConvertTest () throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, NoSuchFieldException, BuildFailedException {
        try {
//            final var clz = Class.forName("top.kkoishi.proc.json.JsonParser$Builder");
//            final var inst = clz.getConstructor(JsonParser.TokenList.class).newInstance(convertor.tokens());
//            clz.getDeclaredMethod("build").invoke(inst);
//            final var resField = clz.getDeclaredField("result");
//            resField.setAccessible(true);
//            final var o = JsonJavaBridge.cast(Node.class, (JsonObject) resField.get(inst));
//            System.out.println(o);
            final var loader = new JsonParser("{\"code\":0,\"message\":\"0\",\"ttl\":1,\"data\":{\"mid\":40599528,\"name\":\"白金_世界\",\"sex\":\"保密\",\"face\":\"http://i0.hdslb.com/bfs/face/5a08af22c7b57ee3cafc3361106fbb5e69cc97de.jpg\",\"face_nft\":0,\"face_nft_type\":0,\"sign\":\"可能更新各种游戏的视频\",\"rank\":10000,\"level\":6,\"jointime\":0,\"moral\":0,\"silence\":0,\"coins\":0,\"fans_badge\":true,\"fans_medal\":{\"show\":false,\"wear\":false,\"medal\":null},\"official\":{\"role\":0,\"title\":\"\",\"desc\":\"\",\"type\":-1},\"vip\":{\"type\":2,\"status\":1,\"due_date\":1732291200000,\"vip_pay_type\":0,\"theme_type\":0,\"label\":{\"path\":\"\",\"text\":\"年度大会员\",\"label_theme\":\"annual_vip\",\"text_color\":\"#FFFFFF\",\"bg_style\":1,\"bg_color\":\"#FB7299\",\"border_color\":\"\"},\"avatar_subscript\":1,\"nickname_color\":\"#FB7299\",\"role\":3,\"avatar_subscript_url\":\"http://i0.hdslb.com/bfs/vip/icon_Certification_big_member_22_3x.png\"},\"pendant\":{\"pid\":3399,\"name\":\"2233幻星集\",\"image\":\"http://i0.hdslb.com/bfs/garb/item/20c07ded13498a5b12db99660c766ddd92ecfe31.png\",\"expire\":0,\"image_enhance\":\"http://i0.hdslb.com/bfs/garb/item/20c07ded13498a5b12db99660c766ddd92ecfe31.png\",\"image_enhance_frame\":\"\"},\"nameplate\":{\"nid\":8,\"name\":\"知名偶像\",\"image\":\"http://i0.hdslb.com/bfs/face/27a952195555e64508310e366b3e38bd4cd143fc.png\",\"image_small\":\"http://i0.hdslb.com/bfs/face/0497be49e08357bf05bca56e33a0637a273a7610.png\",\"level\":\"稀有勋章\",\"condition\":\"所有自制视频总播放数\\u003e=100万\"},\"user_honour_info\":{\"mid\":0,\"colour\":null,\"tags\":[]},\"is_followed\":true,\"top_photo\":\"http://i1.hdslb.com/bfs/space/cb1c3ef50e22b6096fde67febe863494caefebad.png\",\"theme\":{},\"sys_notice\":{},\"live_room\":{\"roomStatus\":1,\"liveStatus\":0,\"url\":\"https://live.bilibili.com/14553427?broadcast_type=0\\u0026is_room_feed=1\",\"title\":\"时光之帽——进行一个大跑路\",\"cover\":\"http://i0.hdslb.com/bfs/live/new_room_cover/c981befa854867b366d527f7237d33f86a174600.jpg\",\"roomid\":14553427,\"roundStatus\":0,\"broadcast_type\":0,\"watched_show\":{\"switch\":true,\"num\":2,\"text_small\":\"2\",\"text_large\":\"2人看过\",\"icon\":\"https://i0.hdslb.com/bfs/live/a725a9e61242ef44d764ac911691a7ce07f36c1d.png\",\"icon_location\":\"\",\"icon_web\":\"https://i0.hdslb.com/bfs/live/8d9d0f33ef8bf6f308742752d13dd0df731df19c.png\"}},\"birthday\":\"03-30\",\"school\":null,\"profession\":{\"name\":\"\",\"department\":\"\",\"title\":\"\",\"is_show\":0},\"tags\":null,\"series\":{\"user_upgrade_status\":3,\"show_upgrade_window\":false},\"is_senior_member\":0}}");
            loader.parse();
        } catch (JsonSyntaxException e) {
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
        Node next;

        @Override
        public String toString () {
            return "Node{" +
                    "name='" + name + '\'' +
                    ", value=" + ((value != null && value.getClass().isArray()) ? Arrays.deepToString((Object[]) value) : value) +
                    ", next=" + next +
                    '}';
        }
    }

    @SuppressWarnings({"AlibabaClassNamingShouldBeCamel"})
    public static final class TODOException extends RuntimeException {
        public TODOException () {
            super("Not implemented yet.");
        }
    }
}
