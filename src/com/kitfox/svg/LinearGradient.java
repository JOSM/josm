/*
 * LinearGradient.java
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
 * Created on January 26, 2004, 1:54 AM
 */

package com.kitfox.svg;

import com.kitfox.svg.xml.StyleAttribute;
import java.awt.geom.*;
import java.awt.*;

import com.kitfox.svg.xml.*;
import org.xml.sax.*;

//import org.apache.batik.ext.awt.*;
import com.kitfox.svg.batik.*;


/**
 * @author Mark McKay
 * @author <a href="mailto:mark@kitfox.com">Mark McKay</a>
 */
public class LinearGradient extends Gradient {

    float x1 = 0f;
    float y1 = 0f;
    float x2 = 1f;
    float y2 = 0f;

    /** Creates a new instance of LinearGradient */
    public LinearGradient() {
    }
/*
    public void loaderStartElement(SVGLoaderHelper helper, Attributes attrs, SVGElement parent)
    {
		//Load style string
        super.loaderStartElement(helper, attrs, parent);

        String x1 = attrs.getValue("x1");
        String x2 = attrs.getValue("x2");
        String y1 = attrs.getValue("y1");
        String y2 = attrs.getValue("y2");

        if (x1 != null) this.x1 = (float)XMLParseUtil.parseRatio(x1);
        if (y1 != null) this.y1 = (float)XMLParseUtil.parseRatio(y1);
        if (x2 != null) this.x2 = (float)XMLParseUtil.parseRatio(x2);
        if (y2 != null) this.y2 = (float)XMLParseUtil.parseRatio(y2);
    }
*/
    /*
    public void loaderEndElement(SVGLoaderHelper helper)
    {
        super.loaderEndElement(helper);
        
        build();
    }
    */
    
    protected void build() throws SVGException
    {
        super.build();
        
        StyleAttribute sty = new StyleAttribute();
        
        if (getPres(sty.setName("x1"))) x1 = sty.getFloatValueWithUnits();
        
        if (getPres(sty.setName("y1"))) y1 = sty.getFloatValueWithUnits();
        
        if (getPres(sty.setName("x2"))) x2 = sty.getFloatValueWithUnits();
        
        if (getPres(sty.setName("y2"))) y2 = sty.getFloatValueWithUnits();
    }
    
    public Paint getPaint(Rectangle2D bounds, AffineTransform xform)
    {
        com.kitfox.svg.batik.MultipleGradientPaint.CycleMethodEnum method;
        switch (spreadMethod)
        {
            default:
            case SM_PAD:
                method = com.kitfox.svg.batik.MultipleGradientPaint.NO_CYCLE;
                break;
            case SM_REPEAT:
                method = com.kitfox.svg.batik.MultipleGradientPaint.REPEAT;
                break;
            case SM_REFLECT:
                method = com.kitfox.svg.batik.MultipleGradientPaint.REFLECT;
                break;
        }

        com.kitfox.svg.batik.LinearGradientPaint paint;
        if (gradientUnits == GU_USER_SPACE_ON_USE)
        {
//            paint = new LinearGradientPaint(x1, y1, x2, y2, getStopFractions(), getStopColors(), method);
            paint = new com.kitfox.svg.batik.LinearGradientPaint(
                new Point2D.Float(x1, y1),
                new Point2D.Float(x2, y2),
                getStopFractions(),
                getStopColors(),
                method,
                com.kitfox.svg.batik.MultipleGradientPaint.SRGB,
                gradientTransform);
        }
        else
        {
            AffineTransform viewXform = new AffineTransform();
            viewXform.translate(bounds.getX(), bounds.getY());
            
            //This is a hack to get around shapes that have a width or height of 0.  Should be close enough to the true answer.
            double width = bounds.getWidth();
            double height = bounds.getHeight();
            if (width == 0) width = 1;
            if (height == 0) height = 1;
            viewXform.scale(width, height);

            viewXform.concatenate(gradientTransform);

            paint = new com.kitfox.svg.batik.LinearGradientPaint(
                new Point2D.Float(x1, y1),
                new Point2D.Float(x2, y2),
                getStopFractions(),
                getStopColors(),
                method,
                com.kitfox.svg.batik.MultipleGradientPaint.SRGB,
                viewXform);
        }

        return paint;
    }
    
    /**
     * Updates all attributes in this diagram associated with a time event.
     * Ie, all attributes with track information.
     * @return - true if this node has changed state as a result of the time
     * update
     */
    public boolean updateTime(double curTime) throws SVGException
    {
//        if (trackManager.getNumTracks() == 0) return stopChange;
        boolean changeState = super.updateTime(curTime);

        //Get current values for parameters
        StyleAttribute sty = new StyleAttribute();
        boolean shapeChange = false;
        
        if (getPres(sty.setName("x1")))
        {
            float newVal = sty.getFloatValueWithUnits();
            if (newVal != x1)
            {
                x1 = newVal;
                shapeChange = true;
            }
        }

        if (getPres(sty.setName("y1")))
        {
            float newVal = sty.getFloatValueWithUnits();
            if (newVal != y1)
            {
                y1 = newVal;
                shapeChange = true;
            }
        }

        if (getPres(sty.setName("x2")))
        {
            float newVal = sty.getFloatValueWithUnits();
            if (newVal != x2)
            {
                x2 = newVal;
                shapeChange = true;
            }
        }

        if (getPres(sty.setName("y2")))
        {
            float newVal = sty.getFloatValueWithUnits();
            if (newVal != y2)
            {
                y2 = newVal;
                shapeChange = true;
            }
        }

        return changeState || shapeChange;
    }
}
