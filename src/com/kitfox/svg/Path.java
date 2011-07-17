/*
 * Rect.java
 *
 *
 *  The Salamander Project - 2D and 3D graphics libraries in Java
 *  Copyright (C) 2004 Mark McKay
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 *  Mark McKay can be contacted at mark@kitfox.com.  Salamander and other
 *  projects can be found at http://www.kitfox.com
 *
 * Created on January 26, 2004, 5:25 PM
 */

package com.kitfox.svg;

import com.kitfox.svg.xml.StyleAttribute;
import java.awt.*;
import java.awt.geom.*;

import com.kitfox.svg.pathcmd.*;
import com.kitfox.svg.xml.*;
import org.xml.sax.*;

//import org.apache.batik.ext.awt.geom.ExtendedGeneralPath;

/**
 * @author Mark McKay
 * @author <a href="mailto:mark@kitfox.com">Mark McKay</a>
 */
public class Path extends ShapeElement {

//    PathCommand[] commands = null;

    int fillRule = GeneralPath.WIND_NON_ZERO;
    String d = "";
//    ExtendedGeneralPath path;
    GeneralPath path;

    /** Creates a new instance of Rect */
    public Path() {
    }

    /*
    public void loaderStartElement(SVGLoaderHelper helper, Attributes attrs, SVGElement parent)
    {
		//Load style string
        super.loaderStartElement(helper, attrs, parent);

        StyleAttribute styleAttrib = getStyle("fill-rule");
        String fillRule = (styleAttrib == null) ? "nonzero" : styleAttrib.getStringValue();
        
        String d = attrs.getValue("d");
        path = buildPath(d, fillRule.equals("evenodd") ? GeneralPath.WIND_EVEN_ODD : GeneralPath.WIND_NON_ZERO);
    }
    */
    
    protected void build() throws SVGException
    {
        super.build();
        
        StyleAttribute sty = new StyleAttribute();
        
    
        String fillRuleStrn  = (getStyle(sty.setName("fill-rule"))) ? sty.getStringValue() : "nonzero";
        fillRule = fillRuleStrn.equals("evenodd") ? GeneralPath.WIND_EVEN_ODD : GeneralPath.WIND_NON_ZERO;
        
//        String d = "";
        if (getPres(sty.setName("d"))) d = sty.getStringValue();

//System.err.println(d);
        
        path = buildPath(d, fillRule);
        
//System.err.println(d);
    }
    
    public void render(Graphics2D g) throws SVGException
    {
        beginLayer(g);
        renderShape(g, path);
        finishLayer(g);
    }

    public Shape getShape()
    {
        return shapeToParent(path);
    }

    public Rectangle2D getBoundingBox() throws SVGException
    {
        return boundsToParent(includeStrokeInBounds(path.getBounds2D()));
    }

    /**
     * Updates all attributes in this diagram associated with a time event.
     * Ie, all attributes with track information.
     * @return - true if this node has changed state as a result of the time
     * update
     */
    public boolean updateTime(double curTime) throws SVGException
    {
//        if (trackManager.getNumTracks() == 0) return false;
        boolean changeState = super.updateTime(curTime);

        //Get current values for parameters
        StyleAttribute sty = new StyleAttribute();
        boolean shapeChange = false;
        
        if (getStyle(sty.setName("fill-rule")))
        {
            int newVal = sty.getStringValue().equals("evenodd") 
                ? GeneralPath.WIND_EVEN_ODD 
                : GeneralPath.WIND_NON_ZERO;
            if (newVal != fillRule)
            {
                fillRule = newVal;
                changeState = true;
            }
        }
        
        if (getPres(sty.setName("d")))
        {
            String newVal = sty.getStringValue();
            if (!newVal.equals(d))
            {
                d = newVal;
                shapeChange = true;
            }
        }
        
        if (shapeChange)
        {
            build();
//            path = buildPath(d, fillRule);
//            return true;
        }
        
        return changeState || shapeChange;
    }
}
