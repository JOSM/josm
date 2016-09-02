// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.protocols.data;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Base64;

/**
 * Connection for "data:" protocol allowing to read inlined base64 images.
 * <p>
 * See <a href="http://stackoverflow.com/a/9388757/2257172">StackOverflow</a>.
 * @since 10931
 */
public class DataConnection extends URLConnection {

    /**
     * Constructs a new {@code DataConnection}.
     * @param u data url
     */
    public DataConnection(URL u) {
        super(u);
    }

    @Override
    public void connect() throws IOException {
        connected = true;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(Base64.getDecoder().decode(url.toString().replaceFirst("^.*;base64,", "")));
    }
}
