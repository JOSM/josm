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
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;

/**
 * @author Mark McKay
 * @author <a href="mailto:mark@kitfox.com">Mark McKay</a>
 */
public class Circle extends ShapeElement 
{

    float cx = 0f;
    float cy = 0f;
    float r = 0f;


    Ellipse2D.Float circle = new Ellipse2D.Float();

    /** Creates a new instance of Rect */
    public Circle() {
    }
/*
    public void loaderStartElement(SVGLoaderHelper helper, Attributes attrs, SVGElement parent)
    {
		//Load style string
        super.loaderStartElement(helper, attrs, parent);

        String cx = attrs.getValue("cx");
        String cy = attrs.getValue("cy");
        String r = attrs.getValue("r");

        this.cx = XMLParseUtil.parseFloat(cx);
        this.cy = XMLParseUtil.parseFloat(cy);
        this.r = XMLParseUtil.parseFloat(r);

        build();
        
        //setBounds(this.cx - this.r, this.cy - this.r, this.r * 2.0, this.r * 2.0);
    }
*/
    /*
    public void loaderEndElement(SVGLoaderHelper helper)
    {
//        super.loaderEndElement(helper);

//        build();
    }
     */
    
    protected void build() throws SVGException
    {
        super.build();
        
        StyleAttribute sty = new StyleAttribute();
        
        if (getPres(sty.setName("cx"))) cx = sty.getFloatValueWithUnits();
        
        if (getPres(sty.setName("cy"))) cy = sty.getFloatValueWithUnits();
        
        if (getPres(sty.setName("r"))) r = sty.getFloatValueWithUnits();
        
        circle.setFrame(cx - r, cy - r, r * 2f, r * 2f);
    }

    public void render(Graphics2D g) throws SVGException
    {
        beginLayer(g);
        renderShape(g, circle);
        finishLayer(g);
    }

    public Shape getShape()
    {
        return shapeToParent(circle);
    }

    public Rectangle2D getBoundingBox() throws SVGException
    {
        return boundsToParent(includeStrokeInBounds(circle.getBounds2D()));
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
        
        if (getPres(sty.setName("cx")))
        {
            float newVal = sty.getFloatValueWithUnits();
            if (newVal != cx)
            {
                cx = newVal;
                shapeChange = true;
            }
        }
        
        if (getPres(sty.setName("cy")))
        {
            float newVal = sty.getFloatValueWithUnits();
            if (newVal != cy)
            {
                cy = newVal;
                shapeChange = true;
            }
        }
        
        if (getPres(sty.setName("r")))
        {
            float newVal = sty.getFloatValueWithUnits();
            if (newVal != r)
            {
                r = newVal;
                shapeChange = true;
            }
        }
        
        if (shapeChange)
        {
            build();
//            circle.setFrame(cx - r, cy - r, r * 2f, r * 2f);
//            return true;
        }
        
        return changeState || shapeChange;
    }
    
}



