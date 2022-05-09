package top.kkoishi.proc.property;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public final class Files {

    public static final int BUF_SIZE = 1 << 10;

    private Files () {
    }

    public static String fopen (File src, Charset charset) throws IOException {
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(src))) {
            int len;
            final byte[] buf = new byte[BUF_SIZE];
            final StringBuilder sb = new StringBuilder();
            while ((len = bis.read(buf)) != -1) {
                sb.append(new String(buf, 0, len, charset));
            }
            bis.close();
            return sb.toString();
        }
    }

    public static String open (File src, Charset charset) {
        try {
            return fopen(src, charset);
        } catch (IOException e) {
            return null;
        }
    }

    public static String fopenAsUtf (File src) throws IOException {
        return fopen(src, StandardCharsets.UTF_8);
    }

    public static String openAsUtf (File src) {
        return open(src, StandardCharsets.UTF_8);
    }

    public static byte[] fopen (File src) throws IOException {
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(src))) {
            final byte[] data = bis.readNBytes(bis.available());
            bis.close();
            return data;
        }
    }

    public static byte[] open (File src) {
        try {
            return fopen(src);
        } catch (IOException e) {
            return null;
        }
    }

    public static void fWrite (File target, byte[] bytes) throws IOException {
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(target))) {
            bos.write(bytes);
        }
    }
}
