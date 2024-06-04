package app;

import java.io.File;
import java.io.Serializable;

public class MyFile implements Serializable {
    private File file;

    private FileType access;

    public MyFile(File file, FileType access) {
        this.file = file;
        this.access = access;
    }

    public MyFile(MyFile myFile){
        if (myFile != null) {
            this.file = myFile.file;
            this.access = myFile.access;
        }
    }

    public File getFile() {
        return file;
    }

    public FileType getAccess() {
        return access;
    }
}
