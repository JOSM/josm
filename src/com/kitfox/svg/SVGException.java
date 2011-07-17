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
public class SVGException extends java.lang.Exception
{
    public static final long serialVersionUID = 0;
    
    /**
     * Creates a new instance of <code>SVGException</code> without detail message.
     */
    public SVGException()
    {
    }
    
    
    /**
     * Constructs an instance of <code>SVGException</code> with the specified detail message.
     * @param msg the detail message.
     */
    public SVGException(String msg)
    {
        super(msg);
    }
    
    public SVGException(String msg, Throwable cause)
    {
        super(msg, cause);
    }
    
    public SVGException(Throwable cause)
    {
        super(cause);
    }
}
