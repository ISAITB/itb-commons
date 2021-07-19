package eu.europa.ec.itb.validation.commons.test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class TestSupport {

    protected Path createFileWithContents(Path file, String contents) throws IOException {
        Files.createDirectories(file.getParent());
        Files.createFile(file);
        Files.writeString(file, contents);
        return file;
    }

}
