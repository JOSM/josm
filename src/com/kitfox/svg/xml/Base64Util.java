/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.kitfox.svg.xml;

/**
 *
 * @author kitfox
 */
public class Base64Util 
{
    static final byte[] valueToBase64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".getBytes();
    static final byte[] base64ToValue = new byte[128];
    static {
        for (int i = 0; i < valueToBase64.length; ++i)
        {
            base64ToValue[valueToBase64[i]] = (byte)i;
        }
    }
    
    static public byte encodeByte(int value)
    {
        return valueToBase64[value];
    }
    
    static public byte decodeByte(int base64Char)
    {
        return base64ToValue[base64Char];
    }
}
