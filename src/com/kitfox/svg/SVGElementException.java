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
public class SVGElementException extends SVGException
{
    public static final long serialVersionUID = 0;
    
    private final SVGElement element;
    
    /**
     * Creates a new instance of <code>SVGException</code> without detail message.
     */
    public SVGElementException(SVGElement element)
    {
        this(element, null, null);
    }
    
    
    /**
     * Constructs an instance of <code>SVGException</code> with the specified detail message.
     * @param msg the detail message.
     */
    public SVGElementException(SVGElement element, String msg)
    {
        this(element, msg, null);
    }
    
    public SVGElementException(SVGElement element, String msg, Throwable cause)
    {
        super(msg, cause);
        this.element = element;
    }
    
    public SVGElementException(SVGElement element, Throwable cause)
    {
        this(element, null, cause);
    }

    public SVGElement getElement()
    {
        return element;
    }
}
