package eu.europa.ec.itb.validation.commons;

import java.io.InputStream;
import java.util.Optional;

/**
 * Record used to wrap a stream and its content type.
 *
 * @param stream The wrapped stream.
 * @param contentType The content type.
 */
public record StreamInfo(InputStream stream, Optional<String> contentType) {
}
