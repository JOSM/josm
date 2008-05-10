/* Stupid package to override the restriction on payloads with DELETEs */
package org.openstreetmap.josm.io;

import java.net.ProtocolException;
import java.io.*;
import java.net.URL;
import java.net.Proxy;

public class MyHttpURLConnection extends sun.net.www.protocol.http.HttpURLConnection {
            protected MyHttpURLConnection(URL u, Proxy p, sun.net.www.protocol.http.Handler handler)
            {
              super(u,p,handler);
            }

public synchronized OutputStream getOutputStream()
                    throws IOException {
                String oldmethod = method;
                method = "POST";
                OutputStream temp = super.getOutputStream();
                method = oldmethod;
                return temp;
}
}
