package eu.europa.ec.itb.validation.commons;

import java.io.File;

public class FileInfo {

    private File file;
    private String type;

    public FileInfo(File file) {
        this(file, null);
    }

    public FileInfo(File file, String type) {
        this.file = file;
        this.type = type;
    }

    public File getFile() {
        return file;
    }

    public String getType() {
        return type;
    }
}
