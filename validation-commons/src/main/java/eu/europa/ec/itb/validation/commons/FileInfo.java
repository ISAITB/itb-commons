/*
 * Copyright (C) 2025 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

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
