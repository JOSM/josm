/* Dummy handler just to load the override URLConnection class */
package org.openstreetmap.josm.io;

import java.io.IOException;
import java.net.URL;
import java.net.Proxy;

// This is also a java.net.URLStreamHandler
// Basically a copy of sun.net.www.protocol.http.Handler
public class MyHttpHandler extends sun.net.www.protocol.http.Handler  {
            protected String proxy;
            protected int proxyPort;

            public MyHttpHandler() {
                super();
                proxy = null;
                proxyPort = -1;
            }

            protected java.net.URLConnection openConnection(URL u)
                    throws IOException {
                return openConnection(u, (Proxy) null);
            }
            public MyHttpHandler(String proxy, int port) {
                proxy = proxy;
                proxyPort = port;
            }
  
            protected java.net.URLConnection openConnection(URL u, Proxy p)
                    throws IOException {
                return new MyHttpURLConnection(u, p, this);
            }
}
