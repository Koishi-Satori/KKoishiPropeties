package top.kkoishi;

import top.kkoishi.proc.ini.INIPropertiesLoader;
import top.kkoishi.proc.ini.Section;
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
    }
}
