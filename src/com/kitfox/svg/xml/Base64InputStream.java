/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.kitfox.svg.xml;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 *
 * @author kitfox
 */
public class Base64InputStream extends FilterInputStream
{
    int buf;  //Cached bytes to read
    int bufSize;  //Number of bytes waiting to be read from buffer
    boolean drain = false;  //After set, read no more chunks
    
    public Base64InputStream(InputStream in)
    {
        super(in);
    }

    public int read() throws IOException
    {
        if (drain && bufSize == 0)
        {
            return -1;
        }
        
        if (bufSize == 0)
        {
            //Read next chunk into 4 byte buffer
            int chunk = in.read();
            if (chunk == -1)
            {
                drain = true;
                return -1;
            }
            
            //get remaining 3 bytes
            for (int i = 0; i < 3; ++i)
            {
                int value = in.read();
                if (value == -1)
                {
                    throw new IOException("Early termination of base64 stream");
                }
                chunk = (chunk << 8) | (value & 0xff);
            }

            //Check for special termination characters
            if ((chunk & 0xffff) == (((byte)'=' << 8) | (byte)'='))
            {
                bufSize = 1;
                drain = true;
            }
            else if ((chunk & 0xff) == (byte)'=')
            {
                bufSize = 2;
                drain = true;
            }
            else
            {
                bufSize = 3;
            }
            
            //Fill buffer with decoded characters
            for (int i = 0; i < bufSize + 1; ++i)
            {
                buf = (buf << 6) | Base64Util.decodeByte((chunk >> 24) & 0xff);
                chunk <<= 8;
            }
        }
        
        //Return nth remaing bte & decrement counter
        return (buf >> (--bufSize * 8)) & 0xff;
    } 
}
