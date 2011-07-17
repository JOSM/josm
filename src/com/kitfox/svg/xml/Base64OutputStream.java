/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.kitfox.svg.xml;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 *
 * @author kitfox
 */
public class Base64OutputStream extends FilterOutputStream
{
    int buf;
    int numBytes;
    int numChunks;
    
    public Base64OutputStream(OutputStream out)
    {
        super(out);
    }
    
    public void flush() throws IOException
    {
        out.flush();
    }
    
    public void close() throws IOException
    {
        switch (numBytes)
        {
            case 1:
                buf <<= 4;
                out.write(getBase64Byte(1));
                out.write(getBase64Byte(0));
                out.write('=');
                out.write('=');
                break;
            case 2:
                buf <<= 2;
                out.write(getBase64Byte(2));
                out.write(getBase64Byte(1));
                out.write(getBase64Byte(0));
                out.write('=');
                break;
            case 3:
                out.write(getBase64Byte(3));
                out.write(getBase64Byte(2));
                out.write(getBase64Byte(1));
                out.write(getBase64Byte(0));
                break;
            default:
                assert false;
        }
        
        out.close();
    }
    
    public void write(int b) throws IOException
    {
        buf = (buf << 8) | (0xff & b);
        numBytes++;
        
        if (numBytes == 3)
        {
            out.write(getBase64Byte(3));
            out.write(getBase64Byte(2));
            out.write(getBase64Byte(1));
            out.write(getBase64Byte(0));
            
            numBytes = 0;
            numChunks++;
            if (numChunks == 16)
            {
//                out.write('\r');
//                out.write('\n');
                numChunks = 0;
            }
        }
    }
    
    public byte getBase64Byte(int index)
    {
        return Base64Util.encodeByte((buf >> (index * 6)) & 0x3f);
    }
}
