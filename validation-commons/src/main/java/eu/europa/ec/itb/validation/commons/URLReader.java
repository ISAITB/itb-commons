package eu.europa.ec.itb.validation.commons;

import eu.europa.ec.itb.validation.commons.error.ValidatorException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.util.List;

/**
 * Component responsible of reading a stream from a given URL.
 */
@Component
public class URLReader {

    /**
     * Open a stream to the provided URL.
     *
     * @param uri The URL.
     * @return The stream.
     * @throws ValidatorException If the URL cannot be read.
     */
    InputStream stream(URI uri) {
        Proxy proxy = null;
        List<Proxy> proxies = ProxySelector.getDefault().select(uri);
        if (proxies != null && !proxies.isEmpty()) {
            proxy = proxies.get(0);
        }
        try {
            URLConnection connection;
            if (proxy == null) {
                connection = uri.toURL().openConnection();
            } else {
                connection = uri.toURL().openConnection(proxy);
            }
            return connection.getInputStream();
        } catch (IOException e) {
            throw new ValidatorException("Unable to read provided URI", e);
        }
    }

}
