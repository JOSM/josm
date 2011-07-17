/*
 * PathUtil.java
 *
 * Created on May 10, 2005, 5:56 AM
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package com.kitfox.svg.pathcmd;

import java.awt.geom.*;

/**
 *
 * @author kitfox
 */
public class PathUtil
{
    
    /** Creates a new instance of PathUtil */
    public PathUtil()
    {
    }
    
    /**
     * Converts a GeneralPath into an SVG representation
     */
    public static String buildPathString(GeneralPath path)
    {
        float[] coords = new float[6];
        
        StringBuffer sb = new StringBuffer();
        
        for (PathIterator pathIt = path.getPathIterator(new AffineTransform()); !pathIt.isDone(); pathIt.next())
        {
            int segId = pathIt.currentSegment(coords);
            
            switch (segId)
            {
                case PathIterator.SEG_CLOSE:
                {
                    sb.append(" Z");
                    break;
                }
                case PathIterator.SEG_CUBICTO:
                {
                    sb.append(" C " + coords[0] + " " + coords[1] + " " + coords[2] + " " + coords[3] + " " + coords[4] + " " + coords[5]);
                    break;
                }
                case PathIterator.SEG_LINETO:
                {
                    sb.append(" L " + coords[0] + " " + coords[1]);
                    break;
                }
                case PathIterator.SEG_MOVETO:
                {
                    sb.append(" M " + coords[0] + " " + coords[1]);
                    break;
                }
                case PathIterator.SEG_QUADTO:
                {
                    sb.append(" Q " + coords[0] + " " + coords[1] + " " + coords[2] + " " + coords[3]);
                    break;
                }
            }
        }
        
        return sb.toString();
    }
}
