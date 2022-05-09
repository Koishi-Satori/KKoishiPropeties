package top.kkoishi.proc.property;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;

public abstract class PropertiesFile {

    protected static final int ERROR = 0;

    protected static final int NORMAL = 1;

    @SuppressWarnings("all")
    protected static final FileSystem fs = FileSystems.getDefault();

    protected final String path;
    protected final int status = ERROR;
    protected final FileSystem fileSystem;

    public PropertiesFile (String path) throws IOException {
        this.path = path;
        fileSystem = FileSystems.newFileSystem(fs.getPath(path));
    }

    void checkStatus () {
        for (FileStore fileStore : fileSystem.getFileStores()) {

        }
    }
}
