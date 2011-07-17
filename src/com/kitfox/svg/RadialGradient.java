/*
 * RadialGradient.java
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
 * Created on January 26, 2004, 1:55 AM
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
public class RadialGradient extends Gradient {

    float cx = 0.5f;
    float cy = 0.5f;
    float fx = 0.5f;
    float fy = 0.5f;
    float r = 0.5f;

    /** Creates a new instance of RadialGradient */
    public RadialGradient() {
    }
/*
    public void loaderStartElement(SVGLoaderHelper helper, Attributes attrs, SVGElement parent)
    {
		//Load style string
        super.loaderStartElement(helper, attrs, parent);

        String cx = attrs.getValue("cx");
        String cy = attrs.getValue("cy");
        String fx = attrs.getValue("fx");
        String fy = attrs.getValue("fy");
        String r = attrs.getValue("r");

        if (cx != null) this.cx = (float)XMLParseUtil.parseRatio(cx);
        if (cy != null) this.cy = (float)XMLParseUtil.parseRatio(cy);
        if (fx != null) this.fx = (float)XMLParseUtil.parseRatio(fx);
        if (fy != null) this.fy = (float)XMLParseUtil.parseRatio(fy);
        if (r != null) this.r = (float)XMLParseUtil.parseRatio(r);
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
        
        if (getPres(sty.setName("cx"))) cx = sty.getFloatValueWithUnits();
        
        if (getPres(sty.setName("cy"))) cy = sty.getFloatValueWithUnits();
        
        if (getPres(sty.setName("fx"))) fx = sty.getFloatValueWithUnits();
        
        if (getPres(sty.setName("fy"))) fy = sty.getFloatValueWithUnits();
        
        if (getPres(sty.setName("r"))) r = sty.getFloatValueWithUnits();
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

        com.kitfox.svg.batik.RadialGradientPaint paint;

        if (gradientUnits == GU_USER_SPACE_ON_USE)
        {
            paint = new com.kitfox.svg.batik.RadialGradientPaint(
                new Point2D.Float(cx, cy),
                r,
                new Point2D.Float(fx, fy),
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
            viewXform.scale(bounds.getWidth(), bounds.getHeight());

            viewXform.concatenate(gradientTransform);

            paint = new com.kitfox.svg.batik.RadialGradientPaint(
                new Point2D.Float(cx, cy),
                r,
                new Point2D.Float(fx, fy),
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
        
        if (getPres(sty.setName("fx")))
        {
            float newVal = sty.getFloatValueWithUnits();
            if (newVal != fx)
            {
                fx = newVal;
                shapeChange = true;
            }
        }
        
        if (getPres(sty.setName("fy")))
        {
            float newVal = sty.getFloatValueWithUnits();
            if (newVal != fy)
            {
                fy = newVal;
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
        
        return changeState;
    }
}
