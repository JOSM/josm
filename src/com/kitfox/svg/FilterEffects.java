/*
 * FillElement.java
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
 * Created on March 18, 2004, 6:52 AM
 */

package com.kitfox.svg;

import com.kitfox.svg.xml.StyleAttribute;
import java.awt.*;
import java.awt.geom.*;
import java.net.*;
import java.util.*;

import com.kitfox.svg.xml.*;
import org.xml.sax.*;

/**
 * @author Mark McKay
 * @author <a href="mailto:mark@kitfox.com">Mark McKay</a>
 */
public class FilterEffects extends SVGElement 
{
    public static final int FP_SOURCE_GRAPHIC = 0;
    public static final int FP_SOURCE_ALPHA = 1;
    public static final int FP_BACKGROUND_IMAGE = 2;
    public static final int FP_BACKGROUND_ALPHA = 3;
    public static final int FP_FILL_PAINT = 4;
    public static final int FP_STROKE_PAINT = 5;
    public static final int FP_CUSTOM = 5;

    private int filterPrimitiveTypeIn;
    private String filterPrimitiveRefIn;
    
    
    float x = 0f;
    float y = 0f;
    float width = 1f;
    float height = 1f;
    
    String result = "defaultFilterName";
    
    
    
    URL href = null;


    /** Creates a new instance of FillElement */
    public FilterEffects() {
    }

    /**
     * Called after the start element but before the end element to indicate
     * each child tag that has been processed
     */
    public void loaderAddChild(SVGLoaderHelper helper, SVGElement child) throws SVGElementException
    {
        super.loaderAddChild(helper, child);

        if (child instanceof FilterEffects)
        {
//            filterEffects.add(child);
        }
    }
    
    protected void build() throws SVGException
    {
        super.build();
        
        StyleAttribute sty = new StyleAttribute();
        String strn;
        /*
        if (getPres(sty.setName("filterUnits")))
        {
            strn = sty.getStringValue().toLowerCase();
            if (strn.equals("userspaceonuse")) filterUnits = FU_USER_SPACE_ON_USE;
            else filterUnits = FU_OBJECT_BOUNDING_BOX;
        }

        if (getPres(sty.setName("primitiveUnits")))
        {
            strn = sty.getStringValue().toLowerCase();
            if (strn.equals("userspaceonuse")) primitiveUnits = PU_USER_SPACE_ON_USE;
            else primitiveUnits = PU_OBJECT_BOUNDING_BOX;
        }

        if (getPres(sty.setName("x"))) x = sty.getFloatValue();

        if (getPres(sty.setName("y"))) y = sty.getFloatValue();

        if (getPres(sty.setName("width"))) width = sty.getFloatValue();

        if (getPres(sty.setName("height"))) height = sty.getFloatValue();

        try {
            if (getPres(sty.setName("xlink:href")))
            {
                URI src = sty.getURIValue(getXMLBase());
                href = src.toURL();
            }
        }
        catch (Exception e)
        {
            throw new SVGException(e);
        }
*/
    }

    public float getX() { return x; }
    public float getY() { return y; }
    public float getWidth() { return width; }
    public float getHeight() { return height; }
    
    public boolean updateTime(double curTime) throws SVGException
    {
//        if (trackManager.getNumTracks() == 0) return false;

        //Get current values for parameters
        StyleAttribute sty = new StyleAttribute();
        boolean stateChange = false;
        
        if (getPres(sty.setName("x")))
        {
            float newVal = sty.getFloatValueWithUnits();
            if (newVal != x)
            {
                x = newVal;
                stateChange = true;
            }
        }
        
        if (getPres(sty.setName("y")))
        {
            float newVal = sty.getFloatValueWithUnits();
            if (newVal != y)
            {
                y = newVal;
                stateChange = true;
            }
        }
        
        if (getPres(sty.setName("width")))
        {
            float newVal = sty.getFloatValueWithUnits();
            if (newVal != width)
            {
                width = newVal;
                stateChange = true;
            }
        }
        
        if (getPres(sty.setName("height")))
        {
            float newVal = sty.getFloatValueWithUnits();
            if (newVal != height)
            {
                height = newVal;
                stateChange = true;
            }
        }
        
        try {
            if (getPres(sty.setName("xlink:href")))
            {
                URI src = sty.getURIValue(getXMLBase());
                URL newVal = src.toURL();
                
                if (!newVal.equals(href))
                {
                    href = newVal;
                    stateChange = true;
                }
            }
        }
        catch (Exception e)
        {
            throw new SVGException(e);
        }

        /*
        if (getPres(sty.setName("filterUnits")))
        {
            int newVal;
            String strn = sty.getStringValue().toLowerCase();
            if (strn.equals("userspaceonuse")) newVal = FU_USER_SPACE_ON_USE;
            else newVal = FU_OBJECT_BOUNDING_BOX;
            if (newVal != filterUnits)
            {
                filterUnits = newVal;
                stateChange = true;
            }
        }

        if (getPres(sty.setName("primitiveUnits")))
        {
            int newVal;
            String strn = sty.getStringValue().toLowerCase();
            if (strn.equals("userspaceonuse")) newVal = PU_USER_SPACE_ON_USE;
            else newVal = PU_OBJECT_BOUNDING_BOX;
            if (newVal != filterUnits)
            {
                primitiveUnits = newVal;
                stateChange = true;
            }
        }

        */
        
        return stateChange;
    }
}

