/*
 * SVGUniverseSingleton.java
 *
 * Created on April 2, 2005, 1:54 AM
 */

package com.kitfox.svg;

/**
 * A convienience singleton for allowing all classes to access a common SVG universe.
 *
 * @author kitfox
 */
public class SVGCache
{
    private static final SVGUniverse svgUniverse = new SVGUniverse();
    
    /** Creates a new instance of SVGUniverseSingleton */
    private SVGCache()
    {
    }

    public static SVGUniverse getSVGUniverse()
    {
        return svgUniverse;
    }
    
}
