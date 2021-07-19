package eu.europa.ec.itb.validation.commons;

import java.io.File;

/**
 * Record information on a file.
 */
public class FileInfo {

    private final File file;
    private final String type;

    /**
     * Constructor.
     *
     * @param file The file.
     */
    public FileInfo(File file) {
        this(file, null);
    }

    /**
     * Constructor.
     *
     * @param file The file.
     * @param type The type string (artifact type).
     */
    public FileInfo(File file, String type) {
        this.file = file;
        this.type = type;
    }

    /**
     * @return The wrapped file.
     */
    public File getFile() {
        return file;
    }

    /**
     * @return The file type.
     */
    public String getType() {
        return type;
    }
}
