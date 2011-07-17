/*
 * SVGException.java
 *
 * Created on May 12, 2005, 11:32 PM
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package com.kitfox.svg;

/**
 *
 * @author kitfox
 */
public class SVGParseException extends java.lang.Exception
{
    public static final long serialVersionUID = 0;
    
    /**
     * Creates a new instance of <code>SVGException</code> without detail message.
     */
    public SVGParseException()
    {
    }
    
    
    /**
     * Constructs an instance of <code>SVGException</code> with the specified detail message.
     * @param msg the detail message.
     */
    public SVGParseException(String msg)
    {
        super(msg);
    }
    
    public SVGParseException(String msg, Throwable cause)
    {
        super(msg, cause);
    }
    
    public SVGParseException(Throwable cause)
    {
        super(cause);
    }
}
