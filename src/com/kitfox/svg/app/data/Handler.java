/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.kitfox.svg.app.data;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 *
 * @author kitfox
 */
public class Handler extends URLStreamHandler
{
    class Connection extends URLConnection
    {
        String mime;
        byte[] buf;

        public Connection(URL url)
        {
            super(url);

            String path = url.getPath();
            int idx = path.indexOf(';');
            mime = path.substring(0, idx);
            String content = path.substring(idx + 1);

            if (content.startsWith("base64,"))
            {
                content = content.substring(7);
                try {
                    buf = new sun.misc.BASE64Decoder().decodeBuffer(content);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
        
        public void connect() throws IOException
        {
        }

        public String getHeaderField(String name)
        {
            if ("content-type".equals(name))
            {
                return mime;
            }

            return super.getHeaderField(name);
        }

        public InputStream getInputStream() throws IOException
        {
            return new ByteArrayInputStream(buf);
        }

//        public Object getContent() throws IOException
//        {
//            BufferedImage img = ImageIO.read(getInputStream());
//        }
    }

    protected URLConnection openConnection(URL u) throws IOException
    {
        return new Connection(u);
    }

}
